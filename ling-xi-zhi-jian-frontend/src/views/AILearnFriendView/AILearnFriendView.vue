<script setup lang="ts">
/**
 * AI 学习伴侣页面 - 高级设计
 */
import { computed, nextTick, onMounted, ref, useTemplateRef, watch } from 'vue';
import Header from '@/views/Header/Header.vue';
import Button from '@/components/通用/Button/Button.vue';
import { useAiNoteStore } from '@/stores/aiLearnFriendStore';
import VideoUpload from '@/views/AILearnFriendView/VideoUpload.vue';
import { JSONUtil } from '@/utils/JSONUtil';
import Checkbox from '@/components/数据录入/Checkbox.vue';
import { type ECharts, init, type EChartsOption } from 'echarts';
import 'echarts-wordcloud';
import { useAiChatStore } from '@/stores/aiChatStore';
import InputText from '@/components/数据录入/input/input/InputText.vue';
import { marked } from 'marked';
import Modal from '@/components/反馈/Modal/Modal.vue';
import MessageUtil from '@/utils/MessageUtil';
import { copyTextToClipboard } from '@/utils/copyTextUtil';
import RichTextEditor from '@/components/数据录入/RichTextEditor/RichTextEditor.vue';
import { ElSwitch } from 'element-plus';
import 'element-plus/dist/index.css';

const aiNoteStore = useAiNoteStore();
const aiLearnFriendRef = useTemplateRef<HTMLElement>('ai-learn-friend');

const collapse = ref(false);
const canMultipleDelete = ref(false);
const polishedMarkdown = ref('');

const getAiSummaryList = computed(() =>
    JSONUtil.parse<
        {
            Headline: string;
            Summary: string;
        }[]
    >(aiNoteStore.computedCurrentNote?.autoChapters),
);

const getKeyWordList = computed(
    () =>
        JSONUtil.parse<{
            Keywords: string[];
        }>(aiNoteStore.computedCurrentNote?.meetingAssistance)?.Keywords,
);

const aiChatStore = useAiChatStore();
const isPolishing = ref(false);

onMounted(() => {
    aiNoteStore.getHistoryList();
});

const openMultipleDelete = () => {
    if (canMultipleDelete.value) {
        const indexList: number[] = [];
        let i = 0;
        for (const note of aiNoteStore.historyList) {
            if (note._isSelected) {
                indexList.push(i);
            }
            i++;
        }
        aiNoteStore.remove(indexList);
    } else {
        aiNoteStore.historyList.forEach((note) => (note._isSelected = false));
    }
    canMultipleDelete.value = !canMultipleDelete.value;
};

const closeMultipleDelete = () => {
    canMultipleDelete.value = false;
    aiNoteStore.historyList.forEach((note) => (note._isSelected = false));
};

const polish = () => {
    if (isPolishing.value) {
        MessageUtil.info('正在润色中，请稍等。。。');
        return;
    }
    if (!inputHtml.value) {
        MessageUtil.warning('请先添加你的笔记');
        return;
    }
    polishedMarkdown.value = '';
    isPolishing.value = true;
    aiNoteStore
        .notePolishing(inputHtml.value)
        .then((markdown) => {
            polishedMarkdown.value = markdown;
        })
        .finally(() => {
            isPolishing.value = false;
        });
};

const inputHtml = ref('');

watch(
    () => aiNoteStore.computedCurrentNote?.personalNote,
    (newVal) => {
        inputHtml.value = newVal!;
    },
);

const isFinishedAnalysis = computed(() => {
    return aiNoteStore.selectedIndex != null;
});

type TopicNode = {
    Title: string;
    Topic: TopicNode[];
};

type EchartsTreeMode = {
    name: string;
    children: EchartsTreeMode[] | [];
};

let chart: ECharts | null = null;
let keywordChart: ECharts | null = null;

const showAIMindMap = (data: TopicNode, isTree: boolean) => {
    chart?.dispose();
    chart = init(
        aiLearnFriendRef.value?.querySelector('.mind-mapping-chart-box .chart'),
    );
    let option: EChartsOption | null;
    if (isTree) {
        const convertData = (data: TopicNode): EchartsTreeMode => {
            return {
                name: data.Title,
                children:
                    data.Topic.length > 0
                        ? data.Topic.map((item: TopicNode) => convertData(item))
                        : [],
            };
        };
        option = {
            backgroundColor: 'transparent',
            tooltip: {
                trigger: 'item',
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                borderColor: '#91d5ff',
                borderWidth: 1,
                borderRadius: 8,
                padding: [10, 14],
                textStyle: { color: '#333', fontSize: 13 },
            },
            series: [
                {
                    type: 'tree',
                    data: [convertData(data)],
                    layout: 'orthogonal',
                    orient: 'LR',
                    symbolSize: [70, 30],
                    initialTreeDepth: 2,
                    path: 'path',
                    nodePadding: 60,
                    symbol: 'roundRect',
                    itemStyle: {
                        borderRadius: 8,
                        borderColor: '#91d5ff',
                        borderWidth: 2,
                        color: '#e6f7ff',
                        shadowBlur: 15,
                        shadowColor: 'rgba(24, 144, 255, 0.3)',
                    },
                    lineStyle: {
                        width: 2,
                        curveness: 0.5,
                        color: '#69c0ff',
                    },
                    label: {
                        show: true,
                        position: 'inside',
                        color: '#333',
                        fontSize: 12,
                        fontWeight: 500,
                    },
                    emphasis: {
                        focus: 'ancestor',
                        itemStyle: {
                            borderColor: '#ff7a45',
                            borderWidth: 2,
                            shadowBlur: 15,
                            shadowColor: 'rgba(255, 122, 69, 0.4)',
                        },
                        lineStyle: {
                            color: '#ff7a45',
                            width: 2,
                        },
                    },
                    expandAndCollapse: true,
                    animationDuration: 500,
                    animationDurationUpdate: 300,
                    zoom: 1,
                    draggable: true,
                    roam: true,
                },
            ],
        };
    } else {
        option = {
            backgroundColor: 'transparent',
            tooltip: {
                trigger: 'item',
                formatter: '{b}',
            },
            series: [
                {
                    type: 'graph',
                    layout: 'force',
                    data: [{ name: data.Title, symbolSize: 40 }],
                    links: [],
                    roam: true,
                    label: {
                        show: true,
                        position: 'right',
                    },
                    force: {
                        repulsion: 200,
                        edgeLength: [80, 100],
                    },
                    itemStyle: {
                        normal: {
                            borderType: 'solid',
                            color: '#e6f7ff',
                            borderColor: '#91d5ff',
                            borderWidth: 2,
                        },
                    },
                    zoom: 1,
                    symbol: 'circle',
                    draggable: true,
                },
            ],
        };
    }

    chart.setOption(option as echarts.EChartsOption);
    window.addEventListener('resize', () => {
        chart?.resize();
    });
};

const showKeywordCloud = (keywords: string[]) => {
    keywordChart?.dispose();
    keywordChart = init(
        aiLearnFriendRef.value?.querySelector('.keyword-cloud-chart-box .chart'),
    );
    const colors = ['#1890ff', '#69c0ff', '#36cfc9', '#73d13d', '#ffa940', '#ff7a45'];
    const option: EChartsOption = {
        backgroundColor: 'transparent',
        tooltip: {
            show: true,
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            borderColor: '#91d5ff',
            borderWidth: 1,
            borderRadius: 8,
            padding: [10, 14],
            textStyle: { color: '#333', fontSize: 13 },
            formatter: (params: any) => `${params.name}`,
        },
        series: [
            {
                type: 'wordCloud',
                shape: 'circle',
                left: 'center',
                top: 'center',
                width: '100%',
                height: '100%',
                sizeRange: [16, 32],
                rotationRange: [0, 0],
                gridSize: 16,
                drawOutOfBound: false,
                layoutAnimation: true,
                textStyle: {
                    fontFamily: 'sans-serif',
                    fontWeight: 'bold',
                    color: function (params: any) {
                        return colors[params.dataIndex % colors.length];
                    },
                },
                emphasis: {
                    textStyle: {
                        shadowBlur: 8,
                        shadowColor: 'rgba(24, 144, 255, 0.3)',
                    },
                },
                data: keywords.map((name, index) => ({
                    name,
                    value: 100 - index * 5,
                })),
            },
        ],
    };
    keywordChart.setOption(option);
    window.addEventListener('resize', () => {
        keywordChart?.resize();
    });
};

const getMindMapList = computed(
    () =>
        JSONUtil.parse<{
            MindMapSummary: TopicNode[];
        }>(aiNoteStore.computedCurrentNote?.summarization)?.MindMapSummary ??
        null,
);

watch(getMindMapList, (newVal: TopicNode[] | null) => {
    nextTick(() => {
        newVal && showAIMindMap(newVal[0], isTree.value);
    });
});

watch(getKeyWordList, (newVal: string[] | null) => {
    nextTick(() => {
        newVal && newVal.length > 0 && showKeywordCloud(newVal);
    });
});

const isTree = ref(true);
watch(isTree, () => {
    showAIMindMap(getMindMapList.value?.[0] as TopicNode, isTree.value);
});

const copyText = () => {
    const polishingResult = aiLearnFriendRef.value!.querySelector(
        '.polishing-result',
    ) as HTMLElement;
    if (copyTextToClipboard(polishingResult.innerText)) {
        MessageUtil.success('复制成功');
    } else {
        MessageUtil.error('复制失败');
    }
};

const apply = (markdown: string) => {
    inputHtml.value = marked.parse(markdown) as string;
};

watch(
    () => aiNoteStore.selectedIndex,
    () => {
        aiChatStore.clearMessageList();
        polishedMarkdown.value = '';
        newTitle.value = aiNoteStore.computedCurrentNote?.title;
    },
);

let editingNoteIndex = 0;
const isShowModal = ref(false);

const onModalOk = () => {
    aiNoteStore
        .editHistory(
            newTitle.value,
            '',
            aiNoteStore.historyList[editingNoteIndex].id,
        )
        .then(() => {
            isShowModal.value = false;
            newTitle.value = '';
        });
};

const showModal = (index: number) => {
    editingNoteIndex = index;
    newTitle.value = aiNoteStore.historyList[index].title;
    isShowModal.value = true;
};

const newTitle = ref('');
const isFullScreen = ref(false);
watch(isFullScreen, () => {
    const personalNoteBox = aiLearnFriendRef.value?.querySelector(
        '.personal-note-box',
    ) as HTMLElement;
    if (document.fullscreenElement) {
        document.exitFullscreen().then();
    } else {
        personalNoteBox?.requestFullscreen().then();
    }
});
const isShowDeleteModal = ref(false);
let willDeleteNoteIndex = 0;
const showDeleteModal = (index: number) => {
    willDeleteNoteIndex = index;
    isShowDeleteModal.value = true;
};
const deleteNote = () => {
    isShowDeleteModal.value = false;
    aiNoteStore.remove([willDeleteNoteIndex]);
};
</script>

<template>
    <div class="ai-learn-friend" ref="ai-learn-friend">
        <Header />
        <Modal v-model:open="isShowModal" title="修改标题" @ok="onModalOk">
            <InputText v-model="newTitle" />
        </Modal>

        <Modal v-model:open="isShowDeleteModal" @ok="deleteNote">
            确认删除？
        </Modal>

        <div class="main">
            <div class="sidebar" :class="{ collapse }">
                <div class="sidebar-decoration">
                    <div class="decoration-circle circle-1"></div>
                    <div class="decoration-circle circle-2"></div>
                </div>

                <div class="sidebar-header">
                    <div class="header-icon">
                        <span class="iconfont icon-Ai"></span>
                    </div>
                    <div class="header-text">
                        <h3>AI学习</h3>
                        <p>智能学习伴侣</p>
                    </div>
                </div>

                <div class="create-btn" @click="aiNoteStore.createNote()">
                    <span class="iconfont icon-tianjia"></span>
                    <span>新建任务</span>
                </div>

                <div class="task-list">
                    <div
                        v-for="(note, index) in aiNoteStore.historyList"
                        :key="index"
                        class="task-item"
                        :class="{ active: aiNoteStore.selectedIndex == index }"
                        @click="aiNoteStore.selectedIndex = index"
                    >
                        <div class="task-checkbox" v-show="canMultipleDelete">
                            <Checkbox v-model="note._isSelected" @click.stop />
                        </div>
                        <div class="task-info">
                            <span class="task-title">{{ note.title }}</span>
                            <span class="task-status" :class="note.taskStatus">
                                {{ note.taskStatus === '进行中' ? '分析中' : note.taskStatus === '已完成' ? '已完成' : '失败' }}
                            </span>
                        </div>
                        <a-dropdown :trigger="['click']" placement="bottomRight">
                            <span class="task-more iconfont icon-more" @click.stop></span>
                            <template #overlay>
                                <a-menu>
                                    <a-menu-item @click="showModal(index)">编辑</a-menu-item>
                                    <a-menu-item @click="showDeleteModal(index)">删除</a-menu-item>
                                </a-menu>
                            </template>
                        </a-dropdown>
                    </div>
                </div>

                <div class="sidebar-footer">
                    <div class="batch-actions" v-if="canMultipleDelete">
                        <button class="batch-btn delete" @click="openMultipleDelete">
                            <span class="iconfont icon-delete"></span>
                            确认删除
                        </button>
                        <button class="batch-btn cancel" @click="closeMultipleDelete">取消</button>
                    </div>
                    <button v-else class="multi-delete-btn" @click="openMultipleDelete">
                        <span class="iconfont icon-delete"></span>
                        批量删除
                    </button>
                </div>
            </div>

            <div class="content">
                <div class="content-header">
                    <div class="collapse-btn" @click="collapse = !collapse">
                        <span class="iconfont" :class="collapse ? 'icon-zuo' : 'icon-you'"></span>
                    </div>
                    <div class="header-center" v-if="aiNoteStore.selectedIndex != null">
                        <div class="task-title">{{ aiNoteStore.historyList[aiNoteStore.selectedIndex!]?.title }}</div>
                        <a-dropdown :trigger="['click']" placement="bottomRight">
                            <span class="iconfont icon-xia dropdown-icon"></span>
                            <template #overlay>
                                <a-menu>
                                    <a-menu-item @click="showModal(aiNoteStore.selectedIndex!)">编辑标题</a-menu-item>
                                </a-menu>
                            </template>
                        </a-dropdown>
                    </div>
                </div>

                <div class="content-body" v-if="!isFinishedAnalysis">
                    <div class="upload-zone">
                        <div class="upload-icon">
                            <span class="iconfont icon-shipinshangchuan"></span>
                        </div>
                        <div class="upload-text">
                            <h3>上传训练视频</h3>
                            <p>AI将分析你的动作并提供学习建议</p>
                        </div>
                        <div class="upload-component">
                            <VideoUpload />
                        </div>
                    </div>
                </div>

                <div class="content-body has-data" v-else>
                    <div class="video-section">
                        <video
                            controls
                            autoplay
                            class="main-video"
                            :src="aiNoteStore.historyList[aiNoteStore.selectedIndex!]?.videoUrl"
                        />
                    </div>

                    <div class="info-grid">
                        <div class="info-card summary-card">
                            <div class="card-header">
                                <span class="iconfont icon-Ai"></span>
                                <span>AI总结</span>
                            </div>
                            <div class="card-content">
                                <a-skeleton active :loading="aiNoteStore.historyList[aiNoteStore.selectedIndex!]?.taskStatus == '进行中'">
                                    <template v-if="getAiSummaryList">
                                        <div
                                            v-for="(chapter, index) in getAiSummaryList"
                                            :key="index"
                                            class="summary-item"
                                        >
                                            <div class="summary-num">{{ index + 1 }}</div>
                                            <div class="summary-body">
                                                <h4>{{ chapter?.Headline }}</h4>
                                                <p>{{ chapter?.Summary }}</p>
                                            </div>
                                        </div>
                                    </template>
                                    <div v-else class="empty">暂无总结数据</div>
                                </a-skeleton>
                            </div>
                        </div>

                        <div class="info-card keywords-card">
                            <div class="card-header">
                                <span class="iconfont icon-tianjia"></span>
                                <span>关键词</span>
                            </div>
                            <div class="card-content">
                                <a-skeleton active :loading="aiNoteStore.historyList[aiNoteStore.selectedIndex!]?.taskStatus == '进行中'">
                                    <template v-if="getKeyWordList && getKeyWordList.length">
                                        <div class="keyword-cloud-chart-box">
                                            <div class="chart"></div>
                                        </div>
                                    </template>
                                    <div v-else class="empty">暂无关键词</div>
                                </a-skeleton>
                            </div>
                        </div>

                        <div class="info-card mindmap-card">
                            <div class="card-header">
                                <div class="header-left">
                                    <span class="iconfont icon-book-open"></span>
                                    <span>思维导图</span>
                                </div>
                                <ElSwitch
                                    v-model="isTree"
                                    inline-prompt
                                    size="small"
                                    active-text="导图"
                                    inactive-text="图谱"
                                    style="--el-switch-off-color: #69c0ff"
                                />
                            </div>
                            <div class="card-content">
                                <a-skeleton active :loading="aiNoteStore.historyList[aiNoteStore.selectedIndex!]?.taskStatus == '进行中'">
                                    <div v-if="getMindMapList && getMindMapList.length" class="mindmap-container">
                                        <div class="mind-mapping-chart-box">
                                            <div class="chart"></div>
                                        </div>
                                    </div>
                                    <div v-else class="empty">暂无思维导图</div>
                                </a-skeleton>
                            </div>
                        </div>



                        <div class="info-card notes-card">
                            <div class="card-header">
                                <div class="header-left">
                                    <span class="iconfont icon-bianji"></span>
                                    <span>个人笔记</span>
                                </div>
                                <div class="header-actions">
                                    <span
                                        class="iconfont"
                                        :class="isFullScreen ? 'icon-compress' : 'icon-quanping'"
                                        @click="isFullScreen = !isFullScreen"
                                    ></span>
                                    <div class="polish-btn" @click="polish">
                                        <span class="iconfont icon-Ai"></span>
                                        <span>AI润色</span>
                                        <img v-show="isPolishing" class="loading-img" src="@/assets/ai_learn_friend_view/loading.gif" alt="" />
                                    </div>
                                </div>
                            </div>
                            <div class="card-content">
                                <div class="polish-result" v-show="polishedMarkdown.length > 0">
                                    <div class="polish-header">
                                        <span>润色结果</span>
                                        <div class="polish-actions">
                                            <button @click="apply(polishedMarkdown)">应用</button>
                                            <button @click="copyText">复制</button>
                                        </div>
                                    </div>
                                    <div class="polish-content" v-html="marked.parse(polishedMarkdown)"></div>
                                </div>
                                <RichTextEditor
                                    @blur="aiNoteStore.saveNote(inputHtml)"
                                    v-model:html="inputHtml"
                                    :disabled="!isFinishedAnalysis"
                                    :placeholder="isFinishedAnalysis ? '记录你的学习心得' : '上传视频后开始记录'"
                                />
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
.ai-learn-friend {
    display: flex;
    flex-direction: column;
    height: 100vh;
    overflow: hidden;
    background: linear-gradient(135deg, #f0f7ff 0%, #e6f0ff 100%);

    .main {
        flex: 1;
        display: flex;
        overflow: hidden;

        .sidebar {
            width: 300px;
            background: rgba(255, 255, 255, 0.9);
            backdrop-filter: blur(20px);
            border-right: 1px solid rgba(24, 144, 255, 0.1);
            display: flex;
            flex-direction: column;
            position: relative;
            overflow: hidden;
            transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
            box-shadow: 4px 0 24px rgba(24, 144, 255, 0.08);

            &.collapse {
                width: 0;
            }

            .sidebar-decoration {
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                height: 180px;
                pointer-events: none;

                .decoration-circle {
                    position: absolute;
                    border-radius: 50%;
                    background: linear-gradient(135deg, rgba(24, 144, 255, 0.15), rgba(105, 192, 255, 0.08));

                    &.circle-1 {
                        width: 160px;
                        height: 160px;
                        top: -50px;
                        right: -50px;
                    }

                    &.circle-2 {
                        width: 100px;
                        height: 100px;
                        top: 70px;
                        left: -30px;
                    }
                }
            }

            .sidebar-header {
                display: flex;
                align-items: center;
                gap: 12px;
                padding: 24px 20px;
                position: relative;
                z-index: 1;

                .header-icon {
                    width: 44px;
                    height: 44px;
                    border-radius: 12px;
                    background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    box-shadow: 0 4px 16px rgba(24, 144, 255, 0.35);

                    .iconfont {
                        font-size: 22px;
                        color: #fff;
                    }
                }

                .header-text {
                    h3 {
                        margin: 0;
                        font-size: 17px;
                        font-weight: 700;
                        color: #333;
                    }

                    p {
                        margin: 2px 0 0 0;
                        font-size: 11px;
                        color: #999;
                    }
                }
            }

            .create-btn {
                display: flex;
                align-items: center;
                justify-content: center;
                gap: 8px;
                margin: 0 16px 16px;
                padding: 12px;
                background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                border-radius: 12px;
                cursor: pointer;
                transition: all 0.3s;
                box-shadow: 0 4px 16px rgba(24, 144, 255, 0.3);

                .iconfont {
                    font-size: 18px;
                    color: #fff;
                }

                span:last-child {
                    font-size: 14px;
                    font-weight: 600;
                    color: #fff;
                }

                &:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 6px 20px rgba(24, 144, 255, 0.4);
                }
            }

            .task-list {
                flex: 1;
                overflow-y: auto;
                padding: 0 12px;
                position: relative;
                z-index: 1;

                &::-webkit-scrollbar {
                    width: 4px;
                }

                &::-webkit-scrollbar-thumb {
                    background: rgba(24, 144, 255, 0.2);
                    border-radius: 2px;
                }

                .task-item {
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    padding: 12px 14px;
                    border-radius: 12px;
                    cursor: pointer;
                    transition: all 0.25s;
                    margin-bottom: 6px;
                    border: 1px solid transparent;

                    &:hover {
                        background: rgba(24, 144, 255, 0.06);

                        .task-more {
                            opacity: 1;
                        }
                    }

                    &.active {
                        background: rgba(24, 144, 255, 0.12);
                        border-color: rgba(24, 144, 255, 0.2);
                    }

                    .task-checkbox {
                        flex-shrink: 0;
                    }

                    .task-info {
                        flex: 1;
                        min-width: 0;
                        display: flex;
                        flex-direction: column;
                        gap: 4px;

                        .task-title {
                            font-size: 13px;
                            color: #333;
                            font-weight: 500;
                            overflow: hidden;
                            text-overflow: ellipsis;
                            white-space: nowrap;
                        }

                        .task-status {
                            font-size: 10px;
                            padding: 2px 8px;
                            border-radius: 8px;
                            width: fit-content;

                            &.进行中 {
                                background: rgba(24, 144, 255, 0.1);
                                color: #1890ff;
                            }

                            &.已完成 {
                                background: rgba(82, 196, 26, 0.1);
                                color: #52c41a;
                            }

                            &.失败 {
                                background: rgba(255, 78, 78, 0.1);
                                color: #ff4d4f;
                            }
                        }
                    }

                    .task-more {
                        font-size: 18px;
                        color: #999;
                        opacity: 0;
                        transition: opacity 0.2s;
                        cursor: pointer;
                        padding: 4px;

                        &:hover {
                            color: #1890ff;
                        }
                    }
                }
            }

            .sidebar-footer {
                padding: 16px;
                position: relative;
                z-index: 1;

                .multi-delete-btn {
                    width: 100%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 8px;
                    padding: 10px;
                    background: rgba(255, 78, 78, 0.1);
                    border: 1px solid rgba(255, 78, 78, 0.2);
                    border-radius: 10px;
                    cursor: pointer;
                    transition: all 0.3s;
                    color: #ff4d4f;
                    font-size: 13px;

                    .iconfont {
                        font-size: 16px;
                    }

                    &:hover {
                        background: rgba(255, 78, 78, 0.15);
                    }
                }

                .batch-actions {
                    display: flex;
                    gap: 10px;

                    .batch-btn {
                        flex: 1;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        gap: 6px;
                        padding: 10px;
                        border-radius: 10px;
                        cursor: pointer;
                        font-size: 13px;
                        transition: all 0.3s;

                        &.delete {
                            background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
                            color: #fff;
                            border: none;
                        }

                        &.cancel {
                            background: #f5f5f5;
                            color: #666;
                            border: 1px solid #e8e8e8;
                        }
                    }
                }
            }
        }

        .content {
            flex: 1;
            display: flex;
            flex-direction: column;
            overflow: hidden;

            .content-header {
                display: flex;
                align-items: center;
                gap: 16px;
                padding: 16px 24px;
                background: rgba(255, 255, 255, 0.9);
                backdrop-filter: blur(10px);
                border-bottom: 1px solid rgba(24, 144, 255, 0.1);

                .collapse-btn {
                    width: 36px;
                    height: 36px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                    border-radius: 10px;
                    cursor: pointer;
                    transition: all 0.3s;
                    box-shadow: 0 2px 10px rgba(24, 144, 255, 0.3);

                    .iconfont {
                        font-size: 16px;
                        color: #fff;
                    }

                    &:hover {
                        transform: scale(1.05);
                    }
                }

                .header-center {
                    display: flex;
                    align-items: center;
                    gap: 8px;

                    .task-title {
                        font-size: 16px;
                        font-weight: 600;
                        color: #333;
                    }

                    .dropdown-icon {
                        font-size: 14px;
                        color: #999;
                        cursor: pointer;

                        &:hover {
                            color: #1890ff;
                        }
                    }
                }
            }

            .content-body {
                flex: 1;
                overflow-y: auto;
                padding: 24px;

                &::-webkit-scrollbar {
                    width: 6px;
                }

                &::-webkit-scrollbar-thumb {
                    background: rgba(24, 144, 255, 0.2);
                    border-radius: 3px;
                }
            }

            .upload-zone {
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                min-height: 60vh;
                background: rgba(255, 255, 255, 0.9);
                border-radius: 20px;
                padding: 48px;
                border: 2px dashed rgba(24, 144, 255, 0.3);
                transition: all 0.3s;

                &:hover {
                    border-color: #1890ff;
                    background: rgba(255, 255, 255, 0.95);
                }

                .upload-icon {
                    width: 80px;
                    height: 80px;
                    border-radius: 50%;
                    background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    margin-bottom: 24px;
                    box-shadow: 0 8px 30px rgba(24, 144, 255, 0.4);

                    .iconfont {
                        font-size: 36px;
                        color: #fff;
                    }
                }

                .upload-text {
                    text-align: center;
                    margin-bottom: 32px;

                    h3 {
                        margin: 0 0 8px 0;
                        font-size: 22px;
                        font-weight: 700;
                        color: #333;
                    }

                    p {
                        margin: 0;
                        font-size: 14px;
                        color: #999;
                    }
                }
            }

            .has-data {
                padding: 20px;
                display: flex;
                flex-direction: column;
                gap: 20px;

                .video-section {
                    background: #000;
                    border-radius: 16px;
                    overflow: hidden;
                    box-shadow: 0 8px 40px rgba(0, 0, 0, 0.2);

                    .main-video {
                        width: 100%;
                        max-height: 450px;
                        display: block;
                    }
                }

                .info-grid {
                    display: grid;
                    grid-template-columns: repeat(2, 1fr);
                    gap: 20px;

                    .info-card {
                        background: rgba(255, 255, 255, 0.95);
                        border-radius: 16px;
                        overflow: hidden;
                        box-shadow: 0 4px 20px rgba(24, 144, 255, 0.08);
                        border: 1px solid rgba(24, 144, 255, 0.1);
                        transition: all 0.3s;

                        &:hover {
                            box-shadow: 0 8px 30px rgba(24, 144, 255, 0.12);
                            transform: translateY(-2px);
                        }

                        .card-header {
                            display: flex;
                            align-items: center;
                            gap: 10px;
                            padding: 16px 20px;
                            background: linear-gradient(135deg, rgba(24, 144, 255, 0.08) 0%, rgba(105, 192, 255, 0.04) 100%);
                            border-bottom: 1px solid rgba(24, 144, 255, 0.08);

                            .header-left {
                                display: flex;
                                align-items: center;
                                gap: 10px;
                            }

                            .iconfont {
                                font-size: 18px;
                                color: #1890ff;
                            }

                            > span:not(.header-left span) {
                                font-size: 15px;
                                font-weight: 600;
                                color: #333;
                            }
                        }

                        .card-content {
                            padding: 16px 20px;

                            &.qa-content {
                                padding: 0;
                                max-height: 350px;
                                overflow-y: auto;
                            }
                        }

                        .summary-item {
                            display: flex;
                            gap: 12px;
                            padding-bottom: 16px;
                            margin-bottom: 16px;
                            border-bottom: 1px solid #f0f0f0;

                            &:last-child {
                                margin-bottom: 0;
                                padding-bottom: 0;
                                border-bottom: none;
                            }

                            .summary-num {
                                width: 24px;
                                height: 24px;
                                border-radius: 50%;
                                background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                                color: #fff;
                                font-size: 12px;
                                font-weight: 600;
                                display: flex;
                                align-items: center;
                                justify-content: center;
                                flex-shrink: 0;
                            }

                            .summary-body {
                                flex: 1;

                                h4 {
                                    margin: 0 0 6px 0;
                                    font-size: 14px;
                                    font-weight: 600;
                                    color: #1890ff;
                                }

                                p {
                                    margin: 0;
                                    font-size: 13px;
                                    color: #666;
                                    line-height: 1.6;
                                }
                            }
                        }

                        .keywords-wrap {
                            display: flex;
                            flex-wrap: wrap;
                            gap: 8px;

                            .keyword {
                                padding: 6px 14px;
                                background: linear-gradient(135deg, rgba(24, 144, 255, 0.1) 0%, rgba(105, 192, 255, 0.05) 100%);
                                border: 1px solid rgba(24, 144, 255, 0.2);
                                border-radius: 20px;
                                font-size: 13px;
                                color: #1890ff;
                                transition: all 0.3s;
                                cursor: pointer;

                                &:hover {
                                    background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                                    color: #fff;
                                    border-color: transparent;
                                    transform: translateY(-2px);
                                }
                            }
                        }

                        .keyword-cloud-chart-box {
                            height: 200px;

                            .chart {
                                width: 100%;
                                height: 100%;
                            }
                        }

                        .mindmap-container {
                            .mind-mapping-chart-box {
                                height: 280px;

                                .chart {
                                    width: 100%;
                                    height: 100%;
                                }
                            }
                        }

                        .empty {
                            text-align: center;
                            padding: 32px;
                            color: #999;
                            font-size: 13px;
                        }

                        .header-actions {
                            display: flex;
                            align-items: center;
                            gap: 12px;

                            .iconfont {
                                font-size: 16px;
                                color: #666;
                                cursor: pointer;
                                padding: 6px;
                                border-radius: 6px;
                                transition: all 0.3s;

                                &:hover {
                                    background: rgba(24, 144, 255, 0.1);
                                    color: #1890ff;
                                }
                            }

                            .polish-btn {
                                display: flex;
                                align-items: center;
                                gap: 6px;
                                padding: 8px 16px;
                                background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                                border-radius: 20px;
                                cursor: pointer;
                                transition: all 0.3s;
                                box-shadow: 0 4px 16px rgba(24, 144, 255, 0.3);
                                position: relative;

                                .iconfont {
                                    font-size: 14px;
                                    color: #fff;
                                    padding: 0;
                                }

                                span:not(.iconfont):not(.loading-img) {
                                    font-size: 13px;
                                    font-weight: 500;
                                    color: #fff;
                                }

                                .loading-img {
                                    width: 14px;
                                    height: 14px;
                                }

                                &:hover {
                                    transform: translateY(-2px);
                                    box-shadow: 0 6px 20px rgba(24, 144, 255, 0.4);
                                }
                            }
                        }

                        .polish-result {
                            background: linear-gradient(135deg, rgba(24, 144, 255, 0.08) 0%, rgba(105, 192, 255, 0.04) 100%);
                            border: 1px solid rgba(24, 144, 255, 0.15);
                            border-radius: 12px;
                            padding: 14px;
                            margin-bottom: 14px;

                            .polish-header {
                                display: flex;
                                justify-content: space-between;
                                align-items: center;
                                margin-bottom: 10px;

                                > span:first-child {
                                    font-size: 13px;
                                    font-weight: 600;
                                    color: #1890ff;
                                }

                                .polish-actions {
                                    display: flex;
                                    gap: 8px;

                                    button {
                                        padding: 5px 12px;
                                        background: #fff;
                                        border: 1px solid rgba(24, 144, 255, 0.2);
                                        border-radius: 6px;
                                        font-size: 12px;
                                        color: #1890ff;
                                        cursor: pointer;
                                        transition: all 0.3s;

                                        &:hover {
                                            background: #1890ff;
                                            color: #fff;
                                            border-color: #1890ff;
                                        }
                                    }
                                }
                            }

                            .polish-content {
                                font-size: 13px;
                                color: #666;
                                line-height: 1.6;
                            }
                        }
                    }

                    .notes-card {
                        grid-column: span 2;
                    }

                    .mindmap-card {
                        grid-column: span 2;
                    }
                }
            }
        }
    }
}
</style>
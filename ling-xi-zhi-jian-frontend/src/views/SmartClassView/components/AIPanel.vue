<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';
import { type ECharts, init, type EChartsOption } from 'echarts';

type TopicNode = {
    Title: string;
    Topic: TopicNode[];
};

type EchartsTreeMode = {
    name: string;
    children: EchartsTreeMode[] | [];
};

const props = defineProps<{
    summarization?: string;
    meetingAssistance?: string;
    autoChapters?: string;
}>();

const getAiSummaryList = computed(() => {
    try {
        return JSONUtil.parse<{ Headline: string; Summary: string }[]>(props.autoChapters) ?? null;
    } catch {
        return null;
    }
});

const getKeyWordList = computed(() => {
    try {
        return JSONUtil.parse<{ Keywords: string[] }>(props.meetingAssistance)?.Keywords ?? null;
    } catch {
        return null;
    }
});

const getMindMapList = computed(() => {
    try {
        return JSONUtil.parse<{ MindMapSummary: TopicNode[] }>(props.summarization)?.MindMapSummary ?? null;
    } catch {
        return null;
    }
});

const isTree = ref(true);
let chart: ECharts | null = null;
const chartBoxRef = ref<HTMLElement | null>(null);

const JSONUtil = {
    parse<T>(str: string | undefined | null): T | null {
        if (!str) return null;
        try {
            return JSON.parse(str) as T;
        } catch {
            return null;
        }
    }
};

const showAIMindMap = (data: TopicNode, treeMode: boolean) => {
    if (!chartBoxRef.value) return;
    
    chart?.dispose();
    chart = init(chartBoxRef.value);

    const convertData = (data: TopicNode): EchartsTreeMode => {
        return {
            name: data.Title,
            children: data.Topic.length > 0
                ? data.Topic.map((item: TopicNode) => convertData(item))
                : []
        };
    };

    if (treeMode) {
        const option: EChartsOption = {
            series: [{
                type: 'tree',
                data: [convertData(data)],
                layout: 'orthogonal',
                orient: 'LR',
                symbolSize: [60, 35],
                initialTreeDepth: 3,
                path: 'hasLabel',
                nodePadding: 20,
                symbol: 'roundRect',
                itemStyle: {
                    color: '#4ECDC4',
                    borderColor: '#fff',
                    borderWidth: 2,
                    shadowBlur: 10,
                    shadowColor: 'rgba(0,0,0,0.2)',
                },
                label: {
                    color: '#fff',
                    fontSize: 12,
                    fontWeight: 'bold',
                },
                roam: true,
                lineStyle: {
                    color: '#45B7D1',
                    width: 2,
                    curveness: 0.5,
                },
            }],
        };
        chart.setOption(option);
    } else {
        const convertToGraph = (node: TopicNode, links: any[], categories: any[]): any => {
            const nodeData = {
                name: node.Title,
                symbolSize: 50,
                category: node.Title,
            };
            
            if (!categories.find(c => c.name === node.Title)) {
                categories.push({ name: node.Title });
            }

            for (const child of node.Topic) {
                links.push({
                    source: node.Title,
                    target: child.Title,
                });
                convertToGraph(child, links, categories);
            }

            return nodeData;
        };

        const links: any[] = [];
        const categories: any[] = [{ name: data.Title }];
        const graphData = convertToGraph(data, links, categories);

        const option: EChartsOption = {
            series: [{
                type: 'graph',
                layout: 'force',
                data: [graphData],
                links,
                roam: true,
                label: {
                    show: true,
                    position: 'right',
                },
                force: {
                    repulsion: 200,
                    edgeLength: [50, 100],
                },
                itemStyle: {
                    color: '#FF6B6B',
                },
                zoom: 2,
                symbol: 'circle',
                draggable: true,
                lineStyle: {
                    color: '#45B7D1',
                    width: 2,
                },
            }],
        };
        chart.setOption(option);
    }
};

watch(getMindMapList, (newVal) => {
    if (newVal && newVal.length > 0) {
        nextTick(() => {
            showAIMindMap(newVal[0], isTree.value);
        });
    }
});

watch(isTree, () => {
    if (getMindMapList.value && getMindMapList.value.length > 0) {
        nextTick(() => {
            showAIMindMap(getMindMapList.value![0], isTree.value);
        });
    }
});
</script>

<template>
    <div class="ai-panel">
        <!-- AI总结 -->
        <div class="info-card summary-card">
            <div class="card-header">
                <span class="iconfont icon-Ai"></span>
                <span>AI总结</span>
            </div>
            <div class="card-content">
                <template v-if="getAiSummaryList && getAiSummaryList.length">
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
            </div>
        </div>

        <!-- 关键词 -->
        <div class="info-card keywords-card">
            <div class="card-header">
                <span class="iconfont icon-tianjia"></span>
                <span>关键词</span>
            </div>
            <div class="card-content">
                <template v-if="getKeyWordList && getKeyWordList.length">
                    <div class="keywords-wrap">
                        <span v-for="(kw, index) in getKeyWordList" :key="index" class="keyword">
                            {{ kw }}
                        </span>
                    </div>
                </template>
                <div v-else class="empty">暂无关键词</div>
            </div>
        </div>

        <!-- 思维导图 -->
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
                <div v-if="getMindMapList && getMindMapList.length" class="mindmap-container">
                    <div class="mind-mapping-chart-box" ref="chartBoxRef">
                        <div class="chart"></div>
                    </div>
                </div>
                <div v-else class="empty">暂无思维导图</div>
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
.ai-panel {
    display: flex;
    flex-direction: column;
    gap: 16px;
    height: 100%;
    overflow-y: auto;
    padding-right: 8px;

    &::-webkit-scrollbar {
        width: 4px;
    }

    &::-webkit-scrollbar-thumb {
        background: rgba(24, 144, 255, 0.3);
        border-radius: 2px;
    }

    .info-card {
        background: #fff;
        border-radius: 16px;
        box-shadow: 0 4px 20px rgba(24, 144, 255, 0.08);
        border: 1px solid rgba(24, 144, 255, 0.08);
        overflow: hidden;

        .card-header {
            padding: 14px 18px;
            background: linear-gradient(180deg, rgba(24, 144, 255, 0.06) 0%, #fff 100%);
            border-bottom: 1px solid #f0f0f0;
            display: flex;
            align-items: center;
            gap: 8px;

            .iconfont {
                font-size: 18px;
                color: #1890ff;
            }

            span {
                font-size: 15px;
                font-weight: 600;
                color: #333;
            }
        }

        .card-content {
            padding: 16px 18px;
        }
    }

    .summary-card {
        .summary-item {
            display: flex;
            gap: 12px;
            margin-bottom: 14px;

            &:last-child {
                margin-bottom: 0;
            }

            .summary-num {
                width: 24px;
                height: 24px;
                background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                color: #fff;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 12px;
                font-weight: bold;
                flex-shrink: 0;
            }

            .summary-body {
                flex: 1;

                h4 {
                    margin: 0 0 6px 0;
                    font-size: 14px;
                    color: #333;
                    font-weight: 600;
                }

                p {
                    margin: 0;
                    font-size: 13px;
                    color: #666;
                    line-height: 1.5;
                }
            }
        }
    }

    .keywords-card {
        .keywords-wrap {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;

            .keyword {
                padding: 6px 14px;
                background: linear-gradient(135deg, rgba(24, 144, 255, 0.1) 0%, rgba(105, 192, 255, 0.1) 100%);
                border: 1px solid rgba(24, 144, 255, 0.2);
                border-radius: 20px;
                font-size: 13px;
                color: #1890ff;
            }
        }
    }

    .mindmap-card {
        flex: 1;
        min-height: 200px;
        display: flex;
        flex-direction: column;

        .card-content {
            flex: 1;
            display: flex;
            flex-direction: column;
        }

        .mindmap-container {
            flex: 1;
            min-height: 180px;

            .mind-mapping-chart-box {
                width: 100%;
                height: 100%;
                min-height: 180px;

                .chart {
                    width: 100%;
                    height: 100%;
                    min-height: 180px;
                }
            }
        }
    }

    .empty {
        text-align: center;
        color: #999;
        font-size: 14px;
        padding: 20px 0;
    }
}
</style>

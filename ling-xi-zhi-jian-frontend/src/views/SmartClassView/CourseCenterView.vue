<script setup lang="ts">
/**
 * 课程中心 - 高级画中画模式
 */
import {
    computed,
    onMounted,
    onUnmounted,
    ref,
    watch,
} from 'vue';
import { type ECharts, init } from 'echarts';

import { useCourseCenterStore } from '@/stores/courseCenterStore';
import { useAiChatStore } from '@/stores/aiChatStore';
import { useAiNoteStore } from '@/stores/aiLearnFriendStore';
import { useAiAnalysisCacheStore } from '@/stores/aiAnalysisCacheStore';
import AIPanel from './components/AIPanel.vue';

type BaseMotion = {
    name: string;
    description?: string;
    children?: BaseMotion[];
    videoUrl?: string;
    startTime?: number;
    endTime?: number;
};

let baseMotionMapping: ECharts | null = null;

const courseCenterStore = useCourseCenterStore();
const aiChatStore = useAiChatStore();
const aiNoteStore = useAiNoteStore();
const aiAnalysisCacheStore = useAiAnalysisCacheStore();

const selectedCourse = ref<VideoCourse | null>(null);
const selectedVideoId = ref<string | null>(null);
const showMindMap = ref(false);
const mindMapLoading = ref(false);
const videoRef = ref<HTMLVideoElement | null>(null);
const showAIPanel = ref(false);
const currentAIData = ref<{ summarization: string; meetingAssistance: string; autoChapters: string } | null>(null);
const rightPanelTab = ref<'list' | 'ai'>('list');
const isAnalyzingVideo = ref(false);
const analyzingVideoPath = ref<string | null>(null);

const switchRightTab = (tab: 'list' | 'ai') => {
    rightPanelTab.value = tab;
};

const isPipMode = ref(false);
const currentVideoTime = ref(0);
const currentVideoDuration = ref(0);
const currentUserId = ref<number>(1); // 默认用户ID，实际应从登录状态获取

interface VideoCourse {
    id: string;
    name: string;
    category: string;
    duration: string;
    durationSeconds: number;
    teacher: string;
    progress: number;
    videoUrl: string;
    startTime: number;
    endTime: number;
}

interface FolderNode {
    name: string;
    isFolder: boolean;
    path: string;
    children?: FolderNode[];
    isExpanded?: boolean;
}

const expandedFolders = ref<Set<string>>(new Set(['单人专项动作', '基本动作']));

const folderStructure = computed<FolderNode[]>(() => {
    return courseCenterStore.categoryList.map(category => ({
        name: category.subCategoryName,
        isFolder: true,
        path: category.subCategoryName,
        isExpanded: expandedFolders.value.has(category.subCategoryName),
        children: category.subCategoryCourse.map(course => ({
            name: course.name + '.mp4',
            isFolder: false,
            path: course.videoUrl.replace('/videos/', ''),
            videoUrl: course.videoUrl,
            duration: course.videoDuration,
            teacherName: course.teacherName,
        }))
    }));
});

const videoCourseList = computed<VideoCourse[]>(() => {
    const list: VideoCourse[] = [];
    courseCenterStore.categoryList.forEach(category => {
        category.subCategoryCourse.forEach(course => {
            list.push({
                id: String(course.id),
                name: course.name,
                category: category.subCategoryName,
                duration: course.videoDuration,
                durationSeconds: 0,
                teacher: course.teacherName,
                progress: 0,
                videoUrl: course.videoUrl,
                startTime: 0,
                endTime: 0,
            });
        });
    });
    return list;
});

const selectedFolderPath = ref<string>('单人专项动作');

const toggleFolder = (folder: FolderNode) => {
    if (folder.isFolder) {
        if (expandedFolders.value.has(folder.path)) {
            expandedFolders.value.delete(folder.path);
        } else {
            expandedFolders.value.add(folder.path);
        }
    } else {
        selectedFolderPath.value = folder.path;
    }
};

const selectVideoFromFolder = (node: FolderNode) => {
    if (!node.isFolder) {
        selectedFolderPath.value = node.path;
        const matchedCourse = videoCourseList.value.find(
            course => course.videoUrl.includes(node.path)
        );
        if (matchedCourse) {
            selectVideoCourse(matchedCourse);
        } else {
            const newCourse: VideoCourse = {
                id: node.path,
                name: node.name.replace('.mp4', ''),
                category: node.path.split('/')[0],
                duration: (node as any).duration || '--:--',
                durationSeconds: 0,
                teacher: (node as any).teacherName || '未知教练',
                progress: 0,
                videoUrl: `/videos/${node.path}`,
                startTime: 0,
                endTime: 0
            };
            selectVideoCourse(newCourse);
        }
    }
};

const activeNodeName = ref<string>('');

const getProgressColor = (progress: number) => {
    if (progress === 0) return '#e8e8e8';
    if (progress < 30) return '#ff7875';
    if (progress < 70) return '#ffa940';
    return '#52c41a';
};

const getProgressText = (progress: number) => {
    if (progress === 0) return '未开始';
    if (progress === 100) return '已完成';
    return `${progress}%`;
};

const getVideoProgress = (path: string): number => {
    return courseCenterStore.videoProgressMap[path] || 0;
};

const selectVideoCourse = (course: VideoCourse) => {
    selectedVideoId.value = course.id;
    selectedFolderPath.value = course.videoUrl.replace('/videos/', '');
    const videoPath = course.videoUrl.replace('/videos/', '');
    if (courseCenterStore.videoProgressMap[videoPath] !== undefined) {
        (course as any).progress = courseCenterStore.videoProgressMap[videoPath];
    }
    selectedCourse.value = course as any;
    
    // 查找对应的AI分析数据
    // 1. 先检查本地缓存
    const localCache = aiAnalysisCacheStore.getCache(videoPath);
    if (localCache && localCache.taskStatus === '已完成') {
        currentAIData.value = {
            summarization: localCache.summarization,
            meetingAssistance: localCache.meetingAssistance,
            autoChapters: localCache.autoChapters,
        };
        showAIPanel.value = true;
        return;
    }
    
    // 2. 检查 aiNoteStore (AI学习伴侣的历史数据)
    const aiNote = aiNoteStore.historyList.find(
        (note) => note.videoUrl && note.videoUrl.includes(videoPath)
    );
    if (aiNote && aiNote.taskStatus === '已完成') {
        currentAIData.value = {
            summarization: aiNote.summarization,
            meetingAssistance: aiNote.meetingAssistance,
            autoChapters: aiNote.autoChapters,
        };
        // 同步到本地缓存
        aiAnalysisCacheStore.setCache(videoPath, {
            taskId: aiNote.taskId || '',
            summarization: aiNote.summarization,
            meetingAssistance: aiNote.meetingAssistance,
            autoChapters: aiNote.autoChapters,
            taskStatus: '已完成',
        });
        showAIPanel.value = true;
        return;
    }
    
    // 3. 如果正在分析中
    if (aiAnalysisCacheStore.isAnalyzing(videoPath) || (localCache && localCache.taskStatus === '进行中')) {
        isAnalyzingVideo.value = true;
        analyzingVideoPath.value = videoPath;
        currentAIData.value = null;
        showAIPanel.value = false;
        return;
    }
    
    // 4. 无缓存，显示开始分析按钮
    currentAIData.value = null;
    showAIPanel.value = false;
    isAnalyzingVideo.value = false;
    analyzingVideoPath.value = null;
};

const startAIAnalysis = async () => {
    if (!selectedCourse.value) return;
    
    const videoPath = selectedCourse.value.videoUrl.replace('/videos/', '');
    analyzingVideoPath.value = videoPath;
    isAnalyzingVideo.value = true;
    currentAIData.value = null;
    showAIPanel.value = false;
    
    const result = await aiAnalysisCacheStore.triggerAnalysis(videoPath, selectedCourse.value.videoUrl);
    isAnalyzingVideo.value = false;
    
    if (result && result.taskStatus === '已完成') {
        currentAIData.value = {
            summarization: result.summarization,
            meetingAssistance: result.meetingAssistance,
            autoChapters: result.autoChapters,
        };
        showAIPanel.value = true;
    }
    analyzingVideoPath.value = null;
};

const openMindMap = () => {
    showMindMap.value = true;
    setTimeout(() => {
        showMindingMap();
    }, 100);
};

const closeMindMap = () => {
    showMindMap.value = false;
};

const showMindingMap = () => {
    if (baseMotionMapping) {
        baseMotionMapping.dispose();
    }
    
    const chartDom = document.getElementById('mind-map-chart');
    if (!chartDom) return;
    
    baseMotionMapping = init(chartDom);
    baseMotionMapping.showLoading();
    mindMapLoading.value = true;
    
    import('@/assets/course_center_view/shuttlecock_knowledge.json').then(
        (res) => {
            baseMotionMapping?.hideLoading();
            mindMapLoading.value = false;
            const baseMotion = res.default as BaseMotion;
            
            const addLevel = (node: any, level: number) => {
                node.level = level;
                if (node.children) {
                    node.children.forEach((child: any) => addLevel(child, level + 1));
                }
            };
            addLevel(baseMotion, 0);
            
            const option = {
                backgroundColor: 'transparent',
                tooltip: {
                    trigger: 'item',
                    backgroundColor: 'rgba(255, 255, 255, 0.95)',
                    borderColor: '#91d5ff',
                    borderWidth: 1,
                    borderRadius: 10,
                    padding: [10, 14],
                    textStyle: { color: '#333', fontSize: 13 },
                    formatter: function (params: { data: { name: string; description?: string } }) {
                        const node = params.data;
                        let html = `<div style="min-width: 150px;">`;
                        html += `<div style="font-weight: 600; font-size: 14px; color: #1890ff;">${node.name}</div>`;
                        if (node.description) {
                            html += `<div style="color: #666; line-height: 1.5; margin-top: 6px;">${node.description}</div>`;
                        }
                        html += `</div>`;
                        return html;
                    },
                },
                series: [{
                    type: 'tree',
                    data: [baseMotion],
                    layout: 'orthogonal',
                    orient: 'LR',
                    symbolSize: [75, 32],
                    initialTreeDepth: 1,
                    path: 'path',
                    nodePadding: 70,
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
                        fontSize: 11,
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
                }],
            };
            baseMotionMapping?.setOption(option);

            baseMotionMapping?.on('click', (params: any) => {
                if (params.data && params.data.name) {
                    const nodeName = params.data.name;
                    const nodeData = params.data;
                    
                    if (nodeData.level && nodeData.level >= 2) {
                        const matchedCourse = videoCourseList.value.find(
                            course => course.name.includes(nodeName) || nodeName.includes(course.name)
                        );
                        if (matchedCourse) {
                            selectVideoCourse(matchedCourse);
                            activeNodeName.value = nodeName;
                            closeMindMap();
                        }
                    }
                }
            });

            baseMotionMapping?.on('mouseover', (params: any) => {
                if (params.data && params.data.name) {
                    activeNodeName.value = params.data.name;
                }
            });

            window.addEventListener('resize', () => baseMotionMapping?.resize());
        },
    );
};

const togglePipMode = () => {
    isPipMode.value = !isPipMode.value;
};

const handleTimeUpdate = () => {
    if (videoRef.value) {
        currentVideoTime.value = videoRef.value.currentTime;
        currentVideoDuration.value = videoRef.value.duration || 0;
        
        // 更新进度
        const matchedCourse = selectedCourse.value;
        if (matchedCourse && currentVideoDuration.value > 0) {
            const progress = Math.floor((currentVideoTime.value / currentVideoDuration.value) * 100);
            matchedCourse.progress = progress;
            
            // 保存进度到store（内存）
            const videoPath = matchedCourse.videoUrl.replace('/videos/', '');
            courseCenterStore.videoProgressMap[videoPath] = progress;
            
            // 进度超过80%时保存到后端
            if (progress >= 80) {
                courseCenterStore.saveVideoProgress(currentUserId.value, videoPath, progress);
            }
        }
    }
};

const handleVideoLoadedMetadata = () => {
    if (videoRef.value) {
        currentVideoDuration.value = videoRef.value.duration || 0;
    }
};

onMounted(async () => {
    // 获取用户ID（从localStorage）
    const userIdStr = localStorage.getItem('user_id') || localStorage.getItem('userId');
    if (userIdStr) {
        currentUserId.value = parseInt(userIdStr);
    }
    
    // 初始化AI分析本地缓存
    aiAnalysisCacheStore.init();
    
    // 加载视频进度
    courseCenterStore.getVideoProgress(currentUserId.value);
    
    // 加载课程列表
    courseCenterStore.getCourseList();
    
    // 加载AI笔记历史
    aiNoteStore.getHistoryList();
    
    // 等待数据加载完成后选择第一个视频
    let initialized = false;
    watch(
        () => courseCenterStore.categoryList.length,
        (length) => {
            if (length > 0 && !initialized) {
                initialized = true;
                const firstFolder = folderStructure.value.find(f => f.isExpanded && f.children && f.children.length > 0);
                if (firstFolder && firstFolder.children && firstFolder.children.length > 0) {
                    selectVideoFromFolder(firstFolder.children[0]);
                } else if (videoCourseList.value.length > 0) {
                    selectVideoCourse(videoCourseList.value[0]);
                }
            }
        },
        { immediate: true }
    );
});

onUnmounted(() => {
    aiChatStore.closeWs();
    if (baseMotionMapping) {
        baseMotionMapping.dispose();
    }
});
</script>

<template>
    <div class="course-center-view">
        <div class="main-content" :class="{ 'pip-mode': isPipMode }">
            <div class="video-capsule">
                <div class="capsule-left">
                    <div class="video-player-wrapper">
                        <video 
                            ref="videoRef"
                            controls 
                            preload="metadata"
                            class="video-player" 
                            :src="selectedCourse?.videoUrl"
                            @timeupdate="handleTimeUpdate"
                            @loadedmetadata="handleVideoLoadedMetadata"
                        />
                        <div class="mind-map-float-btn" @click="openMindMap">
                            <span class="iconfont icon-mind-map"></span>
                            <span class="btn-text">知识导图</span>
                        </div>
                    </div>
                    <div class="video-info-bar">
                        <div class="video-info">
                            <span class="category-tag">{{ selectedCourse?.category }}</span>
                            <h2 class="video-title">{{ selectedCourse?.name }}</h2>
                            <div class="video-meta">
                                <span class="meta-item">{{ selectedCourse?.teacher }}</span>
                                <span class="meta-divider">|</span>
                                <span class="meta-item">{{ selectedCourse?.duration }}</span>
                            </div>
                        </div>
                        <div class="progress-section">
                            <div class="progress-bar-wrapper">
                                <div class="progress-bar-track">
                                    <div class="progress-bar-fill" :style="{ width: (selectedCourse?.progress || 0) + '%', backgroundColor: getProgressColor((selectedCourse?.progress || 0)) }"></div>
                                </div>
                                <span class="progress-text">{{ getProgressText((selectedCourse?.progress || 0)) }}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="capsule-right">
                    <div class="video-list-header">
                        <div class="header-tabs">
                            <button 
                                class="tab-btn" 
                                :class="{ active: rightPanelTab === 'list' }"
                                @click="switchRightTab('list')"
                            >
                                课程目录
                            </button>
                            <button 
                                class="tab-btn" 
                                :class="{ active: rightPanelTab === 'ai' }"
                                @click="switchRightTab('ai')"
                            >
                                AI分析
                            </button>
                        </div>
                    </div>
                    
                    <div class="folder-list" v-if="rightPanelTab === 'list'">
                        <!-- 文件夹课程列表 -->
                        <template v-for="folder in folderStructure" :key="folder.path">
                            <div 
                                class="folder-item"
                                @click="toggleFolder(folder)"
                            >
                                <span class="folder-name">{{ folder.name }}</span>
                                <span class="folder-count" v-if="folder.children">{{ folder.children.length }}</span>
                                <span class="expand-icon">{{ folder.isExpanded ? '▼' : '▶' }}</span>
                            </div>
                            <div class="folder-children" v-show="folder.isExpanded && folder.children">
                                <div 
                                    v-for="child in folder.children" 
                                    :key="child.path"
                                    class="video-item"
                                    :class="{ active: selectedFolderPath === child.path }"
                                    @click.stop="selectVideoFromFolder(child)"
                                >
                                    <div class="item-indicator" v-if="selectedFolderPath === child.path"></div>
                                    <div class="item-thumbnail">
                                        <div class="thumbnail-bg">
                                            <span class="play-icon">▶</span>
                                        </div>
                                        <div class="video-progress-bar" v-if="getVideoProgress(child.path) > 0">
                                            <div class="progress-fill" :style="{ width: getVideoProgress(child.path) + '%' }"></div>
                                        </div>
                                    </div>
                                    <div class="item-content">
                                        <h4 class="item-title">{{ child.name }}</h4>
                                        <div class="item-meta">
                                            <span class="item-teacher">mp4</span>
                                            <span class="item-progress" v-if="getVideoProgress(child.path) > 0">
                                                {{ getVideoProgress(child.path) }}%
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </template>
                    </div>
                    
                    <div class="ai-panel-container" v-else-if="rightPanelTab === 'ai'">
                        <div v-if="isAnalyzingVideo" class="analyzing-status">
                            <div class="analyzing-spinner"></div>
                            <p>正在分析中...</p>
                            <span>AI正在生成总结、关键词和知识导图</span>
                        </div>
                        <AIPanel 
                            v-else-if="currentAIData"
                            :summarization="currentAIData.summarization"
                            :meetingAssistance="currentAIData.meetingAssistance"
                            :autoChapters="currentAIData.autoChapters"
                        />
                        <div v-else-if="!currentAIData && !isAnalyzingVideo" class="no-ai-data">
                            <p>暂无AI分析数据</p>
                            <button class="start-analysis-btn" @click="startAIAnalysis">
                                开始AI分析
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <div class="pip-video" v-if="isPipMode" @click="togglePipMode">
                <video controls preload="metadata" class="pip-video-player" :src="selectedCourse?.videoUrl" />
                <div class="pip-overlay">
                    <span class="pip-hint">点击退出画中画</span>
                </div>
            </div>
        </div>

        <Transition name="mind-map">
            <div class="mind-map-overlay" v-if="showMindMap" @click.self="closeMindMap">
                <div class="mind-map-modal">
                    <div class="modal-header">
                        <div class="header-content">
                            <span class="iconfont icon-mind-map"></span>
                            <div class="header-text">
                                <h2>毽球知识导图</h2>
                                <p>点击节点跳转视频课程</p>
                            </div>
                        </div>
                        <button class="close-btn" @click="closeMindMap">
                            <span class="iconfont icon-close"></span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <div class="mind-map-chart" id="mind-map-chart"></div>
                    </div>
                </div>
            </div>
        </Transition>
    </div>
</template>

<style scoped lang="scss">
.course-center-view {
    height: 100vh;
    overflow: hidden;
    background: linear-gradient(180deg, #f0f7ff 0%, #e6f0ff 100%);
    position: relative;

    .main-content {
        height: 100%;
        padding: 20px;
        display: flex;
        flex-direction: column;
        gap: 16px;
        transition: all 0.4s ease;
        overflow: hidden;

        &.pip-mode {
            .video-capsule {
                transform: scale(0.85);
                transform-origin: top left;
                opacity: 0.6;
            }
        }

        &::-webkit-scrollbar {
            width: 6px;
        }

        &::-webkit-scrollbar-thumb {
            background: rgba(24, 144, 255, 0.2);
            border-radius: 3px;
        }

        .video-capsule {
            flex: 1;
            display: flex;
            gap: 16px;
            background: #fff;
            border-radius: 20px;
            overflow: hidden;
            box-shadow: 0 4px 24px rgba(24, 144, 255, 0.1);
            border: 1px solid rgba(24, 144, 255, 0.1);
            transition: all 0.4s ease;
            min-height: 0;
            height: 100%;

            .capsule-left {
                flex: 1;
                display: flex;
                flex-direction: column;
                background: #fafcff;
                border-radius: 16px;
                overflow: hidden;
                margin: 12px;
                margin-right: 8px;
                position: relative;
                min-width: 0;

                .video-player-wrapper {
                    width: 100%;
                    height: 0;
                    padding-bottom: 56.25%;
                    position: relative;
                    background: #1a1a1a;

                    .video-player {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        object-fit: contain;
                    }

                    .mind-map-float-btn {
                        position: absolute;
                        top: 16px;
                        right: 16px;
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        padding: 10px 18px;
                        background: rgba(24, 144, 255, 0.9);
                        backdrop-filter: blur(10px);
                        border-radius: 25px;
                        cursor: pointer;
                        transition: all 0.3s ease;
                        box-shadow: 0 4px 20px rgba(24, 144, 255, 0.4);

                        .icon-mind-map {
                            font-size: 18px;
                            color: #fff;
                        }

                        .btn-text {
                            font-size: 13px;
                            font-weight: 600;
                            color: #fff;
                        }

                        &:hover {
                            background: rgba(24, 144, 255, 1);
                            transform: scale(1.05);
                            box-shadow: 0 6px 25px rgba(24, 144, 255, 0.5);
                        }
                    }
                }

                .video-info-bar {
                    padding: 16px 20px;
                    background: linear-gradient(180deg, rgba(24, 144, 255, 0.06) 0%, #fff 100%);
                    border-top: 1px solid #f0f0f0;

                    .video-info {
                        margin-bottom: 12px;

                        .category-tag {
                            display: inline-block;
                            background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                            color: #fff;
                            font-size: 11px;
                            font-weight: 600;
                            padding: 3px 12px;
                            border-radius: 20px;
                            margin-bottom: 8px;
                        }

                        .video-title {
                            margin: 0 0 6px 0;
                            font-size: 18px;
                            font-weight: 700;
                            color: #333;
                        }

                        .video-meta {
                            display: flex;
                            align-items: center;
                            gap: 8px;

                            .meta-item {
                                font-size: 12px;
                                color: #666;
                            }

                            .meta-divider {
                                color: #ddd;
                            }
                        }
                    }

                    .progress-section {
                        .progress-bar-wrapper {
                            display: flex;
                            align-items: center;
                            gap: 12px;

                            .progress-bar-track {
                                flex: 1;
                                height: 5px;
                                background: #f0f0f0;
                                border-radius: 3px;
                                overflow: hidden;

                                .progress-bar-fill {
                                    height: 100%;
                                    border-radius: 3px;
                                    transition: width 0.3s ease;
                                }
                            }

                            .progress-text {
                                font-size: 12px;
                                color: #1890ff;
                                font-weight: 600;
                                min-width: 50px;
                                text-align: right;
                            }
                        }
                    }
                }
            }

            .capsule-right {
                width: 440px;
                display: flex;
                flex-direction: column;
                background: #fff;
                border-left: 1px solid #f0f0f0;
                overflow-y: auto;
                flex-shrink: 0;
                min-height: 0;

                .video-list-header {
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    padding: 16px 18px;
                    border-bottom: 1px solid #f0f0f0;

                    .header-tabs {
                        display: flex;
                        gap: 8px;

                        .tab-btn {
                            padding: 6px 16px;
                            border: none;
                            background: #f5f5f5;
                            border-radius: 20px;
                            font-size: 13px;
                            cursor: pointer;
                            transition: all 0.2s;
                            color: #666;

                            &.active {
                                background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                                color: #fff;
                            }

                            &:hover:not(.active) {
                                background: #e6f4ff;
                            }
                        }
                    }
                }

                .ai-panel-container {
                    flex: 1;
                    padding: 12px;
                    overflow-y: auto;

                    .no-ai-data {
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        height: 300px;
                        text-align: center;

                        p {
                            margin: 0 0 8px 0;
                            font-size: 15px;
                            color: #333;
                        }

                        span {
                            font-size: 13px;
                            color: #999;
                        }
                    }

                    .analyzing-status {
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        height: 300px;
                        text-align: center;

                        .analyzing-spinner {
                            width: 48px;
                            height: 48px;
                            border: 4px solid rgba(24, 144, 255, 0.2);
                            border-top-color: #1890ff;
                            border-radius: 50%;
                            animation: spin 1s linear infinite;
                            margin-bottom: 16px;
                        }

                        p {
                            margin: 0 0 8px 0;
                            font-size: 15px;
                            color: #333;
                        }

                        span {
                            font-size: 13px;
                            color: #999;
                        }
                    }

                    @keyframes spin {
                        to {
                            transform: rotate(360deg);
                        }
                    }

                    .start-analysis-btn {
                        margin-top: 16px;
                        padding: 12px 24px;
                        background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                        border: none;
                        border-radius: 24px;
                        color: #fff;
                        font-size: 14px;
                        font-weight: 600;
                        cursor: pointer;
                        transition: all 0.3s ease;
                        box-shadow: 0 4px 16px rgba(24, 144, 255, 0.3);

                        &:hover {
                            transform: translateY(-2px);
                            box-shadow: 0 6px 20px rgba(24, 144, 255, 0.4);
                        }

                        &:active {
                            transform: translateY(0);
                        }
                    }
                }

                .folder-list {
                    max-height: calc(100vh - 180px);
                    overflow-y: auto;
                    padding: 8px;

                    &::-webkit-scrollbar {
                        width: 4px;
                    }

                    &::-webkit-scrollbar-thumb {
                        background: rgba(24, 144, 255, 0.2);
                        border-radius: 2px;
                    }

                    .folder-item {
                        display: flex;
                        align-items: center;
                        gap: 10px;
                        padding: 12px 14px;
                        border-radius: 10px;
                        cursor: pointer;
                        transition: all 0.2s ease;
                        margin-bottom: 4px;
                        background: #f5f5f5;
                        border: 1px solid #e0e0e0;

                        &:hover {
                            background: #e6f4ff;
                            border-color: #d4eaff;
                        }

                        .folder-name {
                            flex: 1;
                            font-size: 14px;
                            font-weight: 600;
                            color: #333;
                        }

                        .folder-count {
                            font-size: 11px;
                            background: #e6f4ff;
                            color: #1890ff;
                            padding: 2px 8px;
                            border-radius: 10px;
                        }

                        .expand-icon {
                            font-size: 10px;
                            color: #999;
                            transition: transform 0.2s ease;
                        }
                    }

                    .folder-children {
                        padding-left: 20px;
                        margin-bottom: 8px;

                        .video-item {
                            display: flex;
                            align-items: center;
                            gap: 12px;
                            padding: 10px 14px;
                            border-radius: 10px;
                            cursor: pointer;
                            transition: all 0.2s ease;
                            position: relative;
                            margin-bottom: 4px;
                            border: 1px solid transparent;

                            &:hover {
                                background: #f8fbff;
                                border-color: #d4eaff;
                            }

                            &.active {
                                background: #f0f7ff;
                                border-color: rgba(24, 144, 255, 0.3);

                                .item-indicator {
                                    display: block;
                                }
                            }

                            .item-indicator {
                                display: none;
                                position: absolute;
                                left: 0;
                                top: 50%;
                                transform: translateY(-50%);
                                width: 3px;
                                height: 50%;
                                background: linear-gradient(180deg, #1890ff, #69c0ff);
                                border-radius: 0 3px 3px 0;
                            }

                            .item-thumbnail {
                                width: 80px;
                                height: 50px;
                                border-radius: 8px;
                                background: linear-gradient(135deg, #e8f4ff 0%, #d4eaff 100%);
                                display: flex;
                                align-items: center;
                                justify-content: center;
                                flex-shrink: 0;
                                position: relative;
                                overflow: hidden;

                                .play-icon {
                                    font-size: 16px;
                                    color: rgba(24, 144, 255, 0.6);
                                }

                                .video-progress-bar {
                                    position: absolute;
                                    bottom: 0;
                                    left: 0;
                                    right: 0;
                                    height: 3px;
                                    background: rgba(0, 0, 0, 0.2);

                                    .progress-fill {
                                        height: 100%;
                                        background: linear-gradient(90deg, #1890ff, #52c41a);
                                        transition: width 0.3s ease;
                                    }
                                }
                            }

                            .item-content {
                                flex: 1;
                                min-width: 0;

                                .item-title {
                                    margin: 0 0 4px 0;
                                    font-size: 13px;
                                    color: #333;
                                    font-weight: 500;
                                    overflow: hidden;
                                    text-overflow: ellipsis;
                                    white-space: nowrap;
                                }

                                .item-meta {
                                    display: flex;
                                    align-items: center;
                                    gap: 8px;

                                    .item-teacher {
                                        font-size: 11px;
                                        color: #999;
                                    }

                                    .item-progress {
                                        font-size: 11px;
                                        color: #1890ff;
                                        font-weight: 500;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        .pip-video {
            position: fixed;
            bottom: 24px;
            right: 24px;
            width: 360px;
            border-radius: 16px;
            overflow: hidden;
            box-shadow: 0 8px 40px rgba(0, 0, 0, 0.3);
            z-index: 1000;
            cursor: pointer;
            transition: all 0.3s ease;

            &:hover {
                transform: scale(1.02);
                box-shadow: 0 12px 50px rgba(0, 0, 0, 0.4);
            }

            .pip-video-player {
                width: 100%;
                display: block;
                background: #000;
            }

            .pip-overlay {
                position: absolute;
                inset: 0;
                background: linear-gradient(transparent 60%, rgba(0,0,0,0.6));
                display: flex;
                align-items: flex-end;
                justify-content: center;
                padding-bottom: 12px;
                opacity: 0;
                transition: opacity 0.3s;

                .pip-hint {
                    font-size: 12px;
                    color: #fff;
                    background: rgba(0,0,0,0.5);
                    padding: 4px 12px;
                    border-radius: 12px;
                }
            }

            &:hover .pip-overlay {
                opacity: 1;
            }
        }
    }

    .mind-map-overlay {
        position: fixed;
        inset: 0;
        background: rgba(24, 144, 255, 0.3);
        backdrop-filter: blur(10px);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 2000;

        .mind-map-modal {
            width: 90%;
            height: 85%;
            max-width: 1400px;
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(20px);
            border-radius: 24px;
            border: 1px solid rgba(24, 144, 255, 0.2);
            box-shadow: 
                0 8px 40px rgba(24, 144, 255, 0.15),
                0 0 0 1px rgba(255, 255, 255, 0.5) inset;
            display: flex;
            flex-direction: column;
            overflow: hidden;

            .modal-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 20px 28px;
                background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);

                .header-content {
                    display: flex;
                    align-items: center;
                    gap: 14px;

                    .icon-mind-map {
                        font-size: 28px;
                        color: #fff;
                    }

                    .header-text {
                        h2 {
                            margin: 0;
                            font-size: 20px;
                            font-weight: 700;
                            color: #fff;
                        }

                        p {
                            margin: 2px 0 0 0;
                            font-size: 12px;
                            color: rgba(255, 255, 255, 0.8);
                        }
                    }
                }

                .close-btn {
                    width: 40px;
                    height: 40px;
                    border-radius: 50%;
                    background: rgba(255, 255, 255, 0.25);
                    border: none;
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    transition: all 0.3s ease;

                    .icon-close {
                        font-size: 18px;
                        color: #fff;
                    }

                    &:hover {
                        background: rgba(255, 255, 255, 0.35);
                        transform: rotate(90deg);
                    }
                }
            }

            .modal-body {
                flex: 1;
                padding: 16px;

                .mind-map-chart {
                    width: 100%;
                    height: 100%;
                    background: #fafcff;
                    border-radius: 16px;
                    border: 1px solid rgba(24, 144, 255, 0.1);
                }
            }
        }
    }
}

.mind-map-enter-active,
.mind-map-leave-active {
    transition: all 0.35s ease;

    .mind-map-modal {
        transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
    }
}

.mind-map-enter-from,
.mind-map-leave-to {
    opacity: 0;

    .mind-map-modal {
        transform: scale(0.92) translateY(20px);
        opacity: 0;
    }
}
</style>
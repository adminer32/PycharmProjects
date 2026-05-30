<script setup lang="ts">
/**
 * 智能课堂
 */
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import Button from '@/components/通用/Button/Button.vue';
import * as echarts from 'echarts/core';
import { RadarChart } from 'echarts/charts';
import {
    TitleComponent,
    TooltipComponent,
    LegendComponent
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { useAiNoteStore } from '@/stores/aiLearnFriendStore';
import { useMotionAssessmentStore } from '@/stores/motionAssessmentStore';
import { useUserStore } from '@/stores/userStore';
import AIPanel from './components/AIPanel.vue';
import HttpUtil from '@/utils/HttpUtil';

const router = useRouter();

const goToMotionAnalysis = () => {
    router.push({ path: '/motion_assessment', query: { tab: 'motionAnalysis' } });
};

echarts.use([
    RadarChart,
    TitleComponent,
    TooltipComponent,
    LegendComponent,
    CanvasRenderer
]);

const aiNoteStore = useAiNoteStore();
const motionAssessmentStore = useMotionAssessmentStore();
const userStore = useUserStore();
const videoRef = ref<HTMLVideoElement | null>(null);
const radarChartRef = ref<echarts.ECharts | null>(null);
const currentAIData = ref<{ summarization: string; meetingAssistance: string; autoChapters: string } | null>(null);

// 动作类型到视频URL的映射
const actionVideoMap: Record<string, { name: string; videoUrl: string }> = {
    '绷踢': { name: '毽球绷踢技巧', videoUrl: '/videos/单人专项动作/绷踢.mp4' },
    '绷踢': { name: '毽球绷踢技巧', videoUrl: '/videos/单人专项动作/绷踢.mp4' },
    '盘踢': { name: '毽球盘踢技巧', videoUrl: '/videos/单人专项动作/盘踢.mp4' },
    '磕踢': { name: '毽球磕踢技巧', videoUrl: '/videos/单人专项动作/磕踢.mp4' },
    '拐踢': { name: '毽球拐踢技巧', videoUrl: '/videos/单人专项动作/拐踢.mp4' },
};

// 薄弱动作推荐
const recommendedAction = ref<{ action: string; score: number; reason: string } | null>(null);
const isLoadingRecommendation = ref(false);

const selectedReplay = ref({
    id: '1',
    name: '毽球基础技巧课程',
    coverImageUrl: '',
    videoUrl: '',
    duration: '45:30',
    date: '2024-03-20',
    teacher: '张教练',
    views: 1234
});

const replayList = ref([
    {
        id: '1',
        name: '毽球基础技巧课程',
        coverImageUrl: '',
        duration: '45:30',
        date: '2024-03-20',
        teacher: '张教练',
        views: 1234
    },
    {
        id: '2',
        name: '毽球高级战术分析',
        coverImageUrl: '',
        duration: '52:15',
        date: '2024-03-18',
        teacher: '李教练',
        views: 892
    },
    {
        id: '3',
        name: '毽球比赛规则解读',
        coverImageUrl: '',
        duration: '38:45',
        date: '2024-03-15',
        teacher: '王裁判',
        views: 657
    }
]);

// 计算薄弱动作并推荐视频
const calculateWeakestAction = async () => {
    isLoadingRecommendation.value = true;
    
    try {
        // 获取用户ID
        const userId = userStore.myInfo.userId;
        if (!userId) {
            console.log('用户未登录');
            isLoadingRecommendation.value = false;
            return;
        }
        
        // 从 FastAPI 获取动作分数
        const res = await HttpUtil.get<{ action_type: string; current_score: number; history_avg_score: number }[]>(
            `/api/v0/student/learning/action-scores/${userId}`
        );
        
        console.log('动作分数数据:', res);
        
        if (!res || res.length === 0) {
            console.log('没有动作分数数据');
            isLoadingRecommendation.value = false;
            return;
        }
        
        // 找出最低分的动作
        const sortedActions = [...res].sort((a, b) => a.current_score - b.current_score);
        const weakest = sortedActions[0];
        
        console.log('最薄弱动作:', weakest);
        
        // 找到对应的视频
        const videoInfo = actionVideoMap[weakest.action_type];
        console.log('视频信息:', videoInfo);
        
        if (videoInfo) {
            recommendedAction.value = {
                action: weakest.action_type,
                score: Math.round(weakest.current_score),
                reason: `您的${weakest.action_type}动作当前评分最低（${Math.round(weakest.current_score)}分），建议优先练习`
            };
            
            // 自动选择并播放推荐视频
            selectedReplay.value = {
                id: weakest.action_type,
                name: videoInfo.name,
                coverImageUrl: '',
                videoUrl: videoInfo.videoUrl,
                duration: '00:00',
                date: new Date().toLocaleDateString(),
                teacher: 'AI推荐',
                views: 0
            };
            
            console.log('设置视频源:', videoInfo.videoUrl);
            
            // 更新视频后自动播放
            setTimeout(() => {
                if (videoRef.value) {
                    console.log('videoRef 存在，准备播放');
                    videoRef.value.load();
                    videoRef.value.play().catch(e => console.log('自动播放失败:', e));
                } else {
                    console.log('videoRef 不存在');
                }
            }, 500);
        } else {
            console.log('没有找到对应的视频:', weakest.action_type);
        }
        
    } catch (e) {
        console.error('计算薄弱动作失败:', e);
    } finally {
        isLoadingRecommendation.value = false;
    }
};

const currentAIDataComputed = computed(() => {
    if (!selectedReplay.value) return null;
    const videoPath = selectedReplay.value.videoUrl.replace('/videos/', '');
    const aiNote = aiNoteStore.historyList.find(
        (note) => note.videoUrl && note.videoUrl.includes(videoPath)
    );
    if (aiNote && aiNote.taskStatus === '已完成') {
        return {
            summarization: aiNote.summarization,
            meetingAssistance: aiNote.meetingAssistance,
            autoChapters: aiNote.autoChapters,
        };
    }
    return null;
});

const currentTime = ref('00:00');
const totalTime = ref('45:30');
const isMuted = ref(false);
const volume = ref(80);
const isPlaying = ref(false);
const playbackSpeed = ref(1);
const showSpeedMenu = ref(false);
const progressPercentage = ref(0);
const showVolumeValue = ref(false);
const isCollected = ref(false);

const selectReplay = (replay: any) => {
    selectedReplay.value = replay;
    totalTime.value = replay.duration;
    currentTime.value = '00:00';
    progressPercentage.value = 0;
    isPlaying.value = false;
    isCollected.value = false;
    if (videoRef.value) {
        videoRef.value.pause();
        videoRef.value.currentTime = 0;
    }
    
    // 查找对应的AI分析数据
    const videoPath = replay.videoUrl.replace('/videos/', '');
    const aiNote = aiNoteStore.historyList.find(
        (note) => note.videoUrl && note.videoUrl.includes(videoPath)
    );
    if (aiNote && aiNote.taskStatus === '已完成') {
        currentAIData.value = {
            summarization: aiNote.summarization,
            meetingAssistance: aiNote.meetingAssistance,
            autoChapters: aiNote.autoChapters,
        };
    } else {
        currentAIData.value = null;
    }
};

const toggleSpeedMenu = () => {
    showSpeedMenu.value = !showSpeedMenu.value;
};

const setSpeed = (speed: number) => {
    playbackSpeed.value = speed;
    showSpeedMenu.value = false;
};

const seekTo = (event: MouseEvent) => {
    const progressBar = event.currentTarget as HTMLElement;
    const rect = progressBar.getBoundingClientRect();
    const clickPosition = event.clientX - rect.left;
    const percentage = (clickPosition / rect.width) * 100;
    progressPercentage.value = Math.max(0, Math.min(100, percentage));
    // 更新播放时间
    updateCurrentTime(percentage);
    // 控制视频播放位置
    if (videoRef.value && videoRef.value.duration) {
        const seekTime = videoRef.value.duration * (percentage / 100);
        videoRef.value.currentTime = seekTime;
    }
};

const startDrag = (event: MouseEvent) => {
    event.preventDefault();
    document.addEventListener('mousemove', onDrag);
    document.addEventListener('mouseup', stopDrag);
    seekTo(event);
};

const onDrag = (event: MouseEvent) => {
    const progressBar = document.querySelector('.progress-bar') as HTMLElement;
    if (progressBar) {
        const rect = progressBar.getBoundingClientRect();
        const clickPosition = event.clientX - rect.left;
        const percentage = (clickPosition / rect.width) * 100;
        progressPercentage.value = Math.max(0, Math.min(100, percentage));
        // 更新播放时间
        updateCurrentTime(percentage);
        // 控制视频播放位置
        if (videoRef.value && videoRef.value.duration) {
            const seekTime = videoRef.value.duration * (percentage / 100);
            videoRef.value.currentTime = seekTime;
        }
    }
};

const stopDrag = () => {
    document.removeEventListener('mousemove', onDrag);
    document.removeEventListener('mouseup', stopDrag);
};

const updateCurrentTime = (percentage: number) => {
    // 确保百分比在0-100之间
    const validPercentage = Math.max(0, Math.min(100, percentage));
    
    // 解析总时间
    const totalTimeParts = totalTime.value.split(':');
    const totalMinutes = parseInt(totalTimeParts[0]);
    const totalSeconds = parseInt(totalTimeParts[1]);
    const totalTotalSeconds = totalMinutes * 60 + totalSeconds;
    
    // 根据百分比计算当前时间
    const currentTotalSeconds = Math.max(0, Math.floor(totalTotalSeconds * (validPercentage / 100)));
    const currentMinutes = Math.max(0, Math.floor(currentTotalSeconds / 60));
    const currentSeconds = Math.max(0, currentTotalSeconds % 60);
    
    // 格式化为时间字符串
    currentTime.value = `${currentMinutes.toString().padStart(2, '0')}:${currentSeconds.toString().padStart(2, '0')}`;
};

const toggleMute = () => {
    isMuted.value = !isMuted.value;
};

const adjustVolume = (value: number) => {
    volume.value = value;
    isMuted.value = value === 0;
    // 更新音量滑块的CSS变量
    const volumeSliders = document.querySelectorAll('.volume-slider-container input[type="range"]');
    volumeSliders.forEach(slider => {
        slider.style.setProperty('--volume-percentage', `${value}%`);
    });
};

const togglePlay = () => {
    if (videoRef.value) {
        if (isPlaying.value) {
            videoRef.value.pause();
        } else {
            videoRef.value.play();
        }
    }
};

const toggleCollect = () => {
    isCollected.value = !isCollected.value;
};

const handleTimeUpdate = () => {
    if (videoRef.value) {
        const { currentTime: videoCurrentTime, duration } = videoRef.value;
        if (duration > 0) {
            // 确保当前时间不为负数
            const validCurrentTime = Math.max(0, videoCurrentTime);
            
            // 更新进度百分比
            const percentage = (validCurrentTime / duration) * 100;
            progressPercentage.value = Math.max(0, Math.min(100, percentage));
            
            // 更新当前时间
            const minutes = Math.max(0, Math.floor(validCurrentTime / 60));
            const seconds = Math.max(0, Math.floor(validCurrentTime % 60));
            currentTime.value = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
            
            // 更新总时间
            if (totalTime.value === '45:30') { // 只有在初始值时才更新
                const totalMinutes = Math.max(0, Math.floor(duration / 60));
                const totalSeconds = Math.max(0, Math.floor(duration % 60));
                totalTime.value = `${totalMinutes.toString().padStart(2, '0')}:${totalSeconds.toString().padStart(2, '0')}`;
            }
        }
    }
};

const getProgressColor = (percentage: number) => {
    if (percentage < 30) {
        return '#F56C6C'; // 红色
    } else if (percentage < 70) {
        return '#E6A23C'; // 橙色
    } else {
        return '#67C23A'; // 绿色
    }
};

onMounted(() => {
    // 初始化智能课堂
    console.log('智能课堂初始化');
    
    // 加载AI笔记历史
    aiNoteStore.getHistoryList();
    
    // 加载动作评估历史并计算薄弱动作
    calculateWeakestAction();
    
    // 初始化毽球运动雷达图
    initShuttlecockRadarChart();
    
    // 初始化音量滑块的CSS变量
    const volumeSliders = document.querySelectorAll('.volume-slider-container input[type="range"]');
    volumeSliders.forEach(slider => {
        slider.style.setProperty('--volume-percentage', `${volume.value}%`);
    });
    
    // 添加视频播放事件监听器
    if (videoRef.value) {
        videoRef.value.addEventListener('timeupdate', handleTimeUpdate);
        videoRef.value.addEventListener('play', () => {
            isPlaying.value = true;
        });
        videoRef.value.addEventListener('pause', () => {
            isPlaying.value = false;
        });
    }
});

const initShuttlecockRadarChart = () => {
    console.log('初始化雷达图');
    const chartDom = document.getElementById('shuttlecock-radar-chart');
    console.log('雷达图容器:', chartDom);
    if (chartDom) {
        console.log('雷达图容器尺寸:', chartDom.offsetWidth, 'x', chartDom.offsetHeight);
        radarChartRef.value = echarts.init(chartDom);
        console.log('ECharts实例:', radarChartRef.value);
        
        const option = {
            tooltip: {},
            radar: {
                indicator: [
                    { name: '盘踢', max: 100 },
                    { name: '绷踢', max: 100 },
                    { name: '拐踢', max: 100 },
                    { name: '磕踢', max: 100 },
                    { name: '踏踢', max: 100 },
                    { name: '跳踢', max: 100 }
                ]
            },
            series: [
                {
                    name: '毽球技能',
                    type: 'radar',
                    data: [
                        {
                            value: [85, 78, 90, 75, 88, 92],
                            name: '个人水平',
                            areaStyle: {
                                color: 'rgba(120, 186, 245, 0.3)'
                            },
                            lineStyle: {
                                color: '#409EFF'
                            },
                            itemStyle: {
                                color: '#409EFF'
                            }
                        },
                        {
                            value: [65, 70, 60, 80, 75, 78],
                            name: '班级平均',
                            areaStyle: {
                                color: 'rgba(255, 153, 153, 0.3)'
                            },
                            lineStyle: {
                                color: '#F56C6C'
                            },
                            itemStyle: {
                                color: '#F56C6C'
                            }
                        }
                    ]
                }
            ]
        };
        
        console.log('雷达图配置:', option);
        radarChartRef.value?.setOption(option);
        console.log('雷达图设置完成');
        
        // 响应式调整
        window.addEventListener('resize', () => {
            radarChartRef.value?.resize();
        });
    }
};

// 监听推荐内容变化，重新调整图表大小
watch(recommendedAction, () => {
    setTimeout(() => {
        radarChartRef.value?.resize();
    }, 100);
});
</script>

<template>
    <div class="ai-class-view">
            <div class="main">
                <div class="left-container">
                <div class="video-player-box">
                    <div class="video-info">
                            <div class="title-row">
                                <h1 class="video-title">{{ selectedReplay?.name }}</h1>
                                <button class="collect-btn" @click="toggleCollect">
                                    <img v-if="isCollected" src="@/assets/video_icon/AlreadyFavorite.svg" alt="已收藏" class="collect-icon" />
                                    <img v-else src="@/assets/video_icon/Favorite.svg" alt="收藏" class="collect-icon" />
                                    {{ isCollected ? '已收藏' : '收藏' }}
                                </button>
                            </div>
                            <div class="video-stats">
                                <span class="stat-item">{{ selectedReplay?.teacher }}</span>
                                <span class="stat-item">{{ selectedReplay?.date }}</span>
                            </div>

                        </div>
                        <div class="video-wrapper">
                            <div class="bilibili-player">
                                <video
                                    ref="videoRef"
                                    class="course-video"
                                    :poster="selectedReplay?.coverImageUrl"
                                    :src="selectedReplay?.videoUrl"
                                >
                                    <track kind="captions" src="" label="字幕" />
                                </video>
                                <div class="player-controls">
                                    <div class="progress-bar" @click="seekTo" @mousedown="startDrag">
                                        <div class="progress-filled" :style="{ width: progressPercentage + '%' }"></div>
                                        <div class="progress-handle" :style="{ left: progressPercentage + '%' }"></div>
                                    </div>
                                    <div class="control-row">
                                        <div class="left-controls">
                                            <button class="control-btn" @click="togglePlay">
                                                {{ isPlaying ? '⏸' : '▶' }}
                                            </button>
                                            <div class="time-info">
                                                {{ currentTime }} / {{ totalTime }}
                                            </div>
                                        </div>
                                        <div class="right-controls">
                                            <div class="speed-control">
                                                <button class="control-btn" @click="toggleSpeedMenu">
                                                    {{ playbackSpeed }}x
                                                </button>
                                                <div class="speed-menu" v-if="showSpeedMenu">
                                                    <button @click="setSpeed(0.5)">0.5x</button>
                                                    <button @click="setSpeed(0.75)">0.75x</button>
                                                    <button @click="setSpeed(1)">1x</button>
                                                    <button @click="setSpeed(1.25)">1.25x</button>
                                                    <button @click="setSpeed(1.5)">1.5x</button>
                                                    <button @click="setSpeed(2)">2x</button>
                                                </div>
                                            </div>
                                            <div class="volume-control">
                                                <button class="control-btn" @click="isMuted = !isMuted">
                                                    <img v-if="isMuted" src="@/assets/video_icon/voiceOff.svg" alt="静音" class="icon" />
                                                    <img v-else src="@/assets/video_icon/voiceOn.svg" alt="声音" class="icon" />
                                                </button>
                                                <div class="volume-slider-container">
                                                    <input
                                                        type="range"
                                                        min="0"
                                                        max="100"
                                                        v-model.number="volume"
                                                        @input="adjustVolume(volume)"
                                                        @mousedown="showVolumeValue = true"
                                                        @mouseup="showVolumeValue = false"
                                                        @mouseleave="showVolumeValue = false"
                                                    />
                                                    <div class="volume-value" v-if="showVolumeValue">{{ volume }}%</div>
                                                </div>
                                            </div>
                                            <button class="control-btn">
                                                ⛶
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>


            </div>
            <!-- 个人毽球数据雷达图 -->
            <div class="right-container">
                <div class="shuttlecock-radar-box">
                    <div class="radar-chart-container">
                        <div class="chart-header">
                            <h3>🎯 毽球运动技能评估</h3>
                            <div class="chart-legend">
                                <span><span class="legend-dot self"></span> 个人水平</span>
                                <span><span class="legend-dot avg"></span> 班级平均</span>
                            </div>
                        </div>
                        <div v-if="isLoadingRecommendation" class="loading-recommendation">
                            <div class="loading-spinner"></div>
                            <span>正在分析您的历史成绩...</span>
                        </div>
                        <div v-else-if="recommendedAction" class="recommendation-banner">
                            <div class="recommendation-icon">💡</div>
                            <div class="recommendation-content">
                                <div class="recommendation-title">AI推荐：{{ recommendedAction.action }}练习</div>
                                <div class="recommendation-score">综合评分：{{ recommendedAction.score }}分</div>
                                <div class="recommendation-reason">{{ recommendedAction.reason }}</div>
                            </div>
                        </div>
                        <div id="shuttlecock-radar-chart" class="radar-chart"></div>
                    </div>
                </div>

                <div class="task-box">
                    <div class="box-header">
                        <h4>本课任务</h4>
                    </div>
                    <div class="task-content">
                        <div class="task-item">
                            <div class="task-title">毽球基础踢法练习</div>
                            <div class="task-description">练习盘踢、绷踢、拐踢、磕踢、踏踢、跳踢等基础踢法，确保动作规范</div>
                            <div class="task-actions">
                                <span class="task-status">待完成</span>
                                <button class="task-btn" @click="goToMotionAnalysis">去完成</button>
                            </div>
                        </div>
                        <div class="task-item">
                            <div class="task-title">毽球错误动作纠正</div>
                            <div class="task-description">识别并纠正踢毽时的常见错误，如脚型不正确、发力方式错误等</div>
                            <div class="task-actions">
                                <span class="task-status">待完成</span>
                                <button class="task-btn" @click="goToMotionAnalysis">去完成</button>
                            </div>
                        </div>
                        <div class="task-item">
                            <div class="task-title">毽球实战技巧应用</div>
                            <div class="task-description">将所学踢法应用到实际对踢中，提高实战能力</div>
                            <div class="task-actions">
                                <span class="task-status">待完成</span>
                                <button class="task-btn" @click="goToMotionAnalysis">去完成</button>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="class-notes-box">
                    <div class="box-header">
                        <h4>课堂笔记</h4>
                        <Button size="small">保存笔记</Button>
                    </div>
                    <div class="notes-content">
                        <textarea
                            placeholder="在这里记录课堂笔记..."
                            rows="8"
                        ></textarea>
                    </div>
                </div>

                <div class="ai-analysis-box">
                    <div class="box-header">
                        <h4>AI分析</h4>
                    </div>
                    <div class="ai-analysis-content">
                        <AIPanel 
                            v-if="currentAIDataComputed"
                            :summarization="currentAIDataComputed.summarization"
                            :meetingAssistance="currentAIDataComputed.meetingAssistance"
                            :autoChapters="currentAIDataComputed.autoChapters"
                        />
                        <div v-else class="no-ai-data">
                            <p>暂无AI分析数据</p>
                            <span>请在AI学习伴侣中上传视频生成分析</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
.ai-class-view {
    display: flex;
    flex-direction: column;
    height: 100vh;
    overflow: hidden;
    background-color: #f3f5fb;

    .main {
        flex: 1;
        display: flex;
        flex-direction: row;
        overflow: hidden;

        .left-container {
            flex: 1;
            height: 100%;
            overflow-y: auto;
            padding: $gap;

            &::-webkit-scrollbar {
                display: none;
            }

            .video-player-box {
                border-radius: 16px;
                background-color: #fff;
                box-shadow: 0 4px 24px rgba(24, 144, 255, 0.1);
                overflow: hidden;

                .video-info {
                    padding: 16px 20px;
                    background: linear-gradient(180deg, rgba(24, 144, 255, 0.06) 0%, #fff 100%);
                    border-top: 1px solid #f0f0f0;

                    .title-row {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        margin-bottom: 12px;

                        .video-title {
                            font-size: 18px;
                            font-weight: 700;
                            margin: 0;
                            color: #333;
                            flex: 1;
                        }

                        .collect-btn {
                            background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                            border: none;
                            color: white;
                            font-size: 14px;
                            cursor: pointer;
                            padding: 8px 16px;
                            border-radius: 20px;
                            transition: all 0.3s;
                            white-space: nowrap;
                            display: flex;
                            align-items: center;
                            gap: 6px;
                            box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);

                            &:hover {
                                transform: translateY(-2px);
                                box-shadow: 0 6px 16px rgba(24, 144, 255, 0.4);
                            }

                            &:active {
                                transform: translateY(0);
                            }

                            .collect-icon {
                                width: 16px;
                                height: 16px;
                                vertical-align: middle;
                            }
                        }
                    }

                    .video-stats {
                        display: flex;
                        gap: 20px;

                        .stat-item {
                            font-size: 12px;
                            color: #666;
                            display: flex;
                            align-items: center;
                            gap: 5px;

                            .stat-icon {
                                width: 16px;
                                height: 16px;
                                vertical-align: middle;
                            }
                        }
                    }
                }

                .video-wrapper {
                    width: 100%;
                    height: 0;
                    padding-bottom: 56.25%;
                    position: relative;
                    background: #1a1a1a;

                    .bilibili-player {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;

                        video {
                            position: absolute;
                            top: 0;
                            left: 0;
                            width: 100%;
                            height: 100%;
                            object-fit: contain;
                            border-radius: 0;
                        }

                        .player-controls {
                            position: absolute;
                            bottom: 0;
                            left: 0;
                            right: 0;
                            background: linear-gradient(to top, rgba(0,0,0,0.8), transparent);
                            color: white;
                            padding: 15px;
                            display: flex;
                            flex-direction: column;
                            gap: 10px;

                            .progress-bar {
                                position: relative;
                                height: 6px;
                                background: rgba(255,255,255,0.3);
                                border-radius: 3px;
                                cursor: pointer;

                                .progress-filled {
                                    height: 100%;
                                    background: linear-gradient(90deg, #1890ff, #69c0ff);
                                    border-radius: 3px;
                                }

                                .progress-handle {
                                    position: absolute;
                                    top: 50%;
                                    left: 25%;
                                    transform: translate(-50%, -50%);
                                    width: 16px;
                                    height: 16px;
                                    background: white;
                                    border-radius: 50%;
                                    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.5);
                                    opacity: 0;
                                    transition: opacity 0.2s;
                                }

                                &:hover .progress-handle {
                                    opacity: 1;
                                }
                            }

                            .control-row {
                                display: flex;
                                align-items: center;
                                justify-content: space-between;

                                .control-btn {
                                    background: none;
                                    border: none;
                                    color: white;
                                    font-size: 18px;
                                    cursor: pointer;
                                    padding: 5px;
                                    border-radius: 4px;

                                    &:hover {
                                        background: rgba(255,255,255,0.2);
                                    }

                                    .icon {
                                        width: 20px;
                                        height: 20px;
                                        vertical-align: middle;
                                    }
                                }

                                .left-controls {
                                    display: flex;
                                    align-items: center;
                                    gap: 15px;

                                    .time-info {
                                        font-size: 14px;
                                        min-width: 120px;
                                    }
                                }

                                .right-controls {
                                    display: flex;
                                    align-items: center;
                                    gap: 15px;

                                    .speed-control {
                                        position: relative;

                                        .speed-menu {
                                            position: absolute;
                                            bottom: 100%;
                                            left: 0;
                                            background: rgba(0,0,0,0.9);
                                            border-radius: 4px;
                                            padding: 5px 0;
                                            z-index: 100;
                                            min-width: 80px;

                                            button {
                                                display: block;
                                                width: 100%;
                                                background: none;
                                                border: none;
                                                color: white;
                                                padding: 8px 15px;
                                                text-align: left;
                                                cursor: pointer;

                                                &:hover {
                                                    background: rgba(255,255,255,0.2);
                                                }

                                                &:first-child {
                                                    border-radius: 4px 4px 0 0;
                                                }

                                                &:last-child {
                                                    border-radius: 0 0 4px 4px;
                                                }
                                            }
                                        }
                                    }

                                    .volume-control {
                                        display: flex;
                                        align-items: center;
                                        gap: 5px;

                                        .volume-slider-container {
                                            position: relative;

                                            input[type="range"] {
                                                width: 80px;
                                                height: 4px;
                                                background: rgba(255,255,255,0.3);
                                                border-radius: 2px;
                                                outline: none;
                                                -webkit-appearance: none;

                                                &::-webkit-slider-thumb {
                                                    -webkit-appearance: none;
                                                    appearance: none;
                                                    width: 12px;
                                                    height: 12px;
                                                    background: #1E88E5;
                                                    border-radius: 50%;
                                                    cursor: pointer;
                                                }

                                                &::-moz-range-thumb {
                                                    width: 12px;
                                                    height: 12px;
                                                    background: #1E88E5;
                                                    border-radius: 50%;
                                                    cursor: pointer;
                                                    border: none;
                                                }

                                                &::-webkit-slider-runnable-track {
                                                    height: 4px;
                                                    background: linear-gradient(to right, #1E88E5 0%, #1E88E5 var(--volume-percentage, 80%), rgba(255,255,255,0.3) var(--volume-percentage, 80%), rgba(255,255,255,0.3) 100%);
                                                    border-radius: 2px;
                                                }

                                                &::-moz-range-track {
                                                    height: 4px;
                                                    background: linear-gradient(to right, #1E88E5 0%, #1E88E5 var(--volume-percentage, 80%), rgba(255,255,255,0.3) var(--volume-percentage, 80%), rgba(255,255,255,0.3) 100%);
                                                    border-radius: 2px;
                                                }
                                            }

                                            .volume-value {
                                                position: absolute;
                                                top: -30px;
                                                left: 50%;
                                                transform: translateX(-50%);
                                                background: rgba(0,0,0,0.8);
                                                color: white;
                                                padding: 4px 8px;
                                                border-radius: 4px;
                                                font-size: 12px;
                                                white-space: nowrap;

                                                &::after {
                                                    content: '';
                                                    position: absolute;
                                                    top: 100%;
                                                    left: 50%;
                                                    transform: translateX(-50%);
                                                    border-width: 4px;
                                                    border-style: solid;
                                                    border-color: rgba(0,0,0,0.8) transparent transparent transparent;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            .ai-assistant-box {
                border-radius: $border-radius;
                background-color: #fff;
                box-shadow: $box-shadow;
                overflow: hidden;

                .box-header {
                    border-bottom: 0.1mm solid #d1d1d1;
                    padding: 15px 20px;
                    display: flex;
                    align-items: center;

                    h4 {
                        margin: 0;
                    }
                }
            }
        }

        .right-container {
            width: 440px;
            height: 100%;
            overflow-y: auto;
            padding: $gap;
            flex-shrink: 0;

            &::-webkit-scrollbar {
                display: none;
            }

            .box {
                border-radius: $border-radius;
                background-color: #fff;
                box-shadow: $box-shadow;
                overflow: hidden;
                margin-bottom: $gap;

                .box-header {
                    border-bottom: 0.1mm solid #d1d1d1;
                    padding: 15px 20px;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;

                    h4 {
                        margin: 0;
                    }
                }
            }

            .class-info-box {
                .class-info-content {
                    padding: 20px;

                    .info-item {
                        margin-bottom: 15px;
                        display: flex;

                        .label {
                            font-weight: bold;
                            width: 100px;
                            color: #666;
                        }

                        .value {
                            flex: 1;
                        }
                    }
                }
            }

            .class-materials-box {
                .materials-list {
                    padding: 20px;

                    .material-item {
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        padding: 10px 0;
                        border-bottom: 0.1mm solid #f0f0f0;

                        &:last-child {
                            border-bottom: none;
                        }

                        .material-icon {
                            font-size: 1.2rem;
                            margin-right: 10px;
                        }

                        .material-name {
                            flex: 1;
                        }
                    }
                }
            }

            .shuttlecock-radar-box {
                border-radius: $border-radius;
                background-color: #fff;
                box-shadow: $box-shadow;
                overflow: hidden;
                margin-bottom: $gap;

                .box-header {
                    border-bottom: 0.1mm solid #d1d1d1;
                    padding: 15px 20px;
                    display: flex;
                    align-items: center;

                    h4 {
                        margin: 0;
                    }
                }

                .radar-chart-container {
                    padding: 20px;
                    min-height: 350px;
                    display: flex;
                    flex-direction: column;

                    .chart-header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        margin-bottom: 16px;
                        flex-shrink: 0;
                    }

                    .chart-header h3 {
                        font-size: 16px;
                        font-weight: bold;
                        color: #333;
                        margin: 0;
                    }

                    .radar-chart {
                        width: 100%;
                        flex: 1;
                        min-height: 200px;
                    }

                    .chart-legend {
                        display: flex;
                        gap: 16px;
                        font-size: 14px;
                        color: #666;
                        flex-shrink: 0;
                        margin-top: 12px;
                    }

                    .legend-dot {
                        display: inline-block;
                        width: 10px;
                        height: 10px;
                        border-radius: 50%;
                        margin-right: 6px;
                        vertical-align: middle;
                    }

                    .legend-dot.self {
                        background-color: #409EFF;
                    }

                    .legend-dot.avg {
                        background-color: #F56C6C;
                    }

                    .loading-recommendation {
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        height: 80px;
                        margin-bottom: 16px;

                        .loading-spinner {
                            width: 32px;
                            height: 32px;
                            border: 3px solid rgba(24, 144, 255, 0.2);
                            border-top-color: #1890ff;
                            border-radius: 50%;
                            animation: spin 1s linear infinite;
                            margin-bottom: 12px;
                        }

                        span {
                            font-size: 13px;
                            color: #666;
                        }
                    }

                    .recommendation-banner {
                        display: flex;
                        align-items: flex-start;
                        gap: 12px;
                        padding: 16px;
                        background: linear-gradient(135deg, rgba(24, 144, 255, 0.08) 0%, rgba(24, 144, 255, 0.02) 100%);
                        border: 1px solid rgba(24, 144, 255, 0.2);
                        border-radius: 12px;
                        margin-bottom: 16px;

                        .recommendation-icon {
                            font-size: 24px;
                            line-height: 1;
                        }

                        .recommendation-content {
                            flex: 1;

                            .recommendation-title {
                                font-size: 15px;
                                font-weight: 600;
                                color: #333;
                                margin-bottom: 6px;
                            }

                            .recommendation-score {
                                font-size: 13px;
                                color: #1890ff;
                                font-weight: 500;
                                margin-bottom: 4px;
                            }

                            .recommendation-reason {
                                font-size: 12px;
                                color: #666;
                                line-height: 1.4;
                            }
                        }
                    }

                    @keyframes spin {
                        to {
                            transform: rotate(360deg);
                        }
                    }
                }
            }

            .task-box {
                border-radius: $border-radius;
                background-color: #fff;
                box-shadow: $box-shadow;
                overflow: hidden;
                margin-bottom: $gap;

                .box-header {
                    border-bottom: 0.1mm solid #d1d1d1;
                    padding: 15px 20px;
                    display: flex;
                    align-items: center;

                    h4 {
                        margin: 0;
                    }
                }

                .task-content {
                    padding: 15px;

                    .task-item {
                        padding: 12px;
                        margin-bottom: 10px;
                        border: 1px solid #e8e8e8;
                        border-radius: 8px;
                        transition: all 0.3s;

                        &:hover {
                            border-color: #1E88E5;
                            box-shadow: 0 2px 4px rgba(30, 136, 229, 0.1);
                        }

                        &:last-child {
                            margin-bottom: 0;
                        }

                        .task-title {
                            font-weight: bold;
                            margin-bottom: 5px;
                            font-size: 0.9rem;
                            color: #333;
                        }

                        .task-description {
                            font-size: 0.8rem;
                            color: #666;
                            margin-bottom: 12px;
                            line-height: 1.4;
                        }

                        .task-actions {
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                        }

                        .task-status {
                            font-size: 0.75rem;
                            color: #1E88E5;
                            font-weight: 500;
                        }

                        .task-btn {
                            background: #1E88E5;
                            border: none;
                            color: white;
                            font-size: 0.75rem;
                            cursor: pointer;
                            padding: 4px 12px;
                            border-radius: 4px;
                            transition: all 0.3s;

                            &:hover {
                                background: #1565C0;
                            }

                            &:active {
                                transform: translateY(1px);
                            }
                        }
                    }
                }
            }

            .class-notes-box {
                border-radius: $border-radius;
                background-color: #fff;
                box-shadow: $box-shadow;
                overflow: hidden;
                margin-bottom: $gap;

                .box-header {
                    border-bottom: 0.1mm solid #d1d1d1;
                    padding: 15px 20px;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;

                    h4 {
                        margin: 0;
                    }
                }

                .notes-content {
                    padding: 20px;

                    textarea {
                        width: 100%;
                        border: 1px solid #e8e8e8;
                        border-radius: 4px;
                        padding: 10px;
                        resize: vertical;
                        font-size: 14px;
                        line-height: 1.5;

                        &:focus {
                            outline: none;
                            border-color: $blue-5;
                        }
                    }
                }
            }

            .ai-analysis-box {
                border-radius: $border-radius;
                background-color: #fff;
                box-shadow: $box-shadow;
                overflow: hidden;
                margin-bottom: $gap;

                .box-header {
                    border-bottom: 0.1mm solid #d1d1d1;
                    padding: 15px 20px;
                    display: flex;
                    align-items: center;

                    h4 {
                        margin: 0;
                    }
                }

                .ai-analysis-content {
                    padding: 15px;

                    .no-ai-data {
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        height: 200px;
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
                }
            }
        }
    }
}

@keyframes pulse {
    0% {
        opacity: 1;
    }
    50% {
        opacity: 0.5;
    }
    100% {
        opacity: 1;
    }
}
</style>

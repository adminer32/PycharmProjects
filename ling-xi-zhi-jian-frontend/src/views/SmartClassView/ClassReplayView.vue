<script setup lang="ts">
/**
 * 课堂回放
 */
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAiNoteStore } from '@/stores/aiLearnFriendStore';
import { useAiAnalysisCacheStore } from '@/stores/aiAnalysisCacheStore';
import AIPanel from './components/AIPanel.vue';
import { getTeacherVideosApi, type TeacherVideo } from '@/api/smart_class/teacherVideoApi';
import { ElMessage } from 'element-plus';

const router = useRouter();
const aiNoteStore = useAiNoteStore();
const aiAnalysisCacheStore = useAiAnalysisCacheStore();

const videoRef = ref<HTMLVideoElement | null>(null);

const goToMotionAnalysis = () => {
    router.push({ path: '/motion_assessment', query: { tab: 'motionAnalysis' } });
};

const selectedReplay = ref<TeacherVideo>({
    id: '',
    name: '',
    videoUrl: '',
    coverImageUrl: '',
    duration: '',
    category: '',
    description: '',
    teacherId: 0,
    teacherName: '',
    classId: 0,
    date: ''
});

const replayList = ref<TeacherVideo[]>([]);

const loadVideos = async () => {
    try {
        const res = await getTeacherVideosApi();
        if (res.status === 'success' && res.data) {
            replayList.value = res.data;
            if (res.data.length > 0) {
                selectedReplay.value = res.data[0];
                // 将秒数转换为 MM:SS 格式
                totalTime.value = formatDuration(res.data[0].duration);
            }
        }
    } catch (error) {
        console.error('加载视频列表失败:', error);
    }
};

// 格式化时长（秒 -> MM:SS）
const formatDuration = (durationStr: string): string => {
    if (!durationStr) return '00:00';
    // 如果已经是 MM:SS 格式，直接返回
    if (durationStr.includes(':')) return durationStr;
    // 提取数字
    const seconds = parseInt(durationStr.replace(/[^0-9]/g, '')) || 0;
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
};

const currentAIData = ref<{ summarization: string; meetingAssistance: string; autoChapters: string } | null>(null);

const currentAIDataComputed = computed(() => {
    if (!selectedReplay.value) return null;
    const videoPath = selectedReplay.value.videoUrl;
    const cached = aiAnalysisCacheStore.getCache(videoPath);
    if (cached && cached.taskStatus === '已完成') {
        return {
            summarization: cached.summarization,
            meetingAssistance: cached.meetingAssistance,
            autoChapters: cached.autoChapters,
        };
    }
    const videoPath2 = selectedReplay.value.videoUrl.replace('/videos/', '');
    const aiNote = aiNoteStore.historyList.find(
        (note) => note.videoUrl && note.videoUrl.includes(videoPath2)
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
    totalTime.value = formatDuration(replay.duration);
    currentTime.value = '00:00';
    progressPercentage.value = 0;
    isPlaying.value = false;
    isCollected.value = false;
    if (videoRef.value) {
        videoRef.value.pause();
        videoRef.value.currentTime = 0;
    }
    
    const videoPath = replay.videoUrl;
    const cached = aiAnalysisCacheStore.getCache(videoPath);
    if (cached && cached.taskStatus === '已完成') {
        currentAIData.value = {
            summarization: cached.summarization,
            meetingAssistance: cached.meetingAssistance,
            autoChapters: cached.autoChapters,
        };
        return;
    }
    
    const videoPath2 = replay.videoUrl.replace('/videos/', '');
    const aiNote = aiNoteStore.historyList.find(
        (note) => note.videoUrl && note.videoUrl.includes(videoPath2)
    );
    if (aiNote && aiNote.taskStatus === '已完成') {
        currentAIData.value = {
            summarization: aiNote.summarization,
            meetingAssistance: aiNote.meetingAssistance,
            autoChapters: aiNote.autoChapters,
        };
    } else {
        currentAIData.value = null;
        cacheCurrentVideoAI();
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

onMounted(() => {
    // 初始化课堂回放
    console.log('课堂回放初始化');
    
    // 初始化AI分析缓存
    aiAnalysisCacheStore.init();
    
    // 加载教师视频列表
    loadVideos();
    
    // 加载AI笔记历史
    aiNoteStore.getHistoryList();
    
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

const toggleCollect = () => {
    isCollected.value = !isCollected.value;
};

const isGeneratingCache = ref(false);

const preGenerateCache = async () => {
    if (isGeneratingCache.value) return;
    isGeneratingCache.value = true;
    ElMessage.info('正在预生成AI分析数据，请稍候...');
    try {
        for (const video of replayList.value) {
            const videoPath = video.videoUrl;
            if (!aiAnalysisCacheStore.getCache(videoPath)) {
                await aiAnalysisCacheStore.triggerAnalysis(videoPath, video.videoUrl);
            }
        }
        ElMessage.success('AI分析数据缓存完成');
        if (selectedReplay.value) {
            const videoPath = selectedReplay.value.videoUrl;
            const cached = aiAnalysisCacheStore.getCache(videoPath);
            if (cached) {
                currentAIData.value = {
                    summarization: cached.summarization,
                    meetingAssistance: cached.meetingAssistance,
                    autoChapters: cached.autoChapters,
                };
            }
        }
    } catch (e) {
        ElMessage.error('缓存生成失败');
    } finally {
        isGeneratingCache.value = false;
    }
};

const cacheCurrentVideoAI = async () => {
    if (!selectedReplay.value) return;
    const videoPath = selectedReplay.value.videoUrl;
    if (aiAnalysisCacheStore.getCache(videoPath)) return;
    try {
        await aiAnalysisCacheStore.triggerAnalysis(videoPath, selectedReplay.value.videoUrl);
    } catch (e) {
        console.error('Failed to cache current video AI data:', e);
    }
};
</script>

<template>
    <div class="class-replay-view">
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
                            <span class="stat-item">{{ selectedReplay?.teacherName }}</span>
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
                

                
                <div class="ai-summary-box">
                    <div class="ai-panel-header" v-if="!currentAIDataComputed">
                        <button 
                            class="cache-btn" 
                            @click="preGenerateCache"
                            :disabled="isGeneratingCache"
                        >
                            <span v-if="isGeneratingCache">生成中...</span>
                            <span v-else>预生成AI分析数据</span>
                        </button>
                    </div>
                    <AIPanel 
                        v-if="currentAIDataComputed"
                        :summarization="currentAIDataComputed.summarization"
                        :meetingAssistance="currentAIDataComputed.meetingAssistance"
                        :autoChapters="currentAIDataComputed.autoChapters"
                    />
                    <div v-else-if="!isGeneratingCache" class="no-ai-data">
                        <p>暂无AI分析数据</p>
                        <span>点击上方按钮预生成AI分析数据</span>
                    </div>
                </div>
            </div>
            
            <div class="right-container">
                <div class="task-box">
                    <div class="box-header">
                        <h4>本课任务</h4>
                    </div>
                    <div class="task-content">
                        <div class="task-item">
                            <div class="task-title">毽球基础踢法练习</div>
                            <div class="task-description">练习内踢、外踢、正脚背踢等基础踢法，确保动作规范</div>
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
                <div class="replay-list-box">
                    <div class="box-header">
                        <h4>视频列表</h4>
                        <span class="video-count">{{ replayList.length }} 个视频</span>
                    </div>
                    <div class="replay-list">
                        <div 
                            v-for="replay in replayList" 
                            :key="replay.id"
                            class="replay-item"
                            :class="{ selected: selectedReplay.id === replay.id }"
                            @click="selectReplay(replay)"
                        >
                            <div class="replay-cover">
                                <div class="cover-placeholder">
                                    <span class="play-icon">▶</span>
                                </div>
                                <div class="replay-duration-badge">{{ formatDuration(replay.duration) }}</div>
                            </div>
                            <div class="replay-info">
                                <div class="replay-title">{{ replay.name }}</div>
                                <div class="replay-meta">
                                    <span class="teacher-tag">
                                        <span class="teacher-icon">👨‍🏫</span>
                                        {{ replay.teacherName }}
                                    </span>
                                </div>
                                <div class="replay-date">{{ replay.date }}</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
.class-replay-view {
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
                max-height: calc(100vh - 60px); // 考虑顶部导航栏高度

                &::-webkit-scrollbar {
                    width: 8px;
                    height: 8px;
                }

                &::-webkit-scrollbar-track {
                    background: #f1f1f1;
                    border-radius: 4px;
                }

                &::-webkit-scrollbar-thumb {
                    background: #c1c1c1;
                    border-radius: 4px;
                }

                &::-webkit-scrollbar-thumb:hover {
                    background: #a8a8a8;
                }

                .video-player-box {
                    border-radius: 16px;
                    background-color: #fff;
                    box-shadow: 0 4px 24px rgba(24, 144, 255, 0.1);
                    overflow: hidden;

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
                        margin-bottom: 20px;

                        .stat-item {
                            font-size: 0.9rem;
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
            }



            .ai-summary-box {
                border-radius: $border-radius;
                background-color: #fff;
                box-shadow: $box-shadow;
                overflow: hidden;
                min-height: 200px;

                .ai-panel-header {
                    padding: 16px;
                    display: flex;
                    justify-content: center;

                    .cache-btn {
                        background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                        border: none;
                        color: white;
                        font-size: 14px;
                        cursor: pointer;
                        padding: 10px 20px;
                        border-radius: 20px;
                        transition: all 0.3s;
                        box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);

                        &:hover:not(:disabled) {
                            transform: translateY(-2px);
                            box-shadow: 0 6px 16px rgba(24, 144, 255, 0.4);
                        }

                        &:disabled {
                            opacity: 0.6;
                            cursor: not-allowed;
                        }
                    }
                }

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

        .right-container {
            width: 440px;
            height: 100%;
            overflow-y: auto;
            padding: $gap;
            max-height: calc(100vh - 60px);

            &::-webkit-scrollbar {
                width: 8px;
                height: 8px;
            }

            &::-webkit-scrollbar-track {
                background: #f1f1f1;
                border-radius: 4px;
            }

            &::-webkit-scrollbar-thumb {
                background: #c1c1c1;
                border-radius: 4px;
            }

            &::-webkit-scrollbar-thumb:hover {
                background: #a8a8a8;
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

            .replay-list-box {
                    border-radius: 16px;
                    background-color: #fff;
                    box-shadow: 0 4px 24px rgba(24, 144, 255, 0.1);
                    overflow: hidden;

                    .box-header {
                        padding: 16px 20px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        border-bottom: 1px solid #f0f0f0;

                        h4 {
                            margin: 0;
                            font-size: 15px;
                            font-weight: 600;
                            color: #333;
                        }

                        .video-count {
                            font-size: 12px;
                            color: #999;
                            background: #f5f5f5;
                            padding: 4px 10px;
                            border-radius: 12px;
                        }
                    }

                    .replay-list {
                        padding: 12px;
                        max-height: 500px;
                        overflow-y: auto;

                        &::-webkit-scrollbar {
                            width: 6px;
                            height: 6px;
                        }

                        &::-webkit-scrollbar-track {
                            background: #f1f1f1;
                            border-radius: 3px;
                        }

                        &::-webkit-scrollbar-thumb {
                            background: #c1c1c1;
                            border-radius: 3px;
                        }

                        &::-webkit-scrollbar-thumb:hover {
                            background: #a8a8a8;
                        }

                        .replay-item {
                            display: flex;
                            flex-direction: row;
                            padding: 12px;
                            margin-bottom: 10px;
                            border-radius: 12px;
                            cursor: pointer;
                            transition: all 0.3s;
                            border: 1px solid transparent;

                            &:hover {
                                background: linear-gradient(135deg, rgba(24, 144, 255, 0.04) 0%, rgba(24, 144, 255, 0.08) 100%);
                                border-color: rgba(24, 144, 255, 0.2);
                            }

                            &.selected {
                                background: linear-gradient(135deg, rgba(24, 144, 255, 0.1) 0%, rgba(24, 144, 255, 0.15) 100%);
                                border-color: rgba(24, 144, 255, 0.3);
                                box-shadow: 0 2px 12px rgba(24, 144, 255, 0.15);
                            }

                            .replay-cover {
                                position: relative;
                                width: 100px;
                                height: 65px;
                                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                                border-radius: 10px;
                                overflow: hidden;
                                margin-right: 12px;
                                flex-shrink: 0;

                                .cover-placeholder {
                                    width: 100%;
                                    height: 100%;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;

                                    .play-icon {
                                        font-size: 20px;
                                        color: rgba(255, 255, 255, 0.9);
                                    }
                                }

                                .replay-duration-badge {
                                    position: absolute;
                                    bottom: 4px;
                                    right: 4px;
                                    padding: 2px 6px;
                                    background: rgba(0, 0, 0, 0.65);
                                    color: white;
                                    font-size: 11px;
                                    font-weight: 500;
                                    border-radius: 4px;
                                }
                            }

                            .replay-info {
                                flex: 1;
                                display: flex;
                                flex-direction: column;
                                justify-content: center;
                                min-width: 0;

                                .replay-title {
                                    font-weight: 600;
                                    margin-bottom: 6px;
                                    font-size: 14px;
                                    color: #333;
                                    white-space: nowrap;
                                    overflow: hidden;
                                    text-overflow: ellipsis;
                                }

                                .replay-meta {
                                    display: flex;
                                    align-items: center;
                                    margin-bottom: 4px;

                                    .teacher-tag {
                                        display: inline-flex;
                                        align-items: center;
                                        gap: 4px;
                                        font-size: 12px;
                                        color: #1890ff;
                                        background: rgba(24, 144, 255, 0.08);
                                        padding: 3px 8px;
                                        border-radius: 10px;

                                        .teacher-icon {
                                            font-size: 12px;
                                        }
                                    }
                                }

                                .replay-date {
                                    font-size: 12px;
                                    color: #999;
                                }
                            }
                        }
                    }
                }
        }
    }
}
</style>

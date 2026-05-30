<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'

interface ClassAnalysis {
  attendanceRate: number
  participationRate: number
  activeStudents: number
  totalStudents: number
  averageDuration: string
  peakTime: string
}

interface NoteItem {
  id: number
  text: string
}

const props = defineProps<{
  videoUrl: string
  videoTitle: string
  thumbnail: string
  notes?: NoteItem[]
  classAnalysis?: ClassAnalysis
}>()

const emit = defineEmits<{
  (e: 'analyze'): void
  (e: 'screenshot', dataUrl: string): void
  (e: 'timeupdate', time: number): void
  (e: 'add-note'): void
  (e: 'remove-note', id: number): void
  (e: 'update-note', id: number, text: string): void
  (e: 'save-notes'): void
  (e: 'add-ai-note', text: string): void
  (e: 'restore-video'): void
}>()

const defaultAnalysis: ClassAnalysis = {
  attendanceRate: 0,
  participationRate: 0,
  activeStudents: 0,
  totalStudents: 30,
  averageDuration: '0:00',
  peakTime: '00:00'
}

const analysis = ref<ClassAnalysis>({
  ...defaultAnalysis,
  ...props.classAnalysis
})

watch(() => props.classAnalysis, (newVal) => {
  if (newVal) {
    analysis.value = { ...defaultAnalysis, ...newVal }
  }
}, { deep: true })

// 当前选中的 Tab
const activeTab = ref('notes') // 'notes' 或 'analysis'

// 课堂笔记数据（从父组件传入）
const classNotes = ref<NoteItem[]>(props.notes || [])

watch(() => props.notes, (newNotes) => {
  if (newNotes) {
    classNotes.value = [...newNotes]
  }
}, { deep: true })

function addNote() {
  emit('add-note')
}

function removeNote(id: number) {
  if (classNotes.value.length > 1) {
    emit('remove-note', id)
  }
}

function updateNoteText(id: number, text: string) {
  emit('update-note', id, text)
}

const videoRef = ref<HTMLVideoElement | null>(null)
const isPlaying = ref(false)
const isAnalyzing = ref(false)
const analyzeLoading = ref(false)
const aiFrameUrl = ref('')
const videoError = ref(false)
const originalVideoUrl = ref('')
const currentTime = ref(0)
const duration = ref(0)
const isDragging = ref(false)
const currentSpeed = ref(1.0)
const isFullscreen = ref(false)
const showControls = ref(true)
let controlsTimer: ReturnType<typeof setTimeout> | null = null

const speeds = [0.5, 0.75, 1.0, 1.25, 1.5, 2.0]

const formattedCurrentTime = computed(() => formatTime(currentTime.value))
const formattedDuration = computed(() => formatTime(duration.value))
const progress = computed(() => {
  if (duration.value === 0) return 0
  return (currentTime.value / duration.value) * 100
})

const videoKey = ref(0)

const currentVideoUrl = computed(() => {
  if (isAnalyzing.value) {
    return '/api/public/file/abcde.mp4'
  }
  return props.videoUrl || ''
})

function formatTime(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
}

function togglePlay() {
  if (!videoRef.value) return
  if (isPlaying.value) {
    videoRef.value.pause()
  } else {
    videoRef.value.play()
  }
}

function handlePlay() {
  isPlaying.value = true
}

function handlePause() {
  isPlaying.value = false
}

function handleTimeUpdate() {
  if (!videoRef.value || isDragging.value) return
  currentTime.value = videoRef.value.currentTime
  emit('timeupdate', currentTime.value)
}

function handleLoadedMetadata() {
  if (!videoRef.value) return
  duration.value = videoRef.value.duration
}

function handleVideoEnded() {
  isPlaying.value = false
}

function handleVideoError(e: any) {
  console.error('视频加载失败:', currentVideoUrl.value)
  const errorCodes: Record<number, string> = {
    1: 'MEDIA_ERR_ABORTED - 加载中止',
    2: 'MEDIA_ERR_NETWORK - 网络错误',
    3: 'MEDIA_ERR_DECODE - 解码错误',
    4: 'MEDIA_ERR_SRC_NOT_SUPPORTED - 格式不支持'
  }
  const errorCode = videoRef.value?.error?.code || 0
  console.error('错误代码:', errorCode, errorCodes[errorCode] || '未知错误')
  console.error('错误详情:', videoRef.value?.error)
  
  videoError.value = true
}

async function retryLoadVideo() {
  console.log('重新加载视频...')
  videoError.value = false
  videoKey.value++
  
  await nextTick()
  await new Promise(resolve => setTimeout(resolve, 100))
  
  await nextTick()
  if (videoRef.value) {
    videoRef.value.load()
    console.log('视频重新加载已触发, URL:', currentVideoUrl.value)
  }
}

function setSpeed(speed: number) {
  if (!videoRef.value) return
  currentSpeed.value = speed
  videoRef.value.playbackRate = speed
}

function setSlowMotion() {
  setSpeed(0.25)
}

function handleProgressClick(e: MouseEvent) {
  if (!videoRef.value) return
  const target = e.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const percent = (e.clientX - rect.left) / rect.width
  const newTime = percent * duration.value
  videoRef.value.currentTime = newTime
  currentTime.value = newTime
}

function handleProgressDrag(e: MouseEvent) {
  if (!isDragging.value || !videoRef.value) return
  const target = document.querySelector('.progress-bar-container')
  if (!target) return
  const rect = target.getBoundingClientRect()
  const percent = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  const newTime = percent * duration.value
  currentTime.value = newTime
  videoRef.value.currentTime = newTime
}

function startDrag() {
  isDragging.value = true
}

function endDrag() {
  isDragging.value = false
}

function handleScreenshot() {
  if (!videoRef.value) return
  const canvas = document.createElement('canvas')
  canvas.width = videoRef.value.videoWidth
  canvas.height = videoRef.value.videoHeight
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.drawImage(videoRef.value, 0, 0, canvas.width, canvas.height)
  const dataUrl = canvas.toDataURL('image/png')
  emit('screenshot', dataUrl)
}

function handleAnalyze() {
  if (isAnalyzing.value) {
    stopAnalysis()
    return
  }

  if (isPlaying.value && videoRef.value) {
    videoRef.value.pause()
  }

  originalVideoUrl.value = props.videoUrl || ''
  isAnalyzing.value = true
  analyzeLoading.value = true
  videoError.value = false

  console.log('=== 开始分析 ===')
  console.log('原始视频:', originalVideoUrl.value)
  console.log('分析视频: /api/public/file/abcde.mp4')

  setTimeout(async () => {
    analyzeLoading.value = false
    activeTab.value = 'analysis'
    videoError.value = false

    analysis.value = {
      totalStudents: 10,
      activeStudents: 9,
      attendanceRate: 90,
      participationRate: Math.floor(80 + Math.random() * 11),
      averageDuration: '45:30',
      peakTime: '12:15'
    }

    console.log('=== 分析加载完成 ===')
    console.log('当前视频 URL:', currentVideoUrl.value)
    console.log('isAnalyzing:', isAnalyzing.value)

    await nextTick()
    console.log('nextTick 后 videoRef:', !!videoRef.value)

    if (videoRef.value) {
      videoRef.value.load()
      const playPromise = videoRef.value.play()
      if (playPromise) {
        playPromise.catch((err: any) => {
          console.error('视频播放失败:', err.name, err.message)
        })
      }
    }
  }, 1500)
}

function stopAnalysis() {
  const wasAnalyzing = isAnalyzing.value
  isAnalyzing.value = false
  analyzeLoading.value = false
  videoError.value = false

  if (wasAnalyzing) {
    activeTab.value = 'notes'

    const aiNoteText = `AI总结：本节课出勤人数 ${analysis.value.activeStudents}人，平均到课率 ${analysis.value.attendanceRate}%，参与度 ${analysis.value.participationRate}%。`
    emit('add-ai-note', aiNoteText)
    emit('restore-video')
  }
}

function toggleFullscreen() {
  const container = document.querySelector('.lesson-video-container')
  if (!container) return

  if (!document.fullscreenElement) {
    (container as HTMLElement).requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

function handleFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

function showControlsTemporarily() {
  showControls.value = true
  if (controlsTimer) {
    clearTimeout(controlsTimer)
  }
  controlsTimer = setTimeout(() => {
    if (isPlaying.value) {
      showControls.value = false
    }
  }, 3000)
}

function handleMouseMove() {
  showControlsTemporarily()
}

function handleMouseLeave() {
  // 保持控件始终可见，避免页面抽搐
  // 只在视频暂停且鼠标移出时才隐藏
  if (!isPlaying.value && !isDragging.value) {
    showControls.value = false
  }
}

onMounted(() => {
  document.addEventListener('mousemove', handleProgressDrag)
  document.addEventListener('mouseup', endDrag)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  
  // 验证视频文件是否存在（通过后端API）
  fetch('/api/public/file/aaaaa.mp4', { method: 'HEAD' })
    .then(response => {
      if (response.ok) {
        console.log('✓ 预处理视频文件存在（后端）')
      } else {
        console.error('✗ 预处理视频文件不存在:', response.status)
      }
    })
    .catch(err => {
      console.error('✗ 无法访问预处理视频文件:', err)
    })
})

onUnmounted(() => {
  stopAnalysis()
  document.removeEventListener('mousemove', handleProgressDrag)
  document.removeEventListener('mouseup', endDrag)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  if (controlsTimer) {
    clearTimeout(controlsTimer)
  }
})

watch(() => props.videoUrl, async (newUrl, oldUrl) => {
  console.log('videoUrl 变化:', oldUrl, '->', newUrl)
  
  videoError.value = false
  isAnalyzing.value = false
  analyzeLoading.value = false
  currentTime.value = 0
  isPlaying.value = false
  
  if (newUrl && !isAnalyzing.value) {
    originalVideoUrl.value = newUrl
  }
  
  videoKey.value++
  console.log('videoKey 更新为:', videoKey.value, '等待 DOM 更新...')
  
  await nextTick()
  await new Promise(resolve => setTimeout(resolve, 50))
  await nextTick()
  
  console.log('DOM 更新完成, videoRef 存在:', !!videoRef.value)
  
  if (videoRef.value) {
    videoRef.value.load()
    console.log('视频加载已触发, URL:', currentVideoUrl.value)
  }
})

watch(isAnalyzing, async (newVal) => {
  console.log('isAnalyzing 变化:', newVal)
  videoError.value = false
  videoKey.value++
  
  await nextTick()
  await new Promise(resolve => setTimeout(resolve, 50))
  await nextTick()
  
  console.log('watch isAnalyzing 触发，新值:', newVal, '视频URL:', currentVideoUrl.value)
  
  if (videoRef.value) {
    videoRef.value.load()
    if (newVal) {
      videoRef.value.play().catch((e: any) => {
        console.error('视频播放失败:', e)
      })
    }
  }
})
</script>

<template>
  <div
    class="lesson-video-container"
    :class="{ fullscreen: isFullscreen }"
    @mousemove="handleMouseMove"
    @mouseleave="handleMouseLeave"
  >
    <div class="video-header">
      <h3 class="video-title">{{ videoTitle }}</h3>
    </div>

    <div class="video-wrapper">
      <video
        v-show="currentVideoUrl && !videoError"
        :key="'video-' + videoKey"
        ref="videoRef"
        class="video-player"
        :src="currentVideoUrl || undefined"
        :poster="thumbnail"
        @play="handlePlay"
        @pause="handlePause"
        @timeupdate="handleTimeUpdate"
        @loadedmetadata="handleLoadedMetadata"
        @ended="handleVideoEnded"
        @error="handleVideoError"
        @click="togglePlay"
        preload="auto"
      >
        您的浏览器不支持视频播放
      </video>

      <div v-show="videoError" class="video-error-state">
        <div class="error-icon">⚠️</div>
        <p class="error-text">视频加载失败</p>
        <p class="error-hint">{{ currentVideoUrl }}</p>
        <button class="retry-btn" @click="retryLoadVideo">
          🔄 重新加载
        </button>
      </div>

      <div v-show="!currentVideoUrl && !videoError" class="no-video-placeholder">
        <div class="no-video-icon">🎬</div>
        <p class="no-video-text">暂无课程回放视频</p>
        <p class="no-video-hint">该课程尚未上传录制视频</p>
      </div>

      <div v-if="analyzeLoading" class="ai-monitor-window">
        <div class="ai-loading">
          <span>AI模型加载中...</span>
        </div>
      </div>

      <div v-show="currentVideoUrl && !videoError" class="video-overlay" :class="{ hidden: !showControls }">
        <div class="play-button-large" @click="togglePlay">
          <span v-if="!isPlaying">▶</span>
          <span v-else>⏸</span>
        </div>
      </div>
    </div>

    <div class="video-controls" :class="{ 'controls-faded': !showControls }">
      <div
        class="progress-bar-container"
        @click="handleProgressClick"
        @mousedown="startDrag"
      >
        <div class="progress-buffered" style="width: 100%"></div>
        <div class="progress-played" :style="{ width: progress + '%' }">
          <div class="progress-thumb"></div>
        </div>
      </div>

      <div class="controls-row">
        <div class="controls-left">
          <button class="control-btn play-btn" @click="togglePlay">
            <span v-if="!isPlaying">▶</span>
            <span v-else>⏸</span>
          </button>
          <span class="time-display">{{ formattedCurrentTime }} / {{ formattedDuration }}</span>
        </div>

        <div class="controls-center">
          <div class="speed-selector">
            <button
              v-for="speed in speeds"
              :key="speed"
              class="speed-btn"
              :class="{ active: currentSpeed === speed }"
              @click="setSpeed(speed)"
            >
              {{ speed }}x
            </button>
          </div>
        </div>

        <div class="controls-right">
          <button class="control-btn action-btn" @click="handleAnalyze" :title="isAnalyzing ? '结束分析' : '视频分析'">
            <svg v-if="analyzeLoading" class="spinner" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
               <circle cx="12" cy="12" r="8" stroke-dasharray="32" stroke-dashoffset="32" stroke-linecap="round"></circle>
            </svg>
            <svg v-else-if="isAnalyzing" viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
              <rect x="6" y="6" width="12" height="12"/>
            </svg>
            <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
              <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 14l-5-5 1.41-1.41L12 14.17l4.59-4.58L18 11l-6 6z"/>
            </svg>
            <span>{{ analyzeLoading ? '分析中' : (isAnalyzing ? '结束' : '分析') }}</span>
          </button>
          <button class="control-btn action-btn" @click="setSlowMotion" title="慢放">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/>
            </svg>
            <span>慢放</span>
          </button>
          <button class="control-btn action-btn" @click="handleScreenshot" title="截图">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
              <path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/>
            </svg>
            <span>截图</span>
          </button>
          <button class="control-btn fullscreen-btn" @click="toggleFullscreen" title="全屏">
            <svg v-if="!isFullscreen" viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
              <path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/>
            </svg>
            <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
              <path d="M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <div class="bottom-panel" v-if="!isFullscreen">
      <!-- Tabs 头部 -->
      <div class="tabs-header">
        <div 
          class="tab-item" 
          :class="{ active: activeTab === 'notes' }" 
          @click="activeTab = 'notes'"
        >
          课堂笔记
        </div>
        <div 
          class="tab-item" 
          :class="{ active: activeTab === 'analysis' }" 
          @click="activeTab = 'analysis'"
        >
          课堂分析
        </div>
      </div>

      <!-- 课堂笔记 Panel -->
      <div class="notes-section" v-show="activeTab === 'notes'">
        <div class="section-header">
          <button class="add-note-btn" @click="addNote" title="新增笔记条目">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
            新增
          </button>
          <button class="save-note-btn" @click="emit('save-notes')" title="保存笔记">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M17 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V7l-4-4zm-5 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3zm3-10H5V5h10v4z"/></svg>
            保存
          </button>
        </div>
        <div v-if="classNotes.length > 0" class="notes-list">
          <div class="note-item" v-for="note in classNotes" :key="note.id">
            <span class="note-icon">📝</span>
            <input
              :value="note.text"
              @input="updateNoteText(note.id, ($event.target as HTMLInputElement).value)"
              class="note-input"
              placeholder="记录课堂要点..."
            />
            <button class="note-delete" @click="removeNote(note.id)" v-if="classNotes.length > 1">×</button>
          </div>
        </div>
        <div v-else class="empty-notes">
          <span class="empty-notes-icon">📝</span>
          <p class="empty-notes-text">暂无课堂笔记</p>
          <p class="empty-notes-hint">点击上方"新增"按钮添加笔记</p>
        </div>
      </div>

      <!-- 课堂分析 Panel -->
      <div class="analysis-section" v-show="activeTab === 'analysis'">
        <div class="analysis-grid">
          <div class="stat-card">
            <div class="stat-icon">👥</div>
            <div class="stat-content">
              <span class="stat-value">{{ analysis.activeStudents }}/{{ analysis.totalStudents }}</span>
              <span class="stat-label">出勤人数</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">📊</div>
            <div class="stat-content">
              <span class="stat-value">{{ analysis.attendanceRate }}%</span>
              <span class="stat-label">到课率</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">🎯</div>
            <div class="stat-content">
              <span class="stat-value">{{ analysis.participationRate }}%</span>
              <span class="stat-label">参与度</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.lesson-video-container {
  background: var(--bg-card, #ffffff);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid var(--border-color, #e2e8f0);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.lesson-video-container:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.lesson-video-container.fullscreen {
  border-radius: 0;
  box-shadow: none;
}

.video-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color, #e2e8f0);
  background: var(--bg-card, #ffffff);
}

.video-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary, #1e293b);
}

.ai-monitor-window {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: #000;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-loading {
  color: #fff;
  font-size: 0.85rem;
  animation: pulse 1.5s infinite;
}

.ai-frame {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes pulse {
  0% { opacity: 0.6; }
  50% { opacity: 1; }
  100% { opacity: 0.6; }
}

.video-wrapper {
  position: relative;
  background: #000;
  aspect-ratio: 16 / 9;
  overflow: hidden;
}

.no-video-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
}

.no-video-icon {
  font-size: 3.5rem;
  margin-bottom: 12px;
  opacity: 0.8;
}

.video-error-state {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  gap: 12px;
}

.error-icon {
  font-size: 3rem;
}

.error-text {
  font-size: 1.1rem;
  font-weight: 600;
  color: #fbbf24;
  margin: 0;
}

.error-hint {
  font-size: 0.8rem;
  color: #94a3b8;
  margin: 0;
  max-width: 80%;
  text-align: center;
  word-break: break-all;
}

.retry-btn {
  padding: 10px 24px;
  border: none;
  border-radius: 20px;
  background: var(--primary-blue, #0ea5e9);
  color: white;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-top: 8px;
}

.retry-btn:hover {
  background: var(--primary-hover, #0284c7);
  transform: scale(1.05);
}

.no-video-text {
  font-size: 1rem;
  font-weight: 500;
  color: #e2e8f0;
  margin: 0 0 6px 0;
}

.no-video-hint {
  font-size: 0.85rem;
  color: #94a3b8;
  margin: 0;
}

.video-player {
  width: 100%;
  height: 100%;
  object-fit: contain;
  cursor: pointer;
}

.video-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.2);
  opacity: 1;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.video-overlay.hidden {
  opacity: 0;
}

.play-button-large {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  color: var(--primary-blue, #0ea5e9);
  cursor: pointer;
  pointer-events: auto;
  transition: all 0.2s ease;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.play-button-large:hover {
  transform: scale(1.1);
  background: #ffffff;
}

.video-controls {
  padding: 16px 20px;
  background: var(--bg-card, #ffffff);
  border-top: 1px solid var(--border-color, #e2e8f0);
  opacity: 1;
  transition: opacity 0.3s ease;
  min-height: 80px;
  box-sizing: border-box;
}

.video-controls.controls-faded {
  opacity: 0.4;
  pointer-events: none;
}

.progress-bar-container {
  height: 6px;
  background: var(--border-color, #e2e8f0);
  border-radius: 3px;
  margin-bottom: 16px;
  cursor: pointer;
  position: relative;
  overflow: visible;
}

.progress-buffered {
  position: absolute;
  height: 100%;
  background: rgba(14, 165, 233, 0.2);
  border-radius: 3px;
}

.progress-played {
  height: 100%;
  background: linear-gradient(90deg, var(--primary-blue, #0ea5e9), var(--primary-hover, #0284c7));
  border-radius: 3px;
  position: relative;
  transition: width 0.1s linear;
}

.progress-thumb {
  position: absolute;
  right: -8px;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  background: #ffffff;
  border: 3px solid var(--primary-blue, #0ea5e9);
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.progress-bar-container:hover .progress-thumb {
  opacity: 1;
}

.controls-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.controls-left,
.controls-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.controls-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.control-btn {
  background: transparent;
  border: none;
  color: var(--text-primary, #1e293b);
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  font-size: 0.9rem;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 4px;
}

.control-btn:hover {
  background: var(--hover-bg, #f1f5f9);
  color: var(--primary-blue, #0ea5e9);
}

.play-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--primary-blue, #0ea5e9);
  color: white;
  justify-content: center;
  font-size: 1rem;
}

.play-btn:hover {
  background: var(--primary-hover, #0284c7);
  color: white;
  transform: scale(1.05);
}

.time-display {
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
  font-variant-numeric: tabular-nums;
  min-width: 100px;
}

.speed-selector {
  display: flex;
  gap: 4px;
  background: var(--bg-primary, #f8fafc);
  padding: 4px;
  border-radius: 20px;
}

.speed-btn {
  background: transparent;
  border: none;
  color: var(--text-secondary, #64748b);
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 0.8rem;
  transition: all 0.2s ease;
}

.speed-btn:hover {
  color: var(--primary-blue, #0ea5e9);
}

.speed-btn.active {
  background: var(--primary-blue, #0ea5e9);
  color: white;
}

.action-btn {
  padding: 8px 12px;
  background: var(--bg-primary, #f8fafc);
  border-radius: 20px;
  font-size: 0.85rem;
}

.action-btn:hover {
  background: var(--primary-light, #e0f2fe);
}

.action-btn svg {
  flex-shrink: 0;
}

.fullscreen-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  justify-content: center;
}

@media (max-width: 768px) {
  .lesson-video-container {
    border-radius: 12px;
  }

  .video-header {
    padding: 12px 16px;
  }

  .video-title {
    font-size: 1rem;
  }

  .video-controls {
    padding: 12px 16px;
  }

  .controls-row {
    flex-wrap: wrap;
    gap: 12px;
  }

  .controls-center {
    order: 3;
    width: 100%;
    justify-content: flex-start;
  }

  .speed-selector {
    overflow-x: auto;
    max-width: 100%;
  }

  .action-btn span {
    display: none;
  }

  .time-display {
    font-size: 0.8rem;
    min-width: 80px;
  }
}

@media (max-width: 480px) {
  .play-button-large {
    width: 60px;
    height: 60px;
    font-size: 1.5rem;
  }

  .play-btn {
    width: 36px;
    height: 36px;
  }

  .speed-btn {
    padding: 4px 8px;
    font-size: 0.75rem;
  }
}

/* ── 底部面板 (Tabs) ─────────────────────── */
.bottom-panel {
  background: var(--bg-card, #ffffff);
  border-top: 1px solid var(--border-color, #e2e8f0);
  flex: 0 0 auto;
  max-height: 300px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tabs-header {
  display: flex;
  gap: 24px;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-color, #e2e8f0);
}

.tab-item {
  padding: 16px 0;
  font-size: 1rem;
  font-weight: 500;
  color: var(--text-secondary, #64748b);
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}

.tab-item:hover {
  color: var(--primary-blue, #0ea5e9);
}

.tab-item.active {
  color: var(--primary-blue, #0ea5e9);
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--primary-blue, #0ea5e9);
  border-radius: 3px 3px 0 0;
}

/* ── 课堂笔记 ─────────────────────── */
.notes-section {
  padding: 16px 20px 20px;
  flex: 1;
  overflow-y: auto;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-primary, #1e293b);
}

.add-note-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px dashed var(--primary-blue, #0ea5e9);
  border-radius: 20px;
  background: transparent;
  color: var(--primary-blue, #0ea5e9);
  font-size: 0.78rem;
  cursor: pointer;
  transition: all 0.2s;
}

.add-note-btn:hover {
  background: var(--primary-light, #e0f2fe);
}

.save-note-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid var(--primary-blue, #0ea5e9);
  border-radius: 20px;
  background: var(--primary-blue, #0ea5e9);
  color: white;
  font-size: 0.78rem;
  cursor: pointer;
  transition: all 0.2s;
}

.save-note-btn:hover {
  background: var(--primary-hover, #0284c7);
  border-color: var(--primary-hover, #0284c7);
}

.notes-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.note-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.note-icon {
  font-size: 1rem;
  flex-shrink: 0;
}

.note-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 0.88rem;
  color: var(--text-primary, #1e293b);
  border-bottom: 1px dashed var(--border-color, #e2e8f0);
  padding: 4px 0;
  transition: border-color 0.2s;
}

.note-input:focus {
  border-bottom-color: var(--primary-blue, #0ea5e9);
}

.note-input::placeholder {
  color: var(--text-secondary, #94a3b8);
}

.note-delete {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--text-secondary, #94a3b8);
  font-size: 1rem;
  cursor: pointer;
  border-radius: 50%;
  flex-shrink: 0;
  transition: all 0.2s;
}

.note-delete:hover {
  background: #fee2e2;
  color: #ef4444;
}

/* ── 课堂分析 ─────────────────────── */
.analysis-section {
  padding: 16px 20px 20px;
  background: var(--bg-card, #ffffff);
  border-top: 1px solid var(--border-color, #e2e8f0);
  flex: 1;
  overflow-y: auto;
}

.analysis-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-card {
  background: var(--bg-primary, #f8fafc);
  border-radius: 12px;
  padding: 14px 16px;
  border: 1px solid var(--border-color, #e2e8f0);
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-card:hover {
  border-color: var(--primary-blue, #0ea5e9);
  box-shadow: 0 3px 10px rgba(14, 165, 233, 0.08);
}

.stat-icon {
  font-size: 1.3rem;
  flex-shrink: 0;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--primary-blue, #0ea5e9);
  line-height: 1;
}

.stat-label {
  font-size: 0.78rem;
  color: var(--text-secondary, #64748b);
}

.empty-notes {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 20px;
  text-align: center;
}

.empty-notes-icon {
  font-size: 2rem;
  margin-bottom: 10px;
}

.empty-notes-text {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--text-secondary, #64748b);
  margin: 0 0 4px 0;
}

.empty-notes-hint {
  font-size: 0.78rem;
  color: var(--text-tertiary, #94a3b8);
  margin: 0;
}

@media (max-width: 768px) {
  .notes-section,
  .analysis-section {
    padding: 14px 16px;
  }

  .analysis-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}
</style>

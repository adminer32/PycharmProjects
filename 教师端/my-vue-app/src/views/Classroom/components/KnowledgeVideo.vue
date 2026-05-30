<script setup lang="ts">
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import type { KnowledgePoint } from '../types'
import { uploadFile } from '@/api/fileApi'

const props = defineProps<{
  visible: boolean
  knowledgePoint: KnowledgePoint | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'uploaded', url: string): void
}>()

const videoRef = ref<HTMLVideoElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const videoPreviewUrl = ref<string>('')
const isUploading = ref(false)
const uploadProgress = ref(0)
const isDragging = ref(false)

const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const volume = ref(1)
const isMuted = ref(false)
const isFullscreen = ref(false)

const formattedCurrentTime = computed(() => formatTime(currentTime.value))
const formattedDuration = computed(() => formatTime(duration.value))

const progress = computed(() => {
  if (duration.value === 0) return 0
  return (currentTime.value / duration.value) * 100
})

const allowedFormats = ['video/mp4', 'video/webm', 'video/quicktime']
const maxFileSize = 500 * 1024 * 1024

function formatTime(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
}

function validateFile(file: File): boolean {
  if (!allowedFormats.includes(file.type)) {
    ElMessage.error('不支持的视频格式，请上传 mp4、webm 或 mov 格式的视频')
    return false
  }
  if (file.size > maxFileSize) {
    ElMessage.error('文件大小超过限制，最大支持 500MB')
    return false
  }
  return true
}

function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file && validateFile(file)) {
    setFile(file)
  }
}

function handleDragOver(event: DragEvent) {
  event.preventDefault()
  isDragging.value = true
}

function handleDragLeave(event: DragEvent) {
  event.preventDefault()
  isDragging.value = false
}

function handleDrop(event: DragEvent) {
  event.preventDefault()
  isDragging.value = false
  const file = event.dataTransfer?.files[0]
  if (file && validateFile(file)) {
    setFile(file)
  }
}

function setFile(file: File) {
  if (videoPreviewUrl.value) {
    URL.revokeObjectURL(videoPreviewUrl.value)
  }
  selectedFile.value = file
  videoPreviewUrl.value = URL.createObjectURL(file)
  uploadProgress.value = 0
}

function triggerFileInput() {
  fileInputRef.value?.click()
}

async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择视频文件')
    return
  }

  isUploading.value = true
  uploadProgress.value = 0

  try {
    const progressInterval = setInterval(() => {
      if (uploadProgress.value < 90) {
        uploadProgress.value += Math.random() * 10
        if (uploadProgress.value > 90) uploadProgress.value = 90
      }
    }, 200)

    const url = await uploadFile(selectedFile.value)
    
    clearInterval(progressInterval)
    uploadProgress.value = 100
    
    emit('uploaded', url)
    ElMessage.success('视频上传成功')
    resetUpload()
  } catch (error: any) {
    console.error('视频上传失败:', error)
    ElMessage.error(error?.message || '视频上传失败，请重试')
    uploadProgress.value = 0
  } finally {
    isUploading.value = false
  }
}

function resetUpload() {
  if (videoPreviewUrl.value) {
    URL.revokeObjectURL(videoPreviewUrl.value)
  }
  selectedFile.value = null
  videoPreviewUrl.value = ''
  uploadProgress.value = 0
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

function togglePlay() {
  if (!videoRef.value) return
  if (isPlaying.value) {
    videoRef.value.pause()
  } else {
    videoRef.value.play()
  }
  isPlaying.value = !isPlaying.value
}

function handleTimeUpdate() {
  if (videoRef.value) {
    currentTime.value = videoRef.value.currentTime
  }
}

function handleLoadedMetadata() {
  if (videoRef.value) {
    duration.value = videoRef.value.duration
  }
}

function handleVideoEnded() {
  isPlaying.value = false
}

function handleProgressClick(e: MouseEvent) {
  if (!videoRef.value) return
  const target = e.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const percent = (e.clientX - rect.left) / rect.width
  videoRef.value.currentTime = percent * duration.value
}

function toggleMute() {
  if (!videoRef.value) return
  isMuted.value = !isMuted.value
  videoRef.value.muted = isMuted.value
}

function handleVolumeChange(e: Event) {
  const target = e.target as HTMLInputElement
  volume.value = parseFloat(target.value)
  if (videoRef.value) {
    videoRef.value.volume = volume.value
    isMuted.value = volume.value === 0
  }
}

function toggleFullscreen() {
  const container = document.querySelector('.video-container')
  if (!container) return

  if (document.fullscreenElement) {
    document.exitFullscreen()
    isFullscreen.value = false
  } else {
    (container as HTMLElement).requestFullscreen()
    isFullscreen.value = true
  }
}

function handleClose() {
  resetUpload()
  if (videoRef.value) {
    videoRef.value.pause()
    isPlaying.value = false
  }
  emit('close')
}

function handleOverlayClick(e: MouseEvent) {
  if ((e.target as HTMLElement).classList.contains('modal-overlay')) {
    handleClose()
  }
}

watch(() => props.visible, (newVal) => {
  if (!newVal) {
    resetUpload()
    if (videoRef.value) {
      videoRef.value.pause()
      isPlaying.value = false
    }
  }
})

onBeforeUnmount(() => {
  if (videoPreviewUrl.value) {
    URL.revokeObjectURL(videoPreviewUrl.value)
  }
})
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="visible"
        class="modal-overlay"
        @click="handleOverlayClick"
      >
        <div class="modal-content">
          <div class="modal-header">
            <h3 class="modal-title">
              {{ knowledgePoint?.hasVideo ? '播放知识点视频' : '上传知识点视频' }}
            </h3>
            <button class="close-btn" @click="handleClose">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>

          <div v-if="knowledgePoint" class="knowledge-info">
            <span class="knowledge-order">{{ knowledgePoint.order }}</span>
            <span class="knowledge-title">{{ knowledgePoint.title }}</span>
          </div>

          <div class="video-container">
            <template v-if="knowledgePoint?.hasVideo">
              <video
                ref="videoRef"
                class="video-player"
                :poster="knowledgePoint?.videoThumbnail"
                @timeupdate="handleTimeUpdate"
                @loadedmetadata="handleLoadedMetadata"
                @ended="handleVideoEnded"
              >
                <source v-if="knowledgePoint?.videoUrl" :src="knowledgePoint.videoUrl" type="video/mp4">
              </video>

              <div v-if="!knowledgePoint?.videoUrl" class="no-video-placeholder">
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18"></rect>
                  <line x1="7" y1="2" x2="7" y2="22"></line>
                  <line x1="17" y1="2" x2="17" y2="22"></line>
                  <line x1="2" y1="12" x2="22" y2="12"></line>
                  <line x1="2" y1="7" x2="7" y2="7"></line>
                  <line x1="2" y1="17" x2="7" y2="17"></line>
                  <line x1="17" y1="17" x2="22" y2="17"></line>
                  <line x1="17" y1="7" x2="22" y2="7"></line>
                </svg>
                <p>暂无视频</p>
              </div>

              <div class="video-controls">
                <div class="progress-bar" @click="handleProgressClick">
                  <div class="progress-played" :style="{ width: progress + '%' }"></div>
                </div>

                <div class="controls-row">
                  <div class="controls-left">
                    <button class="control-btn" @click="togglePlay">
                      <svg v-if="isPlaying" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                        <rect x="6" y="4" width="4" height="16"></rect>
                        <rect x="14" y="4" width="4" height="16"></rect>
                      </svg>
                      <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                        <polygon points="5 3 19 12 5 21 5 3"></polygon>
                      </svg>
                    </button>

                    <div class="volume-control">
                      <button class="control-btn" @click="toggleMute">
                        <svg v-if="isMuted || volume === 0" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
                          <line x1="23" y1="9" x2="17" y2="15"></line>
                          <line x1="17" y1="9" x2="23" y2="15"></line>
                        </svg>
                        <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
                          <path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path>
                        </svg>
                      </button>
                      <input
                        type="range"
                        min="0"
                        max="1"
                        step="0.1"
                        :value="volume"
                        class="volume-slider"
                        @input="handleVolumeChange"
                      />
                    </div>

                    <span class="time-display">{{ formattedCurrentTime }} / {{ formattedDuration }}</span>
                  </div>

                  <div class="controls-right">
                    <button class="control-btn" @click="toggleFullscreen">
                      <svg v-if="isFullscreen" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M8 3v3a2 2 0 0 1-2 2H3m18 0h-3a2 2 0 0 1-2-2V3m0 18v-3a2 2 0 0 1 2-2h3M3 16h3a2 2 0 0 1 2 2v3"></path>
                      </svg>
                      <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"></path>
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
            </template>

            <template v-else>
              <div class="upload-container">
                <input
                  ref="fileInputRef"
                  type="file"
                  accept="video/mp4,video/webm,video/quicktime"
                  class="file-input"
                  @change="handleFileSelect"
                />

                <div
                  v-if="!selectedFile"
                  class="upload-area"
                  :class="{ 'is-dragging': isDragging }"
                  @click="triggerFileInput"
                  @dragover="handleDragOver"
                  @dragleave="handleDragLeave"
                  @drop="handleDrop"
                >
                  <div class="upload-icon">
                    <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                      <polyline points="17 8 12 3 7 8"></polyline>
                      <line x1="12" y1="3" x2="12" y2="15"></line>
                    </svg>
                  </div>
                  <p class="upload-text">拖拽视频文件到此处，或点击选择文件</p>
                  <p class="upload-hint">支持 mp4、webm、mov 格式，最大 500MB</p>
                </div>

                <div v-else class="preview-area">
                  <video
                    ref="videoRef"
                    class="preview-video"
                    :src="videoPreviewUrl"
                    @timeupdate="handleTimeUpdate"
                    @loadedmetadata="handleLoadedMetadata"
                    @ended="handleVideoEnded"
                  ></video>

                  <div class="preview-controls">
                    <div class="progress-bar" @click="handleProgressClick">
                      <div class="progress-played" :style="{ width: progress + '%' }"></div>
                    </div>

                    <div class="controls-row">
                      <div class="controls-left">
                        <button class="control-btn" @click="togglePlay">
                          <svg v-if="isPlaying" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                            <rect x="6" y="4" width="4" height="16"></rect>
                            <rect x="14" y="4" width="4" height="16"></rect>
                          </svg>
                          <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                            <polygon points="5 3 19 12 5 21 5 3"></polygon>
                          </svg>
                        </button>

                        <div class="volume-control">
                          <button class="control-btn" @click="toggleMute">
                            <svg v-if="isMuted || volume === 0" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                              <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
                              <line x1="23" y1="9" x2="17" y2="15"></line>
                              <line x1="17" y1="9" x2="23" y2="15"></line>
                            </svg>
                            <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                              <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
                              <path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path>
                            </svg>
                          </button>
                          <input
                            type="range"
                            min="0"
                            max="1"
                            step="0.1"
                            :value="volume"
                            class="volume-slider"
                            @input="handleVolumeChange"
                          />
                        </div>

                        <span class="time-display">{{ formattedCurrentTime }} / {{ formattedDuration }}</span>
                      </div>
                    </div>
                  </div>

                  <div class="file-info">
                    <span class="file-name">{{ selectedFile.name }}</span>
                    <span class="file-size">{{ (selectedFile.size / 1024 / 1024).toFixed(2) }} MB</span>
                  </div>
                </div>

                <div v-if="isUploading" class="upload-progress">
                  <div class="progress-bar-wrapper">
                    <div class="progress-bar-bg">
                      <div class="progress-bar-fill" :style="{ width: uploadProgress + '%' }"></div>
                    </div>
                    <span class="progress-text">{{ Math.round(uploadProgress) }}%</span>
                  </div>
                  <p class="uploading-text">正在上传视频...</p>
                </div>

                <div v-if="selectedFile && !isUploading" class="upload-actions">
                  <button class="action-btn secondary" @click="resetUpload">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    </svg>
                    移除文件
                  </button>
                  <button class="action-btn primary" @click="handleUpload">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                      <polyline points="17 8 12 3 7 8"></polyline>
                      <line x1="12" y1="3" x2="12" y2="15"></line>
                    </svg>
                    确认上传
                  </button>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-content {
  background: var(--bg-card, #ffffff);
  border-radius: 24px;
  width: 90%;
  max-width: 900px;
  max-height: 90vh;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color, #e2e8f0);
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary, #1e293b);
}

.close-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  color: var(--text-secondary, #64748b);
  transition: all 0.2s;
}

.close-btn:hover {
  background: var(--hover-bg, #f1f5f9);
  color: var(--text-primary, #1e293b);
}

.knowledge-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  background: var(--bg-primary, #f8fafc);
  border-bottom: 1px solid var(--border-color, #e2e8f0);
}

.knowledge-order {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--primary-blue, #0ea5e9);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 600;
}

.knowledge-title {
  font-size: 1rem;
  font-weight: 500;
  color: var(--text-primary, #1e293b);
}

.video-container {
  position: relative;
  background: #000;
  aspect-ratio: 16/9;
}

.video-player {
  width: 100%;
  height: 100%;
  object-fit: contain;
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
  color: var(--text-secondary, #64748b);
  background: var(--bg-primary, #f8fafc);
}

.no-video-placeholder p {
  margin-top: 16px;
  font-size: 1rem;
}

.video-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.8));
  padding: 40px 16px 16px;
}

.progress-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
  cursor: pointer;
  margin-bottom: 12px;
}

.progress-played {
  height: 100%;
  background: var(--primary-blue, #0ea5e9);
  border-radius: 2px;
  position: relative;
}

.progress-played::after {
  content: '';
  position: absolute;
  right: -6px;
  top: -4px;
  width: 12px;
  height: 12px;
  background: var(--primary-blue, #0ea5e9);
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s;
}

.progress-bar:hover .progress-played::after {
  opacity: 1;
}

.controls-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.controls-left,
.controls-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.control-btn {
  background: transparent;
  border: none;
  color: white;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.volume-control {
  display: flex;
  align-items: center;
  gap: 4px;
}

.volume-slider {
  width: 80px;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
  cursor: pointer;
}

.volume-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 12px;
  height: 12px;
  background: white;
  border-radius: 50%;
  cursor: pointer;
}

.time-display {
  font-size: 0.85rem;
  color: white;
  margin-left: 8px;
}

.upload-container {
  width: 100%;
  height: 100%;
  background: var(--bg-primary, #f8fafc);
  display: flex;
  flex-direction: column;
  position: relative;
}

.file-input {
  display: none;
}

.upload-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 2px dashed var(--border-color, #e2e8f0);
  border-radius: 16px;
  margin: 24px;
  transition: all 0.3s;
}

.upload-area:hover,
.upload-area.is-dragging {
  border-color: var(--primary-blue, #0ea5e9);
  background: rgba(14, 165, 233, 0.05);
}

.upload-icon {
  color: var(--text-secondary, #64748b);
  margin-bottom: 16px;
}

.upload-area:hover .upload-icon,
.upload-area.is-dragging .upload-icon {
  color: var(--primary-blue, #0ea5e9);
}

.upload-text {
  font-size: 1rem;
  color: var(--text-primary, #1e293b);
  margin: 0 0 8px;
}

.upload-hint {
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
  margin: 0;
}

.preview-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.preview-video {
  width: 100%;
  flex: 1;
  object-fit: contain;
  background: #000;
}

.preview-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.8));
  padding: 40px 16px 16px;
}

.file-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: var(--bg-card, #ffffff);
  border-top: 1px solid var(--border-color, #e2e8f0);
}

.file-name {
  font-size: 0.9rem;
  color: var(--text-primary, #1e293b);
  font-weight: 500;
}

.file-size {
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
}

.upload-progress {
  padding: 16px 24px;
  background: var(--bg-card, #ffffff);
  border-top: 1px solid var(--border-color, #e2e8f0);
}

.progress-bar-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-bar-bg {
  flex: 1;
  height: 8px;
  background: var(--bg-primary, #f1f5f9);
  border-radius: 4px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: var(--primary-blue, #0ea5e9);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--primary-blue, #0ea5e9);
  min-width: 40px;
}

.uploading-text {
  margin: 8px 0 0;
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
  text-align: center;
}

.upload-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 16px 24px;
  background: var(--bg-card, #ffffff);
  border-top: 1px solid var(--border-color, #e2e8f0);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 12px;
  border: none;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn.primary {
  background: var(--primary-blue, #0ea5e9);
  color: white;
}

.action-btn.primary:hover {
  background: #0284c7;
  transform: translateY(-1px);
}

.action-btn.secondary {
  background: var(--bg-primary, #f1f5f9);
  color: var(--text-secondary, #64748b);
}

.action-btn.secondary:hover {
  background: var(--border-color, #e2e8f0);
  color: var(--text-primary, #1e293b);
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active .modal-content,
.modal-leave-active .modal-content {
  transition: transform 0.3s ease;
}

.modal-enter-from .modal-content,
.modal-leave-to .modal-content {
  transform: scale(0.95);
}
</style>

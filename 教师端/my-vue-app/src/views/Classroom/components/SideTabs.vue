<script setup lang="ts">
import { ref } from 'vue'
import type { KnowledgePoint, LessonItem } from '../types'

const props = defineProps<{
  knowledgePoints: KnowledgePoint[]
  lessons: LessonItem[]
  currentLessonId: string
  currentPointId: string | null
  expandedPointId: string | null
}>()

const emit = defineEmits<{
  (e: 'selectLesson', lesson: LessonItem): void
  (e: 'selectPoint', point: KnowledgePoint): void
  (e: 'expandPoint', pointId: string | null): void
  (e: 'addPoint'): void
  (e: 'uploadVideo', point: KnowledgePoint): void
  (e: 'playVideo', point: KnowledgePoint): void
}>()

const activeTab = ref('lessons')

function handleSelectLesson(lesson: LessonItem) {
  emit('selectLesson', lesson)
}

function handleSelectPoint(point: KnowledgePoint) {
  emit('selectPoint', point)
}

function handleToggleExpand(pointId: string) {
  if (props.expandedPointId === pointId) {
    emit('expandPoint', null)
  } else {
    emit('expandPoint', pointId)
  }
}

function handleAddPoint() {
  emit('addPoint')
}

function handleUploadVideo(point: KnowledgePoint) {
  emit('uploadVideo', point)
}

function handlePlayVideo(point: KnowledgePoint) {
  emit('playVideo', point)
}
</script>

<template>
  <div class="side-tabs-container">
    
    <el-tabs v-model="activeTab" class="side-tabs">
      <el-tab-pane label="课堂回放列表" name="lessons">
        <div class="lessons-tab-content">
          <div class="lessons-list">
            <template v-if="lessons.length > 0">
              <div
                v-for="lesson in lessons"
                :key="lesson.id"
                class="lesson-item"
                :class="{ active: String(currentLessonId) === String(lesson.id) }"
                @click="handleSelectLesson(lesson)"
              >
                <div class="lesson-thumbnail">
                  <img :src="lesson.thumbnail" :alt="lesson.title" />
                  <div class="lesson-duration">{{ lesson.duration }}</div>
                </div>
                <div class="lesson-info">
                  <h4 class="lesson-title">{{ lesson.title }}</h4>
                  <p class="lesson-date">{{ lesson.recordedAt }}</p>
                </div>
              </div>
            </template>
            <div v-else class="empty-state">
              <span class="empty-icon">🎬</span>
              <p class="empty-text">暂无课堂回放</p>
              <p class="empty-hint">该班级暂无课程录制记录</p>
            </div>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="知识点" name="knowledge">
        <div class="knowledge-tab-content">
          <div class="add-point-section">
            <el-button type="primary" size="small" @click="handleAddPoint">
              添加知识点
            </el-button>
          </div>
          <div class="knowledge-list">
            <template v-if="knowledgePoints.length > 0">
              <div
                v-for="point in knowledgePoints"
                :key="point.id"
                class="knowledge-item"
                :class="{ 
                  active: currentPointId === point.id,
                  expanded: expandedPointId === point.id
                }"
              >
                <div class="knowledge-header" @click="handleSelectPoint(point)">
                  <div class="knowledge-info">
                    <span class="knowledge-order">{{ point.order }}.</span>
                    <span class="knowledge-title">{{ point.title }}</span>
                  </div>
                  <div class="knowledge-actions">
                    <div class="video-status" v-if="point.hasVideo">
                      <span class="video-icon">🎬</span>
                    </div>
                    <button class="expand-btn" @click.stop="handleToggleExpand(point.id)">
                      {{ expandedPointId === point.id ? '收起' : '展开' }}
                    </button>
                  </div>
                </div>
                <transition name="expand">
                  <div v-if="expandedPointId === point.id" class="knowledge-content">
                    <p class="knowledge-desc">{{ point.description }}</p>
                    <div class="point-actions">
                      <el-button 
                        v-if="!point.hasVideo" 
                        type="primary" 
                        size="small" 
                        @click="handleUploadVideo(point)"
                      >
                        上传视频
                      </el-button>
                      <el-button 
                        v-else 
                        type="success" 
                        size="small" 
                        @click="handlePlayVideo(point)"
                      >
                        播放视频
                      </el-button>
                    </div>
                    <div v-if="point.hasVideo && point.videoThumbnail" class="point-video-preview">
                      <div class="video-thumbnail">
                        <img :src="point.videoThumbnail" :alt="point.title" />
                        <div class="video-duration" v-if="point.videoDuration">{{ point.videoDuration }}</div>
                      </div>
                    </div>
                  </div>
                </transition>
              </div>
            </template>
            <div v-else class="empty-state">
              <span class="empty-icon">📚</span>
              <p class="empty-text">暂无知识点</p>
              <p class="empty-hint">点击上方按钮添加知识点</p>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.side-tabs-container {
  background: var(--bg-card, #ffffff);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid var(--border-color, #e2e8f0);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.side-tabs {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.side-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 16px;
  border-bottom: 1px solid var(--border-color, #e2e8f0);
  background: var(--bg-primary, #f8fafc);
}

.side-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.side-tabs :deep(.el-tabs__item) {
  padding: 0 20px;
  height: 48px;
  line-height: 48px;
  font-size: 0.95rem;
  font-weight: 500;
  color: var(--text-secondary, #64748b);
  border: none;
}

.side-tabs :deep(.el-tabs__item:hover) {
  color: var(--primary-blue, #0ea5e9);
}

.side-tabs :deep(.el-tabs__item.is-active) {
  color: var(--primary-blue, #0ea5e9);
  font-weight: 600;
}

.side-tabs :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, var(--primary-blue, #0ea5e9), var(--primary-hover, #0284c7));
  height: 3px;
  border-radius: 3px 3px 0 0;
}

.side-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.side-tabs :deep(.el-tab-pane) {
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.knowledge-tab-content,
.lessons-tab-content {
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.add-point-section {
  padding: 16px;
  border-bottom: 1px solid var(--border-color, #e2e8f0);
}

.knowledge-list,
.lessons-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.knowledge-item {
  margin-bottom: 8px;
  border-radius: 12px;
  border: 1px solid var(--border-color, #e2e8f0);
  overflow: hidden;
  transition: all 0.3s ease;
}

.knowledge-item:hover {
  border-color: var(--primary-blue, #0ea5e9);
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.1);
}

.knowledge-item.active {
  border-color: var(--primary-blue, #0ea5e9);
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.05) 0%, rgba(14, 165, 233, 0.02) 100%);
}

.knowledge-header {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  background: var(--bg-primary, #f8fafc);
  transition: background 0.2s ease;
}

.knowledge-header:hover {
  background: var(--primary-light, #e0f2fe);
}

.knowledge-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.knowledge-order {
  font-weight: 600;
  color: var(--primary-blue, #0ea5e9);
  font-size: 0.95rem;
  min-width: 24px;
}

.knowledge-title {
  font-weight: 500;
  color: var(--text-primary, #1e293b);
  font-size: 0.95rem;
}

.knowledge-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.video-status {
  display: flex;
  align-items: center;
}

.video-icon {
  font-size: 1.1rem;
}

.expand-btn {
  background: transparent;
  border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 0.8rem;
  color: var(--text-secondary, #64748b);
  cursor: pointer;
  transition: all 0.2s ease;
}

.expand-btn:hover {
  border-color: var(--primary-blue, #0ea5e9);
  color: var(--primary-blue, #0ea5e9);
  background: var(--primary-light, #e0f2fe);
}

.knowledge-content {
  padding: 16px;
  border-top: 1px solid var(--border-color, #e2e8f0);
  background: var(--bg-card, #ffffff);
}

.knowledge-desc {
  margin: 0 0 12px 0;
  color: var(--text-secondary, #64748b);
  font-size: 0.9rem;
  line-height: 1.6;
}

.point-actions {
  margin-bottom: 12px;
}

.point-video-preview {
  margin-top: 12px;
}

.video-thumbnail {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  aspect-ratio: 16 / 9;
}

.video-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.75);
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
}

.lesson-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 8px;
  border: 2px solid transparent;
  background: var(--bg-card, #ffffff);
}

.lesson-item:hover {
  background: var(--bg-primary, #f8fafc);
  border-color: var(--border-color, #e2e8f0);
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.lesson-item.active {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.12) 0%, rgba(14, 165, 233, 0.06) 100%);
  border-color: var(--primary-blue, #0ea5e9);
  box-shadow: 0 4px 16px rgba(14, 165, 233, 0.15);
  transform: translateX(4px);
}

.lesson-thumbnail {
  position: relative;
  width: 120px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.lesson-item:hover .lesson-thumbnail {
  border-color: var(--primary-blue, #0ea5e9);
}

.lesson-item.active .lesson-thumbnail {
  border-color: var(--primary-blue, #0ea5e9);
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.2);
}

.lesson-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.lesson-duration {
  position: absolute;
  bottom: 6px;
  right: 6px;
  background: rgba(0, 0, 0, 0.75);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.75rem;
}

.lesson-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  min-width: 0;
}

.lesson-title {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--text-primary, #1e293b);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lesson-item.active .lesson-title {
  color: var(--primary-blue, #0ea5e9);
  font-weight: 600;
}

.lesson-date {
  margin: 0;
  font-size: 0.8rem;
  color: var(--text-secondary, #64748b);
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 500px;
}

@media (max-width: 768px) {
  .side-tabs :deep(.el-tabs__item) {
    padding: 0 16px;
    font-size: 0.9rem;
  }

  .lesson-thumbnail {
    width: 100px;
  }

  .knowledge-header {
    padding: 12px 14px;
  }
}

@media (max-width: 480px) {
  .side-tabs-container {
    border-radius: 12px;
  }

  .side-tabs :deep(.el-tabs__header) {
    padding: 0 12px;
  }

  .knowledge-list,
  .lessons-list {
    padding: 6px;
  }

  .lesson-item {
    padding: 10px;
    gap: 10px;
  }

  .lesson-thumbnail {
    width: 80px;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 2.5rem;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 0.95rem;
  font-weight: 500;
  color: var(--text-secondary, #64748b);
  margin: 0 0 4px 0;
}

.empty-hint {
  font-size: 0.8rem;
  color: var(--text-tertiary, #94a3b8);
  margin: 0;
}
</style>

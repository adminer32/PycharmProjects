<script setup lang="ts">
import { ref, computed } from 'vue'
import type { VideoItem } from '../types'

const props = defineProps<{
  videoList: VideoItem[]
}>()

const emit = defineEmits<{
  (e: 'select', video: VideoItem): void
  (e: 'recommend', video: VideoItem): void
  (e: 'add'): void
}>()

const activeCategory = ref('all')
const isCollapsed = ref(false)

const categories = [
  { key: 'all', label: '全部' },
  { key: 'panti', label: '盘踢' },
  { key: 'bengti', label: '绷踢' },
  { key: 'guaiti', label: '拐踢' },
  { key: 'keti', label: '磕踢' },
  { key: 'tati', label: '踏踢' },
  { key: 'tiaoti', label: '跳踢' },
  { key: 'waibai', label: '外摆' },
  { key: 'lihe', label: '里合' }
]

const filteredVideos = computed(() => {
  if (activeCategory.value === 'all') {
    return props.videoList
  }
  return props.videoList.filter(v => v.category === activeCategory.value)
})

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const handleSelect = (video: VideoItem) => {
  emit('select', video)
}

const handleRecommend = (video: VideoItem, event: Event) => {
  event.stopPropagation()
  emit('recommend', video)
}

const handleAdd = () => {
  emit('add')
}
</script>

<template>
  <div class="video-library">
    <div class="library-header" @click="toggleCollapse">
      <h3>动作视频库</h3>
      <div class="header-actions">
        <el-button
          type="primary"
          size="small"
          @click.stop="handleAdd"
        >
          + 添加视频
        </el-button>
        <button class="library-toggle">{{ isCollapsed ? '+' : '−' }}</button>
      </div>
    </div>

    <div v-if="!isCollapsed" class="library-content">
      <div class="video-categories">
        <button
          v-for="cat in categories"
          :key="cat.key"
          class="cat-btn"
          :class="{ active: activeCategory === cat.key }"
          @click.stop="activeCategory = cat.key"
        >
          {{ cat.label }}
        </button>
      </div>

      <div class="video-list">
        <div
          v-for="video in filteredVideos"
          :key="video.id"
          class="video-item"
          @click="handleSelect(video)"
        >
          <div class="video-thumb"></div>
          <div class="video-info">
            <div class="video-title">{{ video.title }}</div>
            <div class="video-meta">
              <span class="video-duration">{{ video.duration }}</span>
              <el-tag
                v-if="video.recommendedTo && video.recommendedTo.length > 0"
                type="success"
                size="small"
                class="recommend-tag"
              >
                已推荐 {{ video.recommendedTo.length }} 人
              </el-tag>
            </div>
          </div>
          <el-button
            type="primary"
            size="small"
            class="recommend-btn"
            @click="handleRecommend(video, $event)"
          >
            推荐给学生
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.video-library {
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 24px;
  overflow: hidden;
  margin-top: 24px;
}

.library-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color, #e2e8f0);
  cursor: pointer;
}

.library-header h3 {
  font-size: 1rem;
  font-weight: 600;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.library-toggle {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  color: var(--text-secondary, #64748b);
}

.library-content {
  padding: 16px 20px;
}

.video-categories {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.cat-btn {
  padding: 6px 16px;
  border-radius: 30px;
  border: 1px solid var(--border-color, #e2e8f0);
  background: transparent;
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.2s;
}

.cat-btn.active {
  background: var(--primary-blue, #0ea5e9);
  color: white;
  border-color: var(--primary-blue, #0ea5e9);
}

.video-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.video-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-primary, #f8fafc);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.video-item:hover {
  background: var(--hover-bg, #f1f5f9);
}

.video-thumb {
  width: 48px;
  height: 48px;
  background: var(--border-color, #e2e8f0);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.video-info {
  flex: 1;
  min-width: 0;
}

.video-title {
  font-size: 0.9rem;
  font-weight: 500;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.video-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.video-duration {
  font-size: 0.75rem;
  color: var(--text-light, #94a3b8);
}

.recommend-tag {
  font-size: 0.7rem;
}

.recommend-btn {
  flex-shrink: 0;
}
</style>

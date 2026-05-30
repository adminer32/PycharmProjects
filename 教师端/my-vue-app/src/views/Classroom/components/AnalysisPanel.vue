<script setup lang="ts">
import { ref, computed } from 'vue'
import type { VideoAnalysis, AnalysisResult } from '../types'

const props = defineProps<{
  visible: boolean
  analysisData: VideoAnalysis | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'jumpTo', time: number): void
}>()

type AnalysisTab = 'action' | 'pose' | 'technique'

const activeTab = ref<AnalysisTab>('action')

const tabConfig = {
  action: { label: '动作识别', key: 'actionAnalysis' as const },
  pose: { label: '姿态分析', key: 'poseAnalysis' as const },
  technique: { label: '技术评估', key: 'techniqueAnalysis' as const }
}

const currentResults = computed<AnalysisResult[]>(() => {
  if (!props.analysisData) return []
  const key = tabConfig[activeTab.value].key
  return props.analysisData[key]
})

const formatTime = (seconds: number): string => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

const getScoreColor = (score: number): string => {
  if (score >= 90) return '#10b981'
  if (score >= 70) return '#0ea5e9'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}

const getScoreLabel = (score: number): string => {
  if (score >= 90) return '优秀'
  if (score >= 70) return '良好'
  if (score >= 60) return '及格'
  return '需改进'
}

const handleJumpTo = (time: number) => {
  emit('jumpTo', time)
}

const handleClose = () => {
  emit('close')
}
</script>

<template>
  <Teleport to="body">
    <Transition name="overlay">
      <div v-if="visible" class="analysis-overlay" @click.self="handleClose">
        <Transition name="panel">
          <div v-if="visible" class="analysis-panel">
            <div class="panel-header">
              <h3>视频分析</h3>
              <button class="close-btn" @click="handleClose">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>

            <div v-if="analysisData" class="panel-content">
              <div class="total-score-section">
                <div class="score-circle" :style="{ borderColor: getScoreColor(analysisData.totalScore) }">
                  <span class="score-value" :style="{ color: getScoreColor(analysisData.totalScore) }">
                    {{ analysisData.totalScore }}
                  </span>
                  <span class="score-label">{{ getScoreLabel(analysisData.totalScore) }}</span>
                </div>
                <div class="score-info">
                  <h4>总体评分</h4>
                  <p>基于动作识别、姿态分析和技术评估综合计算</p>
                </div>
              </div>

              <div class="tabs-container">
                <button
                  v-for="(config, key) in tabConfig"
                  :key="key"
                  class="tab-btn"
                  :class="{ active: activeTab === key }"
                  @click="activeTab = key as AnalysisTab"
                >
                  {{ config.label }}
                  <span class="tab-count">
                    {{ analysisData[config.key]?.length || 0 }}
                  </span>
                </button>
              </div>

              <div class="results-list">
                <div v-if="currentResults.length === 0" class="empty-state">
                  <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="12" y1="8" x2="12" y2="12"></line>
                    <line x1="12" y1="16" x2="12.01" y2="16"></line>
                  </svg>
                  <p>暂无分析结果</p>
                </div>

                <div
                  v-for="result in currentResults"
                  :key="result.id"
                  class="result-card"
                  @click="handleJumpTo(result.timestamp)"
                >
                  <div class="result-header">
                    <span class="result-time">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"></circle>
                        <polyline points="12 6 12 12 16 14"></polyline>
                      </svg>
                      {{ formatTime(result.timestamp) }}
                    </span>
                    <span v-if="result.score" class="result-score" :style="{ color: getScoreColor(result.score) }">
                      {{ result.score }}分
                    </span>
                  </div>

                  <h4 class="result-title">{{ result.title }}</h4>
                  <p class="result-desc">{{ result.description }}</p>

                  <div v-if="result.suggestions && result.suggestions.length > 0" class="result-suggestions">
                    <div v-for="(suggestion, idx) in result.suggestions" :key="idx" class="suggestion-item">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="9 11 12 14 22 4"></polyline>
                        <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path>
                      </svg>
                      {{ suggestion }}
                    </div>
                  </div>

                  <div class="jump-hint">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polygon points="5 3 19 12 5 21 5 3"></polygon>
                    </svg>
                    点击跳转
                  </div>
                </div>
              </div>

              <div v-if="analysisData.summary" class="summary-section">
                <h4>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                    <polyline points="14 2 14 8 20 8"></polyline>
                    <line x1="16" y1="13" x2="8" y2="13"></line>
                    <line x1="16" y1="17" x2="8" y2="17"></line>
                    <polyline points="10 9 9 9 8 9"></polyline>
                  </svg>
                  分析建议
                </h4>
                <p>{{ analysisData.summary }}</p>
              </div>
            </div>

            <div v-else class="empty-panel">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0-2-2h-3.5L14 14"></path>
                <circle cx="12" cy="13" r="3"></circle>
              </svg>
              <p>暂无分析数据</p>
              <span>请先上传视频进行分析</span>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script lang="ts">
export default {
  name: 'AnalysisPanel'
}
</script>

<style scoped>
.analysis-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
}

.analysis-panel {
  width: 420px;
  height: 100%;
  background: var(--bg-card, #ffffff);
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color, #e2e8f0);
  flex-shrink: 0;
}

.panel-header h3 {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary, #1e293b);
}

.close-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--bg-primary, #f8fafc);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  color: var(--text-secondary, #64748b);
}

.close-btn:hover {
  background: var(--hover-bg, #f1f5f9);
  color: var(--text-primary, #1e293b);
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.total-score-section {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: var(--bg-primary, #f8fafc);
  border-radius: 16px;
}

.score-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 4px solid;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.score-value {
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1;
}

.score-label {
  font-size: 0.7rem;
  color: var(--text-secondary, #64748b);
  margin-top: 4px;
}

.score-info h4 {
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 6px 0;
  color: var(--text-primary, #1e293b);
}

.score-info p {
  font-size: 0.8rem;
  color: var(--text-secondary, #64748b);
  margin: 0;
  line-height: 1.5;
}

.tabs-container {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.tab-btn {
  flex: 1;
  padding: 10px 12px;
  border-radius: 10px;
  border: 2px solid var(--border-color, #e2e8f0);
  background: var(--bg-card, #ffffff);
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--text-secondary, #64748b);
}

.tab-btn:hover {
  border-color: var(--primary-blue, #0ea5e9);
  color: var(--primary-blue, #0ea5e9);
}

.tab-btn.active {
  background: var(--primary-light, #e0f2fe);
  border-color: var(--primary-blue, #0ea5e9);
  color: var(--primary-blue, #0ea5e9);
}

.tab-count {
  font-size: 0.7rem;
  padding: 2px 6px;
  border-radius: 10px;
  background: var(--border-color, #e2e8f0);
}

.tab-btn.active .tab-count {
  background: var(--primary-blue, #0ea5e9);
  color: white;
}

.results-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 200px;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary, #64748b);
  gap: 12px;
}

.empty-state p {
  margin: 0;
  font-size: 0.9rem;
}

.result-card {
  padding: 16px;
  background: var(--bg-primary, #f8fafc);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.result-card:hover {
  background: var(--hover-bg, #f1f5f9);
  border-color: var(--primary-blue, #0ea5e9);
  transform: translateX(4px);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.result-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.8rem;
  color: var(--text-secondary, #64748b);
}

.result-score {
  font-size: 0.85rem;
  font-weight: 600;
}

.result-title {
  font-size: 0.95rem;
  font-weight: 600;
  margin: 0 0 6px 0;
  color: var(--text-primary, #1e293b);
}

.result-desc {
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
  margin: 0 0 10px 0;
  line-height: 1.5;
}

.result-suggestions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 10px;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 0.8rem;
  color: var(--text-secondary, #64748b);
  line-height: 1.4;
}

.suggestion-item svg {
  flex-shrink: 0;
  margin-top: 2px;
  color: var(--primary-blue, #0ea5e9);
}

.jump-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.75rem;
  color: var(--primary-blue, #0ea5e9);
  font-weight: 500;
}

.summary-section {
  padding: 16px;
  background: linear-gradient(135deg, #e0f2fe 0%, #f0f9ff 100%);
  border-radius: 12px;
  flex-shrink: 0;
}

.summary-section h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  margin: 0 0 10px 0;
  color: var(--primary-blue, #0ea5e9);
}

.summary-section p {
  font-size: 0.85rem;
  color: var(--text-primary, #1e293b);
  margin: 0;
  line-height: 1.6;
}

.empty-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--text-secondary, #64748b);
  padding: 40px;
}

.empty-panel p {
  margin: 0;
  font-size: 1rem;
  font-weight: 500;
}

.empty-panel span {
  font-size: 0.85rem;
}

.overlay-enter-active,
.overlay-leave-active {
  transition: opacity 0.3s ease;
}

.overlay-enter-from,
.overlay-leave-to {
  opacity: 0;
}

.panel-enter-active,
.panel-leave-active {
  transition: transform 0.3s ease;
}

.panel-enter-from,
.panel-leave-to {
  transform: translateX(100%);
}

.panel-content::-webkit-scrollbar {
  width: 4px;
}

.panel-content::-webkit-scrollbar-track {
  background: transparent;
}

.panel-content::-webkit-scrollbar-thumb {
  background: var(--border-color, #e2e8f0);
  border-radius: 2px;
}

.panel-content::-webkit-scrollbar-thumb:hover {
  background: var(--primary-blue, #0ea5e9);
}
</style>

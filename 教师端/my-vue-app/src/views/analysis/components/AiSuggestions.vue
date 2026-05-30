<script setup lang="ts">
import { getPriorityClass, getPriorityLabel } from '../constants'

defineProps<{
  suggestions: Array<{
    priority: string
    title: string
    desc: string
    items: string[]
  }>
}>()
</script>

<template>
  <el-card class="analysis-card ai-suggestions" shadow="never">
    <template #header>
      <h3>🤖 AI教学建议</h3>
    </template>
    <div class="suggestion-list">
      <div
        v-for="suggestion in suggestions"
        :key="suggestion.title"
        :class="['suggestion-item', getPriorityClass(suggestion.priority)]"
      >
        <div class="suggestion-header">
          <span :class="['priority-badge', getPriorityClass(suggestion.priority)]">
            {{ getPriorityLabel(suggestion.priority) }}
          </span>
          <span class="suggestion-title">{{ suggestion.title }}</span>
        </div>
        <p>{{ suggestion.desc }}</p>
        <ul>
          <li v-for="(item, idx) in suggestion.items" :key="idx">{{ item }}</li>
        </ul>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.ai-suggestions {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #bae6fd;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.suggestion-item {
  background: white;
  padding: 16px;
  border-radius: 12px;
  border-left: 4px solid #94a3b8;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

.suggestion-item.high-priority { border-left-color: #ef4444; }
.suggestion-item.medium-priority { border-left-color: #f59e0b; }
.suggestion-item.low-priority { border-left-color: #10b981; }

.suggestion-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.priority-badge {
  font-size: 0.7rem;
  padding: 2px 8px;
  border-radius: 10px;
  background: #f1f5f9;
  color: #64748b;
  font-weight: 600;
}

.priority-badge.high-priority { background: #fee2e2; color: #ef4444; }
.priority-badge.medium-priority { background: #fef3c7; color: #d97706; }
.priority-badge.low-priority { background: #d1fae5; color: #10b981; }

.suggestion-title {
  font-weight: 700;
  color: #1e293b;
}

.suggestion-item p {
  font-size: 0.9rem;
  color: #475569;
  margin-bottom: 8px;
}

.suggestion-item ul {
  padding-left: 20px;
  margin: 0;
}

.suggestion-item li {
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 4px;
}
</style>

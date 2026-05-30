<script setup lang="ts">
import { computed } from 'vue'
import type { Student } from '../types'

const props = defineProps<{
  studentList: Student[]
  selectedId: number | null
}>()

const emit = defineEmits<{
  (e: 'select', student: Student): void
}>()

const getStatusClass = (status: string) => {
  const classes: Record<string, string> = {
    completed: 'completed',
    progress: 'progress',
    pending: 'pending'
  }
  return classes[status] || ''
}

const handleSelect = (student: Student) => {
  emit('select', student)
}
</script>

<template>
  <div class="student-list-panel">
    <div class="panel-header">
      <h3>班级学生列表</h3>
      <span class="student-count">共{{ studentList.length }}人</span>
    </div>
    <div class="student-list">
      <div
        v-for="student in studentList"
        :key="student.id"
        class="student-item"
        :class="{ active: student.id === selectedId }"
        @click="handleSelect(student)"
      >
        <div class="student-avatar">{{ student.name.charAt(0) }}</div>
        <div class="student-info">
          <div class="student-name">{{ student.name }}</div>
          <div class="student-meta">
            {{ student.studentId }} · 薄弱: {{ student.weakness || '无' }}
          </div>
        </div>
        <div class="student-status" :class="getStatusClass(student.planStatus)"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.student-list-panel {
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 24px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color, #e2e8f0);
}

.panel-header h3 {
  font-size: 1rem;
  font-weight: 600;
  margin: 0;
}

.student-count {
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
  background: var(--bg-primary, #f8fafc);
  padding: 4px 10px;
  border-radius: 30px;
}

.student-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.student-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.student-item:hover {
  background: var(--hover-bg, #f1f5f9);
}

.student-item.active {
  background: var(--primary-light, #e0f2fe);
  border-left-color: var(--primary-blue, #0ea5e9);
}

.student-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--primary-light, #e0f2fe);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: var(--primary-blue, #0ea5e9);
}

.student-info {
  flex: 1;
}

.student-name {
  font-weight: 500;
  margin-bottom: 4px;
}

.student-meta {
  font-size: 0.75rem;
  color: var(--text-secondary, #64748b);
}

.student-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.student-status.completed {
  background: var(--success, #10b981);
}

.student-status.progress {
  background: var(--warning, #f59e0b);
}

.student-status.pending {
  background: var(--danger, #ef4444);
}
</style>

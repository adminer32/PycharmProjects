<script setup lang="ts">
import { ref, computed } from 'vue'
import { User } from '@element-plus/icons-vue'

const props = defineProps<{
  students: Array<{ id: number, name: string, score: number, avatar?: string }>
  modelValue: number | null
}>()

const emit = defineEmits(['update:modelValue'])

const searchKeyword = ref('')

const filteredStudents = computed(() => {
  if (!searchKeyword.value) return props.students
  const kw = searchKeyword.value.toLowerCase()
  return props.students.filter(s => s.name.toLowerCase().includes(kw))
})

const selectStudent = (id: number) => {
  emit('update:modelValue', id)
}
</script>

<template>
  <el-card class="student-selector-card" shadow="never">
    <div class="selector-header">
      <span class="selector-label">选择学生</span>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索学生姓名"
        :prefix-icon="User"
        clearable
        size="small"
        style="width: 160px"
      />
    </div>
    <div class="student-selector-list custom-scrollbar">
      <div
        v-for="student in filteredStudents"
        :key="student.id"
        :class="['student-option', { active: modelValue === student.id }]"
        @click="selectStudent(student.id)"
      >
        <span class="student-avatar">{{ student.name.charAt(0) }}</span>
        <div class="student-info">
          <span class="student-name">{{ student.name }}</span>
          <span class="student-score">{{ student.score }}分</span>
        </div>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.student-selector-card {
  height: 100%;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
}

.selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.selector-label {
  font-weight: 600;
  color: #1e293b;
  font-size: 0.9rem;
}

.student-selector-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
}

.student-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  margin-bottom: 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.student-option:hover {
  background: #f8fafc;
  border-color: #e2e8f0;
}

.student-option.active {
  background: #f0f9ff;
  border-color: #0ea5e9;
  box-shadow: 0 4px 6px -1px rgba(14, 165, 233, 0.1);
}

.student-avatar {
  width: 36px;
  height: 36px;
  background: #e0f2fe;
  color: #0ea5e9;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 0.9rem;
  flex-shrink: 0;
}

.student-info {
  display: flex;
  flex-direction: column;
}

.student-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: #334155;
}

.student-score {
  font-size: 0.75rem;
  color: #64748b;
}

/* 自定义滚动条 */
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 2px;
}
</style>

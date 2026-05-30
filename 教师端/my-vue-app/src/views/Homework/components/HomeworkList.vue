<script setup lang="ts">
import { computed } from 'vue'
import { Edit, Delete, Bell, Download } from '@element-plus/icons-vue'
import type { HomeworkListVO } from '@/api/homeworkApi'

const props = defineProps<{
  homeworkList: HomeworkListVO[]
}>()

const emit = defineEmits<{
  (e: 'grade', homework: HomeworkListVO): void
  (e: 'viewDetail', homework: HomeworkListVO): void
  (e: 'edit', homework: HomeworkListVO): void
  (e: 'delete', homework: HomeworkListVO): void
  (e: 'remind', homework: HomeworkListVO): void
  (e: 'export', homework: HomeworkListVO): void
}>()

const getStatusType = (status: number) => {
  const types: Record<number, string> = {
    1: 'primary',
    2: 'success',
    3: 'info'
  }
  return types[status] || 'info'
}

const getStatusLabel = (status: number) => {
  const labels: Record<number, string> = {
    1: '进行中',
    2: '已批改',
    3: '已截止'
  }
  return labels[status] || '未知'
}

const getProgressPercentage = (homework: HomeworkListVO) => {
  return homework.totalCount > 0 
    ? Math.round((homework.submissionCount / homework.totalCount) * 100)
    : 0
}

const formatDeadline = (deadline: string) => {
  if (!deadline) return ''
  const date = new Date(deadline)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  const isTomorrow = new Date(now.getTime() + 86400000).toDateString() === date.toDateString()
  
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  
  if (isToday) {
    return `今天 ${hours}:${minutes}`
  } else if (isTomorrow) {
    return `明天 ${hours}:${minutes}`
  } else {
    return `${month}-${day} ${hours}:${minutes}`
  }
}
</script>

<template>
  <div class="homework-list">
    <el-card
      v-for="homework in homeworkList"
      :key="homework.id"
      class="homework-card"
      shadow="hover"
    >
      <div class="homework-header">
        <div class="homework-info">
          <h3 class="homework-title">{{ homework.title }}</h3>
          <el-tag :type="getStatusType(homework.status)" size="small">
            {{ getStatusLabel(homework.status) }}
          </el-tag>
        </div>
        <div class="homework-meta">
          <span class="meta-item"> 截止：{{ formatDeadline(homework.deadline) }}</span>
          <span class="meta-item">📊 提交：{{ homework.submissionCount }}/{{ homework.totalCount }}人</span>
          <span v-if="homework.pendingGradeCount && homework.pendingGradeCount > 0" class="meta-item">
             待批改：{{ homework.pendingGradeCount }}份
          </span>
          <span v-if="homework.averageScore !== null && homework.averageScore !== undefined" class="meta-item">
            ⭐ 平均分：{{ homework.averageScore }}
          </span>
        </div>
      </div>

      <div class="homework-content">
        <div class="homework-requirements">
          <p>📋 作业要求：{{ homework.requirements }}</p>
        </div>
        <div class="homework-progress">
          <div class="progress-header">
            <span class="progress-label">提交进度</span>
            <span class="progress-value">{{ homework.submissionCount }}/{{ homework.totalCount }}</span>
          </div>
          <el-progress
            :percentage="getProgressPercentage(homework)"
            :status="homework.status === 3 ? 'exception' : ''"
          />
        </div>
      </div>

      <div class="homework-footer">
        <div class="homework-actions">
          <el-button
            size="small"
            @click="emit('viewDetail', homework)"
          >
            📋 查看提交 ({{ homework.submissionCount }}人)
          </el-button>
          
          <el-button
            v-if="homework.status === 1"
            type="primary"
            size="small"
            @click="emit('grade', homework)"
          >
            ️ 批改作业
          </el-button>
          
          <el-button
            v-if="homework.status === 2 || homework.status === 3"
            size="small"
            :icon="Download"
            @click="emit('export', homework)"
          >
             导出成绩
          </el-button>
          
          <el-button
            v-if="homework.status === 1"
            size="small"
            :icon="Bell"
            @click="emit('remind', homework)"
          >
            📢 提醒未交
          </el-button>
          
          <el-button
            v-if="homework.status === 1"
            size="small"
            :icon="Edit"
            @click="emit('edit', homework)"
          >
            编辑
          </el-button>
          
          <el-button
            v-if="homework.status === 1"
            size="small"
            type="danger"
            :icon="Delete"
            @click="emit('delete', homework)"
          >
            撤回
          </el-button>
        </div>
      </div>
    </el-card>

    <el-empty v-if="homeworkList.length === 0" description="暂无作业" />
  </div>
</template>

<style scoped>
.homework-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.homework-card {
  border-radius: 16px;
  transition: all 0.2s;
}

.homework-card:hover {
  transform: translateY(-2px);
}

.homework-header {
  margin-bottom: 16px;
}

.homework-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  gap: 12px;
}

.homework-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.homework-meta {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  font-size: 0.85rem;
  color: #64748b;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.homework-content {
  margin-bottom: 16px;
}

.homework-requirements {
  background: #f8fafc;
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 16px;
}

.homework-requirements p {
  margin: 0;
  color: #475569;
}

.homework-progress {
  margin-bottom: 8px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 0.85rem;
  color: #64748b;
}

.homework-footer {
  border-top: 1px solid #e2e8f0;
  padding-top: 16px;
}

.homework-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>

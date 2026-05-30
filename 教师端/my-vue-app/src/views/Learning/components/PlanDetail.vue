<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { TrainingPlan, Student } from '../types'

const props = defineProps<{
  student: Student | null
  plan: TrainingPlan | null
  currentWeek?: number
  totalWeeks?: number
}>()

watch(() => props.plan, (newPlan) => {
  console.log('PlanDetail - plan changed:', newPlan)
  console.log('PlanDetail - tasks:', newPlan?.tasks)
  if (newPlan?.tasks) {
    newPlan.tasks.forEach((task, i) => {
      console.log(`Task ${i}:`, task)
    })
  }
}, { immediate: true })

const emit = defineEmits<{
  (e: 'edit'): void
  (e: 'review'): void
  (e: 'changeWeek', week: number): void
  (e: 'showHistory'): void
}>()

const progressPercent = computed(() => {
  return props.plan?.progress || 0
})

const planStatusText = computed(() => {
  if (!props.plan?.reviewStatus) return '待审核'
  const statusMap: Record<string, string> = {
    pending: '待审核',
    approved: '已批准',
    rejected: '已驳回'
  }
  return statusMap[props.plan.reviewStatus] || '待审核'
})

const planStatusType = computed(() => {
  if (!props.plan?.reviewStatus) return 'warning'
  const typeMap: Record<string, 'success' | 'warning' | 'danger'> = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  }
  return typeMap[props.plan.reviewStatus] || 'warning'
})

const displayWeek = computed(() => props.currentWeek || 1)
const displayTotalWeeks = computed(() => props.totalWeeks || 1)

function changeWeek(delta: number) {
  const newWeek = displayWeek.value + delta
  if (newWeek >= 1 && newWeek <= displayTotalWeeks.value) {
    emit('changeWeek', newWeek)
  }
}
</script>

<template>
  <div class="plan-detail-panel">
    <div class="panel-header">
      <h3>{{ student?.name || '请选择学生' }}</h3>
      <div class="plan-actions">
        <el-button
          v-if="plan"
          size="small"
          @click="emit('showHistory')"
        >
          📋 历史
        </el-button>
        <el-button
          type="primary"
          size="small"
          :disabled="!student || !plan"
          @click="emit('review')"
        >
          审核计划
        </el-button>
      </div>
    </div>

    <div class="plan-detail-content">
      <div v-if="!student" class="empty-state">
        <span class="empty-icon"></span>
        <p>点击左侧学生查看训练计划</p>
      </div>

      <div v-else-if="!plan" class="empty-state">
        <span class="empty-icon">📋</span>
        <p>该学生暂无本周AI计划</p>
        <p class="empty-hint">AI将根据学生训练数据自动生成计划</p>
      </div>

      <div v-else class="plan-card">
        <!-- 周导航器 -->
        <div v-if="displayTotalWeeks > 1" class="week-navigator">
          <el-button :disabled="displayWeek <= 1" size="small" circle @click="changeWeek(-1)">
            ←
          </el-button>
          <span class="week-label">
            第 {{ displayWeek }} 周 / 共 {{ displayTotalWeeks }} 周
            <span v-if="plan.weekStart" class="week-date">({{ plan.weekStart }} ~ {{ plan.weekEnd }})</span>
          </span>
          <el-button :disabled="displayWeek >= displayTotalWeeks" size="small" circle @click="changeWeek(1)">
            →
          </el-button>
        </div>

        <div class="plan-header">
          <div class="plan-status">
            <span class="status-label">审核状态：</span>
            <el-tag :type="planStatusType" size="small">{{ planStatusText }}</el-tag>
          </div>
          <div v-if="plan.generatedAt" class="plan-time">
            生成时间：{{ plan.generatedAt }}
          </div>
        </div>

        <div class="plan-goal">
          <div class="plan-goal-label">训练目标</div>
          <div class="plan-goal-text">{{ plan.goal }}</div>
        </div>

        <div class="plan-tasks">
          <h4>{{ displayTotalWeeks > 1 ? `第${displayWeek}周 ` : '' }}每日训练内容</h4>
          <div class="task-list">
            <div
              v-for="(task, index) in plan.tasks"
              :key="index"
              class="task-item-card"
            >
              <div class="task-info">
                <div class="task-name">{{ task.name || task.day || '训练任务' }}</div>
                <div class="task-spec">{{ task.spec || task.content || (task.target ? '目标: ' + task.target : '') || '' }}</div>
                <div v-if="task.duration" class="task-duration">{{ task.duration }}分钟</div>
              </div>
              <div class="task-progress">
                <span class="task-progress-text">完成度 {{ progressPercent }}%</span>
              </div>
            </div>
          </div>
          <div v-if="!plan.tasks || plan.tasks.length === 0" class="no-tasks-hint">
            暂无详细任务内容
          </div>
        </div>

        <div class="plan-progress">
          <h4>整体完成进度</h4>
          <div class="progress-container">
            <el-progress :percentage="progressPercent" :stroke-width="12" />
          </div>
        </div>

        <div v-if="plan.reviewComment" class="review-comment">
          <h4>审核意见</h4>
          <p class="comment-text">{{ plan.reviewComment }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.plan-detail-panel {
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
  flex-wrap: wrap;
  gap: 12px;
}

.panel-header h3 {
  font-size: 1rem;
  font-weight: 600;
  margin: 0;
}

.plan-actions {
  display: flex;
  gap: 8px;
}

.plan-detail-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  min-height: 0;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-secondary, #64748b);
}

.empty-icon {
  font-size: 3rem;
  display: block;
  margin-bottom: 12px;
}

.empty-hint {
  font-size: 0.85rem;
  margin-top: 8px;
  color: var(--text-tertiary, #94a3b8);
}

.plan-card {
  background: var(--bg-primary, #f8fafc);
  border-radius: 20px;
  padding: 20px;
}

.week-navigator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
  padding: 10px 16px;
  background: linear-gradient(135deg, #667eea11, #764ba211);
  border-radius: 12px;
  border: 1px solid #667eea22;
}

.week-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--primary-blue, #0ea5e9);
  min-width: 180px;
  text-align: center;
}

.week-date {
  font-size: 0.8rem;
  font-weight: normal;
  color: var(--text-secondary, #64748b);
}

.week-navigator .el-button {
  width: 32px;
  height: 32px;
  min-height: 32px;
}

.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color, #e2e8f0);
  flex-wrap: wrap;
  gap: 8px;
}

.plan-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-label {
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
}

.plan-time {
  font-size: 0.8rem;
  color: var(--text-tertiary, #94a3b8);
}

.plan-goal {
  background: var(--primary-light, #e0f2fe);
  padding: 12px 16px;
  border-radius: 16px;
  margin-bottom: 20px;
}

.plan-goal-label {
  font-size: 0.8rem;
  color: var(--text-secondary, #64748b);
  margin-bottom: 4px;
}

.plan-goal-text {
  font-weight: 600;
  color: var(--primary-blue, #0ea5e9);
}

.plan-tasks {
  margin-bottom: 20px;
}

.plan-tasks h4 {
  font-size: 0.9rem;
  margin-bottom: 12px;
  color: var(--text-primary, #1e293b);
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item-card {
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 12px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-info {
  flex: 1;
}

.task-name {
  font-weight: 500;
  margin-bottom: 4px;
}

.task-spec {
  font-size: 0.8rem;
  color: var(--text-secondary, #64748b);
}

.task-duration {
  font-size: 0.75rem;
  color: var(--primary-blue, #0ea5e9);
  margin-top: 2px;
}

.no-tasks-hint {
  text-align: center;
  padding: 16px;
  color: var(--text-tertiary, #94a3b8);
  font-size: 0.9rem;
}

.task-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}

.task-progress-text {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--primary-blue, #0ea5e9);
}

.plan-progress {
  margin-top: 20px;
}

.plan-progress h4 {
  font-size: 0.9rem;
  margin-bottom: 12px;
  color: var(--text-primary, #1e293b);
}

.progress-container {
  margin-top: 8px;
}

.review-comment {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color, #e2e8f0);
}

.review-comment h4 {
  font-size: 0.9rem;
  margin-bottom: 8px;
  color: var(--text-primary, #1e293b);
}

.comment-text {
  padding: 12px;
  background: rgba(239, 68, 68, 0.05);
  border-radius: 8px;
  color: var(--danger, #ef4444);
  font-size: 0.9rem;
  margin: 0;
}
</style>

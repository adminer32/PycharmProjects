<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { TrainingPlan, Student } from '../types'

const props = defineProps<{
  modelValue: boolean
  student: Student | null
  plan: TrainingPlan | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'approve', plan: TrainingPlan): void
  (e: 'reject', plan: TrainingPlan, reason: string): void
}>()

const rejectReason = ref('')
const showRejectInput = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
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

const handleApprove = () => {
  if (!props.plan) return
  emit('approve', props.plan)
  ElMessage.success('计划已批准')
  visible.value = false
}

const handleReject = () => {
  if (!props.plan) return
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  emit('reject', props.plan, rejectReason.value)
  ElMessage.success('计划已驳回')
  rejectReason.value = ''
  showRejectInput.value = false
  visible.value = false
}

const handleClose = () => {
  rejectReason.value = ''
  showRejectInput.value = false
  visible.value = false
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="审核训练计划"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-if="student && plan" class="review-content">
      <div class="student-info">
        <span class="label">学生：</span>
        <span class="value">{{ student.name }} ({{ student.studentId }})</span>
      </div>
      
      <div class="plan-status">
        <span class="label">审核状态：</span>
        <el-tag :type="planStatusType">{{ planStatusText }}</el-tag>
      </div>

      <div class="plan-goal">
        <span class="label">训练目标：</span>
        <p class="goal-text">{{ plan.goal }}</p>
      </div>

      <div class="plan-tasks">
        <span class="label">训练任务：</span>
        <div class="tasks-list">
          <div v-for="(task, index) in plan.tasks" :key="index" class="task-item">
            <span class="task-index">{{ index + 1 }}.</span>
            <div class="task-content">
              <span class="task-name">{{ task.name }}</span>
              <span class="task-spec">{{ task.spec }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="plan.reviewComment" class="review-comment">
        <span class="label">审核意见：</span>
        <p class="comment-text">{{ plan.reviewComment }}</p>
      </div>

      <div v-if="showRejectInput" class="reject-section">
        <el-input
          v-model="rejectReason"
          type="textarea"
          placeholder="请输入驳回原因（必填）"
          :rows="3"
          maxlength="200"
          show-word-limit
        />
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <template v-if="!showRejectInput">
          <el-button @click="handleClose">关闭</el-button>
          <el-button type="danger" @click="showRejectInput = true">驳回</el-button>
          <el-button type="primary" @click="handleApprove">批准</el-button>
        </template>
        <template v-else>
          <el-button @click="showRejectInput = false">取消</el-button>
          <el-button type="danger" @click="handleReject">确认驳回</el-button>
        </template>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.review-content {
  padding: 0 10px;
}

.student-info,
.plan-status,
.plan-goal,
.plan-tasks,
.review-comment {
  margin-bottom: 20px;
}

.label {
  font-weight: 600;
  color: var(--text-primary, #1e293b);
  margin-right: 8px;
}

.value {
  color: var(--text-secondary, #64748b);
}

.goal-text {
  margin: 8px 0 0 0;
  padding: 12px;
  background: var(--bg-primary, #f8fafc);
  border-radius: 8px;
  color: var(--text-primary, #1e293b);
}

.tasks-list {
  margin-top: 12px;
}

.task-item {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: var(--bg-primary, #f8fafc);
  border-radius: 8px;
  margin-bottom: 8px;
}

.task-index {
  font-weight: 600;
  color: var(--primary-blue, #0ea5e9);
}

.task-content {
  flex: 1;
}

.task-name {
  display: block;
  font-weight: 500;
  margin-bottom: 4px;
}

.task-spec {
  display: block;
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
}

.comment-text {
  margin: 8px 0 0 0;
  padding: 12px;
  background: rgba(239, 68, 68, 0.05);
  border-radius: 8px;
  color: var(--danger, #ef4444);
}

.reject-section {
  margin-top: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

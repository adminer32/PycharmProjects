<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSubmissions, type SubmissionVO, type HomeworkListVO } from '@/api/homeworkApi'

const props = defineProps<{
  modelValue: boolean
  homework: HomeworkListVO | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const router = useRouter()
const submissions = ref<SubmissionVO[]>([])
const loading = ref(false)

const formatDateTime = (dateTime: string) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

const loadSubmissions = async () => {
  if (!props.homework?.id) return

  loading.value = true
  try {
    const res = await getSubmissions(props.homework.id)
    if (res.data) {
      submissions.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载提交列表失败')
  } finally {
    loading.value = false
  }
}

watch(() => props.modelValue, (val) => {
  if (val && props.homework?.id) {
    loadSubmissions()
  }
})

const getStatusType = (status: number) => {
  if (status === -1) return 'info'
  return status === 0 ? 'warning' : 'success'
}

const getStatusLabel = (status: number) => {
  if (status === -1) return '未提交'
  return status === 0 ? '待批改' : '已批改'
}

const stats = computed(() => {
  const total = submissions.value.length
  const submitted = submissions.value.filter(s => s.submitted).length
  const notSubmitted = total - submitted
  const submissionRate = total > 0 ? Math.round((submitted / total) * 100) : 0
  return { total, submitted, notSubmitted, submissionRate }
})

const handleGrade = (submission: SubmissionVO) => {
  emit('update:modelValue', false)
  router.push({
    path: '/homework/grade',
    query: { homeworkId: props.homework?.id, studentId: submission.studentId }
  })
}

const handleViewFeedback = (submission: SubmissionVO) => {
  emit('update:modelValue', false)
  router.push({
    path: '/homework/grade',
    query: { homeworkId: props.homework?.id, studentId: submission.studentId }
  })
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="`作业提交详情 - ${homework?.title || ''}`"
    width="800px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <!-- 统计信息 -->
    <div class="stats-summary">
      <div class="stat-item">
        <span class="stat-label">班级总人数</span>
        <span class="stat-value">{{ stats.total }}人</span>
      </div>
      <div class="stat-item submitted">
        <span class="stat-label">已提交</span>
        <span class="stat-value">{{ stats.submitted }}人</span>
      </div>
      <div class="stat-item not-submitted">
        <span class="stat-label">未提交</span>
        <span class="stat-value">{{ stats.notSubmitted }}人</span>
      </div>
      <div class="stat-item rate">
        <span class="stat-label">提交率</span>
        <span class="stat-value">{{ stats.submissionRate }}%</span>
      </div>
    </div>

    <el-table v-loading="loading" :data="submissions" style="width: 100%">
      <el-table-column label="学生" width="150">
        <template #default="{ row }">
          <div class="student-cell">
            <div class="student-avatar-small">
              <img v-if="row.studentAvatar" :src="row.studentAvatar" alt="avatar" />
              <span v-else>{{ row.studentName?.charAt(0) }}</span>
            </div>
            <span>{{ row.studentName }}</span>
          </div>
        </template>
      </el-table-column>
      
      <el-table-column label="提交时间" width="160">
        <template #default="{ row }">
          <span v-if="row.submitted">{{ formatDateTime(row.submitTime || '') }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      
      <el-table-column label="AI评分" width="80" align="center">
        <template #default="{ row }">
          <span v-if="row.submitted && row.aiScore">{{ row.aiScore }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      
      <el-table-column label="教师评分" width="100" align="center">
        <template #default="{ row }">
          <span v-if="row.teacherScore">{{ row.teacherScore }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <template v-if="row.submitted">
            <el-button
              v-if="row.status === 0"
              type="primary"
              size="small"
              text
              @click="handleGrade(row)"
            >
              批改
            </el-button>
            <el-button
              v-else
              size="small"
              text
              @click="handleViewFeedback(row)"
            >
              查看
            </el-button>
          </template>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<style scoped>
.student-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.student-avatar-small {
  width: 28px;
  height: 28px;
  background: #0ea5e9;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 600;
  overflow: hidden;
}

.student-avatar-small img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.text-muted {
  color: #94a3b8;
}

.stats-summary {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.stat-label {
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
}

.stat-item.submitted .stat-value {
  color: #10b981;
}

.stat-item.not-submitted .stat-value {
  color: #f59e0b;
}

.stat-item.rate .stat-value {
  color: #0ea5e9;
}
</style>

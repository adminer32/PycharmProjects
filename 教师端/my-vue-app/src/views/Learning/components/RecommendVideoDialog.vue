<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { VideoItem, Student } from '../types'

const props = defineProps<{
  modelValue: boolean
  video: VideoItem | null
  students: Student[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'recommend', studentIds: number[], videoId: string, comment: string): void
}>()

const selectedStudentIds = ref<number[]>([])
const comment = ref('')
const activeTab = ref('select')

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const recommendedStudents = computed(() => {
  if (!props.video?.recommendedTo) return []
  return props.students.filter(s => props.video!.recommendedTo!.includes(s.id))
})

const unrecommendedStudents = computed(() => {
  if (!props.video?.recommendedTo) return props.students
  return props.students.filter(s => !props.video!.recommendedTo!.includes(s.id))
})

const handleSelectAll = () => {
  const availableStudents = unrecommendedStudents.value
  const allSelected = availableStudents.every(s => selectedStudentIds.value.includes(s.id))
  
  if (allSelected) {
    selectedStudentIds.value = selectedStudentIds.value.filter(id => 
      !availableStudents.some(s => s.id === id)
    )
  } else {
    const availableIds = availableStudents.map(s => s.id)
    selectedStudentIds.value = [...new Set([...selectedStudentIds.value, ...availableIds])]
  }
}

const handleConfirm = () => {
  if (selectedStudentIds.value.length === 0) {
    ElMessage.warning('请至少选择一名学生')
    return
  }
  if (!props.video) return
  
  emit('recommend', selectedStudentIds.value, props.video.id, comment.value)
  ElMessage.success(`已成功推荐给 ${selectedStudentIds.value.length} 名学生`)
  handleClose()
}

const handleClose = () => {
  selectedStudentIds.value = []
  comment.value = ''
  activeTab.value = 'select'
  visible.value = false
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="推荐视频给学生"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
    class="recommend-dialog"
  >
    <div v-if="video" class="recommend-content">
      <div class="video-preview">
        <div class="video-thumb"></div>
        <div class="video-details">
          <div class="video-title">{{ video.title }}</div>
          <div class="video-duration">{{ video.duration }}</div>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="recommend-tabs">
        <el-tab-pane label="选择学生" name="select">
          <div class="select-section">
            <div class="section-header">
              <span class="section-title">选择要推荐的学生</span>
              <el-button 
                v-if="unrecommendedStudents.length > 0"
                type="primary" 
                link 
                size="small" 
                @click="handleSelectAll"
              >
                {{ unrecommendedStudents.every(s => selectedStudentIds.includes(s.id)) ? '取消全选' : '全选' }}
              </el-button>
            </div>
            
            <div v-if="unrecommendedStudents.length === 0" class="empty-state">
              <span class="empty-icon">✓</span>
              <p>该视频已推荐给所有学生</p>
            </div>
            
            <el-checkbox-group v-else v-model="selectedStudentIds" class="students-grid">
              <el-checkbox 
                v-for="student in unrecommendedStudents" 
                :key="student.id" 
                :label="student.id"
                class="student-card"
              >
                <div class="student-avatar">{{ student.name[0] }}</div>
                <div class="student-info">
                  <div class="student-name">{{ student.name }}</div>
                  <div class="student-id">{{ student.studentId }}</div>
                </div>
              </el-checkbox>
            </el-checkbox-group>
            
            <div class="selected-bar">
              <span class="selected-text">已选择 {{ selectedStudentIds.length }} 名学生</span>
            </div>
          </div>

          <div class="comment-section">
            <span class="section-label">备注（可选）</span>
            <el-input
              v-model="comment"
              type="textarea"
              placeholder="给学生留言..."
              :rows="2"
              maxlength="200"
              show-word-limit
            />
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`已推荐 (${recommendedStudents.length})`" name="recommended">
          <div class="recommended-section">
            <div v-if="recommendedStudents.length === 0" class="empty-state">
              <span class="empty-icon">📭</span>
              <p>该视频尚未推荐给任何学生</p>
            </div>
            
            <div v-else class="recommended-list">
              <div 
                v-for="student in recommendedStudents" 
                :key="student.id" 
                class="recommended-student-card"
              >
                <div class="student-avatar">{{ student.name[0] }}</div>
                <div class="student-info">
                  <div class="student-name">{{ student.name }}</div>
                  <div class="student-id">{{ student.studentId }}</div>
                </div>
                <el-tag type="success" size="small">已推荐</el-tag>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button 
          v-if="activeTab === 'select'"
          type="primary" 
          :disabled="selectedStudentIds.length === 0"
          @click="handleConfirm"
        >
          确认推荐
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.recommend-content {
  padding: 0;
}

.video-preview {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: var(--bg-primary, #f8fafc);
  border-radius: 10px;
  margin-bottom: 20px;
  border: 1px solid var(--border-color, #e2e8f0);
}

.video-thumb {
  width: 72px;
  height: 54px;
  background: var(--border-color, #e2e8f0);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.video-thumb::before {
  content: '▶';
  font-size: 1.5rem;
  color: var(--text-secondary, #64748b);
}

.video-details {
  flex: 1;
}

.video-title {
  font-weight: 600;
  color: var(--text-primary, #1e293b);
  margin-bottom: 4px;
}

.video-duration {
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
}

.recommend-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.select-section,
.recommended-section {
  min-height: 200px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  font-weight: 600;
  color: var(--text-primary, #1e293b);
}

.section-label {
  display: block;
  font-weight: 600;
  color: var(--text-primary, #1e293b);
  margin-bottom: 8px;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-secondary, #64748b);
}

.empty-icon {
  font-size: 2.5rem;
  display: block;
  margin-bottom: 12px;
}

.students-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  max-height: 240px;
  overflow-y: auto;
  padding: 4px;
}

.student-card {
  margin: 0;
  padding: 12px;
  border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 12px;
  transition: all 0.2s;
}

.student-card:hover {
  border-color: var(--primary-blue, #0ea5e9);
  background: var(--bg-primary, #f8fafc);
}

.student-card :deep(.el-checkbox__input) {
  position: absolute;
  top: 8px;
  right: 8px;
}

.student-card :deep(.el-checkbox__label) {
  padding: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.student-avatar {
  width: 36px;
  height: 36px;
  background: var(--primary-blue, #0ea5e9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 0.9rem;
  flex-shrink: 0;
}

.student-info {
  flex: 1;
}

.student-name {
  font-weight: 500;
  color: var(--text-primary, #1e293b);
  margin-bottom: 2px;
}

.student-id {
  font-size: 0.8rem;
  color: var(--text-secondary, #64748b);
}

.selected-bar {
  margin-top: 14px;
  padding: 10px 14px;
  background: var(--bg-primary, #f8fafc);
  border-radius: 8px;
  text-align: center;
  border: 1px solid var(--border-color, #e2e8f0);
}

.selected-text {
  font-weight: 500;
  color: var(--text-secondary, #64748b);
  font-size: 0.9rem;
}

.comment-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color, #e2e8f0);
}

.recommended-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 280px;
  overflow-y: auto;
}

.recommended-student-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: var(--bg-primary, #f8fafc);
  border-radius: 10px;
  border: 1px solid var(--border-color, #e2e8f0);
}

.recommended-student-card .student-avatar {
  background: #10b981;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 600px) {
  .students-grid {
    grid-template-columns: 1fr;
  }
}
</style>

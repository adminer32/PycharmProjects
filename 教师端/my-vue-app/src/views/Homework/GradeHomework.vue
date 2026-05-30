<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, VideoPlay, Check, User, Document, Timer } from '@element-plus/icons-vue'
import { getHomeworkDetail, getSubmissions, gradeSubmission, type HomeworkDetailVO, type SubmissionVO, type GradeSubmissionDTO } from '@/api/homeworkApi'

const route = useRoute()
const router = useRouter()

const homeworkId = computed(() => route.query.homeworkId as string)

const homework = ref<HomeworkDetailVO | null>(null)
const submissions = ref<SubmissionVO[]>([])
const currentStudentIndex = ref(0)
const scoreInput = ref(0)
const feedbackInput = ref('')
const searchKeyword = ref('')
const loading = ref(false)

const currentSubmission = computed(() => submissions.value[currentStudentIndex.value])

const stats = computed(() => ({
  total: submissions.value.length,
  pending: submissions.value.filter(s => s.status === 0).length,
  graded: submissions.value.filter(s => s.status === 1).length,
  unsubmitted: submissions.value.filter(s => s.status === -1).length
}))

const filteredSubmissions = computed(() => {
  if (!searchKeyword.value) return submissions.value
  const keyword = searchKeyword.value.toLowerCase()
  return submissions.value.filter(s => 
    s.studentName.toLowerCase().includes(keyword) ||
    String(s.studentId).includes(keyword)
  )
})

const loadHomeworkData = async () => {
  if (!homeworkId.value) return
  
  loading.value = true
  try {
    const detailRes = await getHomeworkDetail(Number(homeworkId.value))
    if (detailRes.data) {
      homework.value = detailRes.data
    }
    
    const submissionsRes = await getSubmissions(Number(homeworkId.value))
    if (submissionsRes.data) {
      submissions.value = submissionsRes.data
      if (submissions.value.length > 0) {
        loadCurrentSubmission()
      }
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadHomeworkData()
})

const loadCurrentSubmission = () => {
  if (currentSubmission.value) {
    scoreInput.value = currentSubmission.value.teacherScore || currentSubmission.value.aiScore || 0
    feedbackInput.value = currentSubmission.value.feedback || ''
  }
}

const handlePrevStudent = () => {
  if (currentStudentIndex.value > 0) {
    currentStudentIndex.value--
    loadCurrentSubmission()
  }
}

const handleNextStudent = () => {
  if (currentStudentIndex.value < submissions.value.length - 1) {
    currentStudentIndex.value++
    loadCurrentSubmission()
  }
}

const handleSelectStudent = (index: number) => {
  const submission = filteredSubmissions.value[index]
  if (submission) {
    currentStudentIndex.value = submissions.value.findIndex(s => s.id === submission.id)
    loadCurrentSubmission()
  }
}

const handleSaveGrade = async () => {
  if (!currentSubmission.value) return
  
  if (scoreInput.value < 0 || scoreInput.value > 100) {
    ElMessage.warning('请输入有效的分数 (0-100)')
    return
  }
  
  try {
    const dto: GradeSubmissionDTO = {
      teacherScore: scoreInput.value,
      teacherComment: feedbackInput.value,
      aiSuggestion: currentSubmission.value.aiSuggestion || undefined
    }
    await gradeSubmission(currentSubmission.value.id, dto)
    
    currentSubmission.value.teacherScore = scoreInput.value
    currentSubmission.value.feedback = feedbackInput.value
    currentSubmission.value.status = 1
    
    ElMessage.success(`${currentSubmission.value.studentName} 的作业已批改完成`)
    
    if (currentStudentIndex.value < submissions.value.length - 1) {
      handleNextStudent()
    }
  } catch (error) {
    ElMessage.error('保存批改失败')
  }
}

const handleBack = () => {
  router.push('/homework')
}

const getScoreColor = (score: number | null) => {
  if (!score) return '#64748b'
  if (score >= 90) return '#10b981'
  if (score >= 80) return '#3b82f6'
  if (score >= 70) return '#f59e0b'
  if (score >= 60) return '#f97316'
  return '#ef4444'
}

const handleVideoError = (event: Event) => {
  const video = event.target as HTMLVideoElement
  console.error('视频加载失败:', {
    src: video.src,
    error: video.error,
    submission: currentSubmission.value
  })
  ElMessage.error('视频加载失败，请检查视频链接是否有效')
}

const formatDeadline = (dateTime: string) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
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

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formatTime = (timeStr: string) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

const parseAiSuggestion = (suggestion: string | null | undefined): string => {
  if (!suggestion) return ''
  try {
    const parsed = JSON.parse(suggestion)
    if (typeof parsed === 'object' && parsed !== null) {
      if (parsed.suggestions && Array.isArray(parsed.suggestions)) {
        return parsed.suggestions.join('\n')
      }
      if (parsed.advice) {
        return parsed.advice
      }
      return JSON.stringify(parsed, null, 2)
    }
    return suggestion
  } catch {
    return suggestion
  }
}

const isValidAvatarUrl = (url: string | null | undefined): boolean => {
  if (!url) return false
  if (url === 'avatar') return false
  return url.startsWith('http') || url.startsWith('/') || url.startsWith('data:')
}
</script>

<template>
  <div class="grade-page" v-loading="loading">
    <div class="page-header">
      <div class="header-left">
        <el-button class="back-btn" @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
          返回作业列表
        </el-button>
        <div class="homework-title" v-if="homework">
          <h1>{{ homework.title }}</h1>
          <div class="homework-meta">
            <span><el-icon><Timer /></el-icon> 截止：{{ formatDeadline(homework.deadline) }}</span>
            <span><el-icon><Document /></el-icon> 提交：{{ homework.submissionCount }}/{{ homework.totalCount }} 人</span>
          </div>
        </div>
      </div>
      <div class="header-stats">
        <div class="stat-item">
          <span class="stat-value">{{ stats.total }}</span>
          <span class="stat-label">总人数</span>
        </div>
        <div class="stat-item unsubmitted">
          <span class="stat-value">{{ stats.unsubmitted }}</span>
          <span class="stat-label">未提交</span>
        </div>
        <div class="stat-item pending">
          <span class="stat-value">{{ stats.pending }}</span>
          <span class="stat-label">待批改</span>
        </div>
        <div class="stat-item done">
          <span class="stat-value">{{ stats.graded }}</span>
          <span class="stat-label">已完成</span>
        </div>
      </div>
    </div>

    <div class="main-content">
      <div class="student-sidebar">
        <div class="sidebar-header">
          <h3>学生列表</h3>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索学生"
            clearable
            prefix-icon="Search"
            size="small"
          />
        </div>
        <div class="student-list">
          <div
            v-for="(submission, index) in filteredSubmissions"
            :key="submission.studentId"
            :class="['student-card', { 
              active: currentSubmission?.studentId === submission.studentId,
              graded: submission.status === 1
            }]"
            @click="handleSelectStudent(index)"
          >
            <div class="student-avatar">
              <img v-if="isValidAvatarUrl(submission.studentAvatar)" :src="submission.studentAvatar" alt="avatar" />
              <span v-else>{{ submission.studentName?.charAt(0) }}</span>
            </div>
            <div class="student-info">
              <span class="student-name">{{ submission.studentName }}</span>
              <span class="student-id" v-if="submission.studentAccount">{{ submission.studentAccount }}</span>
            </div>
            <div class="student-score">
              <span v-if="submission.status === 1" class="score-badge graded">
                {{ submission.teacherScore }}
              </span>
              <span v-else-if="submission.status === 0" class="score-badge pending">待批改</span>
              <span v-else class="score-badge unsubmitted">未提交</span>
            </div>
          </div>
        </div>
      </div>

      <div class="content-area" v-if="currentSubmission">
        <div class="video-panel">
          <div class="video-header">
            <h3>视频预览</h3>
            <div class="video-actions">
              <el-button-group size="small">
                <el-button>0.5x</el-button>
                <el-button type="primary">1x</el-button>
                <el-button>1.5x</el-button>
                <el-button>2x</el-button>
              </el-button-group>
            </div>
          </div>
          <div class="video-player">
            <div class="video-placeholder" v-if="!currentSubmission.videoUrl">
              <el-icon :size="64"><VideoPlay /></el-icon>
              <p>暂无视频</p>
            </div>
            <video 
              v-else 
              :src="currentSubmission.videoUrl" 
              controls 
              class="video-element"
              @error="handleVideoError"
            ></video>
          </div>
        </div>

        <div class="grade-panel">
          <div class="scroll-area">
            <div class="panel-section student-section">
            <div class="student-profile">
              <div class="avatar-large" :style="isValidAvatarUrl(currentSubmission.studentAvatar) ? {} : { background: getScoreColor(currentSubmission.aiScore) }">
                <img v-if="isValidAvatarUrl(currentSubmission.studentAvatar)" :src="currentSubmission.studentAvatar" alt="avatar" />
                <span v-else>{{ currentSubmission.studentName?.charAt(0) }}</span>
              </div>
              <div class="profile-info">
                <h2>{{ currentSubmission.studentName }}</h2>
                <p>
                  <span v-if="currentSubmission.studentAccount">{{ currentSubmission.studentAccount }}</span>
                  <span class="divider" v-if="currentSubmission.submitTime">|</span>
                  <span v-if="currentSubmission.submitTime">提交于 {{ formatDateTime(currentSubmission.submitTime) }}</span>
                </p>
              </div>
              <el-tag 
                v-if="currentSubmission.status !== -1"
                :type="currentSubmission.status === 1 ? 'success' : 'warning'" 
                size="large"
                effect="dark"
              >
                {{ currentSubmission.status === 1 ? '已批改' : '待批改' }}
              </el-tag>
              <el-tag v-else type="info" size="large" effect="dark">未提交</el-tag>
            </div>
          </div>

          <div class="panel-section" v-if="homework">
            <div class="section-header">
              <el-icon><Document /></el-icon>
              <span>作业要求</span>
            </div>
            <div class="requirement-content">
              {{ homework.requirements }}
            </div>
          </div>

          <div class="panel-section ai-section">
            <div class="section-header">
              <span class="ai-badge">AI</span>
              <span>智能分析</span>
            </div>
            <div class="ai-content">
              <div class="ai-score-display">
                <div class="score-ring" :style="{ borderColor: getScoreColor(currentSubmission.aiScore) }">
                  <span class="score-number" :style="{ color: getScoreColor(currentSubmission.aiScore) }">
                    {{ currentSubmission.aiScore }}
                  </span>
                  <span class="score-label">AI评分</span>
                </div>
              </div>
              <div class="ai-feedback">
                <h4>AI 建议</h4>
                <div v-if="currentSubmission.aiSuggestion" class="ai-suggestion-content">
                  <p v-for="(line, idx) in parseAiSuggestion(currentSubmission.aiSuggestion).split('\n')" :key="idx">
                    {{ line }}
                  </p>
                </div>
                <p v-else class="text-muted">暂无 AI 建议</p>
              </div>
            </div>
          </div>

          <div class="panel-section grade-section">
            <div class="section-header">
              <el-icon><Check /></el-icon>
              <span>教师批改</span>
            </div>
            <div class="grade-content">
              <div class="score-input-row">
                <label>教师评分</label>
                <div class="score-slider">
                  <el-slider
                    v-model="scoreInput"
                    :min="0"
                    :max="100"
                    :format-tooltip="(val: number) => `${val}分`"
                  />
                </div>
                <el-input-number
                  v-model="scoreInput"
                  :min="0"
                  :max="100"
                  size="large"
                  controls-position="right"
                />
                <span class="score-suffix">分</span>
              </div>
              <div class="feedback-input">
                <label>教师评语</label>
                <el-input
                  v-model="feedbackInput"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入对学生作业的评语和建议..."
                  resize="none"
                />
              </div>
            </div>
          </div>
          </div>

          <div class="panel-actions">
            <el-button size="large" @click="handlePrevStudent" :disabled="currentStudentIndex === 0">
              <el-icon><ArrowLeft /></el-icon>
              上一个
            </el-button>
            <el-button size="large" @click="handleNextStudent" :disabled="currentStudentIndex === submissions.length - 1">
              下一个
              <el-icon class="el-icon--right"><ArrowLeft /></el-icon>
            </el-button>
            <el-button type="primary" size="large" @click="handleSaveGrade">
              <el-icon><Check /></el-icon>
              确认批改
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.grade-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: white;
  border-bottom: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.back-btn {
  border: none;
  background: #f1f5f9;
  color: #64748b;
  font-weight: 500;
}

.back-btn:hover {
  background: #e2e8f0;
  color: #334155;
}

.homework-title h1 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: #0f172a;
}

.homework-meta {
  display: flex;
  gap: 16px;
  margin-top: 4px;
  color: #64748b;
  font-size: 0.875rem;
}

.homework-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  text-align: center;
  padding: 12px 20px;
  background: #f8fafc;
  border-radius: 12px;
  min-width: 80px;
}

.stat-item .stat-value {
  display: block;
  font-size: 1.75rem;
  font-weight: 700;
  color: #0f172a;
}

.stat-item .stat-label {
  font-size: 0.75rem;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-item.pending .stat-value {
  color: #f59e0b;
}

.stat-item.done .stat-value {
  color: #10b981;
}

.main-content {
  flex: 1;
  display: flex;
  gap: 24px;
  padding: 24px;
  overflow: hidden;
}

.student-sidebar {
  width: 300px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #e2e8f0;
}

.sidebar-header h3 {
  margin: 0 0 12px;
  font-size: 1rem;
  font-weight: 600;
  color: #0f172a;
}

.student-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.student-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  margin-bottom: 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.student-card:hover {
  background: #f8fafc;
}

.student-card.active {
  background: #eff6ff;
  border-color: #3b82f6;
}

.student-card.graded {
  opacity: 0.7;
}

.student-avatar {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 1rem;
  flex-shrink: 0;
  overflow: hidden;
  background: #64748b;
}

.student-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.student-info {
  flex: 1;
  min-width: 0;
}

.student-name {
  display: block;
  font-weight: 600;
  color: #0f172a;
  font-size: 0.9rem;
}

.student-id {
  display: block;
  font-size: 0.75rem;
  color: #94a3b8;
  margin-top: 2px;
}

.score-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
}

.score-badge.graded {
  background: #dcfce7;
  color: #16a34a;
}

.score-badge.pending {
  background: #fef3c7;
  color: #d97706;
}

.score-badge.unsubmitted {
  background: #f1f5f9;
  color: #94a3b8;
}

.stat-item.unsubmitted .stat-value {
  color: #64748b;
}

.content-area {
  flex: 1;
  display: flex;
  gap: 24px;
  overflow: hidden;
}

.video-panel {
  flex: 1;
  background: white;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.video-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.video-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #0f172a;
}

.video-player {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
}

.video-element {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.video-placeholder {
  text-align: center;
  color: #64748b;
}

.video-placeholder .el-icon {
  color: #475569;
  margin-bottom: 16px;
}

.video-placeholder p {
  margin-bottom: 20px;
  font-size: 1rem;
}

.video-timeline {
  padding: 16px 20px;
  background: #f8fafc;
}

.timeline-bar {
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
}

.timeline-progress {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  border-radius: 3px;
}

.timeline-info {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 0.75rem;
  color: #64748b;
}

.grade-panel {
  width: 420px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.scroll-area {
  flex: 1;
  overflow-y: auto;
}

.panel-section {
  padding: 20px;
  border-bottom: 1px solid #f1f5f9;
}

.student-profile {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-large {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 1.25rem;
  flex-shrink: 0;
  overflow: hidden;
}

.avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-info {
  flex: 1;
}

.profile-info h2 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: #0f172a;
}

.profile-info p {
  margin: 4px 0 0;
  font-size: 0.8rem;
  color: #64748b;
}

.profile-info .divider {
  margin: 0 8px;
  color: #cbd5e1;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
  color: #0f172a;
}

.section-header .el-icon {
  color: #64748b;
}

.requirement-content {
  font-size: 0.9rem;
  color: #475569;
  line-height: 1.6;
  background: #f8fafc;
  padding: 14px;
  border-radius: 10px;
}

.ai-section {
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%);
}

.ai-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
  font-size: 0.7rem;
  font-weight: 700;
  border-radius: 8px;
}

.ai-content {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.ai-score-display {
  flex-shrink: 0;
}

.score-ring {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 4px solid;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: white;
}

.score-number {
  font-size: 1.5rem;
  font-weight: 700;
}

.score-ring .score-label {
  font-size: 0.65rem;
  color: #64748b;
  text-transform: uppercase;
}

.ai-feedback {
  flex: 1;
}

.ai-feedback h4 {
  margin: 0 0 8px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #0f172a;
}

.ai-feedback p {
  margin: 0;
  font-size: 0.85rem;
  color: #475569;
  line-height: 1.6;
}

.grade-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.score-input-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.score-input-row label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #0f172a;
  width: 70px;
  flex-shrink: 0;
}

.score-slider {
  flex: 1;
}

.score-suffix {
  font-size: 0.875rem;
  color: #64748b;
  margin-left: -8px;
}

.feedback-input label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: #0f172a;
  margin-bottom: 8px;
}

.text-muted {
  color: #94a3b8;
  font-style: italic;
}

.panel-actions {
  display: flex;
  gap: 12px;
  padding: 20px;
  margin-top: auto;
  background: #f8fafc;
}

@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }
  
  .student-sidebar {
    width: 100%;
    max-height: 200px;
  }
  
  .content-area {
    flex-direction: column;
  }
  
  .video-panel {
    min-height: 300px;
  }
  
  .grade-panel {
    width: 100%;
  }
}
</style>

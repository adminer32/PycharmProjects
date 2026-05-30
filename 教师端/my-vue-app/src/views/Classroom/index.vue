<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { useClassStore } from '@/store/class'
import { storeToRefs } from 'pinia'
import { classroomApi, type ClassroomLesson, type ClassroomKnowledgePoint, type ClassroomNote, type AddKnowledgeInput, type AddNoteInput as NoteInput } from '@/api/classroomApi'
import LessonVideo from './components/LessonVideo.vue'
import SideTabs from './components/SideTabs.vue'
import KnowledgeVideo from './components/KnowledgeVideo.vue'
import AddKnowledgeDialog from './components/AddKnowledgeDialog.vue'
import type { LessonVideo as LessonVideoType, VideoAnalysis, KnowledgePointForm } from './types'

const classStore = useClassStore()
const { currentClass } = storeToRefs(classStore)
const loading = ref(false)

const lessons = ref<ClassroomLesson[]>([])

const currentLessonId = ref<string | null>(null)

const lessonVideo = ref<LessonVideoType>({
  id: '',
  title: '',
  description: '',
  videoUrl: '',
  thumbnail: '',
  duration: '',
  recordedAt: '',
  classId: 0,
  className: ''
})

const knowledgePoints = ref<ClassroomKnowledgePoint[]>([])

const notes = ref<ClassroomNote[]>([])

const analysisData = ref<VideoAnalysis>({
  videoId: '1',
  totalScore: 85,
  poseAnalysis: [
    { id: 'p1', timestamp: 45, type: 'pose', title: '站姿评估', description: '身体重心略微偏前', score: 82, suggestions: ['保持身体直立', '重心均匀分布'] },
    { id: 'p2', timestamp: 120, type: 'pose', title: '膝盖角度', description: '膝盖弯曲角度合适', score: 90 }
  ],
  actionAnalysis: [
    { id: 'a1', timestamp: 200, type: 'action', title: '抬腿动作', description: '抬腿高度适中', score: 85 },
    { id: 'a2', timestamp: 280, type: 'action', title: '触球时机', description: '触球时机把握准确', score: 88 }
  ],
  techniqueAnalysis: [
    { id: 't1', timestamp: 350, type: 'technique', title: '控球能力', description: '控球稳定性良好', score: 80, suggestions: ['加强脚腕力量训练'] }
  ],
  summary: '整体表现良好，动作规范，建议加强核心力量训练。'
})

const showAnalysis = ref(false)
const classAnalysis = ref({
  attendanceRate: 0,
  participationRate: 0,
  activeStudents: 0,
  totalStudents: 30,
  averageDuration: '0:00',
  peakTime: '00:00'
})
const showVideoModal = ref(false)
const showAddDialog = ref(false)
const currentKnowledgePoint = ref<ClassroomKnowledgePoint | null>(null)
const expandedPointId = ref<string | null>(null)

const handleExpandPoint = (pointId: string | null) => {
  expandedPointId.value = expandedPointId.value === pointId ? null : pointId
}

const handleAnalyze = () => {
}

const handleScreenshot = (dataUrl: string) => {
  ElMessage.success('截图已保存到相册')
  console.log('Screenshot saved:', dataUrl.substring(0, 50) + '...')
}

const handleSelectKnowledge = (point: ClassroomKnowledgePoint) => {
  currentKnowledgePoint.value = point
}

const handleUploadVideo = (point: ClassroomKnowledgePoint) => {
  currentKnowledgePoint.value = point
  showVideoModal.value = true
}

const handlePlayVideo = (point: ClassroomKnowledgePoint) => {
  currentKnowledgePoint.value = point
  showVideoModal.value = true
}

const handleVideoUploaded = async (url: string) => {
  if (!currentKnowledgePoint.value) return
  
  try {
    await classroomApi.updateKnowledgeVideo(currentKnowledgePoint.value.id, url)
    
    currentKnowledgePoint.value.videoUrl = url
    currentKnowledgePoint.value.hasVideo = true
    
    const kp = knowledgePoints.value.find(k => String(k.id) === String(currentKnowledgePoint.value!.id))
    if (kp) {
      kp.videoUrl = url
      kp.hasVideo = true
    }
    
    showVideoModal.value = false
    ElMessage.success('视频保存成功')
  } catch (error: any) {
    console.error('保存视频失败:', error)
    ElMessage.error(error?.message || '视频保存失败')
  }
}

const handleCloseVideoModal = () => {
  showVideoModal.value = false
}

const handleCloseAnalysis = () => {
  showAnalysis.value = false
}

const handleRestoreVideo = () => {
  if (lessonVideo.value.id) {
    const lesson = lessons.value.find(l => String(l.id) === lessonVideo.value.id)
    if (lesson && lesson.videoUrl) {
      let url = lesson.videoUrl
      if (url && !url.startsWith('http') && !url.startsWith('/')) {
        url = '/api/file/' + url
      }
      lessonVideo.value.videoUrl = url
    }
  }
}

const handleJumpTo = (time: number) => {
  ElMessage.info(`已跳转到 ${Math.floor(time / 60)}:${String(time % 60).padStart(2, '0')} 位置`)
}

const handleSelectLesson = async (lesson: ClassroomLesson) => {
  currentLessonId.value = String(lesson.id)
  
  let url = lesson.videoUrl || ''
  console.log('[Classroom] 原始 videoUrl:', url)
  
  // 智能判断 URL 前缀
  if (url && !url.startsWith('http') && !url.startsWith('/')) {
    // 如果文件名包含特定前缀或配置，使用公共文件接口
    // 否则使用需要认证的文件接口
    url = '/api/file/' + url
    console.log('[Classroom] 处理后 videoUrl:', url)
  }
  
  console.log('[Classroom] 最终 videoUrl:', url, 'lessonId:', lesson.id)
  
  lessonVideo.value = {
    id: String(lesson.id),
    title: lesson.title,
    description: `${lesson.className} - 课程回放`,
    videoUrl: url,
    thumbnail: lesson.thumbnail,
    duration: lesson.duration,
    recordedAt: lesson.recordedAt,
    classId: Number(classStore.currentClass?.id || 0),
    className: lesson.className
  }
  currentKnowledgePoint.value = null

  await Promise.all([
    loadKnowledgePoints(lesson.id),
    loadNotes(lesson.id)
  ])
}

const handleAddNote = () => {
  const newId = Date.now()
  notes.value.push({ id: newId, content: '' })
}

const handleRemoveNote = (id: number) => {
  if (notes.value.length > 1) {
    notes.value = notes.value.filter(n => n.id !== id)
  }
}

const handleUpdateNote = (id: number, text: string) => {
  const note = notes.value.find(n => n.id === id)
  if (note) {
    note.content = text
  }
}

const handleSaveNotes = async () => {
  if (!currentLessonId.value) {
    ElMessage.warning('请先选择一个课程')
    return
  }
  try {
    await classroomApi.saveNotes({
      lessonId: currentLessonId.value,
      notes: notes.value.map(n => ({ id: n.id, content: n.content }))
    })
    ElMessage.success('笔记保存成功')
    await loadNotes(currentLessonId.value)
  } catch (e) {
    console.error('保存笔记失败', e)
    ElMessage.error('保存笔记失败')
  }
}

const handleAddAiNote = (text: string) => {
  notes.value.unshift({ id: Date.now(), content: text })
}

const handleAddKnowledge = () => {
  showAddDialog.value = true
}

const handleAddKnowledgeSubmit = async (form: KnowledgePointForm & { videoFile?: File }) => {
  if (!currentLessonId.value) {
    ElMessage.warning('请先选择一个课程')
    return
  }
  try {
    const input: AddKnowledgeInput = {
      lessonId: currentLessonId.value,
      title: form.title,
      description: form.description,
      demoVideoUrl: form.videoFile ? '' : undefined
    }
    const res = await classroomApi.addKnowledge(input)
    if (res.data) {
      knowledgePoints.value.push({
        id: res.data.id,
        title: res.data.title,
        description: res.data.description,
        order: knowledgePoints.value.length + 1,
        hasVideo: !!form.videoFile,
        videoUrl: '',
        videoDuration: form.videoFile ? '0:00' : undefined,
        videoThumbnail: undefined,
        lessonId: Number(currentLessonId.value)
      })
      showAddDialog.value = false
      ElMessage.success('知识点添加成功')
    }
  } catch (e) {
    console.error('添加知识点失败', e)
    ElMessage.error('添加知识点失败')
  }
}

const loadLessons = async () => {
  if (!classStore.currentClass?.id) return
  loading.value = true
  try {
    const res = await classroomApi.getLessons(String(classStore.currentClass.id))
    if (res.data?.length > 0) {
      lessons.value = res.data
      handleSelectLesson(res.data[0])
    } else {
      lessons.value = []
      currentLessonId.value = null
      lessonVideo.value = {
        id: '',
        title: '暂无课程回放',
        description: '',
        videoUrl: '',
        thumbnail: '',
        duration: '',
        recordedAt: '',
        classId: Number(classStore.currentClass?.id || 0),
        className: classStore.currentClass?.name || ''
      }
      knowledgePoints.value = []
      notes.value = []
      currentKnowledgePoint.value = null
    }
  } catch (e) {
    console.error('加载课程列表失败', e)
    ElMessage.error('加载课程列表失败')
  } finally {
    loading.value = false
  }
}

const loadKnowledgePoints = async (lessonId: number | string) => {
  try {
    const res = await classroomApi.getKnowledgePoints(lessonId)
    if (res.data) {
      knowledgePoints.value = res.data.map((item: any) => ({
        id: String(item.id),
        title: item.title,
        description: item.description || '',
        order: item.orderNum || item.order || 0,
        hasVideo: Boolean(item.hasVideo) || (item.videoUrl ? item.videoUrl.length > 0 : false),
        videoUrl: item.videoUrl || '',
        videoDuration: item.videoDuration,
        videoThumbnail: item.videoThumbnail,
        lessonId: item.lessonId
      }))
    }
  } catch (e) {
    console.error('加载知识点失败', e)
  }
}

const loadNotes = async (lessonId: number | string) => {
  try {
    const res = await classroomApi.getNotes(lessonId)
    if (res.data) notes.value = res.data
  } catch (e) {
    console.error('加载笔记失败', e)
  }
}

onMounted(() => {
  loadLessons()
})

watch(() => classStore.currentClass?.id, () => {
  if (classStore.currentClass?.id) {
    loadLessons()
  }
})
</script>

<template>
  <div class="classroom-container">
    <div class="main-content">
      <div class="video-section">
        <LessonVideo
          :video-url="lessonVideo.videoUrl"
          :video-title="lessonVideo.title"
          :thumbnail="lessonVideo.thumbnail"
          :notes="notes.map(n => ({ id: n.id, text: n.content }))"
          :class-analysis="classAnalysis"
          @screenshot="handleScreenshot"
          @add-note="handleAddNote"
          @remove-note="handleRemoveNote"
          @update-note="handleUpdateNote"
          @save-notes="handleSaveNotes"
          @add-ai-note="handleAddAiNote"
          @restore-video="handleRestoreVideo"
        />
      </div>

      <div class="tabs-section">
        <SideTabs
          :knowledge-points="knowledgePoints"
          :lessons="lessons"
          :current-lesson-id="currentLessonId"
          :current-point-id="currentKnowledgePoint?.id || null"
          :expanded-point-id="expandedPointId"
          @select-lesson="handleSelectLesson"
          @select-point="handleSelectKnowledge"
          @expand-point="handleExpandPoint"
          @add-point="handleAddKnowledge"
          @upload-video="handleUploadVideo"
          @play-video="handlePlayVideo"
        />
      </div>
    </div>

    <KnowledgeVideo
      :visible="showVideoModal"
      :knowledge-point="currentKnowledgePoint"
      @close="handleCloseVideoModal"
      @uploaded="handleVideoUploaded"
    />

    <AddKnowledgeDialog
      :visible="showAddDialog"
      @close="showAddDialog = false"
      @submit="handleAddKnowledgeSubmit"
    />
  </div>
</template>

<style scoped>
.classroom-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 20px;
  min-height: calc(100vh - 140px);
}

.main-content {
  display: flex;
  gap: 24px;
  flex: 1;
}

.video-section {
  flex: 7;
  min-width: 0;
}

.tabs-section {
  flex: 3;
  min-width: 320px;
  max-width: 400px;
}

@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }
  .video-section,
  .tabs-section {
    flex: none;
    max-width: none;
    width: 100%;
  }
}
</style>

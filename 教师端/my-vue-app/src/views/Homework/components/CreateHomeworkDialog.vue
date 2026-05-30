<script setup lang="ts">
import { ref, watch } from 'vue'
import { Upload, VideoPlay, MagicStick } from '@element-plus/icons-vue'
import type { CreateHomeworkDTO } from '@/api/homeworkApi'
import { useClassStore } from '@/store/class'
import { storeToRefs } from 'pinia'
import { uploadFile } from '@/api/fileApi'
import { aiApi, type AiHomeworkInput, type AiHomeworkResult } from '@/api/aiApi'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', data: CreateHomeworkDTO): void
}>()

const classStore = useClassStore()
const { currentClass } = storeToRefs(classStore)

const form = ref({
  title: '',
  requirements: '',
  deadline: '',
  deadlineTime: '',
  demoVideoUrl: ''
})

const videoPreviewUrl = ref('')
const videoInputRef = ref<HTMLInputElement>()
const uploading = ref(false)

const showAiPanel = ref(false)
const aiGenerating = ref(false)
const aiForm = ref({
  subject: '',
  difficulty: 'medium' as 'easy' | 'medium' | 'hard'
})
const aiGeneratedResult = ref<AiHomeworkResult | null>(null)

watch(() => props.modelValue, (val) => {
  if (!val) {
    resetForm()
  }
})

const resetForm = () => {
  form.value = {
    title: '',
    requirements: '',
    deadline: '',
    deadlineTime: '',
    demoVideoUrl: ''
  }
  videoPreviewUrl.value = ''
}

const handleVideoSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  const validTypes = ['video/mp4', 'video/webm', 'video/ogg', 'video/quicktime']
  if (!validTypes.includes(file.type)) {
    ElMessage.error('请上传有效的视频文件 (MP4, WebM, OGG, MOV)')
    return
  }

  if (file.size > 100 * 1024 * 1024) {
    ElMessage.error('视频文件大小不能超过 100MB')
    return
  }

  videoPreviewUrl.value = URL.createObjectURL(file)

  uploading.value = true
  try {
    const url = await uploadFile(file)
    form.value.demoVideoUrl = url
    ElMessage.success('视频上传成功')
  } catch (error) {
    ElMessage.error('视频上传失败')
    videoPreviewUrl.value = ''
    form.value.demoVideoUrl = ''
  } finally {
    uploading.value = false
  }
}

const handleRemoveVideo = () => {
  form.value.demoVideoUrl = ''
  videoPreviewUrl.value = ''
  if (videoInputRef.value) {
    videoInputRef.value.value = ''
  }
}

const handleSubmit = () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入作业标题')
    return
  }

  if (!currentClass.value?.id) {
    ElMessage.warning('请先选择班级')
    return
  }

  const deadline = form.value.deadline && form.value.deadlineTime
    ? `${form.value.deadline}T${form.value.deadlineTime}:00`
    : form.value.deadline

  if (!deadline) {
    ElMessage.warning('请选择截止时间')
    return
  }

  emit('submit', {
    title: form.value.title,
    requirements: form.value.requirements || '无特殊要求',
    demoVideoUrl: form.value.demoVideoUrl || undefined,
    deadline: deadline,
    classId: currentClass.value.id
  })
  resetForm()
}

const handleClose = () => {
  emit('update:modelValue', false)
}

const handleAiGenerate = async () => {
  if (!aiForm.value.subject.trim()) {
    ElMessage.warning('请输入教学主题')
    return
  }
  if (!currentClass.value?.id) {
    ElMessage.warning('请先选择班级')
    return
  }

  aiGenerating.value = true
  aiGeneratedResult.value = null

  try {
    const input: AiHomeworkInput = {
      subject: aiForm.value.subject,
      difficulty: aiForm.value.difficulty,
      classId: currentClass.value.id
    }

    const res = await aiApi.generateHomework(input)

    if (res.data) {
      aiGeneratedResult.value = res.data
      form.value.title = res.data.title
      form.value.requirements = res.data.requirements
      ElMessage.success('AI生成作业成功！可编辑后发布')
      showAiPanel.value = false
    }
  } catch (e: {
    message?: string
  }) {
    ElMessage.error('AI生成失败：' + (e.message || '请检查网络连接'))
  } finally {
    aiGenerating.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="发布新作业"
    width="600px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form :model="form" label-position="top">
      <div class="ai-generate-header">
        <el-button
          type="primary"
          plain
          :icon="MagicStick"
          @click="showAiPanel = !showAiPanel"
        >
          {{ showAiPanel ? '收起' : '🤖 AI智能生成作业' }}
        </el-button>
      </div>

      <div v-if="showAiPanel" class="ai-generate-panel">
        <div class="ai-panel-content">
          <h4>AI 智能生成作业</h4>
          <p class="ai-hint">输入教学主题和难度，AI将自动为你生成完整的作业内容</p>

          <el-form-item label="教学主题" required>
            <el-input
              v-model="aiForm.subject"
              placeholder="如：盘踢基础、绷踢进阶、外摆组合..."
            />
          </el-form-item>

          <el-form-item label="难度等级">
            <el-radio-group v-model="aiForm.difficulty">
              <el-radio value="easy">初级</el-radio>
              <el-radio value="medium">中级</el-radio>
              <el-radio value="hard">高级</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-button
            type="primary"
            :loading="aiGenerating"
            @click="handleAiGenerate"
            style="width:100%"
          >
            {{ aiGenerating ? 'AI正在思考中...' : '✨ 开始生成作业' }}
          </el-button>

          <div v-if="aiGeneratedResult" class="ai-result-preview">
            <h5>生成结果预览</h5>
            <div class="result-title">{{ aiGeneratedResult.title }}</div>
            <div class="result-requirements" v-html="aiGeneratedResult.requirements"></div>
            <div v-if="aiGeneratedResult.tasks?.length" class="result-tasks">
              <h6>任务列表（{{ aiGeneratedResult.tasks.length }}项）</h6>
              <ol>
                <li v-for="(task, idx) in aiGeneratedResult.tasks" :key="idx">
                  <strong>{{ task.name }}</strong>: {{ task.description }}
                  <br><small>标准: {{ task.standard }}</small>
                </li>
              </ol>
            </div>
          </div>
        </div>
      </div>

      <el-form-item label="作业标题" required>
        <el-input
          v-model="form.title"
          placeholder="请输入作业标题"
        />
      </el-form-item>

      <el-form-item label="作业要求">
        <el-input
          v-model="form.requirements"
          type="textarea"
          :rows="4"
          placeholder="请输入作业要求..."
        />
      </el-form-item>

      <div class="form-row">
        <el-form-item label="截止日期" class="half">
          <el-date-picker
            v-model="form.deadline"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="截止时间" class="half">
          <el-time-picker
            v-model="form.deadlineTime"
            format="HH:mm"
            value-format="HH:mm"
            placeholder="选择时间"
            style="width: 100%"
          />
        </el-form-item>
      </div>

      <el-form-item label="示范视频">
        <div v-if="!videoPreviewUrl" class="video-upload-area" @click="videoInputRef?.click()">
          <span class="upload-icon">📹</span>
          <span>{{ uploading ? '上传中...' : '点击或拖拽上传视频' }}</span>
          <input
            ref="videoInputRef"
            type="file"
            accept="video/*"
            style="display: none"
            @change="handleVideoSelect"
            :disabled="uploading"
          />
        </div>
        <div v-else class="video-preview">
          <video :src="videoPreviewUrl" controls />
          <div v-if="uploading" class="uploading-overlay">
            <span>上传中...</span>
          </div>
          <el-button v-if="!uploading" class="remove-btn" size="small" @click="handleRemoveVideo">
            ✕ 移除
          </el-button>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit">发布作业</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.form-row {
  display: flex;
  gap: 16px;
}

.form-row .half {
  flex: 1;
}

.video-upload-area {
  border: 2px dashed #e2e8f0;
  border-radius: 12px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.video-upload-area:hover {
  border-color: #0ea5e9;
  background: #f8fafc;
}

.upload-icon {
  font-size: 2rem;
  display: block;
  margin-bottom: 8px;
}

.video-preview {
  position: relative;
}

.video-preview video {
  width: 100%;
  max-height: 200px;
  border-radius: 12px;
}

.remove-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  border: none;
}

.uploading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  color: white;
  font-size: 1.2rem;
}

.ai-generate-header {
  margin-bottom: 16px;
  text-align: right;
}

.ai-generate-panel {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid #bae6fd;
}

.ai-panel-content h4 {
  margin: 0 0 8px 0;
  color: #0369a1;
  font-size: 1rem;
}

.ai-hint {
  color: #64748b;
  font-size: 0.85rem;
  margin: 0 0 16px 0;
}

.ai-result-preview {
  margin-top: 16px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.ai-result-preview h5 {
  margin: 0 0 12px 0;
  color: #059669;
}

.result-title {
  font-weight: 600;
  font-size: 1.05rem;
  color: #1e293b;
  margin-bottom: 12px;
}

.result-requirements {
  line-height: 1.6;
  color: #334155;
  margin-bottom: 16px;
}

.result-tasks h6 {
  margin: 12px 0 8px 0;
  color: #475569;
}

.result-tasks ol {
  padding-left: 20px;
  margin: 0;
}

.result-tasks li {
  margin-bottom: 8px;
  line-height: 1.5;
}
</style>

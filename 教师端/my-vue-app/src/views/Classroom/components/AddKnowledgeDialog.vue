<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { KnowledgePointForm } from '../types'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'submit', data: KnowledgePointForm & { videoFile?: File }): void
}>()

const formData = ref<KnowledgePointForm>({
  title: '',
  description: ''
})

const videoFile = ref<File | null>(null)
const videoFileName = ref('')
const uploadProgress = ref(0)
const isUploading = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)

const formRules = {
  title: [
    { required: true, message: '请输入知识点标题', trigger: 'blur' },
    { min: 2, max: 50, message: '标题长度在 2 到 50 个字符', trigger: 'blur' }
  ]
}

const formRef = ref()

const isFormValid = computed(() => {
  return formData.value.title.trim().length >= 2
})

function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    if (!validateFile(file)) return
    videoFile.value = file
    videoFileName.value = file.name
  }
}

function validateFile(file: File): boolean {
  const validTypes = ['video/mp4', 'video/webm', 'video/quicktime', 'video/x-msvideo']
  if (!validTypes.includes(file.type)) {
    ElMessage.error('请选择有效的视频文件（MP4、WebM、MOV、AVI）')
    return false
  }
  const maxSize = 500 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('视频文件大小不能超过 500MB')
    return false
  }
  return true
}

function handleRemoveFile() {
  videoFile.value = null
  videoFileName.value = ''
  uploadProgress.value = 0
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

function simulateUpload() {
  isUploading.value = true
  uploadProgress.value = 0
  
  const interval = setInterval(() => {
    uploadProgress.value += 10
    if (uploadProgress.value >= 100) {
      clearInterval(interval)
      isUploading.value = false
    }
  }, 200)
}

async function handleSubmit() {
  if (!isFormValid.value) {
    ElMessage.warning('请填写知识点标题')
    return
  }
  
  if (videoFile.value && !isUploading.value) {
    simulateUpload()
    await new Promise(resolve => setTimeout(resolve, 2000))
  }
  
  emit('submit', {
    ...formData.value,
    videoFile: videoFile.value || undefined
  })
  
  ElMessage.success('知识点添加成功')
  handleReset()
}

function handleReset() {
  formData.value = { title: '', description: '' }
  videoFile.value = null
  videoFileName.value = ''
  uploadProgress.value = 0
  isUploading.value = false
}

function handleClose() {
  handleReset()
  emit('close')
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="添加知识点"
    width="560px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="80px"
      label-position="top"
    >
      <el-form-item label="知识点标题" prop="title">
        <el-input
          v-model="formData.title"
          placeholder="请输入知识点标题"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>
      
      <el-form-item label="知识点描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          placeholder="请输入知识点描述（可选）"
          :rows="3"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
      
      <el-form-item label="教学视频">
        <div class="upload-section">
          <input
            ref="fileInputRef"
            type="file"
            accept="video/mp4,video/webm,video/quicktime,video/x-msvideo"
            style="display: none"
            @change="handleFileSelect"
          />
          
          <div v-if="!videoFile" class="upload-area" @click="fileInputRef?.click()">
            <div class="upload-icon">
              <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12"/>
              </svg>
            </div>
            <p class="upload-text">点击或拖拽视频文件到此处</p>
            <p class="upload-hint">支持 MP4、WebM、MOV、AVI 格式，最大 500MB</p>
          </div>
          
          <div v-else class="file-preview">
            <div class="file-info">
              <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="23 7 16 12 23 17 23 7"/>
                <rect x="1" y="5" width="15" height="14" rx="2" ry="2"/>
              </svg>
              <div class="file-details">
                <span class="file-name">{{ videoFileName }}</span>
                <span class="file-size">{{ videoFile ? formatFileSize(videoFile.size) : '' }}</span>
              </div>
            </div>
            <el-button type="danger" size="small" text @click="handleRemoveFile">
              删除
            </el-button>
          </div>
          
          <div v-if="isUploading" class="upload-progress">
            <el-progress :percentage="uploadProgress" :stroke-width="8" />
            <span class="progress-text">上传中... {{ uploadProgress }}%</span>
          </div>
        </div>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <div class="submit-section">
          <span v-if="!isFormValid && !isUploading" class="form-hint">
            请输入知识点标题（至少2个字符）
          </span>
          <el-button type="primary" :disabled="!isFormValid || isUploading" @click="handleSubmit">
            {{ isUploading ? '上传中...' : '确定添加' }}
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.upload-section {
  width: 100%;
}

.upload-area {
  border: 2px dashed var(--border-color, #dcdfe6);
  border-radius: 8px;
  padding: 32px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--bg-primary, #f8fafc);
}

.upload-area:hover {
  border-color: var(--primary-blue, #0ea5e9);
  background: var(--primary-light, #e0f2fe);
}

.upload-icon {
  color: var(--text-secondary, #64748b);
  margin-bottom: 12px;
}

.upload-text {
  font-size: 1rem;
  color: var(--text-primary, #1e293b);
  margin: 0 0 8px 0;
}

.upload-hint {
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
  margin: 0;
}

.file-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--bg-primary, #f8fafc);
  border-radius: 8px;
  border: 1px solid var(--border-color, #e2e8f0);
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-info svg {
  color: var(--primary-blue, #0ea5e9);
}

.file-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.file-name {
  font-size: 0.9rem;
  color: var(--text-primary, #1e293b);
  font-weight: 500;
}

.file-size {
  font-size: 0.8rem;
  color: var(--text-secondary, #64748b);
}

.upload-progress {
  margin-top: 12px;
}

.progress-text {
  display: block;
  text-align: center;
  margin-top: 8px;
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
}

.submit-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.form-hint {
  font-size: 0.8rem;
  color: var(--text-tertiary, #94a3b8);
}
</style>

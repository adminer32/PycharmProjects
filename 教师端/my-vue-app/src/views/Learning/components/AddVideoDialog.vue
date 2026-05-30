<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { VideoFormData } from '../types'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'add', video: VideoFormData): void
}>()

const formData = ref<VideoFormData>({
  title: '',
  duration: '',
  url: '',
  category: 'panti'
})

const categories = [
  { key: 'panti', label: '盘踢' },
  { key: 'keti', label: '磕踢' },
  { key: 'waibai', label: '外摆' },
  { key: 'lihe', label: '里合' }
]

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const isValid = computed(() => {
  return formData.value.title.trim() && 
         formData.value.duration.trim() && 
         formData.value.url.trim()
})

const handleConfirm = () => {
  if (!isValid.value) {
    ElMessage.warning('请填写完整的视频信息')
    return
  }
  
  emit('add', { ...formData.value })
  ElMessage.success('视频添加成功')
  handleClose()
}

const handleClose = () => {
  formData.value = {
    title: '',
    duration: '',
    url: '',
    category: 'panti'
  }
  visible.value = false
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="添加新视频"
    width="500px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="add-video-content">
      <el-form label-position="top" class="video-form">
        <el-form-item label="视频标题">
          <el-input
            v-model="formData.title"
            placeholder="请输入视频标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="视频时长">
          <el-input
            v-model="formData.duration"
            placeholder="例如：12:30"
          />
        </el-form-item>

        <el-form-item label="视频链接">
          <el-input
            v-model="formData.url"
            placeholder="请输入视频链接"
          />
        </el-form-item>

        <el-form-item label="动作分类">
          <el-select v-model="formData.category" style="width: 100%">
            <el-option
              v-for="cat in categories"
              :key="cat.key"
              :label="cat.label"
              :value="cat.key"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button 
          type="primary" 
          :disabled="!isValid"
          @click="handleConfirm"
        >
          确认添加
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.add-video-content {
  padding: 0 10px;
}

.video-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--text-primary, #1e293b);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Notice } from '../types'

const props = defineProps<{
  notices: Notice[]
}>()

const emit = defineEmits<{
  (e: 'publish', notice: Notice): void
  (e: 'edit', notice: Notice): void
  (e: 'delete', id: string): void
}>()

const showPublishDialog = ref(false)
const editingNotice = ref<Notice | null>(null)
const formData = ref({
  title: '',
  content: '',
  isImportant: false
})

const handlePublishClick = () => {
  editingNotice.value = null
  formData.value = {
    title: '',
    content: '',
    isImportant: false
  }
  showPublishDialog.value = true
}

const handleEditClick = (notice: Notice) => {
  editingNotice.value = notice
  formData.value = {
    title: notice.title,
    content: notice.content,
    isImportant: notice.isImportant
  }
  showPublishDialog.value = true
}

const handlePublish = () => {
  if (!formData.value.title.trim() || !formData.value.content.trim()) {
    ElMessage.warning('请填写完整通知内容')
    return
  }
  
  const now = new Date()
  const timeStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  
  const notice: Notice = {
    id: editingNotice.value?.id || Date.now().toString(),
    title: formData.value.title,
    content: formData.value.content,
    time: timeStr,
    isImportant: formData.value.isImportant,
    readCount: editingNotice.value?.readCount || 0,
    totalStudents: 42
  }
  
  if (editingNotice.value) {
    emit('edit', notice)
    ElMessage.success('通知已更新')
  } else {
    emit('publish', notice)
    ElMessage.success('通知已发布')
  }
  
  showPublishDialog.value = false
}

const handleDelete = (notice: Notice) => {
  ElMessageBox.confirm('确定要删除这条通知吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    emit('delete', notice.id)
    ElMessage.info('通知已删除')
  }).catch(() => {})
}
</script>

<template>
  <el-card class="notice-card" shadow="never">
    <template #header>
      <div class="card-header">
        <span class="card-title">
          <span class="title-icon">🔔</span>
          <span>班级通知</span>
        </span>
        <el-button type="primary" size="small" @click="handlePublishClick">
          + 发布通知
        </el-button>
      </div>
    </template>
    
    <div class="notice-list">
      <div
        v-for="notice in notices"
        :key="notice.id"
        :class="['notice-item', { important: notice.isImportant }]"
      >
        <div class="notice-header">
          <span class="notice-title">
            {{ notice.isImportant ? '📢 ' : '📄 ' }}{{ notice.title }}
          </span>
          <span class="notice-time">{{ notice.time }}</span>
        </div>
        <p class="notice-content">{{ notice.content }}</p>
        <div class="notice-footer">
          <span class="notice-status">已发送 · {{ notice.readCount }}人已读</span>
          <div class="notice-actions">
            <el-button text size="small" @click="handleEditClick(notice)">编辑</el-button>
            <el-button text size="small" type="danger" @click="handleDelete(notice)">删除</el-button>
          </div>
        </div>
      </div>
    </div>
    
    <el-dialog
      v-model="showPublishDialog"
      :title="editingNotice ? '编辑通知' : '发布班级通知'"
      width="500px"
    >
      <el-form :model="formData" label-position="top">
        <el-form-item label="通知标题">
          <el-input v-model="formData.title" placeholder="请输入通知标题" />
        </el-form-item>
        <el-form-item label="通知内容">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="5"
            placeholder="请输入通知内容"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="formData.isImportant">标记为重要通知</el-checkbox>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="primary" @click="handlePublish">
          {{ editingNotice ? '更新' : '发布' }}
        </el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.notice-card {
  border-radius: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 1rem;
}

.title-icon {
  font-size: 1.2rem;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.notice-item {
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  border-left: 4px solid #e2e8f0;
}

.notice-item.important {
  background: #fefce8;
  border-left-color: #eab308;
}

.notice-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.notice-title {
  font-weight: 500;
  color: #1e293b;
}

.notice-time {
  font-size: 0.75rem;
  color: #94a3b8;
  white-space: nowrap;
}

.notice-content {
  font-size: 0.9rem;
  color: #64748b;
  margin: 0 0 12px 0;
  line-height: 1.5;
}

.notice-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notice-status {
  font-size: 0.8rem;
  color: #94a3b8;
}

.notice-actions {
  display: flex;
  gap: 4px;
}
</style>

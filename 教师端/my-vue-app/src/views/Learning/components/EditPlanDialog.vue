<script setup lang="ts">
import { ref, watch } from 'vue'
import type { TrainingPlan, TrainingTask, VideoItem } from '../types'

const props = defineProps<{
  modelValue: boolean
  studentName: string
  plan: TrainingPlan | null
  videoList: VideoItem[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', plan: TrainingPlan): void
}>()

const weeklyGoal = ref('')
const tasks = ref<TrainingTask[]>([])
const selectedVideos = ref<string[]>([])

watch(() => props.modelValue, (val) => {
  if (val && props.plan) {
    weeklyGoal.value = props.plan.goal
    tasks.value = [...props.plan.tasks]
    selectedVideos.value = props.plan.tasks
      .filter(t => t.video)
      .map(t => t.video as string)
  }
})

const handleClose = () => {
  emit('update:modelValue', false)
}

const addTask = () => {
  tasks.value.push({ name: '', spec: '' })
}

const removeTask = (index: number) => {
  tasks.value.splice(index, 1)
}

const toggleVideo = (videoId: string) => {
  const index = selectedVideos.value.indexOf(videoId)
  if (index > -1) {
    selectedVideos.value.splice(index, 1)
  } else {
    selectedVideos.value.push(videoId)
  }
}

const handleSave = () => {
  const validTasks = tasks.value.filter(t => t.name.trim())
  emit('save', {
    goal: weeklyGoal.value,
    tasks: validTasks,
    progress: props.plan?.progress || 0
  })
  handleClose()
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    title=""
    width="900px"
    :close-on-click-modal="false"
  >
    <template #header>
      <div class="modal-header">
        <h3>编辑训练计划 - {{ studentName }}</h3>
      </div>
    </template>
    
    <div class="edit-plan-form">
      <div class="form-group">
        <label>本周训练目标</label>
        <el-input
          v-model="weeklyGoal"
          placeholder="例如：提升外摆踢稳定性"
        />
      </div>
      
      <div class="form-group">
        <label>每日训练内容</label>
        <div class="task-edit-list">
          <div
            v-for="(task, index) in tasks"
            :key="index"
            class="task-edit-item"
          >
            <div class="task-edit-header">
              <el-input
                v-model="task.name"
                placeholder="训练名称"
                style="flex: 1"
              />
              <el-button
                type="danger"
                size="small"
                @click="removeTask(index)"
              >
                
              </el-button>
            </div>
            <div class="task-edit-spec">
              <el-input
                v-model="task.spec"
                placeholder="训练规格 (如: 3组×20次)"
              />
            </div>
          </div>
        </div>
        <el-button size="small" @click="addTask">+ 添加训练项</el-button>
      </div>
      
      <div class="form-group">
        <label>关联教学视频</label>
        <div class="video-selector">
          <div
            v-for="video in videoList"
            :key="video.id"
            class="video-option"
            :class="{ selected: selectedVideos.includes(video.id) }"
            @click="toggleVideo(video.id)"
          >
            <span></span>
            <span>{{ video.title }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <template #footer>
      <div class="modal-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSave">保存计划</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.modal-header h3 {
  margin: 0;
  font-size: 1.25rem;
  color: var(--text-primary, #1e293b);
}

.edit-plan-form {
  max-height: 400px;
  overflow-y: auto;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: var(--text-primary, #1e293b);
}

.task-edit-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
}

.task-edit-item {
  background: var(--bg-primary, #f8fafc);
  border-radius: 12px;
  padding: 12px;
}

.task-edit-header {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.task-edit-spec {
  margin-top: 8px;
}

.video-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 150px;
  overflow-y: auto;
  padding: 8px;
  border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 12px;
}

.video-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: var(--bg-primary, #f8fafc);
  border-radius: 30px;
  cursor: pointer;
  font-size: 0.85rem;
  transition: all 0.2s;
}

.video-option:hover {
  background: var(--hover-bg, #f1f5f9);
}

.video-option.selected {
  background: var(--primary-blue, #0ea5e9);
  color: white;
}

.modal-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
}
</style>

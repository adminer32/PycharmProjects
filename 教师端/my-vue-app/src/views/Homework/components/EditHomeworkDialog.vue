<script setup lang="ts">
import { ref, watch } from 'vue'
import type { HomeworkListVO, CreateHomeworkDTO } from '@/api/homeworkApi'
import { updateHomework } from '@/api/homeworkApi'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  modelValue: boolean
  homework: HomeworkListVO | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'updated'): void
}>()

const form = ref({
  title: '',
  requirements: '',
  deadline: '',
  deadlineTime: '',
  demoVideoUrl: ''
})

const loading = ref(false)

watch(() => props.modelValue, (val) => {
  if (val && props.homework) {
    form.value = {
      title: props.homework.title || '',
      requirements: props.homework.requirements || '',
      deadline: props.homework.deadline ? props.homework.deadline.split('T')[0] : '',
      deadlineTime: props.homework.deadline ? props.homework.deadline.split('T')[1]?.substring(0, 5) : '',
      demoVideoUrl: ''
    }
  }
})

const handleSubmit = async () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入作业标题')
    return
  }
  
  if (!props.homework?.id) return
  
  const deadline = form.value.deadline && form.value.deadlineTime 
    ? `${form.value.deadline}T${form.value.deadlineTime}:00`
    : form.value.deadline
  
  if (!deadline) {
    ElMessage.warning('请选择截止时间')
    return
  }
  
  loading.value = true
  try {
    const dto: Partial<CreateHomeworkDTO> = {
      title: form.value.title,
      requirements: form.value.requirements || '无特殊要求',
      demoVideoUrl: form.value.demoVideoUrl || undefined,
      deadline: deadline
    }
    await updateHomework(props.homework.id, dto)
    ElMessage.success('作业更新成功')
    emit('update:modelValue', false)
    emit('updated')
  } catch (error) {
    ElMessage.error('更新作业失败')
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="编辑作业"
    width="600px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form :model="form" label-position="top">
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
    </el-form>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">保存修改</el-button>
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
</style>

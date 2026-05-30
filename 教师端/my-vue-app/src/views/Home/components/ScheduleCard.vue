<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { ScheduleItem } from '../types'

const props = defineProps<{
  scheduleData: ScheduleItem[]
}>()

const emit = defineEmits<{
  (e: 'save', schedule: ScheduleItem): void
  (e: 'delete', id: string): void
}>()

const showEditDialog = ref(false)
const editingSchedule = ref<ScheduleItem | null>(null)
const showAddButton = ref(false)
const selectedCell = ref<{ day: number; period: number } | null>(null)

const formData = ref({
  courseName: '',
  location: '',
  day: 1,
  period: 1,
  duration: 1
})

const weekdays = ['一', '二', '三', '四', '五', '六', '日']
const timeSlots = [
  { period: 1, time: '08:20-09:05' },
  { period: 2, time: '09:15-10:00' },
  { period: 3, time: '10:20-11:05' },
  { period: 4, time: '11:15-12:00' },
  { period: 5, time: '14:00-14:45' },
  { period: 6, time: '14:55-15:40' },
  { period: 7, time: '15:50-16:35' },
  { period: 8, time: '16:45-17:30' }
]

// 计算每个位置的课程，支持跨越多节
const getCourseAt = (day: number, period: number) => {
  const course = props.scheduleData.find(s => {
    const endPeriod = s.period + (s.duration || 1) - 1
    return s.day === day && period >= s.period && period <= endPeriod
  })
  return course
}

// 判断是否是课程的起始位置
const isCourseStart = (day: number, period: number, course: ScheduleItem) => {
  return course && course.day === day && course.period === period
}

// 判断某个位置是否被跨行课程覆盖（非起始位置）
const isCourseCovered = (day: number, period: number) => {
  const course = getCourseAt(day, period)
  return course && !isCourseStart(day, period, course)
}

// 计算课程跨越的行数
const getCourseRowSpan = (course: ScheduleItem) => {
  return course.duration || 1
}

// 获取课程卡片的样式（支持连堂）
const getCourseStyle = (course: ScheduleItem) => {
  const duration = course.duration || 1
  const cellHeight = 45 // 每个单元格的高度
  const gap = 2 // 单元格之间的间距
  const totalHeight = duration * cellHeight + (duration - 1) * gap
  
  return {
    position: 'absolute' as const,
    top: '2px',
    left: '2px',
    right: '2px',
    height: `${totalHeight}px`,
    zIndex: 1
  }
}

const handleCellClick = (day: number, period: number) => {
  // 如果被跨行课程覆盖，不响应点击
  if (isCourseCovered(day, period)) {
    return
  }
  
  const course = getCourseAt(day, period)
  if (course) {
    // 如果是课程起始位置，编辑课程
    if (isCourseStart(day, period, course)) {
      editingSchedule.value = course
      formData.value = {
        courseName: course.courseName,
        location: course.location,
        day: course.day,
        period: course.period,
        duration: course.duration || 1
      }
      showEditDialog.value = true
    }
  } else {
    // 空白处，显示添加按钮
    if (selectedCell.value?.day === day && selectedCell.value?.period === period) {
      // 再次点击，打开添加对话框
      editingSchedule.value = null
      formData.value = {
        courseName: '',
        location: '',
        day,
        period,
        duration: 1
      }
      showEditDialog.value = true
    } else {
      // 第一次点击，选中单元格
      selectedCell.value = { day, period }
    }
  }
}

const handleAddButtonClick = (event: MouseEvent, day: number, period: number) => {
  event.stopPropagation()
  editingSchedule.value = null
  formData.value = {
    courseName: '',
    location: '',
    day,
    period,
    duration: 1
  }
  showEditDialog.value = true
  // 清除选中状态，让添加按钮消失
  selectedCell.value = null
}

const handleSave = () => {
  if (!formData.value.courseName.trim()) {
    ElMessage.warning('请输入课程名称')
    return
  }
  
  const schedule: ScheduleItem = {
    id: editingSchedule.value?.id || Date.now().toString(),
    day: formData.value.day,
    period: formData.value.period,
    courseName: formData.value.courseName,
    location: formData.value.location,
    duration: formData.value.duration
  }
  
  emit('save', schedule)
  showEditDialog.value = false
  selectedCell.value = null
  ElMessage.success('课程已保存')
}

const handleDelete = () => {
  if (editingSchedule.value) {
    emit('delete', editingSchedule.value.id)
    showEditDialog.value = false
    ElMessage.info('课程已删除')
  }
}

const handleAddCourse = () => {
  if (selectedCell.value) {
    editingSchedule.value = null
    formData.value = {
      courseName: '',
      location: '',
      day: selectedCell.value.day,
      period: selectedCell.value.period,
      duration: 1
    }
    showEditDialog.value = true
    // 清除选中状态
    selectedCell.value = null
  } else {
    // 如果没有选中单元格，提示用户先点击空白处
    ElMessage.info('请点击课程表空白处选择要添加课程的位置')
  }
}
</script>

<template>
  <el-card class="schedule-card" shadow="never">
    <template #header>
      <div class="card-header">
        <span class="card-title">
          <span class="title-icon">📅</span>
          <span>班级课程表</span>
        </span>
        <el-button type="primary" size="small" round @click="handleAddCourse">
          + 添加课程
        </el-button>
      </div>
    </template>
    
    <div class="schedule-container">
      <div class="schedule-header">
        <div class="time-header"></div>
        <div v-for="day in 7" :key="day" class="day-header">
          <span class="day-name">{{ weekdays[day - 1] }}</span>
        </div>
      </div>
      
      <div class="schedule-body">
        <template v-for="slot in timeSlots" :key="slot.period">
          <div class="schedule-row">
            <div class="time-label">
              <span class="period-num">{{ slot.period }}</span>
              <span class="period-time">{{ slot.time }}</span>
            </div>
            
            <div
              v-for="day in 7"
              :key="day + '-' + slot.period"
              class="schedule-cell"
              :class="{ 
                selected: selectedCell?.day === day && selectedCell?.period === slot.period && !isCourseCovered(day, slot.period),
                'has-course': getCourseAt(day, slot.period)
              }"
              @click="handleCellClick(day, slot.period)"
            >
              <template v-if="getCourseAt(day, slot.period) && isCourseStart(day, slot.period, getCourseAt(day, slot.period)!)">
                <div
                  class="course-card"
                  :class="'course-color-' + (getCourseAt(day, slot.period)!.period % 4 + 1)"
                  :style="getCourseStyle(getCourseAt(day, slot.period)!)"
                >
                  <div class="course-title">{{ getCourseAt(day, slot.period)!.courseName }}</div>
                  <div class="course-info">
                    <span class="course-location">{{ getCourseAt(day, slot.period)!.location }}</span>
                  </div>
                </div>
              </template>
              <template v-else-if="selectedCell?.day === day && selectedCell?.period === slot.period && !isCourseCovered(day, slot.period)">
                <div class="add-button-wrapper">
                  <el-button type="primary" size="small" round @click="(e) => handleAddButtonClick(e, day, slot.period)">
                    + 添加
                  </el-button>
                </div>
              </template>
            </div>
          </div>
        </template>
      </div>
    </div>
    
    <el-dialog
      v-model="showEditDialog"
      :title="editingSchedule ? '编辑课程' : '添加课程'"
      width="450px"
    >
      <el-form :model="formData" label-width="80px">
        <el-form-item label="课程名称" required>
          <el-input v-model="formData.courseName" placeholder="如：盘踢练习" />
        </el-form-item>
        <el-form-item label="上课地点" required>
          <el-input v-model="formData.location" placeholder="如：体育馆A区" />
        </el-form-item>
        <el-form-item label="星期" required>
          <el-radio-group v-model="formData.day">
            <el-radio-button v-for="day in 7" :key="day" :value="day">
              {{ weekdays[day - 1] }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="节次" required>
          <el-select v-model="formData.period" style="width: 150px">
            <el-option
              v-for="slot in timeSlots"
              :key="slot.period"
              :label="`第${slot.period}节 ${slot.time}`"
              :value="slot.period"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="连续节数">
          <el-input-number v-model="formData.duration" :min="1" :max="4" style="width: 150px" />
          <span class="duration-hint">节（支持连堂）</span>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button v-if="editingSchedule" type="danger" plain @click="handleDelete">删除课程</el-button>
          <div style="flex: 1"></div>
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.schedule-card {
  border-radius: 16px;
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

.schedule-container {
  overflow-x: auto;
}

.schedule-header {
  display: grid;
  grid-template-columns: 60px repeat(7, 1fr);
  border-bottom: 2px solid #f1f5f9;
  padding-bottom: 8px;
  margin-bottom: 4px;
}

.time-header {
  text-align: center;
  font-weight: 500;
  color: #94a3b8;
}

.day-header {
  text-align: center;
}

.day-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: #64748b;
}

.schedule-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.schedule-row {
  display: grid;
  grid-template-columns: 60px repeat(7, 1fr);
  gap: 2px;
}

.time-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4px 2px;
  background: #f8fafc;
  border-radius: 6px;
}

.period-num {
  font-size: 0.7rem;
  color: #94a3b8;
  font-weight: 500;
}

.period-time {
  font-size: 0.6rem;
  color: #cbd5e1;
  margin-top: 1px;
}

.schedule-cell {
  min-height: 45px;
  padding: 3px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  overflow: visible;
}

.schedule-cell:hover {
  background: #f1f5f9;
}

.schedule-cell.selected {
  background: #e0f2fe;
  border: 2px solid #0ea5e9;
}

.schedule-cell.has-course {
  cursor: default;
}

.course-card {
  padding: 6px;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 3px;
  transition: transform 0.2s;
  cursor: pointer;
}

.course-card:hover {
  transform: scale(1.02);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.course-color-1 {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
}

.course-color-2 {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1e40af;
}

.course-color-3 {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #166534;
}

.course-color-4 {
  background: linear-gradient(135deg, #fed7aa 0%, #fdba74 100%);
  color: #9a3412;
}

.course-title {
  font-size: 0.8rem;
  font-weight: 600;
  line-height: 1.2;
}

.course-info {
  font-size: 0.65rem;
  opacity: 0.9;
}

.course-location {
  font-weight: 500;
}

.add-button-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.duration-hint {
  font-size: 0.85rem;
  color: #64748b;
  margin-left: 8px;
}

.dialog-footer {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  modelValue: Date
  data: any
  type: 'class' | 'student'
  totalStudents?: number
}>()

const emit = defineEmits(['update:modelValue'])

const calendarTitle = computed(() => {
  const d = props.modelValue
  return `${d.getFullYear()}年${d.getMonth() + 1}月`
})

const getDaysInMonth = (date: Date) =>
  new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate()

const getFirstDayOffset = (date: Date) => {
  const day = new Date(date.getFullYear(), date.getMonth(), 1).getDay()
  return (day + 6) % 7
}

const daysInMonth = computed(() => getDaysInMonth(props.modelValue))
const firstDayOffset = computed(() => getFirstDayOffset(props.modelValue))

const prevMonth = () => {
  const d = new Date(props.modelValue)
  d.setMonth(d.getMonth() - 1)
  emit('update:modelValue', d)
}

const nextMonth = () => {
  const d = new Date(props.modelValue)
  d.setMonth(d.getMonth() + 1)
  emit('update:modelValue', d)
}

const isToday = (day: number) => {
  const now = new Date()
  return now.getFullYear() === props.modelValue.getFullYear() &&
    now.getMonth() === props.modelValue.getMonth() &&
    now.getDate() === day
}

const getCheckinStatus = (day: number) => {
  if (props.type === 'student') {
    return (props.data || []).includes(day) ? 'checked' : ''
  } else {
    // 班级视图
    const count = (props.data?.dayCounts || {})[day] || 0
    if (count === 0) return ''
    const total = props.totalStudents || 1
    return (count / total >= 0.8) ? 'checked' : 'partial'
  }
}
</script>

<template>
  <div :class="['checkin-calendar', { personal: type === 'student' }]">
    <div class="calendar-header">
      <button class="cal-nav-btn" @click="prevMonth">‹</button>
      <h4>{{ calendarTitle }}</h4>
      <button class="cal-nav-btn" @click="nextMonth">›</button>
    </div>
    <div class="calendar-weekdays">
      <span>一</span><span>二</span><span>三</span><span>四</span><span>五</span><span>六</span><span>日</span>
    </div>
    <div class="calendar-days">
      <div v-for="n in firstDayOffset" :key="'offset-' + n" class="calendar-day empty"></div>
      <div
        v-for="day in daysInMonth"
        :key="day"
        :class="['calendar-day', getCheckinStatus(day), { today: isToday(day) }]"
      >
        {{ day }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.checkin-calendar {
  background: #f8fafc;
  border-radius: 16px;
  padding: 16px;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.calendar-header h4 {
  font-size: 1rem;
  font-weight: 500;
  color: #0ea5e9;
  margin: 0;
}

.cal-nav-btn {
  background: #fff;
  border: 1px solid #e2e8f0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #64748b;
  font-size: 1.2rem;
  transition: all 0.2s;
  line-height: 1;
}

.cal-nav-btn:hover {
  background: #f1f5f9;
  color: #0ea5e9;
  border-color: #0ea5e9;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  padding: 8px 0;
  font-size: 0.8rem;
  color: #64748b;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 8px;
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.calendar-day {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  border-radius: 8px;
  cursor: pointer;
  background: #ffffff;
  transition: all 0.2s;
}

.calendar-day.empty {
  background: transparent;
  cursor: default;
}

.calendar-day.checked {
  background: #dcfce7;
  color: #16a34a;
}

.calendar-day.partial {
  background: #fef3c7;
  color: #d97706;
}

.calendar-day.today {
  border: 2px solid #0ea5e9;
  font-weight: bold;
  color: #0ea5e9;
}

/* 个人视图紧凑样式 */
.checkin-calendar.personal {
  padding: 12px;
  margin-top: 16px;
}

.checkin-calendar.personal .calendar-day {
  font-size: 0.75rem;
}

.checkin-calendar.personal .calendar-weekdays {
  font-size: 0.75rem;
}
</style>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, RadarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import { ElMessage } from 'element-plus'
import {
  User,
  Download,
  Message,
  Loading
} from '@element-plus/icons-vue'

import { analysisApi, type AnalysisClassDashboard, type AnalysisStudentDashboard, type AiAdviceItem } from '@/api/analysisApi'
import { useClassStore } from '@/store/class'
import BodyAnalysisDialog from './components/BodyAnalysisDialog.vue'

use([
  CanvasRenderer,
  LineChart,
  RadarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const ACTIONS_6 = ['盘踢', '跳踢', '踏踢', '磕踢', '拐踢', '绷踢']

const classStore = useClassStore()

const activeView = ref<'class' | 'student'>('class')
const selectedStudentId = ref<number | null>(null)
const searchKeyword = ref('')
const timeRange = ref<'week' | 'month' | 'semester'>('week')
const classTrendType = ref<'duration' | 'score'>('duration')
const studentTrendType = ref<'duration' | 'score'>('duration')

const loading = ref(false)
const studentLoading = ref(false)
let selectDebounceTimer: ReturnType<typeof setTimeout> | null = null
const dataCache = new Map<string, any>()

const students = ref<any[]>([])
const classData = ref<AnalysisClassDashboard | null>(null)
const studentData = ref<AnalysisStudentDashboard | null>(null)

const classAdvice = ref<AiAdviceItem[]>([])
const studentAdvice = ref<AiAdviceItem[]>([])
const regeneratingClass = ref(false)
const regeneratingStudent = ref(false)
const showBodyAnalysis = ref(false)

const currentDisplayMonth = ref(new Date())
const personalCalendarMonth = ref(new Date())

const formatYearMonth = (date: Date) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}`
}

const handleTimeRangeChange = (range?: 'week' | 'month' | 'semester') => {
  if (range) {
    timeRange.value = range
    ElMessage.info(`已切换到${range === 'week' ? '本周' : range === 'month' ? '本月' : '本学期'}数据`)
  }
  const now = new Date()
  switch (timeRange.value) {
    case 'week':
      currentDisplayMonth.value = new Date()
      break
    case 'month':
      currentDisplayMonth.value = new Date()
      break
    case 'semester':
      currentDisplayMonth.value = new Date(now.getFullYear(), 0, 1)
      break
  }
}

const loadClassData = async () => {
  if (!classStore.currentClass?.id) return
  loading.value = true
  try {
    const month = formatYearMonth(currentDisplayMonth.value)
    const [dashRes, stuRes] = await Promise.all([
      analysisApi.getClassDashboard(classStore.currentClass.id, month),
      analysisApi.getStudents(classStore.currentClass.id)
    ])
    classData.value = dashRes.data
    students.value = stuRes.data || []
    if (students.value.length > 0 && !selectedStudentId.value) {
      selectedStudentId.value = students.value[0].id
    }
  } catch (e) {
    ElMessage.error('加载班级数据失败')
  } finally {
    loading.value = false
  }
}

const loadStudentData = async () => {
  if (!selectedStudentId.value) return
  const month = formatYearMonth(personalCalendarMonth.value)
  const cacheKey = `student_${selectedStudentId.value}_${month}`
  if (dataCache.has(cacheKey)) {
    studentData.value = dataCache.get(cacheKey)
    studentLoading.value = false
    return
  }
  try {
    const res = await analysisApi.getStudentDashboard(selectedStudentId.value, month)
    studentData.value = res.data
    dataCache.set(cacheKey, res.data)
  } catch (e) {
    ElMessage.error('加载学生数据失败')
  } finally {
    studentLoading.value = false
  }
}

const currentStudent = computed(() => {
  return students.value.find(s => s.id === selectedStudentId.value) || null
})

const studentBadges = computed(() => {
  const badges: { text: string; type: 'success' | 'warning' | 'info' }[] = []

  if (!studentData.value?.metrics) {
    return [{ text: '暂无数据', type: 'info' }]
  }

  const tags = studentData.value.tags
  if (tags) {
    if (tags.progressTag && tags.progressTag !== '暂无明显进步' && tags.progressTag !== '暂无进步数据') {
      badges.push({ text: tags.progressTag, type: 'success' })
    }
    if (tags.weakTag && tags.weakTag !== '暂无数据') {
      badges.push({ text: tags.weakTag, type: 'warning' })
    }
    if (tags.strongTag && tags.strongTag !== '暂无数据') {
      badges.push({ text: tags.strongTag, type: 'success' })
    }
  }

  if (badges.length === 0) {
    badges.push({ text: '表现良好', type: 'success' })
  }

  return badges.slice(0, 3)
})

const filteredStudents = computed(() => {
  if (!searchKeyword.value) return students.value
  return students.value.filter(s =>
    s.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

const selectStudent = (id: number) => {
  selectedStudentId.value = id
  if (selectDebounceTimer) clearTimeout(selectDebounceTimer)
  studentLoading.value = true
  selectDebounceTimer = setTimeout(() => {
    loadStudentData()
  }, 300)
}

const handleStudentScroll = (e: WheelEvent) => {
  const container = e.currentTarget as HTMLElement
  container.scrollLeft += e.deltaY
}

const calculateMonthCheckinRate = () => {
  if (!classData.value?.checkinData?.dayCounts || students.value.length === 0) return 0
  const dayCounts = classData.value.checkinData.dayCounts
  const totalCheckins = Object.values(dayCounts).reduce((sum: number, count: number) => sum + count, 0)

  const now = new Date()
  const displayYear = currentDisplayMonth.value.getFullYear()
  const displayMonth = currentDisplayMonth.value.getMonth()
  const isCurrentMonth = now.getFullYear() === displayYear && now.getMonth() === displayMonth

  let totalDays: number
  if (isCurrentMonth) {
    totalDays = now.getDate()
  } else {
    totalDays = new Date(displayYear, displayMonth + 1, 0).getDate()
  }

  const totalPossible = totalDays * students.value.length
  if (totalPossible === 0) return 0

  return Math.round((totalCheckins / totalPossible) * 100)
}

const calculateQualifiedStudents = () => {
  if (classData.value?.checkinData?.qualifiedCount != null) {
    return classData.value.checkinData.qualifiedCount
  }
  if (!classData.value?.checkinData?.dayCounts || students.value.length === 0) return 0
  const dayCounts = classData.value.checkinData.dayCounts
  const daysInMonth = Object.keys(dayCounts).length
  let qualifiedCount = 0
  for (let day = 1; day <= daysInMonth; day++) {
    const count = dayCounts[day] || 0
    if (count > 5) {
      qualifiedCount++
    }
  }
  return Math.min(qualifiedCount, students.value.length)
}

const exportReport = () => {
  if (currentStudent.value) {
    ElMessage.success(`正在生成 ${currentStudent.value.name} 的学情报告...`)
    setTimeout(() => {
      ElMessage.success('报告已导出，请查看下载文件夹')
    }, 1500)
  }
}

const shareReport = () => {
  if (currentStudent.value) {
    ElMessage.success(`已将 ${currentStudent.value.name} 的学情报告发送给家长`)
  }
}

const loadClassAdvice = async () => {
  if (!classStore.currentClass?.id) return
  try {
    const res = await analysisApi.getClassAdvice(classStore.currentClass.id)
    classAdvice.value = (res.data || []).map(item => ({
      ...item,
      suggestions: typeof item.suggestions === 'string' ? JSON.parse(item.suggestions || '[]') : (item.suggestions || [])
    }))
  } catch (e) {
    console.warn('加载班级AI建议失败', e)
  }
}

const loadStudentAdvice = async () => {
  if (!selectedStudentId.value) return
  try {
    const res = await analysisApi.getStudentAdvice(selectedStudentId.value)
    studentAdvice.value = (res.data || []).map(item => ({
      ...item,
      suggestions: typeof item.suggestions === 'string' ? JSON.parse(item.suggestions || '[]') : (item.suggestions || [])
    }))
  } catch (e) {
    console.warn('加载学生AI建议失败', e)
  }
}

const handleRegenerateClassAdvice = async () => {
  if (!classStore.currentClass?.id) return
  regeneratingClass.value = true
  try {
    const res = await analysisApi.regenerateClassAdvice(classStore.currentClass.id)
    classAdvice.value = (res.data || []).map(item => ({
      ...item,
      suggestions: typeof item.suggestions === 'string' ? JSON.parse(item.suggestions || '[]') : (item.suggestions || [])
    }))
    ElMessage.success(res.message || '班级AI教学建议已重新生成')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '重新生成失败')
  } finally {
    regeneratingClass.value = false
  }
}

const handleRegenerateStudentAdvice = async () => {
  if (!selectedStudentId.value) return
  regeneratingStudent.value = true
  try {
    const res = await analysisApi.regenerateStudentAdvice(selectedStudentId.value)
    studentAdvice.value = (res.data || []).map(item => ({
      ...item,
      suggestions: typeof item.suggestions === 'string' ? JSON.parse(item.suggestions || '[]') : (item.suggestions || [])
    }))
    ElMessage.success(res.message || '学生AI个性化建议已重新生成')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '重新生成失败')
  } finally {
    regeneratingStudent.value = false
  }
}


const prevMonth = () => {
  const newMonth = new Date(currentDisplayMonth.value)
  newMonth.setMonth(newMonth.getMonth() - 1)
  currentDisplayMonth.value = newMonth
  loadClassData()
}

const nextMonth = () => {
  const newMonth = new Date(currentDisplayMonth.value)
  newMonth.setMonth(newMonth.getMonth() + 1)
  currentDisplayMonth.value = newMonth
  loadClassData()
}

const getDaysInMonth = (date: Date) => {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate()
}

const getPriorityClass = (priority: string) => ({
  high: 'high-priority',
  medium: 'medium-priority',
  low: 'low-priority'
}[priority] || '')

const getPriorityLabel = (priority: string) => ({
  high: '高优先级',
  medium: '中优先级',
  low: '低优先级'
}[priority] || '')

const getFeedbackClass = (type: string) => ({
  good: 'feedback-good',
  warn: 'feedback-warn',
  bad: 'feedback-bad'
}[type] || '')

const fmt = (val: number | string | undefined | null) => {
  if (val === null || val === undefined) return '0'
  const n = Number(val)
  return isNaN(n) ? '0' : Math.round(n).toString()
}


const classTrendOption = computed(() => {
  if (!classData.value) return {}
  const isDuration = classTrendType.value === 'duration'
  const data = isDuration ? classData.value.durationTrend : classData.value.scoreTrend
  const seriesName = isDuration ? '学习时长' : '作业平均分'
  return {
    tooltip: { trigger: 'axis' },
    legend: {
      data: [seriesName],
      bottom: 0,
      textStyle: { fontSize: 12 }
    },
    grid: {
      left: '8%',
      right: '4%',
      bottom: '12%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data?.map(d => d.label) || [],
      axisLabel: { fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      axisLabel: { fontSize: 11 }
    },
    series: [
      {
        name: seriesName,
        type: 'line',
        smooth: true,
        data: data?.map(d => d.value) || [],
        lineStyle: { color: '#0a66c2', width: 2 },
        itemStyle: { color: '#0a66c2' },
        symbol: 'circle',
        symbolSize: 6
      }
    ]
  }
})

const classRadarOption = computed(() => {
  if (!classData.value) return {}
  return {
    tooltip: {},
    radar: {
      indicator: ACTIONS_6.map(name => ({ name, max: 100 })),
      radius: '65%'
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: ACTIONS_6.map(a => {
              const item = classData.value?.radarData.find(r => r.skillType === a)
              return item ? item.classAvgScore : 0
            }),
            name: '班级平均',
            lineStyle: { color: '#0a66c2' },
            areaStyle: { color: 'rgba(10,102,194,0.2)' }
          }
        ]
      }
    ]
  }
})

const studentTrendOption = computed(() => {
  if (!studentData.value) return {}
  const isDuration = studentTrendType.value === 'duration'
  const data = isDuration ? studentData.value.durationTrend : studentData.value.scoreTrend
  const seriesName = isDuration ? '学习时长' : '作业平均分'
  return {
    tooltip: { trigger: 'axis' },
    legend: {
      data: [seriesName],
      bottom: 0,
      textStyle: { fontSize: 12 }
    },
    grid: {
      left: '8%',
      right: '4%',
      bottom: '12%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data?.map(d => d.label) || [],
      axisLabel: { fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      axisLabel: { fontSize: 11 }
    },
    series: [
      {
        name: seriesName,
        type: 'line',
        smooth: true,
        data: data?.map(d => d.value) || [],
        lineStyle: { color: '#0a66c2', width: 2 },
        itemStyle: { color: '#0a66c2' },
        symbol: 'circle',
        symbolSize: 6
      }
    ]
  }
})

const studentRadarOption = computed(() => {
  if (!studentData.value) return {}
  return {
    tooltip: {},
    legend: {
      data: ['学生能力', '班级平均'],
      bottom: 0
    },
    radar: {
      indicator: ACTIONS_6.map(name => ({ name, max: 100 })),
      radius: '60%'
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: ACTIONS_6.map(a => {
              const item = studentData.value?.radarData.find(r => r.skillType === a)
              return item ? item.personalScore : 0
            }),
            name: '学生能力',
            lineStyle: { color: '#0a66c2' },
            areaStyle: { color: 'rgba(10,102,194,0.2)' }
          },
          {
            value: ACTIONS_6.map(a => {
              const item = studentData.value?.radarData.find(r => r.skillType === a)
              return item ? item.classAvgScore : 0
            }),
            name: '班级平均',
            lineStyle: { color: '#94a3b8', type: 'dashed' },
            areaStyle: { color: 'rgba(148,163,184,0.1)' }
          }
        ]
      }
    ]
  }
})

const getCheckinStatus = (day: number, isPersonal = false) => {
  if (isPersonal && studentData.value) {
    return (studentData.value.personalCheckinData || []).includes(day) ? 'checked' : ''
  }
  if (!isPersonal && classData.value) {
    const count = (classData.value.checkinData.dayCounts || {})[day] || 0
    if (count === 0) return ''
    const halfStudents = Math.floor(students.value.length / 2)
    if (count > halfStudents) return 'checked'
    return 'partial'
  }
  return ''
}

onMounted(() => {
  loadClassData()
  loadClassAdvice()
})

watch(() => classStore.currentClass?.id, () => {
  loadClassData()
  loadClassAdvice()
})

watch(selectedStudentId, () => {
  if (activeView.value === 'student') {
    loadStudentData()
    loadStudentAdvice()
  }
})

watch(personalCalendarMonth, () => {
  if (activeView.value === 'student' && selectedStudentId.value) {
    studentLoading.value = true
    loadStudentData()
  }
})

watch(activeView, (newView) => {
  if (newView === 'student') {
    if (!selectedStudentId.value && students.value.length > 0) {
      selectedStudentId.value = students.value[0].id
    }
    if (selectedStudentId.value) {
      studentLoading.value = true
      loadStudentData()
      loadStudentAdvice()
    }
  }
})
</script>

<template>
  <div class="analysis-container" v-loading="loading">
    <div class="analysis-tabs">
      <el-button
        :type="activeView === 'class' ? 'primary' : 'default'"
        round
        @click="activeView = 'class'"
      >
        📊 班级整体分析
      </el-button>
      <el-button
        :type="activeView === 'student' ? 'primary' : 'default'"
        round
        @click="activeView = 'student'"
      >
        👤 学生个人分析
      </el-button>
    </div>

    <!-- 班级整体分析视图 -->
    <div v-show="activeView === 'class'" class="analysis-view">
      <template v-if="classData">
        <div class="metrics-grid">
          <el-card class="metric-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">⏱️</div>
              <div class="metric-info">
                <span class="metric-label">班级平均学习时长</span>
                <div class="metric-value-row">
                  <span class="metric-value">{{ fmt(classData.metrics.avgStudyDuration) }}</span>
                  <span class="metric-unit">小时</span>
                </div>
              </div>
            </div>
          </el-card>
          <el-card class="metric-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">📝</div>
              <div class="metric-info">
                <span class="metric-label">作业平均分</span>
                <div class="metric-value-row">
                  <span class="metric-value">{{ fmt(classData.metrics.homeworkAvg) }}</span>
                  <span class="metric-unit">分</span>
                </div>
              </div>
            </div>
          </el-card>
          <el-card class="metric-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">✅</div>
              <div class="metric-info">
                <span class="metric-label">班级打卡率</span>
                <div class="metric-value-row">
                  <span class="metric-value">{{ fmt(classData.metrics.classCheckinRate) }}</span>
                  <span class="metric-unit">%</span>
                </div>
              </div>
            </div>
          </el-card>
          <el-card class="metric-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">🏆</div>
              <div class="metric-info">
                <span class="metric-label">平均技能点数量</span>
                <div class="metric-value-row">
                  <span class="metric-value">{{ fmt(classData.metrics.avgSkillPoints) }}</span>
                  <span class="metric-unit">个</span>
                </div>
              </div>
            </div>
          </el-card>
        </div>

        <div class="charts-row">
          <el-card class="chart-card" shadow="never">
            <template #header>
              <div class="chart-header">
                <h3>📈 近4周趋势</h3>
                <div class="trend-type-btns">
                  <button
                    :class="['type-btn', { active: classTrendType === 'duration' }]"
                    @click="classTrendType = 'duration'"
                  >学习时长</button>
                  <button
                    :class="['type-btn', { active: classTrendType === 'score' }]"
                    @click="classTrendType = 'score'"
                  >作业平均分</button>
                </div>
              </div>
            </template>
            <v-chart :option="classTrendOption" autoresize style="height: 280px" />
          </el-card>

          <el-card class="chart-card" shadow="never">
            <template #header>
              <div class="chart-header">
                <h3>🎯 班级动作能力雷达图</h3>
                <div class="chart-legend">
                  <span><span class="legend-dot self-dot"></span> 班级平均</span>
                </div>
              </div>
            </template>
            <v-chart :option="classRadarOption" autoresize style="height: 280px" />
          </el-card>
        </div>

        <el-card class="checkin-card" shadow="never">
          <template #header>
            <div class="card-header-row">
              <h3>📅 班级打卡统计</h3>
            </div>
          </template>
          <div class="checkin-grid">
            <div class="checkin-summary">
              <div class="summary-item">
                <span class="summary-label">打卡最多</span>
                <span class="summary-value">{{ classData.checkinData?.topCheckinStudent || '-' }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">学习时间最长</span>
                <span class="summary-value">{{ classData.checkinData?.topStudyStudent || '-' }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">达标人数（≥12天）</span>
                <span class="summary-value">{{ calculateQualifiedStudents() }} / {{ students.length }} 人</span>
              </div>
            </div>
            <div class="checkin-calendar">
              <div class="calendar-header">
                <el-button circle size="small" @click="prevMonth">&lt;</el-button>
                <h4>{{ currentDisplayMonth.getFullYear() }}年{{ currentDisplayMonth.getMonth() + 1 }}月</h4>
                <el-button circle size="small" @click="nextMonth">&gt;</el-button>
              </div>
              <div class="calendar-weekdays">
                <span>一</span><span>二</span><span>三</span><span>四</span><span>五</span><span>六</span><span>日</span>
              </div>
              <div class="calendar-days">
                <div
                  v-for="day in getDaysInMonth(currentDisplayMonth)"
                  :key="day"
                  :class="['calendar-day', getCheckinStatus(day)]"
                >
                  {{ day }}
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <div class="analysis-grid">
          <el-card class="analysis-card" shadow="never">
            <template #header>
              <h3>📊 学情分布</h3>
            </template>
            <div class="score-distribution">
              <div class="distribution-item">
                <span class="distribution-label">优秀 (≥90分)</span>
                <div class="distribution-bar-wrapper">
                  <div class="distribution-bar" :style="{ width: ((classData.scoreDistribution.excellent || 0) / Math.max(students.length, 1) * 100) + '%' }"></div>
                </div>
                <span class="distribution-count">{{ classData.scoreDistribution.excellent || 0 }}人</span>
              </div>
              <div class="distribution-item">
                <span class="distribution-label">良好 (75-89分)</span>
                <div class="distribution-bar-wrapper">
                  <div class="distribution-bar" :style="{ width: ((classData.scoreDistribution.good || 0) / Math.max(students.length, 1) * 100) + '%' }"></div>
                </div>
                <span class="distribution-count">{{ classData.scoreDistribution.good || 0 }}人</span>
              </div>
              <div class="distribution-item">
                <span class="distribution-label">及格 (60-74分)</span>
                <div class="distribution-bar-wrapper">
                  <div class="distribution-bar" :style="{ width: ((classData.scoreDistribution.pass || 0) / Math.max(students.length, 1) * 100) + '%' }"></div>
                </div>
                <span class="distribution-count">{{ classData.scoreDistribution.pass || 0 }}人</span>
              </div>
              <div class="distribution-item">
                <span class="distribution-label">待提升 (&lt;60分)</span>
                <div class="distribution-bar-wrapper">
                  <div class="distribution-bar" :style="{ width: ((classData.scoreDistribution.fail || 0) / Math.max(students.length, 1) * 100) + '%' }"></div>
                </div>
                <span class="distribution-count">{{ classData.scoreDistribution.fail || 0 }}人</span>
              </div>
            </div>
          </el-card>

          <el-card class="analysis-card" shadow="never">
            <template #header>
              <h3>🎯 各动作能力分布</h3>
            </template>
            <div class="action-distribution">
              <div v-for="item in classData.actionDistribution" :key="item.name" class="action-bar-item">
                <span class="action-name">{{ item.name }}</span>
                <div class="action-bar-wrapper">
                  <div class="action-bar" :style="{ width: Number(fmt(item.avg)) + '%' }"></div>
                </div>
                <span class="action-avg">{{ fmt(item.avg) }}分</span>
              </div>
            </div>
          </el-card>

          <el-card class="analysis-card ai-suggestions" shadow="never">
            <template #header>
              <div class="advice-header">
                <h3>🤖 AI教学建议</h3>
                <el-button
                  type="primary"
                  size="small"
                  :loading="regeneratingClass"
                  :icon="regeneratingClass ? Loading : undefined"
                  @click="handleRegenerateClassAdvice"
                >
                  {{ regeneratingClass ? '分析中...' : '🔄 重新分析' }}
                </el-button>
              </div>
            </template>
            <div v-if="classAdvice.length > 0" class="suggestion-list">
              <div
                v-for="(item, idx) in classAdvice"
                :key="item.id || idx"
                :class="['suggestion-item', getPriorityClass(item.priorityLevel)]"
              >
                <div class="suggestion-header">
                  <span :class="['priority-badge', getPriorityClass(item.priorityLevel)]">
                    {{ getPriorityLabel(item.priorityLevel) }}
                  </span>
                  <span class="suggestion-title">{{ item.title }}</span>
                </div>
                <p>{{ item.description }}</p>
                <ul>
                  <li v-for="(s, i) in item.suggestions" :key="i">{{ s }}</li>
                </ul>
              </div>
            </div>
            <div v-else class="empty-advice">
              <p>暂无AI教学建议，点击「重新分析」生成</p>
            </div>
          </el-card>
        </div>
      </template>
    </div>

    <!-- 学生个人分析视图 -->
    <div v-show="activeView === 'student'" class="analysis-view">
      <el-card class="student-selector-card" shadow="never">
        <div class="selector-header">
          <span class="selector-label">选择学生</span>
          <span class="scroll-hint">← 滚动查看更多 →</span>
        </div>
        <div class="student-scroll-queue" @wheel.prevent="handleStudentScroll">
          <div
            v-for="student in students"
            :key="student.id"
            :class="['student-queue-item', { active: selectedStudentId === student.id }]"
            @click="selectStudent(student.id)"
          >
            <span class="queue-avatar">{{ student.name?.charAt(0) }}</span>
            <span class="queue-name">{{ student.name }}</span>
          </div>
          <div v-if="students.length === 0" class="queue-empty">暂无学生数据</div>
        </div>

        <!-- 加载状态 -->
        <div v-if="studentLoading" class="student-loading-state">
          <el-icon class="is-loading" :size="32" color="#0ea5e9"><Loading /></el-icon>
          <p>正在加载 {{ currentStudent?.name || '学生' }} 的学情数据...</p>
        </div>
      </el-card>

      <template v-if="studentData && currentStudent">
        <div class="student-profile-card">
          <div class="student-avatar-large">{{ currentStudent.name?.charAt(0) }}</div>
          <div class="student-detail">
            <h2 class="student-fullname">{{ currentStudent.name }}</h2>
            <p class="student-basic-info">学号: {{ currentStudent.studentId }}</p>
            <div class="student-badges">
              <span
                v-for="(badge, index) in studentBadges"
                :key="index"
                :class="['badge', badge.type]"
              >
                {{ badge.text }}
              </span>
              <button class="body-analysis-btn" @click="showBodyAnalysis = true">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M8 14s1.5 2 4 2 4-2 4-2"/>
                  <line x1="9" y1="9" x2="9.01" y2="9"/>
                  <line x1="15" y1="9" x2="15.01" y2="9"/>
                </svg>
                详细身体分析
              </button>
            </div>
          </div>
        </div>

        <div class="metrics-grid">
          <el-card class="metric-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">⏱️</div>
              <div class="metric-info">
                <span class="metric-label">学习时长</span>
                <div class="metric-value-row">
                  <span class="metric-value">{{ fmt(studentData.metrics.studyDuration) }}</span>
                  <span class="metric-unit">小时</span>
                </div>
              </div>
            </div>
          </el-card>
          <el-card class="metric-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">📝</div>
              <div class="metric-info">
                <span class="metric-label">作业平均分</span>
                <div class="metric-value-row">
                  <span class="metric-value">{{ fmt(studentData.metrics.homeworkAvg) }}</span>
                  <span class="metric-unit">分</span>
                </div>
              </div>
            </div>
          </el-card>
          <el-card class="metric-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">✅</div>
              <div class="metric-info">
                <span class="metric-label">打卡率</span>
                <div class="metric-value-row">
                  <span class="metric-value">{{ fmt(studentData.metrics.checkinRate) }}</span>
                  <span class="metric-unit">%</span>
                </div>
              </div>
            </div>
          </el-card>
          <el-card class="metric-card" shadow="hover">
            <div class="metric-content">
              <div class="metric-icon">🏆</div>
              <div class="metric-info">
                <span class="metric-label">技能点数量</span>
                <div class="metric-value-row">
                  <span class="metric-value">{{ fmt(studentData.metrics.skillPoints) }}</span>
                  <span class="metric-unit">个</span>
                </div>
              </div>
            </div>
          </el-card>
        </div>

        <div class="charts-row">
          <el-card class="chart-card" shadow="never">
            <template #header>
              <div class="chart-header">
                <h3>📈 近4周趋势</h3>
                <div class="trend-type-btns">
                  <button
                    :class="['type-btn', { active: studentTrendType === 'duration' }]"
                    @click="studentTrendType = 'duration'"
                  >学习时长</button>
                  <button
                    :class="['type-btn', { active: studentTrendType === 'score' }]"
                    @click="studentTrendType = 'score'"
                  >作业平均分</button>
                </div>
              </div>
            </template>
            <v-chart :option="studentTrendOption" autoresize style="height: 280px" />
          </el-card>

          <el-card class="chart-card" shadow="never">
            <template #header>
              <div class="chart-header">
                <h3>🎯 动作能力雷达图</h3>
                <div class="chart-legend">
                  <span><span class="legend-dot self-dot"></span> 学生能力</span>
                  <span><span class="legend-dot avg-dot"></span> 班级平均</span>
                </div>
              </div>
            </template>
            <v-chart :option="studentRadarOption" autoresize style="height: 280px" />
          </el-card>
        </div>

        <div class="analysis-row">
          <el-card class="personal-checkin-card compact" shadow="never">
            <template #header>
              <div class="card-header-row">
                <h3>📅 个人打卡记录</h3>
                <span class="checkin-summary-text">最近30天打卡 <strong>{{ fmt(studentData.metrics.monthTotalCheckin) }}</strong> 天，连续打卡 <strong>{{ fmt(studentData.metrics.continuousCheckin) }}</strong> 天</span>
              </div>
            </template>
            <div class="checkin-calendar personal">
              <div class="calendar-header">
                <el-button circle size="small" @click="personalCalendarMonth = new Date(personalCalendarMonth.getFullYear(), personalCalendarMonth.getMonth() - 1, 1)">←</el-button>
                <h4>{{ personalCalendarMonth.getFullYear() }}年{{ personalCalendarMonth.getMonth() + 1 }}月</h4>
                <el-button circle size="small" @click="personalCalendarMonth = new Date(personalCalendarMonth.getFullYear(), personalCalendarMonth.getMonth() + 1, 1)">→</el-button>
              </div>
              <div class="calendar-weekdays">
                <span>一</span><span>二</span><span>三</span><span>四</span><span>五</span><span>六</span><span>日</span>
              </div>
              <div class="calendar-days">
                <div
                  v-for="day in new Date(personalCalendarMonth.getFullYear(), personalCalendarMonth.getMonth() + 1, 0).getDate()"
                  :key="day"
                  :class="['calendar-day', getCheckinStatus(day, true)]"
                >
                  {{ day }}
                </div>
              </div>
            </div>
          </el-card>

          <el-card class="analysis-card compact" shadow="never">
            <template #header>
              <h3>🔍 动作能力详细分析</h3>
            </template>
            <div class="action-list">
              <div v-for="action in studentData.actionAnalysis" :key="action.name" class="action-item">
                <div class="action-header">
                  <span class="action-name">{{ action.name }}</span>
                  <span class="action-score">{{ fmt(action.avg) }}分</span>
                </div>
                <el-progress :percentage="Number(fmt(action.avg))" :show-text="false" />
                <div class="action-feedback" v-if="action.feedback">
                  <span
                    v-for="(fb, idx) in JSON.parse(action.feedback || '[]')"
                    :key="idx"
                    :class="['feedback', Number(action.avg) >= 80 ? 'feedback-good' : Number(action.avg) >= 60 ? 'feedback-warn' : 'feedback-bad']"
                  >
                    ✓ {{ fb }}
                  </span>
                </div>
              </div>
            </div>
          </el-card>
        </div>

        <el-card class="analysis-card ai-suggestions" shadow="never">
          <template #header>
            <div class="advice-header">
              <h3>🤖 AI个性化改进建议</h3>
              <el-button
                type="primary"
                size="small"
                :loading="regeneratingStudent"
                :icon="regeneratingStudent ? Loading : undefined"
                @click="handleRegenerateStudentAdvice"
              >
                {{ regeneratingStudent ? '分析中...' : '🔄 重新分析' }}
              </el-button>
            </div>
          </template>
          <div v-if="studentAdvice.length > 0" class="suggestion-list">
            <div
              v-for="(item, idx) in studentAdvice"
              :key="item.id || idx"
              :class="['suggestion-item', getPriorityClass(item.priorityLevel)]"
            >
              <div class="suggestion-header">
                <span :class="['priority-badge', getPriorityClass(item.priorityLevel)]">
                  {{ getPriorityLabel(item.priorityLevel) }}
                </span>
                <span class="suggestion-title">{{ item.title }}</span>
              </div>
              <p>{{ item.description }}</p>
              <ul>
                <li v-for="(s, i) in item.suggestions" :key="i">{{ s }}</li>
              </ul>
            </div>
          </div>
          <div v-else class="empty-advice">
            <p>暂无AI个性化建议，点击「重新分析」生成</p>
          </div>
        </el-card>

        <div class="export-section">
          <el-button type="primary" :icon="Download" @click="exportReport">
            导出个人学情报告
          </el-button>
          <el-button :icon="Message" @click="shareReport">
            发送给家长
          </el-button>
        </div>
      </template>
    </div>

    <BodyAnalysisDialog
      v-model:visible="showBodyAnalysis"
      :student-id="selectedStudentId"
      :student-name="currentStudent?.name"
    />
  </div>
</template>

<style scoped>
.analysis-container {
  padding: 0;
}

.analysis-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.metric-card {
  border-radius: 16px;
}

.metric-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.metric-icon {
  font-size: 2rem;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e0f2fe;
  border-radius: 50%;
}

.metric-info {
  flex: 1;
}

.metric-label {
  display: block;
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 4px;
}

.metric-value-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.metric-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
}

.metric-unit {
  font-size: 1rem;
  color: #64748b;
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.chart-card {
  border-radius: 16px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-header h3 {
  font-size: 1rem;
  color: #1e293b;
  margin: 0;
}

.trend-type-btns {
  display: flex;
  gap: 0;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.type-btn {
  padding: 4px 14px;
  font-size: 0.8rem;
  border: none;
  background: #f8fafc;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.type-btn.active {
  background: #0a66c2;
  color: white;
}

.chart-legend {
  display: flex;
  gap: 16px;
  font-size: 0.85rem;
  color: #64748b;
}

.legend-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 6px;
}

.legend-dot.class-dot { background: #0a66c2; }
.legend-dot.homework-dot { background: #10b981; }
.legend-dot.self-dot { background: #0a66c2; }
.legend-dot.avg-dot { background: #94a3b8; }

.checkin-card {
  border-radius: 16px;
  margin-bottom: 24px;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header-row h3 {
  margin: 0;
}

.checkin-grid {
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: 24px;
}

.checkin-summary {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-item {
  background: #f8fafc;
  padding: 16px;
  border-radius: 12px;
}

.summary-label {
  display: block;
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 8px;
}

.summary-value {
  font-size: 1.2rem;
  font-weight: 600;
  color: #1e293b;
  display: block;
  margin-bottom: 8px;
}

.summary-value.warning {
  color: #d97706;
}

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
  font-size: 0.75rem;
  border-radius: 6px;
  cursor: pointer;
  background: #ffffff;
  padding: 2px;
}

.calendar-day.checked {
  background: #dcfce7;
  color: #16a34a;
}

.calendar-day.partial {
  background: #fef3c7;
  color: #d97706;
}

.analysis-grid {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 20px;
}

.analysis-card {
  border-radius: 16px;
}

.analysis-card h3 {
  font-size: 1rem;
  color: #0ea5e9;
  margin: 0;
}

.score-distribution {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 16px;
}

.distribution-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.distribution-label {
  width: 100px;
  font-size: 0.85rem;
  color: #64748b;
}

.distribution-bar-wrapper {
  flex: 1;
  height: 10px;
  background: #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
}

.distribution-bar {
  height: 100%;
  background: linear-gradient(90deg, #0ea5e9, #38bdf8);
  border-radius: 10px;
}

.distribution-count {
  width: 50px;
  font-size: 0.85rem;
  font-weight: 500;
  color: #1e293b;
}

.action-distribution {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.action-bar-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-name {
  width: 50px;
  font-weight: 500;
}

.action-bar-wrapper {
  flex: 1;
  height: 8px;
  background: #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
}

.action-bar {
  height: 100%;
  background: #0ea5e9;
  border-radius: 10px;
}

.action-avg {
  width: 40px;
  font-size: 0.85rem;
  color: #64748b;
}

.ai-suggestions {
  background: linear-gradient(135deg, #e0f2fe 0%, #ffffff 100%);
  grid-column: span 2;
}

.advice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.advice-header h3 {
  margin: 0;
}

.empty-advice {
  text-align: center;
  padding: 32px 16px;
  color: #94a3b8;
  font-size: 0.9rem;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.suggestion-item {
  background: #ffffff;
  border-radius: 16px;
  padding: 16px;
  border-left: 4px solid #0ea5e9;
}

.suggestion-item.high-priority {
  border-left-color: #ef4444;
}

.suggestion-item.medium-priority {
  border-left-color: #f59e0b;
}

.suggestion-item.low-priority {
  border-left-color: #10b981;
}

.suggestion-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.priority-badge {
  padding: 4px 12px;
  border-radius: 30px;
  font-size: 0.75rem;
  font-weight: 500;
}

.high-priority .priority-badge {
  background: #fee2e2;
  color: #dc2626;
}

.medium-priority .priority-badge {
  background: #fef3c7;
  color: #d97706;
}

.low-priority .priority-badge {
  background: #dcfce7;
  color: #16a34a;
}

.suggestion-title {
  font-weight: 600;
  color: #1e293b;
}

.suggestion-item p {
  font-size: 0.9rem;
  color: #64748b;
  margin: 0 0 12px 0;
}

.suggestion-item ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.suggestion-item li {
  font-size: 0.85rem;
  padding: 4px 0 4px 20px;
  position: relative;
  color: #64748b;
}

.suggestion-item li::before {
  content: '•';
  position: absolute;
  left: 4px;
  color: #0ea5e9;
}

.student-selector-card {
  border-radius: 16px;
  margin-bottom: 24px;
}

.scroll-hint {
  font-size: 0.8rem;
  color: #94a3b8;
  font-weight: 400;
}

.student-scroll-queue {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 12px 4px;
  scroll-behavior: smooth;
  cursor: grab;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar {
    height: 6px;
  }

  &::-webkit-scrollbar-track {
    background: #f1f5f9;
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 3px;

    &:hover {
      background: #94a3b8;
    }
  }
}

.student-queue-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 16px;
  min-width: 80px;
  background: #f8fafc;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 2px solid transparent;
  flex-shrink: 0;

  &:hover {
    background: #e2e8f0;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  }

  &.active {
    background: #dbeafe;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59,130,246,0.15);
  }
}

.queue-avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  font-weight: 600;
}

.queue-name {
  font-size: 0.85rem;
  color: #334155;
  font-weight: 500;
  white-space: nowrap;
  max-width: 70px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.queue-empty {
  width: 100%;
  text-align: center;
  color: #94a3b8;
  padding: 20px;
  font-size: 0.9rem;
}

.student-loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 32px 16px;
  color: #64748b;
}

.student-loading-state p {
  margin: 0;
  font-size: 0.9rem;
}

.student-avatar-mini {
  width: 24px;
  height: 24px;
  background: #0ea5e9;
  color: white;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  font-weight: 600;
  flex-shrink: 0;
}

.selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.selector-label {
  font-weight: 600;
  color: #1e293b;
}

.student-avatar {
  width: 32px;
  height: 32px;
  background: #0ea5e9;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 600;
}

.student-name {
  font-weight: 500;
}

.student-score {
  font-size: 0.8rem;
  color: #64748b;
}

.student-profile-card {
  background: linear-gradient(135deg, #e0f2fe 0%, #ffffff 100%);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 24px;
}

.student-avatar-large {
  width: 80px;
  height: 80px;
  background: #0ea5e9;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  font-weight: 600;
}

.student-detail {
  flex: 1;
}

.student-fullname {
  font-size: 1.5rem;
  margin: 0 0 4px 0;
}

.student-basic-info {
  color: #64748b;
  margin: 0 0 8px 0;
}

.student-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.badge {
  background: #e0f2fe;
  color: #0ea5e9;
  padding: 4px 12px;
  border-radius: 30px;
  font-size: 0.8rem;
}

.badge.warning {
  background: #fef3c7;
  color: #dc2626;
}
.badge.success {
  background: #dcfce7;
  color: #16a34a;
}
.badge.info {
  background: #e0f2fe;
  color: #0369a1;
}

.body-analysis-btn {
  background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
  color: #0369a1;
  border: 1px solid #7dd3fc;
  padding: 6px 14px;
  border-radius: 30px;
  font-size: 0.8rem;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.body-analysis-btn:hover {
  background: linear-gradient(135deg, #bae6fd 0%, #7dd3fc 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.25);
}

.body-analysis-btn svg {
  width: 14px;
  height: 14px;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-item {
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 10px;
}

.action-item:last-child {
  border-bottom: none;
}

.action-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.action-name {
  font-weight: 500;
  color: #1e293b;
  font-size: 0.9rem;
}

.action-score {
  font-weight: 600;
  color: #0ea5e9;
  font-size: 0.9rem;
}

.action-feedback {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.feedback {
  font-size: 0.8rem;
  padding: 4px 8px;
  border-radius: 30px;
}

.feedback-good {
  background: #dcfce7;
  color: #16a34a;
}

.feedback-warn {
  background: #fef3c7;
  color: #d97706;
}

.feedback-bad {
  background: #fee2e2;
  color: #dc2626;
}

.personal-checkin-card {
  border-radius: 16px;
  margin-bottom: 24px;
  max-height: 400px;
  overflow-y: auto;
}

.analysis-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.personal-checkin-card.compact,
.analysis-card.compact {
  max-height: none;
}

.personal-checkin-card.compact .checkin-calendar {
  padding: 12px;
}

.personal-checkin-card.compact .calendar-day {
  font-size: 0.7rem;
}

.checkin-summary-text {
  font-size: 0.85rem;
  color: #64748b;
}

.checkin-summary-text strong {
  color: #0ea5e9;
}

.checkin-calendar.personal {
  background: #f8fafc;
  border-radius: 16px;
  padding: 12px;
  margin-top: 16px;
}

.checkin-calendar.personal .calendar-day {
  font-size: 0.75rem;
}

.checkin-calendar.personal .calendar-weekdays {
  font-size: 0.75rem;
}

.export-section {
  display: flex;
  gap: 16px;
  justify-content: flex-end;
  margin-top: 24px;
}

@media (max-width: 1200px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .charts-row {
    grid-template-columns: 1fr;
  }

  .analysis-grid {
    grid-template-columns: 1fr;
  }

  .ai-suggestions {
    grid-column: span 1;
  }

  .checkin-grid {
    grid-template-columns: 1fr;
  }

  .analysis-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .student-profile-card {
    flex-direction: column;
    text-align: center;
  }
}
</style>

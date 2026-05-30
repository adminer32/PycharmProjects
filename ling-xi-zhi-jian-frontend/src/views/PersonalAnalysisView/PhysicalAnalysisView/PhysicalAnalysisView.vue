<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import * as echarts from 'echarts'
import { getStudentPhysiqueApi, type StudentPhysique } from '@/api/personal_assessment/studentPhysiqueApi'
import MessageUtil from '@/utils/MessageUtil'

const loading = ref(true)
const physiqueData = ref<StudentPhysique | null>(null)

const bodyMetrics = computed(() => {
  if (!physiqueData.value) {
    return {
      bmi: 0,
      bodyFatRate: 0,
      visceralFat: 0,
      skeletalMuscle: 0,
      limbMuscleIndex: 0,
      basalMetabolism: 0,
      bodyAge: 0,
      bodyWater: 0
    }
  }
  return {
    bmi: physiqueData.value.bmi,
    bodyFatRate: physiqueData.value.fat_percentage,
    visceralFat: physiqueData.value.visceral_fat_level,
    skeletalMuscle: physiqueData.value.skeletal_muscle_mass,
    limbMuscleIndex: physiqueData.value.limb_skeletal_muscle_index,
    basalMetabolism: physiqueData.value.basal_metabolism_rate,
    bodyAge: physiqueData.value.body_age,
    bodyWater: physiqueData.value.moisture_rate
  }
})

const getBMIStatus = (bmi: number) => {
  if (bmi < 18.5) return '偏瘦'
  if (bmi < 24) return '标准'
  if (bmi < 28) return '偏胖'
  return '肥胖'
}

const getFatStatus = (fat: number) => {
  if (fat < 10) return '偏低'
  if (fat < 20) return '标准'
  if (fat < 25) return '偏高'
  return '过高'
}

const getMuscleStatus = (muscle: number) => {
  if (muscle < 25) return '偏低'
  if (muscle < 35) return '标准'
  if (muscle < 40) return '良好'
  return '优秀'
}

const getPhysiqueType = (bodyType: string) => bodyType || '未知'

const healthStatus = computed(() => {
  const bmi = bodyMetrics.value.bmi
  const fat = bodyMetrics.value.bodyFatRate
  const muscle = bodyMetrics.value.skeletalMuscle
  
  let overall = '标准'
  const bmiStatus = getBMIStatus(bmi)
  const fatStatus = getFatStatus(fat)
  const muscleStatus = getMuscleStatus(muscle)
  
  if ((bmiStatus === '标准' || bmiStatus === '偏瘦') && 
      (fatStatus === '标准' || fatStatus === '偏低') && 
      (muscleStatus === '良好' || muscleStatus === '优秀')) {
    overall = '优秀'
  } else if (bmiStatus === '标准' && fatStatus === '标准' && muscleStatus === '标准') {
    overall = '良好'
  } else if (bmiStatus === '偏瘦' || fatStatus === '偏低') {
    overall = '偏瘦'
  } else if (bmiStatus === '偏胖' || fatStatus === '过高') {
    overall = '偏胖'
  }
  
  return {
    overall,
    bmi: bmiStatus,
    bodyFat: fatStatus,
    muscle: muscleStatus,
    physique: getPhysiqueType(physiqueData.value?.body_type || '')
  }
})

const muscleData = computed(() => {
  if (!physiqueData.value) return []
  return [
    { name: '躯干', value: physiqueData.value.segment_skeletal_trunk, part: 'trunk', color: '#1890ff' },
    { name: '右上肢', value: physiqueData.value.segment_skeletal_right_upper, part: 'rightArm', color: '#722ed1' },
    { name: '左上肢', value: physiqueData.value.segment_skeletal_left_upper, part: 'leftArm', color: '#722ed1' },
    { name: '右下肢', value: physiqueData.value.segment_skeletal_right_lower, part: 'rightLeg', color: '#52c41a' },
    { name: '左下肢', value: physiqueData.value.segment_skeletal_left_lower, part: 'leftLeg', color: '#52c41a' }
  ]
})

const fatData = computed(() => {
  if (!physiqueData.value) return []
  return [
    { name: '躯干', value: physiqueData.value.segment_fat_trunk, part: 'trunk', color: '#fa8c16' },
    { name: '右上肢', value: physiqueData.value.segment_fat_right_upper, part: 'rightArm', color: '#ff7b7b' },
    { name: '左上肢', value: physiqueData.value.segment_fat_left_upper, part: 'leftArm', color: '#ff7b7b' },
    { name: '右下肢', value: physiqueData.value.segment_fat_right_lower, part: 'rightLeg', color: '#d0d0d0' },
    { name: '左下肢', value: physiqueData.value.segment_fat_left_lower, part: 'leftLeg', color: '#d0d0d0' }
  ]
})

const getStatusColor = (status: string) => {
  switch (status) {
    case '优秀': return '#52c41a'
    case '良好': return '#1890ff'
    case '标准': return '#1890ff'
    case '偏瘦': return '#faad14'
    case '偏胖': return '#faad14'
    case '肥胖': return '#ff4d4f'
    case '偏低': return '#faad14'
    case '偏高': return '#faad14'
    case '过高': return '#ff4d4f'
    default: return '#1890ff'
  }
}

const currentData = computed(() => {
  return [...muscleData.value, ...fatData.value]
})

const loadPhysiqueData = async () => {
  loading.value = true
  try {
    const res = await getStudentPhysiqueApi()
    if (res.status === 'success' && res.data) {
      physiqueData.value = res.data
    } else {
      MessageUtil.warning('暂未检测到体质数据，请先进行体质检测')
    }
  } catch (e) {
    console.error('加载体质数据失败:', e)
    MessageUtil.error('加载体质数据失败')
  } finally {
    loading.value = false
  }
}

const initCharts = () => {
  if (!physiqueData.value) return

  const bodyChartDom = document.getElementById('bodyCompositionChart')
  if (bodyChartDom) {
    const bodyChart = echarts.init(bodyChartDom)
    bodyChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c}kg ({d}%)'
      },
      legend: {
        orient: 'vertical',
        right: '5%',
        top: 'center',
        textStyle: { color: '#666' }
      },
      series: [{
        name: '身体成分',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['35%', '50%'],
        padAngle: 3,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
        data: [
          { value: physiqueData.value.segment_fat_total, name: '脂肪', itemStyle: { color: '#ff7b7b' } },
          { value: physiqueData.value.segment_skeletal_muscle_total, name: '肌肉', itemStyle: { color: '#7eb7ff' } },
          { value: physiqueData.value.bone_salt_amount, name: '骨骼', itemStyle: { color: '#ffd77e' } },
          { value: physiqueData.value.segment_moisture, name: '水分', itemStyle: { color: '#7eddff' } },
          { value: physiqueData.value.segment_protein, name: '蛋白质', itemStyle: { color: '#d0d0d0' } }
        ]
      }]
    })
    window.addEventListener('resize', () => bodyChart.resize())
  }

  const distributionDom = document.getElementById('distributionChart')
  if (distributionDom) {
    const distributionChart = echarts.init(distributionDom)
    const fatData = [
      physiqueData.value.segment_fat_right_upper,
      physiqueData.value.segment_fat_left_upper,
      physiqueData.value.segment_fat_trunk,
      physiqueData.value.segment_fat_right_lower,
      physiqueData.value.segment_fat_left_lower
    ]
    const muscleDataArr = [
      physiqueData.value.segment_skeletal_right_upper,
      physiqueData.value.segment_skeletal_left_upper,
      physiqueData.value.segment_skeletal_trunk,
      physiqueData.value.segment_skeletal_right_lower,
      physiqueData.value.segment_skeletal_left_lower
    ]
    const maxVal = Math.max(...fatData, ...muscleDataArr, 1)
    
    distributionChart.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        textStyle: { color: '#334155' }
      },
      legend: {
        data: ['脂肪', '骨骼肌'],
        bottom: 0,
        textStyle: { color: '#64748b', fontSize: 12 },
        itemGap: 40
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '18%',
        top: '8%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: ['右上肢', '左上肢', '躯干', '右下肢', '左下肢'],
        axisLabel: { color: '#64748b', fontSize: 11 },
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisTick: { show: false }
      },
      yAxis: {
        type: 'value',
        name: '',
        max: maxVal * 1.15,
        axisLabel: { show: false },
        splitLine: { lineStyle: { color: '#f1f5f9' } }
      },
      series: [
        {
          name: '脂肪',
          type: 'bar',
          barWidth: '28%',
          barGap: '30%',
          data: fatData.map((val, idx) => ({
            value: val,
            itemStyle: {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: '#fb923c' },
                  { offset: 1, color: '#f97316' }
                ]
              },
              borderRadius: [4, 4, 0, 0]
            }
          })),
          emphasis: {
            itemStyle: {
              shadowBlur: 8,
              shadowColor: 'rgba(249, 115, 22, 0.3)'
            }
          }
        },
        {
          name: '骨骼肌',
          type: 'bar',
          barWidth: '28%',
          data: muscleDataArr.map((val, idx) => ({
            value: val,
            itemStyle: {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: '#818cf8' },
                  { offset: 1, color: '#6366f1' }
                ]
              },
              borderRadius: [4, 4, 0, 0]
            }
          })),
          emphasis: {
            itemStyle: {
              shadowBlur: 8,
              shadowColor: 'rgba(99, 102, 241, 0.3)'
            }
          }
        }
      ]
    })
    window.addEventListener('resize', () => distributionChart.resize())
  }

  const radarChartDom = document.getElementById('radarChart')
  if (radarChartDom) {
    const radarChart = echarts.init(radarChartDom)
    const bmiScore = Math.min(100, Math.max(0, (24 - physiqueData.value.bmi) / 24 * 100 + 50))
    const fatScore = Math.min(100, Math.max(0, 100 - physiqueData.value.fat_percentage * 2))
    const muscleScore = Math.min(100, physiqueData.value.skeletal_muscle_mass / 40 * 100)
    const moistureScore = Math.min(100, physiqueData.value.moisture_rate / 70 * 100)
    const metabolismScore = Math.min(100, physiqueData.value.basal_metabolism_rate / 2000 * 100)
    
    radarChart.setOption({
      tooltip: {},
      radar: {
        indicator: [
          { name: '体脂', max: 100 },
          { name: '肌肉', max: 100 },
          { name: '柔韧', max: 100 },
          { name: '耐力', max: 100 },
          { name: '速度', max: 100 },
          { name: '力量', max: 100 }
        ],
        shape: 'polygon',
        splitNumber: 4,
        axisName: { color: '#666' },
        splitLine: { lineStyle: { color: '#e2e8f0' } },
        splitArea: { areaStyle: { color: ['#f8fafc', '#fff'] } },
        axisLine: { lineStyle: { color: '#e2e8f0' } }
      },
      series: [{
        type: 'radar',
        data: [{
          value: [fatScore, muscleScore, 75, metabolismScore * 0.8, metabolismScore * 0.9, muscleScore * 1.1],
          name: '综合评分',
          areaStyle: { color: 'rgba(24, 144, 255, 0.3)' },
          lineStyle: { color: '#1890ff' },
          itemStyle: { color: '#1890ff' }
        }]
      }]
    })
    window.addEventListener('resize', () => radarChart.resize())
  }
}

watch(() => physiqueData.value, (newVal) => {
  if (newVal) {
    setTimeout(initCharts, 100)
  }
})

onMounted(() => {
  loadPhysiqueData()
})
</script>

<template>
  <div class="physical-analysis-view">
    <div class="page-header">
      <h1>体质健康分析</h1>
      <p class="subtitle">基于专业毽球运动体质评估系统</p>
    </div>

    <div class="core-metrics" v-if="!loading && physiqueData">
      <div class="metric-card" :style="{ '--accent-color': '#1890ff' }">
        <div class="metric-glow"></div>
        <div class="metric-content">
          <span class="metric-value">{{ bodyMetrics.bmi }}<span class="metric-unit"></span></span>
          <span class="metric-label">BMI</span>
          <span class="metric-sub">{{ healthStatus.bmi }}</span>
        </div>
        <div class="metric-indicator">
          <div class="indicator-bar"></div>
        </div>
      </div>
      <div class="metric-card" :style="{ '--accent-color': '#52c41a' }">
        <div class="metric-glow"></div>
        <div class="metric-content">
          <span class="metric-value">{{ bodyMetrics.bodyFatRate }}<span class="metric-unit">%</span></span>
          <span class="metric-label">体脂率</span>
          <span class="metric-sub">{{ healthStatus.bodyFat }}</span>
        </div>
        <div class="metric-indicator">
          <div class="indicator-bar"></div>
        </div>
      </div>
      <div class="metric-card" :style="{ '--accent-color': '#722ed1' }">
        <div class="metric-glow"></div>
        <div class="metric-content">
          <span class="metric-value">{{ bodyMetrics.skeletalMuscle }}<span class="metric-unit">kg</span></span>
          <span class="metric-label">骨骼肌</span>
          <span class="metric-sub">{{ healthStatus.muscle }}</span>
        </div>
        <div class="metric-indicator">
          <div class="indicator-bar"></div>
        </div>
      </div>
      <div class="metric-card" :style="{ '--accent-color': '#fa8c16' }">
        <div class="metric-glow"></div>
        <div class="metric-content">
          <span class="metric-value">{{ bodyMetrics.basalMetabolism }}<span class="metric-unit">kcal</span></span>
          <span class="metric-label">基础代谢</span>
          <span class="metric-sub">每日消耗</span>
        </div>
        <div class="metric-indicator">
          <div class="indicator-bar"></div>
        </div>
      </div>
      <div class="metric-card" :style="{ '--accent-color': '#13c2c2' }">
        <div class="metric-glow"></div>
        <div class="metric-content">
          <span class="metric-value">{{ bodyMetrics.bodyWater }}<span class="metric-unit">%</span></span>
          <span class="metric-label">水分率</span>
          <span class="metric-sub">身体水分</span>
        </div>
        <div class="metric-indicator">
          <div class="indicator-bar"></div>
        </div>
      </div>
    </div>

    <div class="loading-state" v-if="loading">
      <div class="loading-spinner"></div>
      <p>正在加载体质数据...</p>
    </div>

    <div class="no-data-state" v-if="!loading && !physiqueData">
      <div class="no-data-icon">📊</div>
      <p>暂未检测到体质数据</p>
      <p class="no-data-hint">请先进行体质检测以查看详细分析</p>
    </div>

    <div class="main-analysis-section" v-if="physiqueData">
      <div class="body-data-card">
        <div class="card-header">
          <h2>身体数据分布</h2>
          <span class="overall-badge" :style="{ background: `linear-gradient(135deg, ${healthStatus.overall === '优秀' ? '#52c41a' : '#1890ff'}, ${healthStatus.overall === '优秀' ? '#95de64' : '#69c0ff'})` }">
            {{ healthStatus.overall }}
          </span>
        </div>
        <div class="data-section">
          <div class="data-section-header purple">
            <span>骨骼肌</span>
            <span class="data-total-inline">{{ physiqueData.segment_skeletal_muscle_total.toFixed(1) }}kg</span>
          </div>
          <div class="data-list compact two-col">
            <div v-for="item in muscleData" :key="item.name" class="data-item compact">
              <div class="data-indicator" :style="{ background: item.color }"></div>
              <div class="data-info">
                <span class="data-name">{{ item.name }}</span>
              </div>
              <span class="data-value-highlight purple">{{ item.value.toFixed(1) }}kg</span>
            </div>
          </div>
        </div>
        <div class="data-section">
          <div class="data-section-header orange">
            <span>脂肪</span>
            <span class="data-total-inline">{{ physiqueData.segment_fat_total.toFixed(1) }}kg</span>
          </div>
          <div class="data-list compact two-col">
            <div v-for="item in fatData" :key="item.name" class="data-item compact">
              <div class="data-indicator" :style="{ background: item.color }"></div>
              <div class="data-info">
                <span class="data-name">{{ item.name }}</span>
              </div>
              <span class="data-value-highlight orange">{{ item.value.toFixed(1) }}kg</span>
            </div>
          </div>
        </div>
        <div class="body-stats-row">
          <div class="stat-badge">
            <span class="stat-badge-label">身体类型</span>
            <span class="stat-badge-value">{{ physiqueData.body_type }}</span>
          </div>
          <div class="stat-badge">
            <span class="stat-badge-label">身体形态</span>
            <span class="stat-badge-value">{{ physiqueData.body_shape }}</span>
          </div>
          <div class="stat-badge">
            <span class="stat-badge-label">身体年龄</span>
            <span class="stat-badge-value">{{ bodyMetrics.bodyAge }}<span style="font-size: 12px">岁</span></span>
          </div>
          <div class="stat-badge">
            <span class="stat-badge-label">心率</span>
            <span class="stat-badge-value">{{ physiqueData.heart_rate }}<span style="font-size: 12px">bpm</span></span>
          </div>
        </div>
      </div>

      <div class="evaluation-card">
        <div class="card-header">
          <h2>综合能力评估</h2>
        </div>
        <div class="radar-wrapper">
          <div id="radarChart" class="radar-chart"></div>
        </div>
        <div class="evaluation-legend">
          <div class="legend-item">
            <span class="legend-dot" style="background: #1890ff;"></span>
            <span class="legend-text">综合评分 85分</span>
          </div>
          <div class="score-breakdown">
            <div class="score-item">
              <span class="score-label">力量</span>
              <span class="score-value">88</span>
            </div>
            <div class="score-item">
              <span class="score-label">速度</span>
              <span class="score-value">82</span>
            </div>
            <div class="score-item">
              <span class="score-label">耐力</span>
              <span class="score-value">78</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="charts-section" v-if="physiqueData">
      <div class="chart-card">
        <h3>身体成分构成</h3>
        <div id="bodyCompositionChart" class="main-chart"></div>
      </div>
      <div class="chart-card">
        <h3>脂肪与骨骼肌分布对比</h3>
        <div id="distributionChart" class="main-chart"></div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.physical-analysis-view {
  padding: 24px;
  background: linear-gradient(180deg, #f0f7ff 0%, #ffffff 50%, #f0f7ff 100%);
  min-height: 100%;
}

.page-header {
  text-align: center;
  margin-bottom: 20px;
  
  h1 {
    font-size: 24px;
    font-weight: 600;
    color: #1e293b;
    margin: 0 0 6px 0;
  }
  
  .subtitle {
    font-size: 13px;
    color: #64748b;
    margin: 0;
  }
}

.core-metrics {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.metric-card {
  position: relative;
  background: linear-gradient(145deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 12px;
  padding: 14px 10px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.06);
  border: 1px solid rgba(24, 144, 255, 0.08);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(24, 144, 255, 0.12);
    border-color: var(--accent-color);
    
    .metric-glow {
      opacity: 1;
    }
    
    .indicator-bar {
      transform: scaleX(1);
    }
  }
}

.metric-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--accent-color), transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.metric-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  position: relative;
  z-index: 1;
}

.metric-value {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  
  .metric-unit {
    font-size: 11px;
    font-weight: 500;
    margin-left: 2px;
    -webkit-text-fill-color: #64748b;
  }
}

.metric-label {
  font-size: 11px;
  font-weight: 600;
  color: #1e293b;
  margin-top: 4px;
}

.metric-sub {
  font-size: 9px;
  color: #94a3b8;
  margin-top: 2px;
}

.metric-indicator {
  margin-top: 8px;
  height: 3px;
  background: #f0f0f0;
  border-radius: 2px;
  overflow: hidden;
}

.indicator-bar {
  height: 100%;
  width: 60%;
  background: linear-gradient(90deg, var(--accent-color), color-mix(in srgb, var(--accent-color), white 30%));
  border-radius: 2px;
  transform: scaleX(0.6);
  transform-origin: left;
  transition: transform 0.6s ease;
}

.main-analysis-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  
  h2 {
    font-size: 18px;
    font-weight: 600;
    color: #1e293b;
    margin: 0;
  }
}

.tab-switch {
  display: flex;
  gap: 6px;
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
  padding: 6px;
  border-radius: 16px;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: none;
  background: transparent;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  
  svg {
    width: 16px;
    height: 16px;
  }
  
  &.active {
    background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
    color: #ffffff;
    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
  }
  
  &:hover:not(.active) {
    color: #4f46e5;
    background: rgba(99, 102, 241, 0.08);
  }
}

.overall-badge {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 4px;
}

.legend-dot.muscle {
  background: linear-gradient(135deg, #8b5cf6, #6366f1);
}

.legend-dot.fat {
  background: linear-gradient(135deg, #ff8c00, #ff6b35);
}

.legend-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.body-data-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 12px;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.06);
  border: 1px solid rgba(99, 102, 241, 0.08);
}

.body-data-card .card-header {
  margin-bottom: 8px;
}

.body-data-card .card-header h2 {
  font-size: 14px;
}

.data-total {
  font-size: 14px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 12px;
}

.data-total.purple {
  background: linear-gradient(135deg, #8b5cf6, #6366f1);
  color: #fff;
}

.data-total.orange {
  background: linear-gradient(135deg, #ff8c00, #ff6b35);
  color: #fff;
}

.data-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.data-list.compact {
  gap: 6px;
}

.data-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 10px;
  border: 1px solid rgba(99, 102, 241, 0.06);
  transition: all 0.3s ease;
  cursor: pointer;
  
  &:hover {
    background: rgba(255, 255, 255, 0.95);
    border-color: rgba(99, 102, 241, 0.15);
    transform: translateX(4px);
    box-shadow: 0 4px 16px rgba(99, 102, 241, 0.1);
  }
  
  &.compact {
    padding: 6px 8px;
    gap: 6px;
  }
}

.data-value-highlight {
  font-size: 13px;
  font-weight: 600;
  margin-left: auto;
}

.data-value-highlight.purple {
  color: #6366f1;
}

.data-value-highlight.orange {
  color: #ff6b35;
}

.data-section {
  margin-bottom: 8px;
}

.data-section:last-of-type {
  margin-bottom: 0;
}

.data-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 4px;
  margin-bottom: 4px;
}

.data-section-header.purple {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.15), rgba(99, 102, 241, 0.1));
  color: #6366f1;
}

.data-section-header.orange {
  background: linear-gradient(135deg, rgba(255, 140, 0, 0.15), rgba(255, 107, 53, 0.1));
  color: #ff6b35;
}

.data-total-inline {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.8);
}

.data-section-header.purple .data-total-inline {
  color: #6366f1;
}

.data-section-header.orange .data-total-inline {
  color: #ff6b35;
}

.data-list.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.data-indicator {
  width: 3px;
  height: 24px;
  border-radius: 2px;
}

.data-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.data-name {
  font-size: 11px;
  color: #64748b;
  font-weight: 500;
}

.data-value {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  
  .data-unit {
    font-size: 11px;
    font-weight: 500;
    color: #94a3b8;
  }
}

.data-bar {
  width: 60px;
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
}

.data-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.6s ease;
}

.body-stats-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.stat-badge {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px;
  background: linear-gradient(135deg, #f8fafc 0%, #f0f7ff 100%);
  border-radius: 8px;
  gap: 2px;
}

.stat-badge-label {
  font-size: 9px;
  color: #64748b;
}

.stat-badge-value {
  font-size: 12px;
  font-weight: 700;
  color: #1e293b;
}

.stat-badge-trend {
  font-size: 11px;
  color: #94a3b8;
  
  &.positive {
    color: #52c41a;
  }
}

.evaluation-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(24, 144, 255, 0.06);
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
}

.radar-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.radar-chart {
  width: 100%;
  height: 280px;
}

.evaluation-legend {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.legend-text {
  font-size: 13px;
  color: #666;
}

.score-breakdown {
  display: flex;
  gap: 24px;
}

.score-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.score-label {
  font-size: 11px;
  color: #94a3b8;
}

.score-value {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.charts-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(24, 144, 255, 0.06);
  border: 1px solid #e2e8f0;
  
  h3 {
    font-size: 14px;
    font-weight: 600;
    color: #1e293b;
    margin: 0 0 12px 0;
  }
}

.main-chart {
  height: 200px;
  width: 100%;
}

.loading-state, .no-data-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #e2e8f0;
  border-top-color: #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-state p, .no-data-state p {
  margin-top: 16px;
  color: #64748b;
  font-size: 14px;
}

.no-data-icon {
  font-size: 64px;
  opacity: 0.5;
}

.no-data-hint {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 8px;
}

@media (max-width: 1200px) {
  .main-analysis-section {
    grid-template-columns: 1fr;
  }
  
  .body-data-card {
    order: 1;
  }
  
  .evaluation-card {
    order: 2;
  }
}

@media (max-width: 768px) {
  .physical-analysis-view {
    padding: 16px;
  }
  
  .core-metrics {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .body-data-card {
    width: 100%;
  }
  
  .charts-section {
    grid-template-columns: 1fr;
  }
}
</style>

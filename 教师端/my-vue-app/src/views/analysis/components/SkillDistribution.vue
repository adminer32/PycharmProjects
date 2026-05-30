<script setup lang="ts">
import { computed, ref } from 'vue'
import VChart from 'vue-echarts'
import { actionNamesList, baseColors } from '../constants'

const props = defineProps<{
  distributionData: Array<{ name: string, avg: number, feedback?: string }>
  homeworkAvg: number
  type: 'class' | 'student'
}>()

const hoveredAction = ref<string | null>(null)

const pieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
    label: { show: false, position: 'center' },
    emphasis: { label: { show: true, fontSize: '18', fontWeight: 'bold' } },
    labelLine: { show: false },
    data: actionNamesList.map((name, i) => ({
      name,
      value: (props.distributionData || []).find(d => d.name === name)?.avg || 1,
      itemStyle: { color: baseColors[i] }
    }))
  }]
}))

const liquidOption = computed(() => ({
  series: [{
    type: 'liquidFill',
    data: [props.homeworkAvg / 100],
    radius: '75%',
    outline: { show: false },
    backgroundStyle: { color: '#f8fafc' },
    label: { 
      formatter: `掌握度\n${props.homeworkAvg}分`,
      fontSize: 18,
      color: '#0ea5e9',
      fontWeight: 'bold'
    }
  }]
}))

const handleMouseOver = (params: any) => {
  if (actionNamesList.includes(params.name)) hoveredAction.value = params.name
}

const handleMouseOut = () => {
  hoveredAction.value = null
}
</script>

<template>
  <div class="skill-distribution-container">
    <div class="charts-row">
      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="chart-header">
            <h3>🔥 动作技能分布</h3>
          </div>
        </template>
        <v-chart 
          :option="pieOption" 
          @mouseover="handleMouseOver" 
          @mouseout="handleMouseOut" 
          autoresize 
          style="height: 320px" 
        />
      </el-card>

      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="chart-header">
            <h3>📊 学习掌握度</h3>
          </div>
        </template>
        <v-chart :option="liquidOption" autoresize style="height: 320px" />
      </el-card>
    </div>

    <!-- 技能进度列表 -->
    <el-card class="analysis-card mt-24" shadow="never">
      <template #header>
        <h3>🎯 各动作能力分值</h3>
      </template>
      <div class="action-bars">
        <div v-for="item in distributionData" :key="item.name" class="action-bar-item">
          <div class="action-info">
            <span class="action-name">{{ item.name }}</span>
            <span class="action-avg">{{ item.avg }}分</span>
          </div>
          <div class="action-bar-wrapper">
            <div class="action-bar" :style="{ width: item.avg + '%' }"></div>
          </div>
          <p v-if="item.feedback" class="action-feedback">{{ item.feedback }}</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.skill-distribution-container {
  display: flex;
  flex-direction: column;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.mt-24 { margin-top: 24px; }

.chart-header h3 {
  font-size: 1rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.action-bars {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.action-bar-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-name {
  font-size: 0.9rem;
  font-weight: 500;
  color: #1e293b;
}

.action-avg {
  font-size: 0.9rem;
  font-weight: 700;
  color: #0ea5e9;
}

.action-bar-wrapper {
  height: 8px;
  background: #f1f5f9;
  border-radius: 4px;
  overflow: hidden;
}

.action-bar {
  height: 100%;
  background: linear-gradient(90deg, #38bdf8 0%, #0ea5e9 100%);
  border-radius: 4px;
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.action-feedback {
  font-size: 0.75rem;
  color: #64748b;
  background: #f8fafc;
  padding: 8px;
  border-radius: 8px;
  margin: 0;
}

@media (max-width: 1200px) {
  .charts-row { grid-template-columns: 1fr; }
}
</style>

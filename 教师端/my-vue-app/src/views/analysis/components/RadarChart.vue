<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { ACTIONS_6 } from '../constants'

const props = defineProps<{
  title: string
  data: Array<{ skillType: string, personalScore: number, classAvgScore: number }>
  type: 'class' | 'student'
}>()

const option = computed(() => ({
  radar: {
    indicator: ACTIONS_6.map(name => ({ name, max: 100 })),
    radius: '65%',
    axisName: { color: '#64748b' }
  },
  tooltip: { trigger: 'item' },
  series: [{
    type: 'radar',
    data: [
      props.type === 'student' ? {
        value: ACTIONS_6.map(a => {
          const item = (props.data || []).find(r => r.skillType === a)
          return item ? item.personalScore : 0
        }),
        name: '学生能力',
        areaStyle: { color: 'rgba(14, 165, 233, 0.2)' }
      } : null,
      {
        value: ACTIONS_6.map(a => {
          const item = (props.data || []).find(r => r.skillType === a)
          return item ? item.classAvgScore : 0
        }),
        name: '班级平均',
        lineStyle: props.type === 'student' ? { type: 'dashed' } : {},
        areaStyle: props.type === 'class' ? { color: 'rgba(14, 165, 233, 0.2)' } : {}
      }
    ].filter(Boolean)
  }]
}))
</script>

<template>
  <el-card class="chart-card" shadow="never">
    <template #header>
      <div class="chart-header">
        <h3>{{ title }}</h3>
        <div class="chart-legend" v-if="type === 'student'">
          <span><span class="legend-dot self-dot"></span> 学生能力</span>
          <span><span class="legend-dot avg-dot"></span> 班级平均</span>
        </div>
      </div>
    </template>
    <v-chart :option="option" autoresize style="height: 280px" />
  </el-card>
</template>

<style scoped>
.chart-card {
  border-radius: 16px;
  border: 1px solid #f1f5f9;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-header h3 {
  font-size: 1rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.chart-legend {
  display: flex;
  gap: 12px;
  font-size: 0.75rem;
  color: #64748b;
}

.legend-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 4px;
}

.self-dot { background: #0ea5e9; }
.avg-dot { border: 1px dashed #0ea5e9; }
</style>

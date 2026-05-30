<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'

const props = defineProps<{
  title: string
  type: 'duration' | 'score'
  data: Array<{ label: string, value: number }>
}>()

const emit = defineEmits(['update:type'])

const option = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', data: props.data?.map(d => d.label) || [] },
  yAxis: { type: 'value' },
  series: [{
    name: props.type === 'duration' ? '学习时长' : '作业平均分',
    type: 'line',
    smooth: true,
    data: props.data?.map(d => d.value) || [],
    itemStyle: { color: props.type === 'duration' ? '#0ea5e9' : '#f97316' },
    areaStyle: { 
      color: props.type === 'duration' ? 'rgba(14, 165, 233, 0.1)' : 'rgba(249, 115, 22, 0.1)' 
    }
  }]
}))
</script>

<template>
  <el-card class="chart-card" shadow="never">
    <template #header>
      <div class="chart-header">
        <h3>{{ title }}</h3>
        <div class="trend-type-btns">
          <button
            :class="['type-btn', { active: type === 'duration' }]"
            @click="emit('update:type', 'duration')"
          >学习时长</button>
          <button
            :class="['type-btn', { active: type === 'score' }]"
            @click="emit('update:type', 'score')"
          >作业平均分</button>
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

.trend-type-btns {
  display: flex;
  gap: 8px;
  background: #f1f5f9;
  padding: 4px;
  border-radius: 8px;
}

.type-btn {
  border: none;
  background: transparent;
  padding: 4px 12px;
  font-size: 0.8rem;
  border-radius: 6px;
  cursor: pointer;
  color: #64748b;
  transition: all 0.2s;
}

.type-btn.active {
  background: white;
  color: #0ea5e9;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}
</style>

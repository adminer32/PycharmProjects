<script setup lang="ts">
import type { ClassOverview } from '../types'

defineProps<{
  data: ClassOverview
}>()
</script>

<template>
  <div class="class-overview">
    <div class="overview-card">
      <div class="overview-icon">👥</div>
      <div class="overview-info">
        <span class="overview-label">班级总人数</span>
        <span class="overview-value">{{ data.totalStudents }}</span>
      </div>
    </div>
    <div class="overview-card">
      <div class="overview-icon">📝</div>
      <div class="overview-info">
        <span class="overview-label">今日作业提交</span>
        <span class="overview-value">
          {{ data.todayHomework.submitted }}
          <span class="overview-unit">/ {{ data.todayHomework.total }}</span>
        </span>
        <span class="overview-trend up">{{ data.todayHomework.rate }}</span>
      </div>
    </div>
    <div class="overview-card">
      <div class="overview-icon">✅</div>
      <div class="overview-info">
        <span class="overview-label">今日打卡完成</span>
        <span class="overview-value">
          {{ data.todayCheckin.submitted }}
          <span class="overview-unit">/ {{ data.todayCheckin.total }}</span>
        </span>
        <span class="overview-trend up">{{ data.todayCheckin.rate }}</span>
      </div>
    </div>
    <div class="overview-card">
      <div class="overview-icon">⏳</div>
      <div class="overview-info">
        <span class="overview-label">待批改作业</span>
        <span class="overview-value">{{ data.pendingHomework }}</span>
        <span class="overview-trend warning">待处理</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.class-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.overview-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #f3f4f6;
  transition: all 0.2s;
}

.overview-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.overview-icon {
  font-size: 2rem;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e0f2fe;
  border-radius: 12px;
}

.overview-info {
  flex: 1;
}

.overview-label {
  display: block;
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 4px;
}

.overview-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
}

.overview-unit {
  font-size: 0.9rem;
  color: #94a3b8;
  font-weight: 400;
}

.overview-trend {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 30px;
  margin-left: 8px;
}

.overview-trend.up {
  background: #dcfce7;
  color: #16a34a;
}

.overview-trend.warning {
  background: #fef3c7;
  color: #d97706;
}

@media (max-width: 1000px) {
  .class-overview {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .class-overview {
    grid-template-columns: 1fr;
  }
}
</style>

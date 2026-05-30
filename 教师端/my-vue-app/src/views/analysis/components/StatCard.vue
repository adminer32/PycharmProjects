<script setup lang="ts">
defineProps<{
  icon: string
  label: string
  value: string | number
  unit: string
}>()
</script>

<template>
  <el-card class="metric-card" shadow="hover">
    <div class="metric-content">
      <div class="metric-icon">{{ icon }}</div>
      <div class="metric-info">
        <span class="metric-label">{{ label }}</span>
        <div class="metric-value-row">
          <span class="metric-value">{{ value }}</span>
          <span class="metric-unit">{{ unit }}</span>
        </div>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.metric-card {
  border-radius: 16px;
  border: none;
  background: white;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.metric-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.05);
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
  background: #f0f9ff;
  border-radius: 12px;
}

.metric-info {
  display: flex;
  flex-direction: column;
}

.metric-label {
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 4px;
}

.metric-value-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.metric-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
}

.metric-unit {
  font-size: 0.8rem;
  color: #94a3b8;
}
</style>

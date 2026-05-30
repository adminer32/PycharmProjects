<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { PendingItem } from '../types'

defineProps<{
  items: PendingItem[]
}>()

const router = useRouter()

const handleAction = (route: string) => {
  router.push(route)
}
</script>

<template>
  <el-card class="pending-card" shadow="never">
    <template #header>
      <div class="card-header">
        <span class="card-title">
          <span class="title-icon">⏰</span>
          <span>待处理事项</span>
        </span>
        <span class="badge-count">{{ items.length }}项</span>
      </div>
    </template>
    
    <div class="pending-list">
      <div v-for="item in items" :key="item.id" class="pending-item">
        <span class="pending-icon">{{ item.icon }}</span>
        <div class="pending-info">
          <span class="pending-title">{{ item.title }}</span>
          <span class="pending-desc">{{ item.description }}</span>
        </div>
        <el-button type="primary" size="small" plain @click="handleAction(item.actionRoute)">
          {{ item.actionText }} →
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.pending-card {
  border-radius: 16px;
  height: 100%;
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

.badge-count {
  background: #0ea5e9;
  color: white;
  padding: 2px 10px;
  border-radius: 30px;
  font-size: 0.85rem;
}

.pending-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pending-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  transition: all 0.2s;
}

.pending-item:hover {
  background: #f1f5f9;
}

.pending-icon {
  font-size: 1.5rem;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border-radius: 10px;
}

.pending-info {
  flex: 1;
  min-width: 0;
}

.pending-title {
  display: block;
  font-weight: 500;
  color: #1e293b;
  margin-bottom: 4px;
}

.pending-desc {
  display: block;
  font-size: 0.85rem;
  color: #64748b;
}
</style>

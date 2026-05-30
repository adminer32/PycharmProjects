<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { House, VideoPlay, Calendar, DataAnalysis, Document } from '@element-plus/icons-vue'

const route = useRoute()
const isFolded = ref(false)

const menuItems = [
  { index: '/home', icon: House, label: '首页' },
  { index: '/classroom', icon: VideoPlay, label: '课堂管理' },
  { index: '/learning', icon: Calendar, label: '学习管理' },
  { index: '/analysis', icon: DataAnalysis, label: '学情分析' },
  { index: '/homework', icon: Document, label: '作业管理' },
]

const isActive = (indexPath: string) => {
  if (indexPath === '/home') {
    return route.path === '/home'
  }
  return route.path.startsWith(indexPath)
}

const toggleFold = () => {
  isFolded.value = !isFolded.value
}
</script>

<template>
  <aside :class="['sidebar', { folded: isFolded }]">
    <div class="logo-area">
      <div class="logo-icon">
        <img src="@/assets/images/logo.png" alt="翎翼毽球" />
      </div>
      <h2 v-show="!isFolded">翎翼毽球</h2>
      <span v-show="!isFolded" class="badge">教师端</span>
    </div>

    <nav class="side-nav">
      <ul>
        <li
          v-for="item in menuItems"
          :key="item.index"
          :class="['nav-item', { active: isActive(item.index) }]"
          @click="$router.push(item.index)"
        >
          <div class="nav-content">
            <span class="nav-icon">
              <el-icon :size="24"><component :is="item.icon" /></el-icon>
            </span>
            <span v-show="!isFolded" class="nav-text">{{ item.label }}</span>
          </div>
        </li>
      </ul>
    </nav>

    <div class="stretch-btn" @click="toggleFold">
      <span :class="['stretch-icon', { rotated: isFolded }]">◀</span>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 260px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-right: 1px solid #f3f4f6;
  position: relative;
  transition: all 0.5s ease;
  overflow: visible; /* 修改这里：让按钮可以溢出容器 */
}

.sidebar.folded {
  width: 80px;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 70px;
  padding: 0 20px;
  border-bottom: 1px solid #f3f4f6;
}

.logo-icon {
  width: 35px;
  height: 35px;
  color: #0ea5e9;
  flex-shrink: 0;
}

.logo-area h2 {
  font-size: 20px;
  font-weight: 700;
  color: #0ea5e9;
  margin: 0;
  white-space: nowrap;
}

.badge {
  padding: 4px 10px;
  background: #e0f2fe;
  color: #0ea5e9;
  font-size: 12px;
  border-radius: 9999px;
  font-weight: 500;
  white-space: nowrap;
}

.side-nav {
  flex: 1;
  padding: 12px 0;
}

.side-nav ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  height: 54px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.nav-content {
  display: flex;
  align-items: center;
  padding-left: 12px;
  height: 100%;
  border-left: 3px solid transparent;
  color: #64748b;
  transition: all 0.2s ease;
  gap: 12px;
}

.nav-content:hover {
  background-color: #f9fafb;
}

.nav-item.active .nav-content {
  border-left-color: #0ea5e9;
  color: #0ea5e9;
  font-weight: 600;
  background-color: #f0f9ff;
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 39px;
  height: 39px;
  flex-shrink: 0;
}

.nav-text {
  font-size: 14px;
  white-space: nowrap;
}

.stretch-btn {
  position: absolute;
  top: 50%;
  right: -20px; /* 按钮中心点距离右侧-20px */
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  background: #ffffff;
  border: 1px solid #f3f4f6;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.35s ease-in-out;
  z-index: 10;
  /* 添加这些样式来确保按钮完全可见 */
  margin-right: 0;
  pointer-events: auto;
}

.stretch-btn:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.stretch-icon {
  color: #64748b;
  font-size: 14px;
  transition: transform 0.3s ease;
}

.stretch-icon.rotated {
  transform: rotate(180deg);
}

/* 当侧边栏折叠时，调整按钮的位置 */
.sidebar.folded .stretch-btn {
  right: -20px; /* 保持相同的位置，确保按钮完全显示 */
}
</style>

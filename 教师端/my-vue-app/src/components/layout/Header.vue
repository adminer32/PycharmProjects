<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import UserDropdown from './components/UserDropdown.vue'
import { useClassStore } from '@/store/class'
import { storeToRefs } from 'pinia'

const route = useRoute()
const router = useRouter()
const searchKeyword = ref('')
const showClassSelector = ref(false)

const classStore = useClassStore()
const { classList, currentClass } = storeToRefs(classStore)

const pageTitle = computed(() => {
  const titleMap: Record<string, string> = {
    '/home': '首页',
    '/classroom': '课堂管理',
    '/learning': '学习管理',
    '/analysis': '学情分析',
    '/homework': '作业管理',
    '/profile': '个人信息'
  }
  return titleMap[route.path] || '首页'
})

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    console.log('搜索:', searchKeyword.value)
  }
}

const handleSelectClass = (classItem: typeof classList.value[0]) => {
  classStore.setCurrentClass(classItem)
  showClassSelector.value = false
}

onMounted(async () => {
  await classStore.fetchClassList()
  if (classList.value.length > 0 && !currentClass.value) {
    classStore.setCurrentClass(classList.value[0])
  }
})
</script>

<template>
  <header class="main-header">
    <div class="header-left">
      <h1 class="page-title">{{ pageTitle }}</h1>
      <div class="class-selector-wrapper">
        <div 
          class="class-selector" 
          :class="{ active: showClassSelector }"
          @click="showClassSelector = !showClassSelector"
        >
          <span class="class-name">{{ currentClass?.name || '暂无班级' }}</span>
          <span class="selector-arrow" :class="{ rotated: showClassSelector }">▼</span>
        </div>
        <div v-if="showClassSelector" class="class-dropdown">
          <template v-if="classList.length > 0">
            <div 
              v-for="item in classList" 
              :key="item.id" 
              class="class-item"
              :class="{ selected: item.id === currentClass?.id }"
              @click="handleSelectClass(item)"
            >
              <span class="item-name">{{ item.name }}</span>
              <span class="item-count">{{ item.studentCount }}人</span>
            </div>
          </template>
          <div v-else class="empty-dropdown">暂无班级</div>
        </div>
      </div>
    </div>
    
    <div class="header-center">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索学生、课程、作业..."
        :prefix-icon="Search"
        class="global-search"
        clearable
        @keyup.enter="handleSearch"
      />
    </div>
    
    <div class="header-right">
      <UserDropdown />
    </div>
  </header>
  
  <div 
    v-if="showClassSelector" 
    class="dropdown-overlay" 
    @click="showClassSelector = false"
  />
</template>

<style scoped>
.main-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 70px;
  padding: 0 24px;
  background: #ffffff;
  border-bottom: 1px solid #f3f4f6;
  gap: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-shrink: 0;
  width: 280px;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #0ea5e9;
  margin: 0;
  white-space: nowrap;
}

.class-selector-wrapper {
  position: relative;
}

.class-selector {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.class-selector:hover {
  background: #f1f5f9;
  border-color: #0ea5e9;
}

.class-selector.active {
  background: #e0f2fe;
  border-color: #0ea5e9;
}

.class-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.selector-arrow {
  font-size: 10px;
  color: #64748b;
  transition: transform 0.2s;
}

.selector-arrow.rotated {
  transform: rotate(180deg);
}

.class-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  min-width: 180px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 100;
  overflow: hidden;
}

.class-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.class-item:hover {
  background: #f8fafc;
}

.class-item.selected {
  background: #e0f2fe;
}

.item-name {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}

.item-count {
  font-size: 12px;
  color: #64748b;
}

.empty-dropdown {
  padding: 16px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}

.dropdown-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 50;
}

.header-center {
  flex: 1;
  max-width: 400px;
  min-width: 200px;
}

.global-search {
  --el-input-border-radius: 20px;
}

.global-search :deep(.el-input__wrapper) {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  box-shadow: none;
  transition: all 0.2s;
}

.global-search :deep(.el-input__wrapper:hover) {
  border-color: #0ea5e9;
}

.global-search :deep(.el-input__wrapper.is-focus) {
  background: #ffffff;
  border-color: #0ea5e9;
  box-shadow: 0 0 0 2px rgba(14, 165, 233, 0.1);
}

.global-search :deep(.el-input__prefix) {
  color: #94a3b8;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

@media (max-width: 900px) {
  .header-center {
    display: none;
  }
}

@media (max-width: 600px) {
  .header-left {
    width: auto;
  }
  
  .class-selector-wrapper {
    display: none;
  }
}
</style>

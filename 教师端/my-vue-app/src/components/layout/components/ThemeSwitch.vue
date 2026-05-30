<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Sunny, Moon } from '@element-plus/icons-vue'

const isDark = ref(false)

const toggleTheme = () => {
  isDark.value = !isDark.value
  
  if (isDark.value) {
    document.documentElement.classList.add('dark')
    localStorage.setItem('theme', 'dark')
  } else {
    document.documentElement.classList.remove('dark')
    localStorage.setItem('theme', 'light')
  }
}

onMounted(() => {
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'dark') {
    isDark.value = true
    document.documentElement.classList.add('dark')
  }
})
</script>

<template>
  <button class="theme-switch-btn" @click="toggleTheme" :title="isDark ? '切换到浅色模式' : '切换到深色模式'">
    <el-icon :size="20">
      <Sunny v-if="!isDark" />
      <Moon v-else />
    </el-icon>
  </button>
</template>

<style scoped>
.theme-switch-btn {
  width: 40px;
  height: 40px;
  border: 1px solid #e2e8f0;
  border-radius: 50%;
  background: #ffffff;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.2s;
  color: #64748b;
}

.theme-switch-btn:hover {
  background: #f8fafc;
  border-color: #0ea5e9;
  color: #0ea5e9;
}
</style>

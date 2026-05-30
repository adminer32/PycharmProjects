<script setup lang="ts">
import { onMounted } from 'vue'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import MainContent from './MainContent.vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

onMounted(async () => {
  if (userStore.token) {
    // 全局并行拉取基本账号和个人档案
    await Promise.all([
      userStore.fetchUserInfo(),
      userStore.fetchMyProfile()
    ])
  }
})
</script>

<template>
  <el-container class="app-layout">
    <Sidebar />
    
    <el-container class="main-container">
      <div class="bg-decoration"></div>
      
      <Header />
      <MainContent />
    </el-container>
  </el-container>
</template>

<style scoped>
.app-layout {
  height: 100vh;
  width: 100%;
  background-color: #f8fafc;
}

.main-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  background-color: rgba(248, 250, 252, 0.5);
  position: relative;
}

.bg-decoration {
  position: absolute;
  top: 0;
  right: 0;
  width: 600px;
  height: 400px;
  background-color: rgba(219, 234, 254, 0.5);
  border-radius: 50%;
  mix-blend-mode: multiply;
  filter: blur(100px);
  opacity: 0.6;
  pointer-events: none;
}
</style>

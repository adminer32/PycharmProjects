<script setup lang="ts">
</script>

<template>
  <el-main class="main-content">
    <router-view v-slot="{ Component }">
      <transition name="fade-transform" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </el-main>
</template>

<style scoped>
.main-content {
  flex: 1;
  padding: 32px;
  overflow-y: auto;
  width: 100%;
  height: 0;
  position: relative;
  z-index: 10;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateY(15px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateY(-15px);
}
</style>

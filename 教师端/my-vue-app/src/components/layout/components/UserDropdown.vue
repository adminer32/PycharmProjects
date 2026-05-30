<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Bell, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { AuthAPI } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const dropdownVisible = ref(false)

const avatarUrl = computed(() => {
  return userStore.userAvatar
})

const userName = computed(() => {
  return userStore.userName
})

const userGrade = computed(() => {
  const department = userStore.profile?.department || '教育工作者'
  const specialty = userStore.profile?.specialty ? ' · ' + userStore.profile.specialty : ''
  return department + specialty
})

const handleLogout = async () => {
  try {
    await AuthAPI.logout()
    ElMessage.success('已安全退出')
  } catch(e) {
    // 忽略异常，强制前端退出
  } finally {
    userStore.logout()
    router.push('/login')
  }
}

const handleCommand = (command: string) => {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'security') {
    ElMessage.info('安全设置功能开发中')
  } else if (command === 'message') {
    ElMessage.info('帮助中心功能开发中')
  }
}
</script>

<template>
  <el-dropdown
    trigger="click"
    placement="bottom-end"
    @command="handleCommand"
    @visible-change="dropdownVisible = $event"
  >
    <div class="user-avatar-wrapper" :class="{ active: dropdownVisible }">
      <el-avatar :size="36" :src="avatarUrl" class="user-avatar" />
      <span class="avatar-name">{{ userName }}</span>
      <span class="dropdown-arrow" :class="{ rotated: dropdownVisible }">▼</span>
    </div>
    <template #dropdown>
      <div class="dropdown-header">
        <el-avatar :size="48" :src="avatarUrl" class="dropdown-avatar" />
        <div class="dropdown-user-info">
          <div class="dropdown-user-name">{{ userName }}</div>
          <div class="dropdown-user-grade">{{ userGrade }}</div>
        </div>
      </div>
      <el-dropdown-menu>
        <el-dropdown-item command="profile">
          <el-icon class="item-icon"><User /></el-icon>
          <span>个人信息</span>
        </el-dropdown-item>
        <el-dropdown-item command="security">
          <el-icon class="item-icon"><Lock /></el-icon>
          <span>安全设置</span>
        </el-dropdown-item>
        <el-dropdown-item command="message">
          <el-icon class="item-icon"><Bell /></el-icon>
          <span>帮助中心</span>
        </el-dropdown-item>
        <el-dropdown-item divided command="logout" class="logout-item">
          <el-icon class="item-icon"><SwitchButton /></el-icon>
          <span>退出登录</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped>
.user-avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 4px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 40px;
  cursor: pointer;
  transition: all 0.2s;
}

.user-avatar-wrapper:hover {
  background: #f8fafc;
  border-color: #0ea5e9;
}

.user-avatar-wrapper.active {
  background: #e0f2fe;
  border-color: #0ea5e9;
}

.user-avatar {
  border: 2px solid #0ea5e9;
}

.avatar-name {
  width: 65px;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-arrow {
  font-size: 12px;
  color: #64748b;
  transition: transform 0.2s;
}

.dropdown-arrow.rotated {
  transform: rotate(180deg);
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  margin: -8px -8px 8px -8px;
}

.dropdown-avatar {
  border: 2px solid #0ea5e9;
}

.dropdown-user-info {
  flex: 1;
  min-width: 0;
}

.dropdown-user-name {
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-user-grade {
  font-size: 12px;
  color: #64748b;
}

.item-icon {
  margin-right: 8px;
  font-size: 16px;
}

.logout-item {
  color: #ef4444 !important;
}

.logout-item:hover {
  background: #fef2f2 !important;
}
</style>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  User, 
  Promotion, 
  Phone, 
  Message, 
  EditPen, 
  CircleCheck, 
  Camera,
  School,
  Location,
  Briefcase
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import ProfileApi from '@/api/ProfileApi'
import type { TeacherProfile } from '@/types/TeacherProfile'

const userStore = useUserStore()
const isEditing = ref(false)
const loading = ref(false)

// 编辑用的表单数据
const editForm = reactive<TeacherProfile>({
  name: '',
  avatar: '',
  email: '',
  phone: '',
  gender: 1,
  specialty: '',
  bio: '',
  active: 1,
  department: '',
  teachingYears: 0
})

// 初始加载
onMounted(async () => {
  loading.value = true
  await Promise.all([
    userStore.fetchUserInfo(),
    userStore.fetchMyProfile()
  ])
  loading.value = false
})

// 映射状态配置
const statusConfig = computed(() => {
  const isActive = userStore.profile?.active === 1
  return {
    label: isActive ? '在职' : '他岗/离职',
    color: isActive ? '#10b981' : '#6b7280',
    bg: isActive ? '#ecfdf5' : '#f3f4f6'
  }
})

// 性别文本解析
const genderText = computed(() => {
  const g = userStore.profile?.gender
  return g === 1 ? '男' : g === 2 ? '女' : '保密'
})

// 头像上传成功后的回调
const loadDataAfterUpload = async (res: any) => {
  if (res.status === 'success') {
    const newAvatarUrl = res.data
    try {
      // --- 关键修复：必须调用保存接口将新 URL 持久化到数据库 ---
      await ProfileApi.saveProfile({
        ...userStore.profile,
        avatar: newAvatarUrl
      } as any)
      
      ElMessage.success('头像已更新并同步到数据库')
      await userStore.fetchMyProfile() // 刷新全局状态和头象
    } catch (e) {
      console.error('同步头像到档案失败', e)
      ElMessage.error('头像上传成功但保存到档案失败')
    }
  } else {
    ElMessage.error(res.message || '上传异常')
  }
}

// 上传前的校验
const beforeUpload = (file: any) => {
  const isImg = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImg) ElMessage.error('仅支持 JPG/PNG 格式')
  if (!isLt2M) ElMessage.error('文件大小不能超过 2MB')
  return isImg && isLt2M
}

// 进入编辑模式
const handleEdit = () => {
  if (userStore.profile) {
    Object.assign(editForm, {
      ...userStore.profile,
      // 确保教龄是数字
      teachingYears: Number(userStore.profile.teachingYears || 0)
    })
  } else {
    // 没档案时初始化一些基本值
    editForm.name = userStore.userName
    editForm.active = 1
  }
  isEditing.value = true
}

// 保存修改到后端
const handleSave = async () => {
  loading.value = true
  try {
    const res = await ProfileApi.saveProfile(editForm)
    if (res.status === 'success') {
      ElMessage.success('个人档案已同步更新')
      isEditing.value = false
      await userStore.fetchMyProfile() // 重新拉取
    }
  } catch (e) {
    console.error('保存失败', e)
  } finally {
    loading.value = false
  }
}

// 头像上传
const handleAvatarChange = async (uploadFile: any) => {
  const file = uploadFile.raw
  if (!file) return
  
  loading.value = true
  try {
    const res = await ProfileApi.uploadAvatar(file)
    if (res.status === 'success') {
      // 头像上传成功后立即同步到档案
      const avatarUrl = res.data
      await ProfileApi.saveProfile({
        ...userStore.profile,
        avatar: avatarUrl
      } as any)
      
      await userStore.fetchMyProfile()
      ElMessage.success('头像已更新')
    }
  } catch (e) {
    console.error('头像更新失败', e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="profile-wrapper" v-loading="loading">
    <!-- 背景渐变横幅 -->
    <div class="profile-banner">
      <div class="banner-pattern"></div>
    </div>

    <div class="profile-container">
      <!-- 左侧个人信息卡片 -->
      <aside class="profile-card">
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <el-avatar :size="140" :src="userStore.userAvatar" shape="circle" class="shadow-lg border-4 border-white" />
             <el-upload
              class="camera-overlay"
              :show-file-list="false"
              action="/api/file/upload"
              name="file"
              :headers="{ 'auth-token': userStore.token }"
              :on-success="loadDataAfterUpload"
              :auto-upload="true"
              :before-upload="beforeUpload"
            >
              <el-button circle size="small" type="primary" :icon="Camera" />
            </el-upload>
          </div>
          
          <div class="user-info">
            <h1 class="user-name">{{ userStore.userName }}</h1>
            <p class="username">@{{ userStore.accountName }}</p>
            
            <div class="status-badge" :style="{ 
              backgroundColor: statusConfig.bg, 
              color: statusConfig.color 
            }">
              <span class="status-dot" :style="{ backgroundColor: statusConfig.color }"></span>
              {{ statusConfig.label }}
            </div>
          </div>
        </div>

        <!-- 个人信息列表 -->
        <div class="info-list">
          <div class="info-item">
            <el-icon><Briefcase /></el-icon>
            <div>
              <p class="label">部门</p>
              <p class="value">{{ userStore.profile?.department || '未设置' }}</p>
            </div>
          </div>
          <div class="info-item">
            <el-icon><User /></el-icon>
            <div>
              <p class="label">教龄</p>
              <p class="value">{{ userStore.profile?.teachingYears ? userStore.profile.teachingYears + '年教龄' : '0年教龄' }}</p>
            </div>
          </div>
          <div class="info-item">
            <el-icon><Location /></el-icon>
            <div>
              <p class="label">位置</p>
              <p class="value">广东省肇庆市</p>
            </div>
          </div>
        </div>

        <!-- 导航菜单 -->
        <div class="navigation">
          <div class="nav-item active">
            <el-icon><User /></el-icon>
            <span>个人档案</span>
          </div>
          <div class="nav-item">
            <el-icon><School /></el-icon>
            <span>教学成果</span>
          </div>
          <div class="nav-item">
            <el-icon><Promotion /></el-icon>
            <span>专业发展</span>
          </div>
        </div>
      </aside>

      <!-- 主要内容区域 -->
      <main class="content-panel">
        <div class="panel-header">
          <div class="header-info">
            <h2>基本信息</h2>
            <p>管理您的个人资料和教学身份信息</p>
          </div>
          
          <el-button 
            v-if="!isEditing" 
            type="primary"
            :icon="EditPen"
            size="default"
            class="!rounded-xl shadow-md"
            @click.stop="handleEdit"
          >
            编辑资料
          </el-button>
        </div>

        <div class="form-grid" :class="{ editing: isEditing }">
          <!-- 任教专长 -->
          <div class="form-field">
            <label class="field-label">任教专长</label>
            <el-input 
              v-if="isEditing" 
              v-model="editForm.specialty" 
              placeholder="请输入任教专长"
              size="large"
            />
            <div v-else class="field-value">{{ userStore.profile?.specialty || '暂未填写' }}</div>
          </div>

          <!-- 联系电话 -->
          <div class="form-field">
            <label class="field-label">联系电话</label>
            <el-input 
              v-if="isEditing" 
              v-model="editForm.phone" 
              placeholder="请输入联系电话"
              size="large"
            />
            <div v-else class="field-value">{{ userStore.profile?.phone || '暂未填写' }}</div>
          </div>

          <!-- 电子邮箱 -->
          <div class="form-field">
            <label class="field-label">电子邮箱</label>
            <el-input 
              v-if="isEditing" 
              v-model="editForm.email" 
              placeholder="请输入电子邮箱"
              size="large"
            />
            <div v-else class="field-value">{{ userStore.profile?.email || '暂未填写' }}</div>
          </div>

          <!-- 性别选择 -->
          <div class="form-field">
            <label class="field-label">性别</label>
            <el-select 
              v-if="isEditing" 
              v-model="editForm.gender" 
              placeholder="请选择性别"
              size="large"
              style="width: 100%"
            >
              <el-option label="保密" :value="0" />
              <el-option label="男" :value="1" />
              <el-option label="女" :value="2" />
            </el-select>
            <div v-else class="field-value">{{ genderText }}</div>
          </div>

          <!-- 教师简介 -->
          <div class="form-field full-width">
            <label class="field-label">教师简介</label>
            <el-input 
              v-if="isEditing" 
              v-model="editForm.bio" 
              type="textarea" 
              :rows="6"
              placeholder="请输入教师简介"
              size="large"
              resize="none"
            />
            <div v-else class="field-value bio-text">{{ userStore.profile?.bio || '这家伙很懒，什么都没留下...' }}</div>
          </div>
        </div>

        <!-- 编辑操作按钮 -->
        <transition name="slide-fade">
          <div v-if="isEditing" class="action-buttons">
            <el-button size="default" @click.stop="isEditing = false" class="!rounded-xl">
              取消
            </el-button>
            <el-button 
              type="primary" 
              size="default"
              :icon="CircleCheck"
              :loading="loading"
              class="!rounded-xl"
              @click.stop="handleSave"
            >
              保存修改
            </el-button>
          </div>
        </transition>
      </main>
    </div>
  </div>
</template>

<style scoped>
/* 全局变量 */
:root {
  --primary-color: #4f46e5;
  --primary-light: #818cf8;
  --secondary-color: #10b981;
  --background-color: #f9fafb;
  --surface-color: #ffffff;
  --text-primary: #1f2937;
  --text-secondary: #6b7280;
  --border-color: #e5e7eb;
  --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
  --shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
  --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);
  --radius-sm: 6px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 24px;
}

.profile-wrapper {
  min-height: calc(100vh - 100px); /* 减少最小高度 */
  background-color: var(--background-color);
  padding-bottom: 20px; /* 减少底部填充 */
}

/* 横幅背景 */
.profile-banner {
  height: 90px;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
  position: relative;
  overflow: hidden;
}

.banner-pattern {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: radial-gradient(circle at 10% 20%, rgba(255,255,255,0.1) 0%, transparent 20%);
  opacity: 0.3;
}

/* 主容器 */
.profile-container {
  max-width: 1200px;
  margin: -60px auto 0;
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 24px;
  padding: 0 20px;
}

/* 左侧卡片 */
.profile-card {
  background: var(--surface-color);
  border-radius: var(--radius-xl);
  padding: 24px;
  box-shadow: var(--shadow-lg);
  height: fit-content;
}

/* 头像区域 */
.avatar-section {
  text-align: center;
  margin-bottom: 24px;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
  margin-bottom: 20px;
}

.camera-overlay {
  position: absolute;
  bottom: 0;
  right: 0;
  z-index: 10;
}

.camera-overlay :deep(.el-button) {
  width: 36px;
  height: 36px;
  border: 2px solid #fff;
}

.user-info {
  text-align: center;
}

.user-name {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px 0;
  line-height: 1.3;
}

.username {
  color: var(--text-secondary);
  font-size: 0.9rem;
  margin: 0 0 16px 0;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  border-radius: 50px;
  font-size: 0.85rem;
  font-weight: 600;
  border: 1px solid currentColor;
  opacity: 0.9;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

/* 信息列表 */
.info-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-color);
}

.info-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-item .el-icon {
  width: 40px;
  height: 40px;
  background: var(--background-color);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-color);
  flex-shrink: 0;
}

.info-item .label {
  color: var(--text-secondary);
  font-size: 0.8rem;
  font-weight: 600;
  margin: 0 0 4px 0;
}

.info-item .value {
  color: var(--text-primary);
  font-size: 1rem;
  font-weight: 600;
  margin: 0;
}

/* 导航菜单 */
.navigation {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 500;
}

.nav-item:hover {
  background-color: var(--background-color);
  color: var(--primary-color);
}

.nav-item.active {
  background-color: var(--primary-color);
  color: white;
}

.nav-item .el-icon {
  width: 20px;
  height: 20px;
}

/* 主要内容面板 */
.content-panel {
  background: var(--surface-color);
  border-radius: var(--radius-xl);
  padding: 40px;
  box-shadow: var(--shadow-lg);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
}

.header-info h2 {
  font-size: 1.75rem;
  color: var(--text-primary);
  margin: 0 0 4px 0;
  font-weight: 700;
}

.header-info p {
  color: var(--text-secondary);
  margin: 0;
  font-size: 1rem;
}

/* 表单网格 */
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 28px;
  margin-bottom: 32px;
}

.full-width {
  grid-column: span 2;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-primary);
  text-transform: uppercase;
  letter-spacing: 0.02em;
  font-size: 0.8rem;
}

.field-value {
  font-size: 1.1rem;
  color: var(--text-primary);
  padding: 16px 16px;
  background: var(--background-color);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  transition: all 0.2s ease;
}

.bio-text {
  line-height: 1.7;
  white-space: pre-wrap;
  font-size: 1rem;
}

/* 编辑状态下样式 */
.editing .field-value {
  display: none;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
}

/* 过渡动画 */
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.2s cubic-bezier(1, 0.5, 0.8, 1);
}

.slide-fade-enter-from {
  transform: translateY(20px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateY(10px);
  opacity: 0;
}

/* 响应式设计 */
@media (max-width: 992px) {
  .profile-container {
    grid-template-columns: 1fr;
    margin-top: -40px;
  }
  
  .profile-card {
    order: 2;
  }
  
  .form-grid {
    grid-template-columns: 1fr;
  }
  
  .full-width {
    grid-column: span 1;
  }
  
  .content-panel {
    order: 1;
  }
}

@media (max-width: 768px) {
  .profile-container {
    padding: 0 16px;
    gap: 16px;
  }
  
  .content-panel, .profile-card {
    padding: 24px;
  }
  
  .panel-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  .el-button {
    width: 100%;
  }
}
</style>



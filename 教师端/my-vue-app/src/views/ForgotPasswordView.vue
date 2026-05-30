<template>
  <div class="forgot-password-container">
    <div class="forgot-password-box">
      <div class="forgot-password-header">
        <h2>忘记密码</h2>
      </div>

      <el-form
        ref="forgotFormRef"
        :model="forgotFormData"
        :rules="forgotRules"
        class="forgot-form"
      >
        <!-- 用户名 -->
        <el-form-item prop="username">
          <el-input
            v-model="forgotFormData.username"
            placeholder="请输入用户名/工号"
            size="large"
            clearable
          />
        </el-form-item>

        <!-- 验证码 -->
        <el-form-item prop="captcha">
          <div class="captcha-item">
            <el-input
              v-model="forgotFormData.captcha"
              placeholder="请输入验证码"
              size="large"
              clearable
              class="captcha-input"
            />
            <el-button
              type="primary"
              :disabled="isCountingDown"
              @click="sendCaptcha"
            >
              {{ isCountingDown ? `${countDown}秒后重新发送` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <!-- 新密码 -->
        <el-form-item prop="newPassword">
          <el-input
            v-model="forgotFormData.newPassword"
            type="password"
            placeholder="请输入新密码"
            size="large"
            show-password
          />
        </el-form-item>

        <!-- 确认新密码 -->
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="forgotFormData.confirmPassword"
            type="password"
            placeholder="请确认新密码"
            size="large"
            show-password
          />
        </el-form-item>

        <!-- 提交按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            class="submit-btn"
            size="large"
            :loading="isLoading"
            @click="handleSubmit"
          >
            重置密码
          </el-button>
        </el-form-item>

        <!-- 返回登录 -->
        <div class="back-to-login">
          <router-link to="/login">返回登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { sendCaptchaApi } from '@/api/auth/sendCaptchaApi'
import { resetPasswordApi } from '@/api/auth/resetPasswordApi'

const router = useRouter()
const forgotFormRef = ref<FormInstance>()
const isLoading = ref(false)
const isCountingDown = ref(false)
const countDown = ref(60)
let timer: number | null = null

// 表单绑定的数据对象
const forgotFormData = reactive({
  username: '',
  captcha: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单校验规则
const forgotRules = reactive<FormRules>({
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  captcha: [{ required: true, message: '验证码不能为空', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '新密码不能为空', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '确认密码不能为空', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== forgotFormData.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

// 发送验证码
const sendCaptcha = async () => {
  if (!forgotFormData.username) {
    ElMessage.warning('请先输入用户名')
    return
  }

  try {
    const res = await sendCaptchaApi(forgotFormData.username)
    if (res.status === 'success') {
      ElMessage.success('验证码发送成功')
      startCountDown()
    } else {
      ElMessage.error(res.message || '验证码发送失败')
    }
  } catch (error) {
    console.error('发送验证码异常', error)
    ElMessage.error('验证码发送失败，请稍后重试')
  }
}

// 开始倒计时
const startCountDown = () => {
  isCountingDown.value = true
  countDown.value = 60
  
  if (timer) {
    clearInterval(timer)
  }
  
  timer = window.setInterval(() => {
    if (countDown.value > 1) {
      countDown.value--
    } else {
      isCountingDown.value = false
      if (timer) {
        clearInterval(timer)
        timer = null
      }
    }
  }, 1000)
}

// 处理重置密码
const handleSubmit = async () => {
  if (!forgotFormRef.value) return

  await forgotFormRef.value.validate(async (valid) => {
    if (valid) {
      isLoading.value = true
      try {
        const res = await resetPasswordApi(
          forgotFormData.username,
          forgotFormData.captcha,
          forgotFormData.newPassword
        )

        if (res.status === 'success') {
          ElMessage.success('密码重置成功，请使用新密码登录')
          router.replace({ name: 'loginView' })
        } else {
          ElMessage.error(res.message || '密码重置失败')
        }
      } catch (error) {
        console.error('重置密码异常', error)
        ElMessage.error('密码重置失败，请稍后重试')
      } finally {
        isLoading.value = false
      }
    }
  })
}
</script>

<style scoped>
.forgot-password-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
  background-image: url('@/assets/images/background.png');
  background-size: cover;
  background-repeat: no-repeat;
  background-position: center;
}

.forgot-password-box {
  width: 400px;
  background: #ffffff;
  padding: 30px 40px;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.forgot-password-header {
  text-align: center;
  margin-bottom: 30px;
}

.forgot-password-header h2 {
  margin: 0;
  color: #333;
  font-size: 24px;
}

.captcha-item {
  display: flex;
  gap: 10px;
}

.captcha-input {
  flex: 1;
}

.submit-btn {
  width: 100%;
  margin-top: 10px;
}

.back-to-login {
  text-align: center;
  margin-top: 20px;
}

.back-to-login a {
  color: #409eff;
  text-decoration: none;
}

.back-to-login a:hover {
  text-decoration: underline;
}
</style>
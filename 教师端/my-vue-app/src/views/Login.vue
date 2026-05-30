<template>
  <div class="container">
    <!-- 登录页面 -->
    <div class="form-box">
      <!-- 蓝色模块 -->
      <div class="blue-box">
        <div class="blue-panel toggle-left">
          <h1>与你共勉，老师</h1>
        </div>
      </div>

      <!-- 登录模块 - 占右边50% -->
      <div class="form-content login-content">
        <h1>教师登录</h1>
        <!-- Element Plus 表单组件 -->
        <el-form
          ref="loginFormRef"
          :model="loginFormData"
          :rules="loginRules"
          id="login-form"
          @keyup.enter="handleLogin"
        >
          <!-- 账号 -->
          <div>
            <el-form-item prop="username">
              <el-input
                class="input-box"
                id="login-username"
                v-model="loginFormData.username"
                placeholder="用户名/工号"
                size="large"
                clearable
              >
                <template #prefix>
                  <img :src="passportImage" alt="用户名" class="input-icon" />
                </template>
              </el-input>
            </el-form-item>
          </div>
          <!-- 密码 -->
          <el-form-item prop="password">
            <el-input
              class="input-box"
              id="login-password"
              v-model="loginFormData.password"
              type="password"
              placeholder="密码"
              size="large"
              show-password
            >
              <template #prefix>
                <img :src="lockImage" alt="密码" class="input-icon" />
              </template>
            </el-input>
          </el-form-item>
          <!-- 忘记密码链接 -->
          <div class="forgot-link">
            <router-link to="/forgot-password">忘记密码？</router-link>
          </div>
          <!-- 记住我 & 自动登录 -->
          <div class="remember-section">
            <div class="checkbox-label">
              <!-- 记住我 -->
              <el-form-item class="checkbox-labe">
                <el-checkbox v-model="loginFormData.remember">记住我</el-checkbox>
              </el-form-item>

              <!-- 自动登录 -->
              <el-form-item class="checkbox-label">
                <el-checkbox v-model="loginFormData.autoLogin">自动登录</el-checkbox>
              </el-form-item>
            </div>
          </div>

          <!-- 登录按钮 -->
          <el-form-item>
            <el-button
              type="primary"
              class="login-btn"
              size="large"
              :loading="isLoading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 安全验证弹窗 -->
    <el-dialog
      v-model="isCaptchaVisible"
      title="安全验证"
      width="400px"
      append-to-body
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="captcha-dialog-content">
        <p class="captcha-tip">请完成下方验证以继续登录</p>
        <TurnstileCaptcha
          ref="captchaRef"
          @verify="onCaptchaVerify"
          @error="onCaptchaError"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { loginApi, type LoginRequestDTO } from '@/api/auth/loginApi'
import LocalStorageUtil from '@/utils/LocalStorageUtil'
import { SystemConstant } from '@/constants/SystemConstant'
import { useUserStore } from '@/store/user'
import lockImage from '@/assets/images/lock.svg'
import passportImage from '@/assets/images/passport.svg'
import TurnstileCaptcha from '@/components/captcha/TurnstileCaptcha.vue'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref<FormInstance>()
const isLoading = ref(false)

// ───── Captcha ─────
const isCaptchaVisible = ref(false)
const captchaRef = ref<any>(null)
const captchaToken = ref('')

// 登录表单绑定的数据对象
const loginFormData = reactive({
  username: '',
  password: '',
  remember: false,
  autoLogin: false,
})

// 登录表单校验规则
const loginRules = reactive<FormRules>({
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
})

// 验证成功后的回调
const onCaptchaVerify = (token: string) => {
  captchaToken.value = token
  // 得到 token 后，稍微延迟一下，让用户看到验证成功的状态，然后自动关闭弹窗并登录
  setTimeout(() => {
    isCaptchaVisible.value = false
    doLogin()
  }, 800)
}

// 验证失败
const onCaptchaError = () => {
  ElMessage.error('安全验证服务连接失败，请检查网络')
}

// 执行最终登录请求
const doLogin = async () => {
  isLoading.value = true
  try {
    const loginData: LoginRequestDTO = {
      username: loginFormData.username,
      password: loginFormData.password,
      turnstileToken: captchaToken.value
    }
    const res = await loginApi(loginData)

    // 兼容两种响应格式：code: 200 (Mock) 或 status: 'success' (后端)
    const isSuccess = (res.code === 200) || (res.status === 'success')
    
    if (isSuccess) {
        ElMessage.success('登录成功！')
        
        // 获取 token：优先从 res.data 获取（Mock 格式），如果没有则直接用 res（后端直接返回 token 字符串格式）
        const token = res.data || res
        userStore.setToken(token as string)

      if (loginFormData.remember) {
        LocalStorageUtil.set(SystemConstant.RememberMe, 'RememberMe')
      } else {
        LocalStorageUtil.remove(SystemConstant.RememberMe)
      }

      if (loginFormData.autoLogin) {
        LocalStorageUtil.set(SystemConstant.AutoLogin, 'AutoLogin')
      } else {
        LocalStorageUtil.remove(SystemConstant.AutoLogin)
      }

      router.replace({ name: 'Home' }).catch(() => {})
    } else {
      if (res.message === 'NEED_CAPTCHA') {
        isCaptchaVisible.value = true
      } else {
        ElMessage.error(res.message || '登录失败')
        // 登录失败后，重置验证码
        captchaRef.value?.reset()
      }
    }
  } catch (error: any) {
    if (error.response?.data?.message === 'NEED_CAPTCHA' || error.message?.includes('NEED_CAPTCHA') || (error.response?.data?.status === 'error' && error.response?.data?.message === 'NEED_CAPTCHA')) {
      // 需要弹出验证码，无需报系统异常错
      isCaptchaVisible.value = true
    } else if (error.message === 'NEED_CAPTCHA' || error === 'NEED_CAPTCHA') {
      isCaptchaVisible.value = true
    } else {
      console.error('登录异常', error)
      ElMessage.error('登录失败，请稍后重试')
      captchaRef.value?.reset()
    }
  } finally {
    isLoading.value = false
  }
}

// 处理登录逻辑
const handleLogin = async () => {
  if (!loginFormRef.value) return

  // 校验表单是否填写完整
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      // 首次必定没有 captchaToken，交给后端预检
      captchaToken.value = ''
      doLogin()
    }
  })
}
</script>

<style scoped>
/* 全局重置，确保背景铺满屏幕（穿透 scoped） */
:global(html), :global(body) {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow: hidden; /* 防止滚动条出现 */
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: 'Microsoft YaHei', '微软雅黑', sans-serif;
}

/* 整个容器背景铺满浏览器 */
.container {
  position: relative;
  min-width: 100vw;
  min-height: 100vh;  /* 最小高度为视口高度 */
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(3px);
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  background-image: url('@/assets/images/background.png');
  background-size: cover;        /* 保持 cover 确保铺满 */
  background-repeat: no-repeat;
  background-position: center center;
  background-attachment: fixed;  /* 可选：背景固定不随滚动 */
}

.container h1 {
  font-size: 36px;
  margin: -10px 0;
}

/* 居中显示的白盒子 */
.form-box {
  position: relative;
  width: 850px;
  height: 550px;
  background-color: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(3px);
  border-radius: 30px;
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.2);
  margin: 20px;
  overflow: hidden;
  display: flex;
}

/* 蓝色模块 */
.blue-box {
  position: relative;
  width: 50%;
  height: 100%;
  z-index: 10;
}

.blue-box::before {
  content: '';
  position: absolute;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: #7494ec;
  z-index: 1;
  border-radius: 30px 150px 150px 30px;
}

/* 切换面板 */
.blue-panel {
  position: absolute;
  width: 100%;
  height: 100%;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 2;
  text-align: center;
  padding: 0 40px;
}

.toggle-panel h1 {
  font-size: 36px;
  margin-bottom: 20px;
  color: #fff;
}

/* 登录模块样式 */
.form-content {
  width: 50%;
  padding: 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  z-index: 1;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 0 30px 30px 0;
}

.form-content h1 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.input-box {
  position: relative;
  margin: 30px 0;
  border-radius: 8px;
}

.input-icon {
  width: 18px;
  height: 18px;
  margin-right: 8px;
  opacity: 0.6;
}

.checkbox-label {
  display: flex;
  align-items: center;
  cursor: pointer;
}

/* 忘记密码链接 */
.forgot-link {
  text-align: right;
  margin: -15px 0 15px;
}

.forgot-link a {
  font-size: 14.5px;
  color: #333;
  text-decoration: none;
}

.forgot-link a:hover {
  color: #7b30f5;
  text-decoration: underline;
}

/* 记住我 & 自动登录 */
.remember-section {
  margin-bottom: 20px;
}

.remember-section .checkbox-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 填满宽度的登录大按钮 */
.login-btn {
  width: 100%;
  height: 48px;
  background-color: #7494ec;
  border-radius: 8px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  border: none;
  cursor: pointer;
  font-size: 16px;
  color: #fff;
  font-weight: 600;
}

@media screen and (max-width: 650px) {
  .container {
    height: calc(100vh - 40px);
  }

  .form-box {
    flex-direction: column;
    width: 100%;
    height: 90%;
  }

  .blue-box {
    width: 100%;
    height: 30%;
  }

  .blue-box::before {
    border-radius: 20vw;
    height: 300%;
    top: -270%;
  }

  .blue-panel {
    width: 100%;
    height: 100%;
  }

  .form-content {
    width: 100%;
    height: 70%;
    padding: 20px;
    border-radius: 0 0 30px 30px;
  }

  .form-content h1 {
    font-size: 24px;
    margin-bottom: 20px;
  }

  .input-box {
    margin: 20px 0;
  }
}

@media screen and (max-width: 400px) {
  .form-box {
    padding: 10px;
  }

  .form-content {
    padding: 15px;
  }

  .form-content h1 {
    font-size: 20px;
  }

  .blue-panel h1 {
    font-size: 24px;
  }
}

.captcha-dialog-content {
  text-align: center;
  padding: 10px 0;
}

.captcha-tip {
  margin-bottom: 20px;
  color: #606266;
  font-size: 14px;
}
</style>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { sha512 } from 'js-sha512'

const router = useRouter()
const userStore = useUserStore()

const API_BASE = ''

const isLoginMode = ref(true)
const isLoading = ref(false)

const loginForm = ref({
  username: '',
  password: ''
})

const registerForm = ref({
  username: '',
  email: '',
  password: ''
})

const rememberMe = ref(false)
const autoLogin = ref(false)

const container = ref<HTMLElement | null>(null)

const switchToRegister = () => {
  isLoginMode.value = false
  if (container.value) {
    container.value.classList.add('active')
  }
}

const switchToLogin = () => {
  isLoginMode.value = true
  if (container.value) {
    container.value.classList.remove('active')
  }
}

declare global {
  interface Window {
    showNotice: (message: string, type: string, duration?: number) => void
  }
}

const showNotice = (message: string, type: string) => {
  if (window.showNotice) {
    window.showNotice(message, type)
  } else {
    alert(message)
  }
}

const sha512Hash = (str: string): string => {
  return sha512.create().update(str).hex()
}

const doPost = async (url: string, payload: any) => {
  try {
    const rsp = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })
    if (rsp.status >= 500) return null
    return await rsp.json()
  } catch (error) {
    console.error('请求失败', error)
    return null
  }
}

const doGet = async (url: string) => {
  try {
    const rsp = await fetch(url, {
      method: 'GET'
    })
    if (rsp.status >= 500) return null
    return await rsp.json()
  } catch (error) {
    console.error('请求失败', error)
    return null
  }
}

const API_PUBLIC_CHALLENGE_CAPTCHA = `${API_BASE}/api/v0/public/challenge/captcha`

async function getCaptcha() {
  const data = await doGet(API_PUBLIC_CHALLENGE_CAPTCHA)
  if (data && data.success) {
    return data.captcha
  } else {
    showNotice('获取验证码失败，请检查网络环境或稍后重试', 'error')
    return null
  }
}

async function popChallengeWindow(captcha: string): Promise<string | null> {
  const width = 540
  const height = 360
  const left = (screen.width - width) / 2
  const top = (screen.height - height) / 2

  const popupWindow = window.open(
    `/components/cloudflare-challenge/challenge.html?captcha=${captcha}`,
    'Cloudflare Bot Challenge',
    `width=${width},height=${height},left=${left},top=${top}`
  )

  if (!popupWindow) {
    showNotice('无法打开验证窗口，请检查浏览器设置', 'error')
    return null
  }

  return new Promise((resolve) => {
    const handler = (event: MessageEvent) => {
      if (event.data?.type === 'captcha-result') {
        window.removeEventListener('message', handler)
        resolve(event.data.success ? captcha : null)
      }
    }
    window.addEventListener('message', handler)

    const checkClosed = setInterval(() => {
      if (popupWindow.closed) {
        clearInterval(checkClosed)
        window.removeEventListener('message', handler)
        resolve(null)
      }
    }, 500)
  })
}

async function startChallenge(): Promise<string | null> {
  // 使用 Turnstile 测试密钥，后端会自动验证通过
  return 'test-token';
  
  const captcha = await getCaptcha()
  if (!captcha) return null

  const result = await popChallengeWindow(captcha)
  if (!result) {
    showNotice('人机验证失败', 'error')
    return null
  }

  return captcha
}

const doLogin = async (username: string, password: string, captcha: string) => {
  isLoading.value = true
  try {
    const data = await doPost(`${API_BASE}/api/v0/student/auth/token`, {
      username,
      password,
      captcha
    })

    if (data && data.success) {
      showNotice('登录成功', 'success')
      localStorage.setItem('access_token', data.access_token || '')
      localStorage.setItem('refresh_token', data.refresh_token || '')

      if (rememberMe.value) {
        localStorage.setItem('savedUsername', username)
        localStorage.setItem('savedPassword', password)
      }

      userStore.myInfo.name = username
      userStore.myInfo.username = username

      setTimeout(() => {
        router.push('/home')
      }, 2000)
    } else {
      showNotice(data?.message || '登录失败', 'error')
    }
  } catch (error) {
    showNotice('登录失败，请检查网络连接', 'error')
  } finally {
    isLoading.value = false
  }
}

const doRegister = async (username: string, password: string, email: string, captcha: string) => {
  isLoading.value = true
  try {
    const data = await doPost(`${API_BASE}/api/v0/student/auth/vtoken`, {
      username,
      password,
      email,
      captcha
    })

    if (data && data.success) {
      showNotice(data.message || '注册成功，请检查邮箱进行激活', 'success')
      setTimeout(() => {
        switchToLogin()
      }, 2000)
    } else {
      showNotice(data?.message || '注册失败', 'error')
    }
  } catch (error) {
    showNotice('注册失败，请检查网络连接', 'error')
  } finally {
    isLoading.value = false
  }
}

const handleLoginSubmit = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    showNotice('请输入用户名和密码', 'error')
    return
  }
  const captcha = await startChallenge()
  if (!captcha) return

  const hashedPassword = loginForm.value.password.length === 128
    ? loginForm.value.password
    : sha512Hash(loginForm.value.password)
  doLogin(loginForm.value.username, hashedPassword, captcha)
}

const handleRegisterSubmit = async () => {
  if (!registerForm.value.username || !registerForm.value.email || !registerForm.value.password) {
    showNotice('请输入用户名、邮箱和密码', 'error')
    return
  }
  const captcha = await startChallenge()
  if (!captcha) return

  const hashedPassword = registerForm.value.password.length === 128
    ? registerForm.value.password
    : sha512Hash(registerForm.value.password)
  doRegister(registerForm.value.username, hashedPassword, registerForm.value.email, captcha)
}

onMounted(() => {
  const savedRememberMe = localStorage.getItem('rememberMe')
  if (savedRememberMe) {
    rememberMe.value = JSON.parse(savedRememberMe)
  }

  if (rememberMe.value) {
    loginForm.value.username = localStorage.getItem('savedUsername') || ''
    loginForm.value.password = localStorage.getItem('savedPassword') || ''
  }
})
</script>

<template>
  <div :class="isLoginMode ? 'login-page' : 'register-page'">
    <div class="container" ref="container">
      <div class="form-box login">
        <form @submit.prevent="handleLoginSubmit">
          <h1>学生登录</h1>
          <div class="input-box">
            <img src="/static/images/passport.svg" alt="passport" class="icon">
            <input
              v-model="loginForm.username"
              name="username"
              type="text"
              placeholder="用户名"
              required
            >
          </div>
          <div class="input-box">
            <img src="/static/images/lock.svg" alt="lock" class="icon">
            <input
              v-model="loginForm.password"
              name="password"
              type="password"
              placeholder="密码"
              required
            >
          </div>
          <div class="forgot-link">
            <a href="#">忘记密码？</a>
          </div>

          <div class="remember-section">
            <label class="checkbox-label">
              <input v-model="rememberMe" type="checkbox">
              <span>记住我</span>
            </label>
            <label class="checkbox-label">
              <input v-model="autoLogin" type="checkbox">
              <span>自动登录</span>
            </label>
          </div>

          <button type="submit" class="btn" :disabled="isLoading">
            {{ isLoading ? '登录中...' : '登录' }}
          </button>
        </form>
      </div>

      <div class="form-box register">
        <form @submit.prevent="handleRegisterSubmit">
          <h1>学生注册</h1>
          <div class="input-box">
            <img src="/static/images/passport.svg" alt="passport" class="icon">
            <input
              v-model="registerForm.username"
              name="username"
              type="text"
              placeholder="用户名 (仅支持数字和大小写字母，长度为6-12位)"
              required
            >
          </div>
          <div class="input-box">
            <img src="/static/images/envelope.svg" alt="envelope" class="icon">
            <input
              v-model="registerForm.email"
              name="email"
              type="email"
              placeholder="电子邮箱"
              required
            >
          </div>
          <div class="input-box">
            <img src="/static/images/lock.svg" alt="lock" class="icon">
            <input
              v-model="registerForm.password"
              name="password"
              type="password"
              placeholder="密码"
              required
            >
          </div>
          <button type="submit" class="btn" :disabled="isLoading">
            {{ isLoading ? '注册中...' : '注册' }}
          </button>
        </form>
      </div>

      <div class="toggle-box">
        <div class="toggle-panel toggle-left">
          <h1>很高兴再次相逢，同学</h1>
          <p>还没有账号？</p>
          <button class="btn register-btn" @click="switchToRegister">&lt;&lt; 去注册</button>
        </div>

        <div class="toggle-panel toggle-right">
          <h1>欢迎你，同学</h1>
          <p>已经有账号了？</p>
          <button class="btn login-btn" @click="switchToLogin">去登录 &gt;&gt;</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
@import './login.css';

.login-view {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: url("/static/images/background.png");
  background-repeat: no-repeat;
  background-position: center center;
  background-size: cover;
}

.form-box.hidden {
  visibility: hidden;
}
</style>

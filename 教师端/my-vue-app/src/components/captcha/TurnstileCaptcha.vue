<template>
  <div class="turnstile-wrapper">
    <div v-if="loadError" class="turnstile-error">
      <el-icon class="error-icon"><WarningFilled /></el-icon>
      <span>{{ loadError }}</span>
      <el-button type="primary" size="small" @click="retry">重试</el-button>
    </div>
    
    <div v-else-if="!sdkReady" class="turnstile-loading">
      <el-icon class="loading-spin"><Loading /></el-icon>
      <span>正在加载安全验证…</span>
    </div>
    
    <div
      v-show="sdkReady && !loadError"
      ref="containerRef"
      class="turnstile-container"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { Loading, WarningFilled } from '@element-plus/icons-vue'
import { SystemConstant } from '@/constants/SystemConstant'

interface Props {
  siteKey?: string
  theme?: 'light' | 'dark' | 'auto'
  size?: 'normal' | 'compact'
}

const props = withDefaults(defineProps<Props>(), {
  siteKey: SystemConstant.Cloudflare.TURNSTILE_SITE_KEY,
  theme: 'auto',
  size: 'normal',
})

const emit = defineEmits<{
  (e: 'verify', token: string): void
  (e: 'error', code: string): void
  (e: 'expire'): void
}>()

const containerRef = ref<HTMLElement>()
const sdkReady = ref(false)
const loadError = ref('')
let widgetId: string | null = null
let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  loadSdk()
})

onBeforeUnmount(() => {
  cleanup()
})

watch(() => props.siteKey, () => {
  if (sdkReady.value) {
    destroyWidget()
    renderWidget()
  }
})

function loadSdk() {
  loadError.value = ''
  sdkReady.value = false
  
  if (!document.getElementById('turnstile-script') && !window.turnstile) {
    const script = document.createElement('script')
    script.id = 'turnstile-script'
    script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'
    script.async = true
    script.defer = true
    script.onerror = () => {
      loadError.value = '验证服务加载失败，请检查网络连接'
      emit('error', 'SDK_LOAD_FAILED')
    }
    document.head.appendChild(script)
  }
  waitForSdk()
}

function waitForSdk() {
  if (window.turnstile) {
    sdkReady.value = true
    renderWidget()
    return
  }

  const maxWait = 15_000
  const interval = 200
  let elapsed = 0

  pollTimer = setInterval(() => {
    elapsed += interval
    if (window.turnstile) {
      clearInterval(pollTimer!)
      pollTimer = null
      sdkReady.value = true
      renderWidget()
    } else if (elapsed >= maxWait) {
      clearInterval(pollTimer!)
      pollTimer = null
      loadError.value = '验证服务加载超时，请检查网络后重试'
      emit('error', 'SDK_LOAD_TIMEOUT')
    }
  }, interval)
}

function renderWidget() {
  if (!containerRef.value || !window.turnstile) return

  try {
    widgetId = window.turnstile.render(containerRef.value, {
      sitekey: props.siteKey,
      theme: props.theme,
      size: props.size,
      callback: (token: string) => {
        emit('verify', token)
      },
      'expired-callback': () => {
        emit('expire')
      },
      'error-callback': (code: string) => {
        loadError.value = `验证出错: ${code}`
        emit('error', code)
      },
    })
  } catch (e: any) {
    loadError.value = '验证组件渲染失败'
    emit('error', 'RENDER_ERROR')
  }
}

function destroyWidget() {
  if (widgetId != null && window.turnstile) {
    window.turnstile.remove(widgetId)
    widgetId = null
  }
}

function cleanup() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  destroyWidget()
}

function reset() {
  if (widgetId != null && window.turnstile) {
    window.turnstile.reset(widgetId)
  }
}

function retry() {
  cleanup()
  loadSdk()
}

defineExpose({ reset, retry })
</script>

<style scoped>
.turnstile-wrapper {
  display: flex;
  justify-content: center;
  margin: 16px 0;
  min-height: 65px;
}

.turnstile-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 13px;
  padding: 12px 0;
}

.loading-spin {
  animation: spin 1.2s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.turnstile-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #f56c6c;
  font-size: 13px;
  padding: 16px;
  background: #fef0f0;
  border-radius: 8px;
}

.error-icon {
  font-size: 24px;
}

.turnstile-container {
  border-radius: 8px;
  overflow: hidden;
}
</style>

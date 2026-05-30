
interface TurnstileRenderOptions {
  /** Cloudflare 控制台获取的 Site Key */
  sitekey: string
  /** 验证成功后的回调，参数为一次性 token */
  callback?: (token: string) => void
  /** 验证过期回调 */
  'expired-callback'?: () => void
  /** 验证出错回调 */
  'error-callback'?: (errorCode: string) => void
  /** 主题：跟随系统 / 浅色 / 深色 */
  theme?: 'light' | 'dark' | 'auto'
  /** 尺寸 */
  size?: 'normal' | 'compact'
  /** Widget 语言，默认自动检测 */
  language?: string
}

interface TurnstileInstance {
  /** 手动渲染 Widget，返回 widgetId */
  render: (container: string | HTMLElement, options: TurnstileRenderOptions) => string
  /** 重置指定 Widget */
  reset: (widgetId?: string) => void
  /** 获取当前 token */
  getResponse: (widgetId?: string) => string | undefined
  /** 移除指定 Widget */
  remove: (widgetId?: string) => void
}

declare global {
  interface Window {
    turnstile: TurnstileInstance
  }
}

export {}

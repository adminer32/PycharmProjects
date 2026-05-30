export class SystemConstant {
    static readonly THEME = '--current-theme';
    static readonly Locale = 'current-i18n';
    static readonly TOKEN = '__token__';
    static readonly RememberMe = 'RememberMe';
    static readonly AutoLogin = "AutoLogin";
    static readonly Event = {
        LocaleChangeEvent: 'i18n-change', // 语言切换事件
        DocumentTitleChangeEvent: 'document-title-change', // 重置页面标题事件

    };

    static readonly Cloudflare = {
        TURNSTILE_SITE_KEY: '1x00000000000000000000AA', // 核心：Turnstile 站点密钥
    };
}

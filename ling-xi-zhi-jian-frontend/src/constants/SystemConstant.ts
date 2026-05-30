export class SystemConstant {
    static readonly THEME = '--current-theme';
    static readonly Locale = 'current-i18n';
    static readonly TOKEN = 'token';
    static readonly Event = {
        LocaleChangeEvent: 'i18n-change', // 语言切换事件
        DocumentTitleChangeEvent: 'document-title-change', // 重置页面标题事件
    };
}

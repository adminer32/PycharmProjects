import en from './en.json';
import zhCN from './zh-CN.json';
import { createI18n } from 'vue-i18n';

const i18nConfig = {
    globalInjection: true,
    fallbackLocale: 'zh-CN', // 默认语言
    locale: 'zh-CN',
    legacy: false, // you must set `false`, to use Composition API
    messages: {
        en,
        'zh-CN': zhCN, // zh-CN 是zh-Hans的别名
        'zh-Hans': zhCN,
    },
};

export const i18n = createI18n(i18nConfig);
// 导出类型增强
export type I18nType = typeof i18n;

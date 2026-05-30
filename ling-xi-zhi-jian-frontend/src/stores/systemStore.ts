import { defineStore } from 'pinia';
import { SystemConstant } from '@/constants/SystemConstant';
import { ThemeEnum } from '@/types/globel.d';
import LocalStorageUtil from '@/utils/LocalStorageUtil';

export const useSystemStore = defineStore('useSystemStore', {
    state: () => ({
        /**
         * 默认跟随系统
         */
        currentTheme: LocalStorageUtil.get<ThemeEnum>(
            SystemConstant.THEME,
            ThemeEnum.LIGHT,
            (str) => Object.values(ThemeEnum).includes(str as ThemeEnum),
        ),
        currentLocale: 'zh-CN',
    }),
    actions: {
        /**
         * 切换主题
         * App.vue 中有监听器，这里只需要改变 state 中的值即可
         * @param theme
         */
        changeTheme(theme: ThemeEnum) {
            this.currentTheme = theme;
        },
    },
});

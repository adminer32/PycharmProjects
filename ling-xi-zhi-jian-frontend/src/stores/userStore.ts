import { defineStore } from 'pinia';
import { message } from 'ant-design-vue';
import { updateAvatarApi } from '@/api/user/updateAvatarApi';
import { saveUserInfoApi } from '@/api/user/saveUserInfoApi';
import { getUserInfoApi } from '@/api/user/getUserInfoApi';

export interface User {
    userId: string; //
    avatar: string;
    name: string; // 昵称
    username: string; // 用户名
    code: string; // 身份标识
    telephone: string;
    email: string;
}

export const useUserStore = defineStore('useUserStore', {
    state: () => {
        return {
            myInfo: {
                userId: '',
                avatar: '/src/assets/avatar.svg',
                name: '请登陆',
                username: '请登陆',
                telephone: '',
                code: '', // 账号
            } as User,
        };
    },

    actions: {
        getMyInfo() {
            return new Promise<void>((resolve, reject) => {
                getUserInfoApi().then((res) => {
                    if (res.status === 'success') {
                        this.myInfo = res.data;
                        resolve();
                    } else {
                        reject();
                    }
                });
            });
        },

        editMyInfo(user: User) {
            return new Promise<void>((resolve, reject) => {
                saveUserInfoApi(user).then((res) => {
                    if (res.status === 'success') {
                        // 克隆user对象
                        this.myInfo = { ...user };
                        message.success(res.message).then();
                        resolve();
                    } else {
                        message.error(res.message).then();
                        reject();
                    }
                });
            });
        },

        updateAvatar(file: File) {
            updateAvatarApi(file).then((res) => {
                if (res.status === 'success') {
                    saveUserInfoApi({
                        ...this.myInfo,
                        avatar: res.data.object,
                    }).then((res) => {
                        if (res.status === 'success') {
                            this.myInfo.avatar = URL.createObjectURL(file);
                            message.success(res.message).then();
                        } else {
                            message.error(res.message).then();
                        }
                    });
                } else {
                    message.error(res.message).then();
                }
            });
        },
    },
});

<script lang="ts" setup>
import Dropdown from '@/components/导航/Dropdown.vue';
import { useUserStore } from '@/stores/userStore';
import CenterOption from './CenterOption.vue';
import Avatar from '@/components/数据展示/Avatar.vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import PersonIcon from '@/assets/nav/Person.svg';
import LogOutIcon from '@/assets/nav/LogOut.svg';
import { computed } from 'vue';

const router = useRouter();
const userStore = useUserStore();
const { t } = useI18n();

const isLoggedIn = computed(() => userStore.myInfo.name !== '请登陆');

const handleLogout = () => {
  localStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');
  userStore.myInfo.name = '请登陆';
  userStore.myInfo.username = '请登陆';
  userStore.myInfo.avatar = '/src/assets/avatar.svg';
  router.push({ name: 'login' });
};
</script>
<template>
    <header class="header" ref="header">
        <div class="left">
            <img class="logo" src="/favicon.png" alt="" />
            <div class="site-title">{{ t('site.title') }}</div>
        </div>
        <CenterOption />
        <div class="right">
            <Dropdown :show-box-shadow="true">
                <template #trigger>
                    <div class="trigger" @click="!isLoggedIn && router.push({ name: 'login' })">
                        <Avatar :src="userStore.myInfo.avatar" />
                        <div class="my-info">
                            <div class="username">
                                {{ userStore.myInfo.name }}
                            </div>
                        </div>
                        <i class="iconfont icon-xia"></i>
                    </div>
                </template>
                <div v-if="isLoggedIn" class="dropdown-card">
                    <div class="user-info-section">
                        <Avatar :src="userStore.myInfo.avatar" class="user-avatar" />
                        <div class="user-details">
                            <div class="user-name">{{ userStore.myInfo.name }}</div>
                        </div>
                    </div>
                    <div class="divider"></div>
                    <div
                        class="item"
                        @click="router.push({ name: 'personalHomepageView' })"
                    >
                        <img class="item-icon" :src="PersonIcon" alt="个人信息" />
                        个人信息
                    </div>
                    <div class="divider"></div>
                    <div
                        class="item"
                        @click="handleLogout"
                    >
                        <img class="item-icon" :src="LogOutIcon" alt="退出登录" />
                        退出登录
                    </div>
                </div>
            </Dropdown>
        </div>
    </header>
</template>

<style lang="scss" scoped>
.header {
    height: $header-height;
    padding: 0 50px;
    display: grid;
    align-items: center;
    justify-content: center;
    grid-template-columns: 1fr 1fr 1fr;
    grid-template-rows: 100%;
    gap: 20px;
    position: sticky;
    top: 0;
    z-index: 100;
    white-space: nowrap;
    background-color: rgba(255, 255, 255);
    box-shadow: $box-shadow;

    @include useMediaQuery(
        phone,
        (
            padding: 0 10px,
        )
    );

    .left {
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: flex-start;
        font-size: 1.7rem;
        white-space: nowrap;
        font-weight: bold;

        .logo {
            height: 60%;
            margin-right: 10px;
            background-color: #fff;
            box-shadow: 0 0 1px #99999999;
            border-radius: 10px;
        }

        .site-title {
            color: #444;
        }
    }

    .right {
        justify-content: flex-end;
        height: 100%;
        display: flex;
        align-items: center;
        gap: 20px;

        .dropdown {
            display: flex;
            flex-direction: row;
            align-items: center;

            .trigger {
                display: flex;
                flex-direction: row;
                align-items: center;
                gap: 6px;
                cursor: pointer;

                .avatar {
                    $size: 40px;
                    width: $size;
                    height: $size;
                    border-radius: 50%;
                    background-color: #818181;
                }

                .my-info {
                    .username {
                        font-size: 14px;
                        font-weight: 500;
                        color: #333;
                    }
                    .login-day-count {
                        font-size: 12px;
                    }
                }

                .icon-xia {
                    font-size: 12px;
                    color: #999;
                    margin-left: 4px;
                }
            }

            .dropdown-card {
                background-color: #fff;
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
                border-radius: 8px;
                width: 240px;
                z-index: 1000;
                padding: 0;

                .user-info-section {
                    display: flex;
                    align-items: center;
                    padding: 16px;
                    gap: 12px;

                    .user-avatar {
                        $size: 48px;
                        width: $size;
                        height: $size;
                        border-radius: 50%;
                        background-color: #818181;
                    }

                    .user-details {
                        flex: 1;

                        .user-name {
                            font-size: 16px;
                            font-weight: 500;
                            color: #333;
                            margin-bottom: 4px;
                        }

                        .user-role {
                            font-size: 12px;
                            color: #999;
                        }
                    }
                }

                .divider {
                    height: 1px;
                    background-color: #f0f0f0;
                    margin: 0;
                }

                .item {
                    display: flex;
                    align-items: center;
                    padding: 12px 16px;
                    border-radius: 0;
                    gap: 12px;
                    color: #333;
                    font-size: 14px;

                    &:hover {
                        background-color: #f5f5f5;
                    }

                    .item-icon {
                        width: 16px;
                        height: 16px;
                        color: #1890ff;
                    }
                }
            }
        }
    }
}
</style>

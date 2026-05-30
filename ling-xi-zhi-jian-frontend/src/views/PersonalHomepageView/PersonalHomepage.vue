<script setup lang="ts">
import { onMounted, reactive, ref, useTemplateRef } from 'vue';
import { type User, useUserStore } from '@/stores/userStore';
import Header from '@/views/Header/Header.vue';
import UploadAvatar from '@/components/数据录入/UploadAvatar.vue';
import Button from '@/components/通用/Button/Button.vue';
import FormItem from '@/components/数据录入/FormItem.vue';
import InputText from '@/components/数据录入/input/input/InputText.vue';
import MySchedule from '@/views/PersonalHomepageView/MySchedule.vue';

const userStore = useUserStore();
onMounted(() => {
    userStore.getMyInfo().then(() => {
        myInfo.username = userStore.myInfo.username;
        myInfo.name = userStore.myInfo.name;
        myInfo.telephone = userStore.myInfo.telephone;
        myInfo.email = userStore.myInfo.email;
    });
});

const cancelModify = () => {
    myInfo.username = userStore.myInfo.username;
    myInfo.name = userStore.myInfo.name;
    myInfo.telephone = userStore.myInfo.telephone;
    myInfo.email = userStore.myInfo.email;
    canInput.value = false;
};

const modify = () => {
    userStore.editMyInfo(myInfo).then(() => {
        canInput.value = false;
    });
};

const motionCourseRef = useTemplateRef<HTMLElement>('base-motion-course');
const myInfo = reactive<User>(userStore.myInfo);

const canInput = ref(false);

const selectedOptionIndex = ref(0);
const options = [
    {
        label: '我的计划',
        pageClassName: 'page-2',
        icon: 'icon-book-open',
        description: '训练计划安排'
    },
    {
        label: '个人资料',
        pageClassName: 'page-1',
        icon: 'icon-bianji',
        description: '基本信息设置'
    },
];

const scrollTo = (index: number) => {
    selectedOptionIndex.value = index;
    const page = motionCourseRef.value?.querySelector(
        `.${options[index].pageClassName}`,
    ) as HTMLElement;
    page.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
    });
};
</script>

<template>
    <div class="personal-homepage" ref="base-motion-course">
        <Header />
        <div class="main">
            <div class="sidebar">
                <div class="sidebar-decoration">
                    <div class="decoration-circle circle-1"></div>
                    <div class="decoration-circle circle-2"></div>
                </div>
                
                <div class="sidebar-header">
                    <div class="user-avatar">
                        <UploadAvatar
                            :src="userStore.myInfo.avatar"
                            @change="(file: File) => userStore.updateAvatar(file)"
                        />
                    </div>
                    <div class="user-info">
                        <div class="user-name">{{ userStore.myInfo.name || '未设置昵称' }}</div>
                        <div class="user-code">{{ userStore.myInfo.code }}</div>
                    </div>
                </div>

                <div class="sidebar-menu">
                    <div
                        v-for="(option, index) in options"
                        :key="index"
                        class="menu-item"
                        :class="{ active: selectedOptionIndex === index }"
                        :style="{ '--delay': `${index * 0.1}s` }"
                        @click="scrollTo(index)"
                    >
                        <div class="menu-item-bg"></div>
                        <div class="menu-item-content">
                            <div class="menu-icon">
                                <span class="iconfont" :class="option.icon"></span>
                                <div class="icon-glow"></div>
                            </div>
                            <div class="menu-info">
                                <span class="menu-text">{{ option.label }}</span>
                                <span class="menu-desc">{{ option.description }}</span>
                            </div>
                            <div class="active-indicator"></div>
                        </div>
                    </div>
                </div>

                <div class="sidebar-footer">
                    <div class="footer-decoration"></div>
                    <div class="footer-text">
                        <span>翎翼毽球</span>
                    </div>
                </div>
            </div>

            <div class="right-container">
                <MySchedule class="page-2" />

                <div class="page-1">
                    <a-card title="个人资料" class="profile-card">
                        <FormItem label="用户名" direction="row">
                            <InputText
                                disabled
                                v-model="userStore.myInfo.code"
                            />
                        </FormItem>
                        <FormItem label="昵&nbsp&nbsp&nbsp称" direction="row">
                            <InputText
                                :disabled="!canInput"
                                v-model="myInfo.name"
                            />
                        </FormItem>
                        <FormItem label="手机号" direction="row">
                            <InputText
                                :disabled="!canInput"
                                v-model="myInfo.telephone"
                            />
                        </FormItem>
                        <FormItem label="邮&nbsp&nbsp&nbsp箱" direction="row">
                            <InputText
                                :disabled="!canInput"
                                v-model="myInfo.email"
                            />
                        </FormItem>
                        <div class="btn-box">
                            <Button @click="canInput = true" v-if="!canInput"
                                >编辑</Button
                            >
                            <template v-else>
                                <Button @click="modify">保存</Button>
                                <Button @click="cancelModify">取消</Button>
                            </template>
                        </div>
                    </a-card>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
.personal-homepage {
    display: flex;
    flex-direction: column;
    height: 100vh;
    overflow: hidden;
    background: linear-gradient(135deg, #f0f7ff 0%, #e6f0ff 100%);

    .main {
        flex: 1;
        display: flex;
        flex-direction: row;
        overflow: hidden;

        .sidebar {
            width: 280px;
            background: rgba(255, 255, 255, 0.85);
            backdrop-filter: blur(20px);
            border-right: 1px solid rgba(24, 144, 255, 0.1);
            display: flex;
            flex-direction: column;
            position: relative;
            overflow: hidden;
            box-shadow: 4px 0 24px rgba(24, 144, 255, 0.08);

            .sidebar-decoration {
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                height: 200px;
                pointer-events: none;
                overflow: hidden;

                .decoration-circle {
                    position: absolute;
                    border-radius: 50%;
                    background: linear-gradient(135deg, rgba(24, 144, 255, 0.15) 0%, rgba(105, 192, 255, 0.08) 100%);
                    
                    &.circle-1 {
                        width: 180px;
                        height: 180px;
                        top: -60px;
                        right: -60px;
                    }
                    
                    &.circle-2 {
                        width: 120px;
                        height: 120px;
                        top: 80px;
                        left: -40px;
                    }
                }
            }

            .sidebar-header {
                display: flex;
                flex-direction: column;
                align-items: center;
                padding: 32px 24px;
                position: relative;
                z-index: 1;

                .user-avatar {
                    width: 80px;
                    height: 80px;
                    border-radius: 50%;
                    overflow: hidden;
                    border: 3px solid #fff;
                    box-shadow: 0 4px 20px rgba(24, 144, 255, 0.25);
                    margin-bottom: 16px;

                    :deep(.upload-avatar) {
                        width: 100%;
                        height: 100%;
                        
                        img, video {
                            width: 100%;
                            height: 100%;
                            object-fit: cover;
                        }
                    }
                }

                .user-info {
                    text-align: center;

                    .user-name {
                        font-size: 17px;
                        font-weight: 600;
                        color: #333;
                        margin-bottom: 4px;
                    }

                    .user-code {
                        font-size: 12px;
                        color: #999;
                    }
                }
            }

            .sidebar-menu {
                flex: 1;
                padding: 16px 16px;
                display: flex;
                flex-direction: column;
                gap: 8px;
                position: relative;
                z-index: 1;

                .menu-item {
                    position: relative;
                    border-radius: 14px;
                    overflow: hidden;
                    cursor: pointer;
                    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                    animation: slideIn 0.4s ease backwards;
                    animation-delay: var(--delay);

                    @keyframes slideIn {
                        from {
                            opacity: 0;
                            transform: translateX(-20px);
                        }
                        to {
                            opacity: 1;
                            transform: translateX(0);
                        }
                    }

                    .menu-item-bg {
                        position: absolute;
                        inset: 0;
                        background: linear-gradient(135deg, rgba(24, 144, 255, 0.08) 0%, rgba(105, 192, 255, 0.04) 100%);
                        opacity: 0;
                        transition: opacity 0.3s ease;
                    }

                    &:hover {
                        transform: translateX(4px);
                        
                        .menu-item-bg {
                            opacity: 1;
                        }

                        .menu-icon .icon-glow {
                            opacity: 0.5;
                        }
                    }

                    &.active {
                        .menu-item-bg {
                            opacity: 1;
                            background: linear-gradient(135deg, rgba(24, 144, 255, 0.15) 0%, rgba(105, 192, 255, 0.08) 100%);
                        }

                        .menu-icon {
                            .iconfont {
                                color: #fff;
                            }
                            
                            .icon-glow {
                                opacity: 1;
                                background: rgba(255, 255, 255, 0.3);
                            }
                        }

                        .menu-info {
                            .menu-text {
                                color: #1890ff;
                                font-weight: 600;
                            }

                            .menu-desc {
                                color: rgba(24, 144, 255, 0.7);
                            }
                        }

                        .active-indicator {
                            width: 4px;
                            height: 70%;
                            background: linear-gradient(180deg, #1890ff, #69c0ff);
                            border-radius: 0 4px 4px 0;
                        }
                    }

                    .menu-item-content {
                        display: flex;
                        align-items: center;
                        gap: 14px;
                        padding: 16px 18px;
                        position: relative;

                        .menu-icon {
                            width: 44px;
                            height: 44px;
                            border-radius: 12px;
                            background: linear-gradient(135deg, rgba(24, 144, 255, 0.12) 0%, rgba(105, 192, 255, 0.08) 100%);
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            position: relative;
                            transition: all 0.3s ease;

                            .iconfont {
                                font-size: 20px;
                                color: #666;
                                transition: all 0.3s ease;
                                position: relative;
                                z-index: 1;
                            }

                            .icon-glow {
                                position: absolute;
                                inset: -2px;
                                border-radius: 14px;
                                background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                                opacity: 0;
                                filter: blur(8px);
                                transition: opacity 0.3s ease;
                            }
                        }

                        .menu-info {
                            display: flex;
                            flex-direction: column;
                            gap: 3px;

                            .menu-text {
                                font-size: 15px;
                                font-weight: 500;
                                color: #333;
                                transition: all 0.3s ease;
                            }

                            .menu-desc {
                                font-size: 11px;
                                color: #999;
                                transition: all 0.3s ease;
                            }
                        }

                        .active-indicator {
                            position: absolute;
                            left: 0;
                            top: 50%;
                            transform: translateY(-50%);
                            width: 0;
                            height: 0;
                            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                        }
                    }
                }
            }

            .sidebar-footer {
                padding: 20px 24px;
                position: relative;
                z-index: 1;

                .footer-decoration {
                    height: 1px;
                    background: linear-gradient(90deg, transparent, rgba(24, 144, 255, 0.2), transparent);
                    margin-bottom: 16px;
                }

                .footer-text {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 8px;

                    span {
                        font-size: 12px;
                        color: #999;
                        letter-spacing: 1px;
                    }
                }
            }
        }

        .right-container {
            flex: 1;
            overflow: hidden;
            padding: 16px;

            .page-1 {
                .profile-card {
                    max-width: 400px;
                    border-radius: 16px;
                    box-shadow: 0 4px 20px rgba(24, 144, 255, 0.1);
                    border: 1px solid rgba(24, 144, 255, 0.1);

                    :deep(.ant-card-head) {
                        background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                        border-radius: 16px 16px 0 0;
                        
                        .ant-card-head-title {
                            color: #fff;
                            font-weight: 600;
                        }
                    }

                    :deep(.ant-card-body) {
                        padding: 24px;
                    }

                    .form-item {
                        margin-bottom: 20px;
                    }

                    .btn-box {
                        display: flex;
                        justify-content: flex-end;
                        gap: 12px;
                        margin-top: 24px;
                    }
                }
            }

            .page-2 {
                border-radius: 16px;
                background: rgba(255, 255, 255, 0.9);
                backdrop-filter: blur(10px);
                box-shadow: 0 4px 20px rgba(24, 144, 255, 0.08);
                border: 1px solid rgba(24, 144, 255, 0.1);
                height: calc(100% - 16px);
            }
        }
    }
}
</style>
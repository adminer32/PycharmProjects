<script setup lang="ts">
import { RouterView, useRouter } from 'vue-router';
import { ThemeEnum } from '@/types/globel.d';
import { watch, watchEffect, ref, onMounted } from 'vue';
import { SystemConstant } from '@/constants/SystemConstant';
import { useSystemStore } from '@/stores/systemStore';
import { eventBus } from '@/utils/eventBus';
import { useI18n } from 'vue-i18n';
import { useAiChatStore } from '@/stores/aiChatStore';
import { useUserStore } from '@/stores/userStore';
import { useTeacherNotificationStore } from '@/stores/teacherNotificationStore';
import { nextTick } from 'vue';
import { marked } from 'marked';
import Avatar from '@/components/数据展示/Avatar.vue';
import AiAvatar from '@/components/数据展示/AiAvatar/AiAvatar.vue';

const router = useRouter();
const aiChatStore = useAiChatStore();
const userStore = useUserStore();
const teacherNotificationStore = useTeacherNotificationStore();

const showChatDialog = ref(false);
const chatInput = ref('');
const messageContainerRef = ref<HTMLElement | null>(null);

onMounted(() => {
    teacherNotificationStore.initWebSocket();
});

const toggleChat = () => {
    showChatDialog.value = !showChatDialog.value;
    if (showChatDialog.value) {
        if (aiChatStore.messageList.length === 0) {
            aiChatStore.clearMessageList();
        }
        aiChatStore.wsInit(() => nextTick(scrollToBottom));
    } else {
        aiChatStore.closeWs();
    }
};

const scrollToBottom = () => {
    nextTick(() => {
        if (messageContainerRef.value) {
            messageContainerRef.value.scrollTo({
                top: messageContainerRef.value.scrollHeight,
                behavior: 'smooth',
            });
        }
    });
};

const sendMessage = () => {
    if (!chatInput.value.trim()) return;
    aiChatStore.sendMessage(chatInput.value);
    chatInput.value = '';
    scrollToBottom();
};

const handleKeydown = (e: KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
};

window.addEventListener('unload', () => {
    localStorage.setItem('leaveTime', new Date().getTime().toString());
});

const { t, locale } = useI18n();
eventBus.on(SystemConstant.Event.DocumentTitleChangeEvent, (title: string) => {
    document.title = t(title);
});

watch(
    () => locale.value,
    () => {
        if (router.currentRoute.value.name === 'login') {
            document.title = t('site.title');
        } else {
            document.title = t(router.currentRoute.value.meta.title as string);
        }
    },
);

const match = matchMedia(`(prefers-color-scheme: ${ThemeEnum.DARK})`);
const systemStore = useSystemStore();

const followOS = () => {
    if (match.matches) {
        document.documentElement.dataset.theme = ThemeEnum.DARK;
    } else {
        document.documentElement.dataset.theme = ThemeEnum.LIGHT;
    }
};

watchEffect(() => {
    localStorage.setItem(SystemConstant.THEME, systemStore.currentTheme);
    if (systemStore.currentTheme === ThemeEnum.AUTO) {
        followOS();
        match.addEventListener('change', followOS);
    } else {
        match.removeEventListener('change', followOS);
        document.documentElement.dataset.theme = systemStore.currentTheme;
    }
});
</script>

<template>
    <RouterView :key="$route.fullPath" />
    
    <!-- AI 答疑悬浮按钮 -->
    <div class="ai-chat-float" v-if="router.currentRoute.value.name === 'aiLearnFriendView'">
        <div class="chat-dialog" v-if="showChatDialog">
            <div class="dialog-header">
                <span class="title">AI 答疑助手</span>
                <button class="close-btn" @click="showChatDialog = false">×</button>
            </div>
            <div class="message-container" ref="messageContainerRef">
                <div
                    class="message-box"
                    v-for="(message, index) in aiChatStore.messageList"
                    :key="index"
                    :class="message.sender"
                >
                    <div class="text-content" v-html="marked.parse(message.text)" :class="message.sender"></div>
                </div>
            </div>
            <div class="dialog-input">
                <input
                    v-model="chatInput"
                    placeholder="输入问题..."
                    @keydown="handleKeydown"
                />
                <button @click="sendMessage">发送</button>
            </div>
        </div>
        <button class="float-btn" @click="toggleChat">
            <span class="iconfont icon-Ai"></span>
        </button>
    </div>
</template>

<style scoped lang="scss">
.ai-chat-float {
    position: fixed;
    bottom: 24px;
    right: 24px;
    z-index: 9999;

    .float-btn {
        width: 56px;
        height: 56px;
        border-radius: 50%;
        background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
        border: none;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4px 20px rgba(24, 144, 255, 0.4);
        transition: all 0.3s ease;

        &:hover {
            transform: scale(1.1);
            box-shadow: 0 6px 25px rgba(24, 144, 255, 0.5);
        }

        .iconfont {
            font-size: 28px;
            color: #fff;
        }
    }

    .chat-dialog {
        position: absolute;
        bottom: 70px;
        right: 0;
        width: 380px;
        height: 520px;
        background: #fff;
        border-radius: 16px;
        box-shadow: 0 8px 40px rgba(0, 0, 0, 0.15);
        display: flex;
        flex-direction: column;
        overflow: hidden;

        .dialog-header {
            padding: 16px 20px;
            background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
            color: #fff;
            display: flex;
            justify-content: space-between;
            align-items: center;

            .title {
                font-size: 16px;
                font-weight: 600;
            }

            .close-btn {
                background: none;
                border: none;
                color: #fff;
                font-size: 24px;
                cursor: pointer;
                padding: 0;
                line-height: 1;
            }
        }

        .message-container {
            flex: 1;
            overflow-y: auto;
            padding: 16px;
            background: #f5f7fa;

            &::-webkit-scrollbar {
                width: 4px;
            }

            &::-webkit-scrollbar-thumb {
                background: rgba(24, 144, 255, 0.3);
                border-radius: 2px;
            }

            .message-box {
                display: flex;
                align-items: flex-start;
                gap: 10px;
                margin-bottom: 16px;

                &.ai {
                    justify-content: flex-start;
                }

                &.user {
                    justify-content: flex-end;

                    .text-content {
                        background: #1890ff;
                        color: #fff;
                        border-radius: 16px 4px 16px 16px;
                    }
                }

                .text-content {
                    padding: 10px 14px;
                    background: #fff;
                    border-radius: 4px 16px 16px 16px;
                    max-width: 70%;
                    word-break: break-word;
                    line-height: 1.5;

                    &.ai {
                        font-size: 14px;
                    }

                    &.user {
                        font-size: 14px;
                    }
                }

                :deep(p) {
                    margin: 0;
                }
            }
        }

        .dialog-input {
            padding: 12px 16px;
            background: #fff;
            border-top: 1px solid #eee;
            display: flex;
            gap: 10px;

            input {
                flex: 1;
                padding: 10px 14px;
                border: 1px solid #ddd;
                border-radius: 20px;
                outline: none;
                font-size: 14px;

                &:focus {
                    border-color: #1890ff;
                }
            }

            button {
                padding: 10px 20px;
                background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                border: none;
                border-radius: 20px;
                color: #fff;
                font-size: 14px;
                cursor: pointer;

                &:hover {
                    opacity: 0.9;
                }
            }
        }
    }
}
</style>

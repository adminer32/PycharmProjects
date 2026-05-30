<script setup lang="ts">
import { ref, computed } from 'vue';
import { coachChatApi } from '@/api/ai/coachApi';

const props = defineProps<{
    actionContext?: {
        action_type?: string;
        hip_score?: number;
        knee_score?: number;
        ankle_score?: number;
        foot_height_score?: number;
        overall_score?: number;
        feedback?: string[];
    };
}>();

const isExpanded = ref(false);
const inputText = ref('');
const messages = ref<Array<{ role: string; content: string; time: Date }>>([]);
const isLoading = ref(false);

const hasResult = computed(() => !!props.actionContext?.action_type);

const toggleExpand = () => {
    isExpanded.value = !isExpanded.value;
};

const sendMessage = async () => {
    if (!inputText.value.trim() || isLoading.value || !hasResult.value) return;

    const userMessage = inputText.value.trim();
    messages.value.push({
        role: 'user',
        content: userMessage,
        time: new Date()
    });
    inputText.value = '';
    isLoading.value = true;

    try {
        const response = await coachChatApi({
            message: userMessage,
            action_context: props.actionContext
        });

        if (response.success) {
            messages.value.push({
                role: 'ai',
                content: response.message,
                time: new Date()
            });
        } else {
            messages.value.push({
                role: 'ai',
                content: '抱歉，AI教练暂时无法回答，请稍后再试。',
                time: new Date()
            });
        }
    } catch (error) {
        messages.value.push({
            role: 'ai',
            content: '网络连接失败，请检查网络后重试。',
            time: new Date()
        });
    } finally {
        isLoading.value = false;
    }
};

const formatTime = (date: Date) => {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
};
</script>

<template>
    <div :class="['floating-coach', { expanded: isExpanded }]">
        <!-- 悬浮球 -->
        <div v-if="!isExpanded" class="float-ball" @click="toggleExpand">
            <div class="ball-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
            </div>
            <div class="ball-pulse"></div>
        </div>

        <!-- 展开的聊天窗口 -->
        <div v-else class="coach-panel">
            <div class="panel-header">
                <div class="header-info">
                    <div class="coach-avatar">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="8" r="4"/>
                            <path d="M4 20c0-4 4-6 8-6s8 2 8 6"/>
                        </svg>
                    </div>
                    <div class="coach-text">
                        <span class="coach-name">翎析 AI教练</span>
                        <span class="coach-desc">毽球动作指导</span>
                    </div>
                </div>
                <button class="close-btn" @click="toggleExpand">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="18" y1="6" x2="6" y2="18"/>
                        <line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                </button>
            </div>

            <div class="messages-area">
                <div v-if="!hasResult" class="no-result">
                    <span class="no-result-icon">🏃</span>
                    <p>完成动作分析即可对话</p>
                </div>
                <template v-else>
                    <div v-if="messages.length === 0" class="welcome">
                        <p>👋 你好，我是翎析</p>
                        <p>可以问我关于动作要领、训练建议等问题</p>
                    </div>
                    <div v-for="(msg, index) in messages" :key="index" :class="['msg', msg.role]">
                        <div class="msg-bubble">{{ msg.content }}</div>
                        <div class="msg-time">{{ formatTime(msg.time) }}</div>
                    </div>
                </template>
            </div>

            <form class="input-area" @submit.prevent="sendMessage">
                <input
                    v-model="inputText"
                    type="text"
                    placeholder="问教练..."
                    :disabled="isLoading || !hasResult"
                />
                <button type="submit" :disabled="!inputText.trim() || isLoading || !hasResult">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="22" y1="2" x2="11" y2="13"/>
                        <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                    </svg>
                </button>
            </form>
        </div>
    </div>
</template>

<style scoped lang="scss">
.floating-coach {
    position: fixed;
    z-index: 9999;

    &.expanded {
        right: 20px;
        bottom: 20px;
    }

    &:not(.expanded) {
        right: 24px;
        bottom: 24px;
    }
}

.float-ball {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: #1890ff;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    box-shadow: 0 4px 20px rgba(24, 144, 255, 0.4);
    transition: transform 0.3s ease;

    &:hover {
        transform: scale(1.1);
    }

    .ball-icon {
        width: 28px;
        height: 28px;
        color: white;
        z-index: 1;

        svg {
            width: 100%;
            height: 100%;
        }
    }

    .ball-pulse {
        position: absolute;
        width: 100%;
        height: 100%;
        border-radius: 50%;
        background: #1890ff;
        animation: pulse-ring 2s infinite;
    }
}

.coach-panel {
    width: 380px;
    height: 520px;
    background: white;
    border-radius: 16px;
    box-shadow: 0 10px 50px rgba(0, 0, 0, 0.15);
    display: flex;
    flex-direction: column;
    overflow: hidden;
    animation: slide-up 0.3s ease;
}

.panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px;
    background: #1890ff;

    .header-info {
        display: flex;
        align-items: center;
        gap: 12px;
    }

    .coach-avatar {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.2);
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;

        svg {
            width: 24px;
            height: 24px;
        }
    }

    .coach-text {
        display: flex;
        flex-direction: column;

        .coach-name {
            color: white;
            font-weight: 600;
            font-size: 12px;
        }

        .coach-desc {
            color: rgba(255, 255, 255, 0.7);
            font-size: 10px;
        }
    }

    .close-btn {
        width: 32px;
        height: 32px;
        border: none;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.2);
        color: white;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: background 0.2s;

        &:hover {
            background: rgba(255, 255, 255, 0.3);
        }

        svg {
            width: 18px;
            height: 18px;
        }
    }
}

.messages-area {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    background: #f3f5fb;

    &::-webkit-scrollbar {
        width: 4px;
    }

    &::-webkit-scrollbar-thumb {
        background: #ddd;
        border-radius: 2px;
    }
}

.no-result {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: #999;

    .no-result-icon {
        font-size: 40px;
        margin-bottom: 12px;
    }

    p {
        font-size: 14px;
    }
}

.welcome {
    background: white;
    border-radius: 10px;
    padding: 12px;
    color: #666;
    font-size: 12px;
    line-height: 1.5;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

    p:first-child {
        font-size: 13px;
        color: #333;
        margin-bottom: 6px;
    }
}

.msg {
    display: flex;
    flex-direction: column;
    margin-bottom: 10px;

    &.user {
        align-items: flex-end;

        .msg-bubble {
            background: #1890ff;
            color: white;
            border-radius: 12px 12px 4px 12px;
        }
    }

    &.ai {
        align-items: flex-start;

        .msg-bubble {
            background: white;
            color: #333;
            border-radius: 12px 12px 12px 4px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
        }
    }

    .msg-bubble {
        padding: 10px 14px;
        font-size: 13px;
        line-height: 1.5;
        max-width: 90%;
    }

    .msg-time {
        font-size: 9px;
        color: #bbb;
        margin-top: 3px;
        padding: 0 4px;
    }
}

.input-area {
    display: flex;
    gap: 8px;
    padding: 12px 16px;
    background: white;
    border-top: 1px solid #eee;

    input {
        flex: 1;
        padding: 10px 14px;
        border: 1px solid #eee;
        border-radius: 20px;
        font-size: 13px;
        outline: none;
        transition: border-color 0.2s;

        &:focus {
            border-color: #1890ff;
        }

        &:disabled {
            background: #f5f5f5;
            cursor: not-allowed;
        }
    }

    button {
        width: 40px;
        height: 40px;
        border: none;
        border-radius: 50%;
        background: #1890ff;
        color: white;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: transform 0.2s, opacity 0.2s;

        svg {
            width: 18px;
            height: 18px;
        }

        &:hover:not(:disabled) {
            transform: scale(1.05);
        }

        &:disabled {
            opacity: 0.5;
            cursor: not-allowed;
            transform: none;
        }
    }
}

@keyframes pulse-ring {
    0% {
        transform: scale(1);
        opacity: 0.8;
    }
    100% {
        transform: scale(1.5);
        opacity: 0;
    }
}

@keyframes slide-up {
    from {
        opacity: 0;
        transform: translateY(20px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}
</style>

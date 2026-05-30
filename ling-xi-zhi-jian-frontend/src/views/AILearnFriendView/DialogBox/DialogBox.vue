<script setup lang="ts">
import ChatController from '@/views/AILearnFriendView/DialogBox/ChatController.vue';
import Avatar from '@/components/数据展示/Avatar.vue';
import AiAvatar from '@/components/数据展示/AiAvatar/AiAvatar.vue';
import { nextTick, onMounted, onUnmounted, useTemplateRef } from 'vue';
import { useAiChatStore } from '@/stores/aiChatStore';
import { useUserStore } from '@/stores/userStore';
import { marked } from 'marked';

const { note } = defineProps<{
    note: {
        id: number;
        taskId: string;
        createTime: string;
        videoUrl: string;
        title: string;
        autoChapters: string; // 笔记   // 没分析出来时返回的是字符串分析出来返回的是JSON
        summarization: string; // json
        meetingAssistance: string; // json
        personalNote: string;
        taskStatus: '进行中' | '已完成' | '失败';
    };
}>();

const aiChatStore = useAiChatStore();
const userStore = useUserStore();
onMounted(() => {
    aiChatStore.wsInit(() => nextTick(scrollToBottom));
});

const scrollToBottom = () => {
    const messageContainer = dialogBoxRef.value?.querySelector(
        '.message-container',
    ) as HTMLElement;
    messageContainer.scrollTo({
        top: messageContainer.scrollHeight,
        behavior: 'smooth',
    });
};
onUnmounted(() => {
    aiChatStore.closeWs();
});

const dialogBoxRef = useTemplateRef<HTMLElement>('dialog-box');
</script>

<template>
    <div class="dialog-box" ref="dialog-box">
        <div class="message-container">
            <!-- 一个 message-box 代表一条消息（包括头像） -->
            <div
                class="message-box"
                v-for="(message, index) of aiChatStore.messageList"
                :key="index"
                :class="message.sender"
            >
                <AiAvatar v-if="message.sender == 'ai'" />
                <div
                    class="text-content"
                    v-html="marked.parse(message.text)"
                    :class="message.sender"
                />
            </div>
        </div>
        <ChatController
            :note="note"
            @clickSendBtn="() => nextTick(scrollToBottom)"
        />
    </div>
</template>

<style scoped lang="scss">
.dialog-box {
    .message-container {
        min-height: 110px;
        max-height: 500px;
        overflow-y: auto;

        &::-webkit-scrollbar {
            width: 5px;
        }

        &::-webkit-scrollbar-thumb {
            background-color: #99bef6;
        }

        .message-box {
            display: flex;
            flex-direction: row;
            align-items: flex-start;
            gap: 10px;
            padding: 0 10px;
            margin-bottom: 20px;
            filter: drop-shadow(0 0 10px rgba(0, 0, 0, 0.1));

            &.ai {
                justify-content: flex-start;
            }

            &.user {
                justify-content: flex-end;
            }

            .avatar,
            .ai-avatar {
                width: 40px;
                height: 40px;
            }

            .text-content {
                padding: 0 16px;
                background-color: #fff;
                margin-top: 15px;
                max-width: 70%;

                &.ai {
                    border-radius: 0 $border-radius $border-radius
                        $border-radius;
                }

                &.user {
                    background-color: #ccebff;
                    border-radius: $border-radius 0 $border-radius
                        $border-radius;
                    word-break: break-word;
                }
            }
        }
    }
}
</style>

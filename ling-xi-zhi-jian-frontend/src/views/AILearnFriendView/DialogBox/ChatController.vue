<script setup lang="ts">
import { ref } from 'vue';
import { useAiChatStore } from '@/stores/aiChatStore';

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
const emits = defineEmits<{
    (e: 'clickSendBtn'): void;
}>();
const onClickSendBtn = () => {
    if (!text.value) {
        return;
    }
    aiChatStore.sendMessage(text.value, note);
    text.value = '';
    emits('clickSendBtn');
};

const text = ref('');
const onEnter = (e: KeyboardEvent) => {
    if (e.shiftKey && e.key === 'Enter') return;
    if (e.key === 'Enter') {
        e.preventDefault();
        onClickSendBtn();
    }
};
</script>

<template>
    <form
        class="chat-controller"
        ref="chat-controller"
        @submit.prevent="onClickSendBtn"
    >
        <textarea
            class="textarea"
            v-model="text"
            @keydown="onEnter"
            placeholder="这里空空的..."
        />
        <div
            class="send-btn"
            @click="onClickSendBtn"
            :class="{ disabled: !text }"
        >
            发送
            <!--            <img src="../../assets/motion_analysis/发送图标.png" alt="">-->
            <span class="iconfont icon-SEND"></span>
        </div>
    </form>
</template>

<style scoped lang="scss">
$gap: 20px;
$border-radius: 8px;
.chat-controller {
    height: 80px;
    display: flex;
    flex-direction: row;
    gap: $gap;
    flex: 1;
    background-color: #fff;
    border-radius: $border-radius;
    overflow: hidden;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);

    .textarea {
        flex: 1;
        border: none;
        padding: 10px;
        resize: none;
        outline: none;
        font-size: 16px;
    }

    .send-btn {
        align-self: flex-start;
        background-color: #ccebff;
        margin: 5px;
        border-radius: 5px;
        display: flex;
        flex-direction: row;
        align-items: center;
        padding: 5px 12px;
        color: #000000;
        font-size: 13px;
        cursor: pointer;
        overflow: hidden;
        box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);

        &:hover {
            background-color: #b4e1ff;
        }

        &.disabled {
            color: #aaaaaa;
            background-color: #f3f3f3;
            cursor: not-allowed;
        }

        .iconfont {
            margin: 3px 0 0 3px;
        }
    }
}
</style>

<script setup lang="ts">
import { computed } from 'vue';
import TimePicker from '@/components/数据录入/TimePicker.vue';

const { time, content } = defineProps<{
    time: Date;
    content: string;
}>();

const emit = defineEmits<{
    (e: 'update:time', value: Date): void;
    (e: 'update:content', value: string): void;
}>();

const computedTime = computed({
    get() {
        return time;
    },
    set(value) {
        emit('update:time', value);
    },
});

const computedContent = computed({
    get() {
        return content;
    },
    set(value) {
        emit('update:content', value);
    },
});
</script>

<template>
    <div class="edit-box">
        <TimePicker v-model:time="computedTime" />
        <textarea rows="2" placeholder="内容" v-model="computedContent" />
    </div>
</template>

<style scoped lang="scss">
.edit-box {
    padding: 5px;

    textarea {
        display: block;
        width: 100%;
        margin-top: 10px;
        border-radius: 5px;
        outline: none;
        resize: none;
        border: 1px solid #99999955;
        font-size: 15px;
        padding: 10px;
    }
}
</style>

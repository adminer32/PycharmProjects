<script setup lang="ts">
/**
 * 上传头像
 */
import Avatar from '@/components/数据展示/Avatar.vue';
import { useTemplateRef } from 'vue';

defineProps<{
    src: string;
}>();

const emits = defineEmits<{
    (e: 'change', file: File): void;
}>();
const change = (e: Event) => {
    const target = e.target as HTMLInputElement;
    if (target.files) {
        emits('change', target.files[0]);
    }
};
const uploadAvatarRef = useTemplateRef<HTMLInputElement>('upload-avatar');

const openFileDialog = () => {
    const fileInput = uploadAvatarRef.value!.querySelector(
        'input[type=file]',
    ) as HTMLInputElement;
    fileInput.click();
};
</script>

<template>
    <div class="upload-avatar" ref="upload-avatar" @click="openFileDialog">
        <input type="file" @change="change" />
        <Avatar :src="src" />
    </div>
</template>

<style scoped lang="scss">
.upload-avatar {
    width: 100px;
    height: 100px;
    cursor: pointer;
    border-radius: 50%;
    overflow: hidden;

    input {
        display: none;
    }

    .avatar {
        width: 100%;
        height: 100%;
    }
}
</style>

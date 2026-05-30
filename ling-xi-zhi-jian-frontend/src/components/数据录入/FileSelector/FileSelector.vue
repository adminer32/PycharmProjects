<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, useTemplateRef } from 'vue';

const fileSelectorRef = useTemplateRef<HTMLDivElement>('file-selector');
const {
    accept,
    drag = false,
    multiple = false,
    directory = false,
} = defineProps<{
    accept?: string; // 返回 true 表示接受
    directory?: boolean; // 是否接受文件夹，如果接受文件夹，那只能选一个文件夹
    multiple?: boolean; // multiple 只针对 文件（非文件夹）
    drag?: boolean;
}>();

const emits = defineEmits<{
    fileChange: [fileList: File[]];
    error: [errorMsg: string];
}>();

const selectItemNameList = ref<string[]>([]); // 文件名

const fileTypeMatch = (file: File, accept: string): boolean => {
    const acceptList = accept.split(',');
    for (const acceptItem of acceptList) {
        const acceptItemSplit = acceptItem.split('/');
        if (acceptItemSplit.length == 1) {
            if (acceptItemSplit[0] == '*') return true;
            if (acceptItemSplit[0] == file.type) return true;
        } else {
            if (acceptItemSplit[0] == '*') return true;
            if (acceptItemSplit[0] == file.type.split('/')[0]) return true;
        }
    }
    return false;
};

onMounted(() => {
    if (drag) {
        fileSelectorRef.value!.ondragover = (e: DragEvent) =>
            e.preventDefault();
        fileSelectorRef.value!.ondrop = (e: DragEvent) => {
            e.preventDefault();
            selectItemNameList.value = [];
            const fileList = e.dataTransfer?.files;
            if (fileList?.length == 0) return;
            const file = fileList?.[0] ?? null;
            if (!file) return;
            if (accept && !fileTypeMatch(file, accept)) {
                emits('error', '文件类型不匹配');
                return;
            }
            emits('fileChange', [file]);
            selectItemNameList.value.push(file.name);
        };
    }
});

const openFileSelectWindow = () => {
    const input = document.createElement('input') as HTMLInputElement;
    input.type = 'file';
    if (accept) input.accept = accept;
    if (directory) {
        input.webkitdirectory = true; // 使用 webkit 内核的浏览器
        // input.directory = true; // opera浏览器
        // input.mozdirectory = true;  // 火狐浏览器
    } else {
        // 如果不是文件夹才多选的概念
        input.multiple = multiple;
    }
    input.onchange = (e) => {
        const files = (e.target as HTMLInputElement)?.files;
        let tempFileList: File[] = [];
        if (files) {
            for (const file of files) {
                tempFileList.push(file);
            }
        }
        emits('fileChange', tempFileList);
    };
    input.click();
};

onBeforeUnmount(() => {
    fileSelectorRef.value!.ondrop = null;
});
</script>

<template>
    <div
        class="file-selector"
        ref="file-selector"
        @click="openFileSelectWindow"
    >
        <span class="iconfont icon-wenjianshangchuan" />
        <slot />
        <div class="select-filename-list">
            <div
                class="select-item-name"
                v-for="(name, index) of selectItemNameList"
                :key="index"
            >
                {{ name }}
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
@import 'iconfont/iconfont.css';

.file-selector {
    border: 1px solid #ccc;
    display: flex;
    flex-direction: column;
    justify-content: center;
    overflow: hidden;
}
</style>

<script setup lang="ts">
/**
 * 富文本编辑器
 */
// import '@wangeditor/editor/dist/css/style.css' // 引入 css，之前用的是 npm 现在是 pnpm
import './wangeditor_style.css'; // 引入 css
import { computed, onBeforeUnmount, shallowRef, watch } from 'vue';
// @ts-ignore
import { Editor, Toolbar } from '@wangeditor/editor-for-vue';
// @ts-ignore
import type { IDomEditor, IEditorConfig } from '@wangeditor/editor';

// 编辑器实例，必须用 shallowRef
const editorRef = shallowRef<IDomEditor>();

const { html, disabled, placeholder } = defineProps<{
    html: string;
    disabled?: boolean;
    placeholder?: string;
}>();

const emits = defineEmits<{
    'update:html': [string];
    blur: [void];
    focus: [void]; // 实验中
}>();

// 内容 HTML
const computedHtml = computed({
    get() {
        return html;
    },
    set(html: string) {
        emits('update:html', html);
    },
});

const toolbarConfig = {
    excludeKeys: [
        'fullScreen', // 去掉全屏按钮
        'uploadImage', // 去掉上传图片按钮
        'uploadVideo', // 去掉上传视频按钮
        'codeBlock', // 去掉代码块按钮
        'insertTable', // 去掉插入表格按钮
        'insertVideo', // 去掉插入视频按钮
        'insertImage', // 去掉插入图片按钮
    ],
};

const editorConfig: Partial<IEditorConfig> = {
    // Partial 的作用是将一个对象类型的所有属性都变为可选属性
    placeholder,
    readOnly: disabled,
};

// 组件销毁时，也及时销毁编辑器
onBeforeUnmount(() => {
    const editor = editorRef.value;
    if (editor == null) return;
    editor.destroy();
});

watch(
    () => disabled,
    (newVal: boolean) => {
        if (newVal) {
            // 禁用编辑器，设置为只读
            editorRef.value?.disable();
        } else {
            // 取消禁用，取消只读
            editorRef.value?.enable();
        }
    },
);

const handleCreated = (editor: IDomEditor) => {
    editorRef.value = editor; // 记录 editor 实例，重要！
};
</script>

<template>
    <div class="rich-text-editor">
        <Toolbar
            style="border-bottom: 1px solid #ccc"
            :editor="editorRef"
            :defaultConfig="toolbarConfig"
            mode="default"
        />
        <Editor
            style="height: 500px; overflow-y: hidden"
            v-model="computedHtml"
            @onBlur="emits('blur')"
            @onFocus="emits('focus')"
            :defaultConfig="editorConfig"
            mode="default"
            @onCreated="handleCreated"
        />
    </div>
</template>

<style scoped lang="scss">
.rich-text-editor {
    border: 1px solid #ccc;
}
</style>

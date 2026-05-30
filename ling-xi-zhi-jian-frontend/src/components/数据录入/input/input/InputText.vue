<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
    modelValue: string; // 使用 modelValue 作为 v-model 的默认属性
    disabled?: boolean;
    placeholder?: string;
}>();

const emits = defineEmits<{
    (e: 'update:modelValue', value: string): void;
    (e: 'focus'): void;
    (e: 'blur'): void;
}>();

// 使用 v-model 绑定输入框的值
const inputValue = computed({
    get: () => props.modelValue,
    set: (value) => {
        emits('update:modelValue', value);
    },
});
</script>

<template>
    <input
        class="input-text"
        :disabled="disabled"
        @focus="emits('focus')"
        @blur="emits('blur')"
        type="text"
        :placeholder="placeholder"
        v-model="inputValue"
    />
</template>

<style scoped lang="scss">
@use 'variable';
@import '../../../iconfont/iconfont.css';

.input-text {
    display: block;
    width: 100%;
    outline: none;
    border-radius: variable.$border-radius;
    padding: variable.$padding;
    color: #666;
    font-size: variable.$font-size;
    border: 0.1mm solid $blue-10;
    background-color: transparent;
}
</style>

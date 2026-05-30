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
    <div class="input-with-icon" :class="{ active: inputValue.length > 0 }">
        <span class="iconfont icon-yonghu" />
        <input
            :disabled="disabled"
            @focus="emits('focus')"
            :placeholder="placeholder"
            @blur="emits('blur')"
            type="text"
            v-model="inputValue"
        />
    </div>
</template>

<style scoped lang="scss">
@use 'variable';
@import '../../../iconfont/iconfont.css';

.input-with-icon {
    height: variable.$height;
    display: grid;
    align-content: center;
    align-items: center;
    justify-items: center;
    grid-template-columns: 20px 1fr;
    grid-template-rows: 1fr;
    padding: variable.$padding;
    background-color: variable.$background-color;
    border-radius: variable.$border-radius;
    box-shadow: variable.$box-shadow;
    gap: variable.$gap;
    transition: all 0.3s;

    &:has(> input:focus) {
        background-color: variable.$focus-background-color;
    }

    &.active {
        box-shadow: variable.$active-box-shadow;
    }

    .iconfont {
        font-size: 14px;
        color: #222;
    }

    input {
        align-self: stretch;
        justify-self: stretch;
        border: none;
        font-size: variable.$font-size;
        outline: none;
        background-color: transparent;
    }
}
</style>

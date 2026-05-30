<script setup lang="ts">
import { computed, ref } from 'vue';

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
const focus = ref(false);
const showPassword = ref(false);

// 切换显示密码的逻辑
const togglePassword = () => {
    showPassword.value = !showPassword.value;
};

const focusFn = () => {
    focus.value = true;
    emits('focus');
};
const blurFn = () => {
    focus.value = false;
    emits('blur');
};
</script>

<template>
    <div
        class="input-password-with-icon"
        :class="{ active: inputValue.length > 0, focus }"
    >
        <span class="iconfont icon-mima left" />
        <input
            :type="showPassword ? 'text' : 'password'"
            v-model="inputValue"
            :placeholder="placeholder"
            autocomplete="off"
            autocapitalize="off"
            @focus="focusFn"
            @blur="blurFn"
        />
        <!-- 切换显示/隐藏密码的图标 -->
        <i
            class="iconfont right"
            :class="
                showPassword
                    ? 'icon-yanjing_xianshi_o'
                    : 'icon-yanjing_yincang_o'
            "
            @click="togglePassword"
        />
    </div>
</template>

<style scoped lang="scss">
@use 'variable';
@import '../../../iconfont/iconfont.css';

.input-password-with-icon {
    height: variable.$height;
    display: grid;
    align-content: center;
    align-items: center;
    justify-items: center;
    grid-template-columns: 20px 1fr 20px;
    grid-template-rows: 1fr;
    padding: variable.$padding;
    background-color: variable.$background-color;
    border-radius: variable.$border-radius;
    box-shadow: variable.$box-shadow;
    gap: variable.$gap;
    transition: all 0.3s;

    &.focus {
        background-color: variable.$focus-background-color;
    }

    &.active {
        box-shadow: variable.$active-box-shadow;
    }

    .iconfont.left {
        font-size: 14px;
    }

    input {
        align-self: stretch;
        justify-self: stretch;
        font-size: variable.$font-size;
        border: none;
        outline: none;
        background-color: transparent;
    }

    .iconfont.right {
        cursor: pointer;
        color: #222;
        font-size: 16px;
        display: flex;
        align-items: center;
        justify-content: center;
        user-select: none;
        border: none;
    }
}
</style>

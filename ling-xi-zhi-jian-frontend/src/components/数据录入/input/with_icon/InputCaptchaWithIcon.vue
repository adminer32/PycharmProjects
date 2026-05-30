<script setup lang="ts">
import { computed, ref } from 'vue';

const props = defineProps<{
    modelValue: string; // 使用 modelValue 作为 v-model 的默认属性
    disabled?: boolean;
    placeholder?: string;
    src: string;
}>();
const emits = defineEmits<{
    (e: 'update:modelValue', value: string): void;
    (e: 'clickCaptcha'): void;
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
        class="input-captcha-with-icon"
        :class="{ active: inputValue.length > 0, focus }"
    >
        <span class="iconfont icon-yanzhengyanzhengma" />
        <!-- 使用 v-model 绑定输入框的值 -->
        <!--        禁用浏览器自带的显示隐藏功能 -->
        <input
            type="text"
            v-model="inputValue"
            @focus="focusFn"
            @blur="blurFn"
            :placeholder="placeholder"
        />
        <img :src="src" alt="" @click="emits('clickCaptcha')" />
    </div>
</template>

<style scoped lang="scss">
@use 'variable';
@import '../../../iconfont/iconfont.css';

.input-captcha-with-icon {
    height: variable.$height;
    display: grid;
    grid-template-columns: 20px 1fr auto;
    grid-template-rows: 1fr;
    align-content: center;
    align-items: center;
    justify-items: center;
    padding: variable.$padding;
    background-color: variable.$background-color;
    border-radius: variable.$border-radius;
    box-shadow: variable.$box-shadow;
    gap: variable.$gap;
    transition: all 0.3s;
    overflow: hidden;

    &.focus {
        background-color: variable.$focus-background-color;
    }

    &.active {
        box-shadow: variable.$active-box-shadow;
    }

    .iconfont {
        font-size: 14px;
    }

    input {
        align-self: stretch;
        justify-self: stretch;
        min-width: 0;
        border: none;
        outline: none;
        font-size: variable.$font-size;
        background-color: transparent;
    }

    img {
        cursor: pointer;
    }
}
</style>

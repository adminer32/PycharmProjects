<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
    src: string;
    modelValue: string; // 使用 modelValue 作为 v-model 的默认属性
    placeholder?: string;
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
</script>

<template>
    <div class="input-captcha">
        <!-- 使用 v-model 绑定输入框的值 -->
        <!--        禁用浏览器自带的显示隐藏功能 -->
        <input
            type="text"
            v-model="inputValue"
            @focus="emits('focus')"
            @blur="emits('blur')"
            :placeholder="placeholder"
        />
        <img :src="src" alt="" @click="emits('clickCaptcha')" />
    </div>
</template>

<style scoped lang="scss">
@forward 'variable';
@import '../../../iconfont/iconfont.css';

.input-captcha {
    display: flex;
    align-items: stretch;
    background-color: #fff;
    border: 0.1mm solid $blue-10;
    overflow: hidden;
    border-radius: 4px;
    padding: 0 5px;

    .input-text {
        flex: 1;
        border: none;
        outline: none;
    }

    .img {
        height: 100%;
        cursor: pointer;
    }
}
</style>

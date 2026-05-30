<script setup lang="ts">
import { computed, ref } from 'vue';

const props = defineProps<{
    src: string;
    modelValue: string; // 使用 modelValue 作为 v-model 的默认属性
    placeholder?: string;
    label?: string;
}>();
const emits = defineEmits<{
    (e: 'update:modelValue', value: string): void;
    (e: 'clickCaptcha'): void;
}>();

// 使用 v-model 绑定输入框的值
const inputValue = computed({
    get: () => props.modelValue,
    set: (value) => {
        emits('update:modelValue', value);
    },
});
const focus = ref(false);
</script>

<template>
    <div>
        <div class="input-captcha-with-border-bottom">
            <!--        禁用浏览器自带的显示隐藏功能 -->
            <input
                type="text"
                v-model="inputValue"
                @focus="focus = true"
                @blur="focus = false"
                :placeholder="placeholder"
            />
            <img :src="src" alt="" @click="emits('clickCaptcha')" />
        </div>
        <div class="label" :class="{ active: inputValue.length > 0 || focus }">
            {{ label }}
        </div>
    </div>
</template>

<style scoped lang="scss">
@import '../../../iconfont/iconfont.css';

.input-captcha {
    display: flex;
    align-items: stretch;
    background-color: #fff;
    border: 0.1mm solid $blue-10;
    overflow: hidden;
    border-radius: 4px;

    input {
        flex: 1;
        padding: 6px 0 6px 8px;
        font-size: 14px;
        outline: none;
        border: none;
        background-color: transparent;
        border-right: 0.1mm solid $blue-10;
    }

    .img {
        width: 100%;
        cursor: pointer;
    }
}
</style>

<script setup lang="ts">
import { computed, ref } from 'vue';

const props = defineProps<{
    modelValue: string; // 使用 modelValue 作为 v-model 的默认属性
    placeholder?: string;
}>();
const emits = defineEmits<{
    (e: 'update:modelValue', value: string): void;
    (e: 'focus'): void;
    (e: 'blur'): void;
}>();

const showPassword = ref(false);

// 使用 v-model 绑定输入框的值
const inputValue = computed({
    get: () => props.modelValue,
    set: (value) => {
        emits('update:modelValue', value);
    },
});

// 切换显示密码的逻辑
const togglePassword = () => {
    showPassword.value = !showPassword.value;
};
</script>

<template>
    <div class="input-password">
        <!-- 使用 v-model 绑定输入框的值 -->
        <!--        禁用浏览器自带的显示隐藏功能 -->
        <input
            :type="showPassword ? 'text' : 'password'"
            v-model="inputValue"
            :placeholder="placeholder"
            autocomplete="off"
            autocapitalize="off"
            @focus="emits('focus')"
            @blur="emits('blur')"
        />
        <!-- 切换显示/隐藏密码的图标 -->
        <i
            class="iconfont"
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
@forward 'variable';
@import '../../../iconfont/iconfont.css';

.input-password {
    display: flex;
    align-items: stretch;
    background-color: #fff;
    border: 0.1mm solid $blue-10;
    border-radius: 4px;
    overflow: hidden;
    color: #666;

    input {
        flex: 1;
        width: 100%;
        outline: none;
        border: none;
        border-radius: 4px;
        color: #666;
        background-color: transparent;
    }

    .iconfont {
        width: 30px;
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

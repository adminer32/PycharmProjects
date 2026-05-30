<script setup lang="ts">
import { computed, ref } from 'vue';

const props = defineProps<{
    modelValue: string; // 使用 modelValue 作为 v-model 的默认属性
    placeholder?: string;
    label: string;
}>();
const emits = defineEmits<{
    (e: 'update:modelValue', value: string): void;
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

const focus = ref(false);
</script>

<template>
    <div class="input-password-width-border-bottom">
        <div class="input-password">
            <!-- 使用 v-model 绑定输入框的值 -->
            <!--        禁用浏览器自带的显示隐藏功能 -->
            <input
                :type="showPassword ? 'text' : 'password'"
                v-model="inputValue"
                :placeholder="placeholder"
                autocomplete="off"
                autocapitalize="off"
                @focus="focus = true"
                @blur="focus = false"
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
        <div class="label" :class="{ active: inputValue.length > 0 || focus }">
            {{ label }}
        </div>
    </div>
</template>

<style scoped lang="scss">
@import '../../../iconfont/iconfont.css';

.input-password-width-border-bottom {
    height: 30px;
    position: relative;
    border-bottom: 0.1mm solid $blue-10;

    .label {
        height: 100%;
        transition: transform 0.3s;
        transform-origin: left top;
        padding-left: 10px;
        color: #777;

        &.active {
            transform: translate(0, -60%) scale(0.95);
        }
    }

    .input-password {
        display: flex;
        align-items: stretch;
        overflow: hidden;
        font-size: 14px;
        position: absolute;
        inset: 0;

        input {
            flex: 1;
            padding: 6px 8px;
            padding-right: 0;
            outline: none;
            border: none;
            font-size: 16px;
            color: #555;
            background-color: transparent;
        }

        .iconfont {
            width: 30px;
            cursor: pointer;
            font-size: 16px;
            display: flex;
            align-items: center;
            justify-content: center;
            user-select: none;
        }
    }
}
</style>

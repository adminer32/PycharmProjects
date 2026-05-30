<script setup lang="ts">
/**
 * 复选框
 */
defineProps<{
    modelValue: boolean; // 使用 modelValue 作为 v-model 的默认属性
    label?: string; // 复选框的标签内容
}>();

// 定义 emits
const emits = defineEmits<{
    (e: 'update:modelValue', value: boolean): void;
}>();
</script>

<template>
    <label class="checkbox">
        <input
            type="checkbox"
            :checked="modelValue"
            @change="emits('update:modelValue', !modelValue)"
        />
        <span class="checkbox-icon checked" v-if="modelValue">
            <span class="iconfont icon-duihao_o" />
        </span>
        <span class="checkbox-icon" v-else />
        <span class="checkbox-label">{{ label }}</span>
    </label>
</template>

<style scoped lang="scss">
@import '../iconfont/iconfont.css';

.checkbox {
    display: flex;
    align-items: center;
    cursor: pointer;
    user-select: none;

    input {
        display: none;
    }

    .checkbox-icon {
        width: 15px;
        height: 15px;
        background-color: #fff;
        border: 0.1mm solid $blue-10;
        margin-right: 5px;
        border-radius: 4px;

        &.checked {
            background-color: $blue-10;
            display: flex;
            align-items: center;
            justify-content: center;

            .iconfont {
                font-size: 11px;
                color: white;
            }
        }
    }
}
</style>

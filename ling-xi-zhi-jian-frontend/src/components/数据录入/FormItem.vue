<script setup lang="ts">
const { direction = 'column' } = defineProps<{
    label?: string;
    required?: boolean;
    errorMessage?: string;
    showError?: boolean;
    direction?: 'row' | 'column';
}>();
</script>

<template>
    <div class="form-item" :style="{ flexDirection: direction }">
        <div class="label" v-if="label">
            <span class="required" v-if="required"> * </span>
            {{ label }}
        </div>
        <div class="input-container">
            <slot></slot>
        </div>
        <Transition name="error-message">
            <div class="error-message" v-show="showError">
                {{ errorMessage }}
            </div>
        </Transition>
    </div>
</template>

<style scoped lang="scss">
.form-item {
    display: flex;
    white-space: nowrap;
    font-size: 14px;
    color: #666;
    position: relative;

    .label {
        position: relative;
        margin: 5px 10px 5px 0;

        .required {
            color: red;
            font-weight: bold;
        }
    }

    .input-container {
        z-index: 1;
    }

    .error-message {
        color: red;
        font-size: 12px;
        position: absolute;
        bottom: -20px;

        &-enter-active,
        &-leave-active {
            transition: all 0.3s ease;
        }

        &-enter-from,
        &-leave-to {
            opacity: 0;
            transform: translateY(-100%);
        }
    }
}
</style>

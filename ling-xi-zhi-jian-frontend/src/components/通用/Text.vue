<script setup lang="ts">
/**
 * 带有下划线动画的文本
 */
</script>
<template>
    <span class="text">
        <slot></slot>
    </span>
</template>

<style scoped lang="scss">
.text {
    transition: background-size 0.3s;
    background: linear-gradient(to right, $blue-10, $blue-10) no-repeat right
        bottom;
    background-size: 0 1px;
    display: inline;
    word-break: break-all;

    &:hover {
        background-position: left bottom;
        background-size: 100% 2px;
    }
}
</style>

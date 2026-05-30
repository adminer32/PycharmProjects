<script setup lang="ts">
/**
 * 分割线
 */
</script>

<template>
    <div class="divider">
        <div class="line" />
        <slot />
        <div class="line" />
    </div>
</template>

<style scoped lang="scss">
.divider {
    display: flex;
    align-items: center;

    .line {
        flex: 1;
        height: 0.1mm;
        background-color: #ccc;
    }
}
</style>

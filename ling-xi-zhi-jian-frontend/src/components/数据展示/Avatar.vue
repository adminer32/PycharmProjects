<script setup lang="ts">
defineProps<{
    src: string;
}>();
</script>

<template>
    <div class="avatar" :style="{ backgroundImage: `url(${src})` }" />
</template>

<style scoped lang="scss">
.avatar {
    width: 100px;
    height: 100px;
    border-radius: 50%;
    background-size: 100% 100%;
    background-color: #c2c4c6;
}
</style>

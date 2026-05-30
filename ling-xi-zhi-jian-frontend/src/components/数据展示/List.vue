<script setup lang="ts">
const { items } = defineProps<{
    items: string[];
}>();
</script>

<template>
    <ul>
        <li v-for="(item, index) of items" :key="index">
            <slot :title="item"></slot>
        </li>
    </ul>
</template>

<style scoped lang="scss"></style>

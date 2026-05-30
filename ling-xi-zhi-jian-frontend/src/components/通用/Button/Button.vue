<script setup lang="ts">
defineProps<{
    loading?: boolean;
    type?: 'submit';
}>();
</script>

<template>
    <button class="button" :type="type">
        <img class="loading" src="./loading.gif" alt="" v-if="loading" />
        <slot />
    </button>
</template>

<style scoped lang="scss">
.button {
    all: unset;
    padding: 5px 15px;
    background-color: $blue-10;
    box-shadow: $box-shadow;
    color: #fff;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background-color 0.2s ease-in-out;
    font-size: 14px;
    cursor: pointer;
    user-select: none;
    overflow: hidden;
    white-space: nowrap;

    .loading {
        width: 20px;
        margin-right: 5px;
    }

    &:hover {
        background-color: $blue-8;
    }
}
</style>

<script setup lang="ts">
defineProps<{
    tips: string;
}>();
</script>

<template>
    <span class="tips">
        <span class="iconfont icon-tishi_o" />
        <span class="card">{{ tips }}</span>
    </span>
</template>

<style scoped lang="scss">
@import '../iconfont/iconfont.css';

.tips {
    position: relative;
    font-size: 14px;

    .iconfont {
        font-size: 18px;

        &:hover ~ .card {
            display: block;
        }
    }

    .card {
        all: unset;
        position: absolute;
        background-color: #fff;
        border-radius: 5px;
        padding: 5px 10px;
        display: none;
        box-shadow: $box-shadow;
    }
}
</style>

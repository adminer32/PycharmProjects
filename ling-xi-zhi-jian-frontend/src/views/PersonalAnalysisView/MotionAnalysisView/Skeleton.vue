<script setup lang="ts"></script>

<template>
    <div class="skeleton">
        <div class="skeleton-item" v-for="index in 3" :key="index" />
    </div>
</template>

<style scoped lang="scss">
.skeleton {
    $gap: 7px;
    width: 100%;
    overflow: hidden;
    padding: $gap;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
    position: relative;

    &::after {
        content: '';
        position: absolute;
        animation: shan 1.3s ease 0s infinite;
        top: 0;
        width: 40%;
        height: 100%;
        background: linear-gradient(
            to left,
            rgba(255, 255, 255, 0) 0,
            rgba(255, 255, 255, 0.4) 50%,
            rgba(255, 255, 255, 0) 100%
        );
        transform: skewX(-45deg);
        @keyframes shan {
            0% {
                left: -100%;
            }
            100% {
                left: 120%;
            }
        }
    }

    .skeleton-item {
        height: 17px;
        width: 100%;
        border-radius: 5px;
        background-color: #f0f2f5;

        &:first-child {
            width: 40%;
        }
    }
}
</style>

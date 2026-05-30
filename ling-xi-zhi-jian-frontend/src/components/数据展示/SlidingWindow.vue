<script setup lang="ts">
import { onMounted } from 'vue';

onMounted(() => {
    // const after = slidingWindowRef.value!.querySelector('.after') as HTMLElement;
    // after.style.width = `${slidingWindowRef.value!.offsetWidth}px`;
    // slidingWindowRef.value!.onmousedown = (e: MouseEvent) => {
    //     const afterBox = slidingWindowRef.value!.querySelector('.after-box') as HTMLElement;
    //     afterBox.style.width = `${e.offsetX}px`;
    //     window.onmousemove = e => {
    //         afterBox.style.width = `${e.offsetX}px`;
    //     };
    //     window.onmouseup = () => {
    //         window.onmousemove = null;
    //         window.onmouseup = null
    //     }
    // }
});
</script>

<template>
    <div class="sliding-window" ref="sliding-window">
        <slot></slot>
        <div class="after-box">
            <div class="after">
                <slot name="after"></slot>
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
.sliding-window {
    position: relative;
    overflow: hidden;

    .after-box {
        position: absolute;
        height: 100%;
        overflow: hidden;
        animation: to 5s forwards infinite;
        animation-delay: 0.3s;
        @keyframes to {
            0%,
            100% {
                width: 100%;
            }
            50% {
                width: 0;
            }
        }

        .after {
            height: 100%;
        }
    }
}
</style>

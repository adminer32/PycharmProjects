<script setup lang="ts">
import { onMounted, useTemplateRef } from 'vue';

onMounted(() => {
    const after = clipWindowRef.value!.querySelector('.after') as HTMLElement;
    after.style.width = `${clipWindowRef.value!.offsetWidth}px`;
    clipWindowRef.value!.onmousedown = (e: MouseEvent) => {
        const afterBox = clipWindowRef.value!.querySelector(
            '.after-box',
        ) as HTMLElement;
        afterBox.style.width = `${e.offsetX}px`;
        window.onmousemove = (e) => {
            afterBox.style.width = `${e.offsetX}px`;
        };
        window.onmouseup = () => {
            window.onmousemove = null;
            window.onmouseup = null;
        };
    };
});

const clipWindowRef = useTemplateRef<HTMLElement>('clip-window');
</script>

<template>
    <div class="clip-window" ref="clip-window">
        <slot></slot>
        <div class="after">
            <slot name="after"></slot>
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

        .after {
            height: 100%;
        }
    }
}
</style>

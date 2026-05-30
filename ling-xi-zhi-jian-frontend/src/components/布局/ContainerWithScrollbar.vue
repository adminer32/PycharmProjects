<script setup lang="ts">
/**
 * 带滚动条的容器
 */
import Scrollbar from '@/components/布局/Scrollbar.vue';

const emits = defineEmits<{
    onScroll: [scrollTop: number];
}>(); // 下划线只是为了与 onscroll 区分开

const {
    scrollbarThumbColor = '#9e9e9e',
    scrollbarThumbMarginRight = '2px',
    scrollbarWidth = '4px',
} = defineProps<{
    scrollbarThumbColor?: string;
    scrollbarThumbMarginRight?: string;
    scrollbarWidth?: string;
}>();
</script>

<template>
    <div class="container-with-scrollbar">
        <div class="content">
            <slot />
        </div>
        <Scrollbar
            @onScroll="(scrollTop) => emits('onScroll', scrollTop)"
            :scrollbarThumbMarginRight="scrollbarThumbMarginRight"
            :scrollbarWidth="scrollbarWidth"
            :scrollbar-thumb-color="scrollbarThumbColor"
        />
    </div>
</template>

<style scoped lang="scss">
.container-with-scrollbar {
    height: 100%;
    position: relative; // 必循是 相对定位
    overflow: hidden; // 创建了一个新的块级格式化上下文（BFC）
    .content {
        height: 100%;
        overflow-y: auto;

        &::-webkit-scrollbar {
            display: none;
        }
    }
}
</style>

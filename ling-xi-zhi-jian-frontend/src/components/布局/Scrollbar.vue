<script setup lang="ts">
/**
 * 滚动条
 *
 * 使用说明：
 *      1.必须 于 .content 同级
 *      2.父容器 必须 是 相对定位
 */
import { onMounted, ref, useTemplateRef } from 'vue';

const isShowScrollbar = ref(false);
const {
    scrollbarThumbColor = '#9e9e9e',
    scrollbarThumbMarginRight = '2px',
    scrollbarWidth = '4px',
} = defineProps<{
    scrollbarThumbColor?: string;
    scrollbarThumbMarginRight?: string;
    scrollbarWidth?: string;
}>();
const h = ref(0); // 值为 .content 的 scrollHeight
const scrollbarRef = useTemplateRef<HTMLElement>('scrollbar');
const emits = defineEmits<{
    onScroll: [scrollTop: number];
}>(); // 下划线只是为了与 onscroll 区分开

const settingScrollbarThumb = () => {
    const scrollbar = scrollbarRef.value as HTMLDivElement;
    const content = (scrollbar.parentNode as HTMLElement).querySelector(
        '.content',
    ) as HTMLElement;

    h.value = content.scrollHeight;
};

const observer = new MutationObserver(settingScrollbarThumb); // 观察子元素变化
let timer: ReturnType<typeof setTimeout>;

const onScroll = (e: Event) => {
    isShowScrollbar.value = true;

    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
        isShowScrollbar.value = false;
        clearTimeout(timer);
    }, 700);

    const scrollbar = e.currentTarget as HTMLElement;

    const containerWithScrollbar = scrollbar.parentNode as HTMLElement;
    const content = containerWithScrollbar.querySelector(
        '.content',
    ) as HTMLElement;

    content.scrollTo({
        // 将页面content的内容滚动到对应位置
        top: scrollbar.scrollTop,
    });
    emits('onScroll', scrollbar.scrollTop);
};

onMounted(() => {
    settingScrollbarThumb();
    const scrollbar = scrollbarRef.value as HTMLDivElement;
    const containerWithScrollbar = scrollbar.parentNode as HTMLElement;
    const content = containerWithScrollbar.querySelector(
        '.content',
    ) as HTMLElement;

    content.onscroll = (e) => {
        // 根据页面的滚动位置动态的设置自定义滚动条的位置
        scrollbar.scrollTo({
            top: (e.currentTarget as HTMLElement).scrollTop,
        });
    };

    observer.observe(content, {
        childList: true, // 监听子节点的变化
        subtree: false, // 监听所有后代节点
    });
});

const onSizeChange = () => {
    // 当滚动条的高度变化时
    settingScrollbarThumb();
};
</script>

<template>
    <div
        class="scrollbar"
        ref="scrollbar"
        :style="{ opacity: isShowScrollbar ? 1 : 0 }"
        @scroll="onScroll"
        v-size-ob="onSizeChange"
    >
        <div :style="{ height: `${h}px`, width: '1px' }" />
        <!-- 填充 1 没有实际含义 , 注意， 如果不设置宽度，那该容器的面积为零，会出问题 -->
    </div>
</template>

<style scoped lang="scss">
.scrollbar {
    $width: v-bind('scrollbarWidth'); // 滚动条 的 宽度
    position: absolute;
    bottom: 0;
    height: 100%;
    right: v-bind('scrollbarThumbMarginRight'); // 滚动条 距离 右边的 距离
    width: $width;
    overflow-y: auto;
    overflow-x: hidden;
    z-index: 1;
    transition: opacity 0.5s;

    &::-webkit-scrollbar {
        width: $width;
    }

    &::-webkit-scrollbar-thumb {
        background-color: v-bind('scrollbarThumbColor'); // 滚动条的滑块的颜色
        opacity: 0.5;
    }

    &::-webkit-scrollbar-track {
        background-color: transparent;
    }
}
</style>

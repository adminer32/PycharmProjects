<script setup lang="ts">
/**
 * 网站介绍
 */
import { onMounted, ref, useTemplateRef } from 'vue';

const emits = defineEmits<{
    (e: 'scrollToBottom'): void;
}>();

const isSliding = ref(false);

const scrollToBottom = () => {
    // 触发向下滑动动画
    isSliding.value = true;
    // 动画结束后触发事件
    setTimeout(() => {
        emits('scrollToBottom');
    }, 800);
};

const websiteIntroductionRef = useTemplateRef<HTMLElement>(
    'website-introduction',
);

onMounted(() => {
    // 移除IntersectionObserver监听，这样用户滚动时不会自动离开该组件
});
</script>

<template>
    <div class="website-introduction" ref="website-introduction" :class="{ 'slide-down': isSliding }">
        <div class="top-tool" />

        <div class="center-box">
            <div class="site-title">翎 翼<br />毽 球</div>

            <div class="site-subtitle">
                欢迎来到毽球教学平台<br />为您提供“学习-训练-评估”闭环毽球技能学习服务
            </div>
        </div>

        <div class="zhan-wei">
            <span
                class="to-bottom-btn iconfont icon-bottom"
                @click="scrollToBottom"
            >
            </span>
        </div>
    </div>
</template>

<style scoped lang="scss">
.website-introduction {
    position: relative;
    display: flex;
    flex-direction: column;
    height: 100vh;
    width: 100vw;
    overflow: hidden;
    transition: transform 0.8s ease;

    &.slide-down {
        transform: translateY(100vh);
    }

    .top-tool {
        position: absolute;
        width: 5px;
        height: 5px;
        top: calc($header-height * -1);
    }

    .center-box {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;

        .site-title {
            user-select: none;
            font-size: 150px;
            font-weight: bold;
            font-family: '楷体', serif;
            margin-bottom: 30px;
            word-spacing: -40px;
            text-shadow:
                1px 1px 5px #99999955,
                -1px -1px 5px #ffffff55;
            color: #222;
        }

        .site-subtitle {
            font-size: 20px;
            text-align: center;
            line-height: 1.8;
            color: #818181;
        }
    }

    .zhan-wei {
        height: calc($header-height + 20px);
        display: flex;
        flex-direction: row;
        justify-content: center;
        align-items: center;

        .to-bottom-btn {
            padding: 12px;
            border-radius: 50%;
            background-color: rgba(255, 255, 255, 0.61);
            animation: to-bottom-btn 1s infinite;
            font-size: 20px;
            cursor: pointer;
            color: #66666699;
            box-shadow: 0 0 10px #99999922;

            @keyframes to-bottom-btn {
                0%,
                100% {
                    transform: translateY(0);
                }
                50% {
                    transform: translateY(10px);
                }
            }
        }
    }
}
</style>

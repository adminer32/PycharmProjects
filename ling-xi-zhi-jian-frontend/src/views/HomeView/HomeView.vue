<script lang="ts" setup>
import Header from '@/views/Header/Header.vue';
import Footer from '@/views/HomeView/components/Footer.vue';
import ContentSection from '@/views/HomeView/components/ContentSection.vue';
import { onMounted, onUnmounted, reactive, ref, useTemplateRef } from 'vue';
import Text from '@/components/通用/Text.vue';
import CourseCenter from '@/views/HomeView/components/ThreeBoxContainer/CourseCenter.vue';
import QuestionsAndAnswers from '@/views/HomeView/components/QuestionsAndAnswers.vue';
import MotionDemo from '@/views/HomeView/components/ThreeBoxContainer/MotionDemo.vue';
import MotionAnalysis from '@/views/HomeView/components/ThreeBoxContainer/MotionAnalysis.vue';
import WebsiteIntroduction from '@/views/HomeView/components/WebsiteIntroduction.vue';


const homeRef = useTemplateRef<HTMLElement>('home-view');

const keyFrames: {
    transform: string;
}[] = [];

const maxX = 496; // 单位px

function fn(x: number) {
    // 抛物线
    return (-600 / 61504) * (x - 248) ** 2 + 600;
}

const createKeyFrames = () => {
    function i2x(i: number) {
        return maxX - (i * maxX) / 100;
    }

    function deg2Rad(deg: number) {
        return (deg * Math.PI) / 180;
    }

    for (let i = 0; i < 101; i++) {
        const x1 = i2x(i);
        const x2 = i2x(i + 1);
        const y1 = fn(x1);
        const y2 = fn(x2);
        const r = Math.atan((y2 - y1) / (x1 - x2));
        keyFrames.push({
            transform: `translateX(${i2x(i)}px) translateY(${fn(i2x(i)) * -1}px) rotate(${r + deg2Rad(90)}rad)`,
        });
    }
};

const playShuttlecockMotion = () => {
    const shuttlecock = homeRef.value!.querySelector(
        '.home-view-shuttlecock',
    ) as HTMLElement;
    createKeyFrames();
    // 创建动画
    const animation = shuttlecock.animate(keyFrames, {
        duration: 2000,
        easing: 'cubic-bezier(0.20, 0.50, 0.82, 0.29)',
    });
    animation.play();
    animation.onfinish = () => {
        shuttlecock.style.transform = keyFrames[keyFrames.length - 1].transform;
    };
};
let boxHeight = 0;

onMounted(() => {
    playShuttlecockMotion(); // 播放毽子动画
    const box = homeRef.value?.querySelector('.box') as HTMLElement;
    boxHeight = box.offsetHeight;
    // 确保页面刷新时停留在WebsiteIntroduction组件
    isShowWebsiteIntroduction.value = true;
    homeRef.value?.scrollTo({
        top: 0,
        behavior: 'auto'
    });
    // 添加滚动事件监听，防止用户在WebsiteIntroduction组件显示时滚动
    homeRef.value?.addEventListener('wheel', handleScroll, { passive: false });
    homeRef.value?.addEventListener('touchmove', handleScroll, { passive: false });
});

onUnmounted(() => {
    // 移除滚动事件监听
    homeRef.value?.removeEventListener('wheel', handleScroll);
    homeRef.value?.removeEventListener('touchmove', handleScroll);
});

const b1 = reactive({
    scale: 0,
    rotate: 0,
});

const b2 = reactive({
    scale: 0,
    translateY: 0,
    rotate: 0,
});

const b3 = reactive({
    scale: 0,
    translateY: 0,
    rotate: 0,
});

const handleScroll = (e: Event) => {
    if (isShowWebsiteIntroduction.value) {
        // 当WebsiteIntroduction组件显示时，检查滚动方向
        const wheelEvent = e as WheelEvent;
        if (wheelEvent.deltaY > 0) {
            // 向下滚动，触发下拉按钮功能
            scrollToCarousel();
        }
        // 阻止默认滚动行为
        e.preventDefault();
        homeRef.value?.scrollTo({
            top: 0,
            behavior: 'auto'
        });
    }
};

const onScroll = () => {
    if (!isShowWebsiteIntroduction.value) {
        let scrollTop = homeRef.value?.scrollTop!;

        // ———————————————————————— b1 ———————————————————————————
        b1.scale = 0.9 + (scrollTop - 500) / 4000;
        if (b1.scale >= 1) {
            b1.scale = 1;
        }
        b1.rotate = -7 + (scrollTop - 450) / 67;
        if (b1.rotate >= 0) {
            b1.rotate = 0;
        }
        // ———————————————————————————————————————————————————————

        // ———————————————————————— b2 ———————————————————————————
        function b2fn(x: number) {
            if (x <= 0) return 0;
            return x ** 2;
        }

        b2.scale = 0.9 + b2fn(scrollTop - 750) / 80000;
        if (b2.scale >= 1) {
            b2.scale = 1;
        }

        b2.translateY = -boxHeight + b2fn(scrollTop - 470) / 300; //  -80
        if (b2.translateY >= 0) {
            b2.translateY = 0;
        }
        b2.rotate = 7 - (scrollTop - 670) / 50; //  7
        if (b2.rotate <= 0) {
            b2.rotate = 0;
        }
        if (b2.rotate >= 7) {
            b2.rotate = 7;
        }
        // ———————————————————————————————————————————————————————

        // ———————————————————————— b3 ———————————————————————————
        b3.scale = 0.9 + (scrollTop - 650) / 10000;
        if (b3.scale >= 1) {
            b3.scale = 1;
        }

        function b3fn(x: number) {
            if (x <= 0) return 0;
            if (x <= 20) {
                return x ** 2;
            } else {
                return (x - 10) ** 2 + x ** 2;
            }
        }

        b3.translateY = -boxHeight * 2 + b3fn(scrollTop - 450) / 800; //  -160
        if (b3.translateY >= 0) {
            b3.translateY = 0;
        }
        b3.rotate = -5 + (scrollTop - 716) / 150; // -5
        if (b3.rotate >= 0) {
            b3.rotate = 0;
        }
        if (b3.rotate <= -5) {
            b3.rotate = -5;
        }
        // ———————————————————————————————————————————————————————
    }
};
const isShowWebsiteIntroduction = ref(true);
const scrollToCarousel = () => {
    const carousel = homeRef.value?.querySelector('.carousel');
    carousel &&
        carousel.scrollIntoView({
            behavior: 'smooth',
            block: 'end',
        });

    setTimeout(() => {
        isShowWebsiteIntroduction.value = false;
    }, 800);
};

const backToTop = () => {
    isShowWebsiteIntroduction.value = true;
    homeRef.value?.scrollTo({
        top: 0,
        behavior: 'smooth',
    });
};
</script>

<template>
    <div class="home-view" ref="home-view" @scroll="onScroll">
        <Header />
        <div class="home-view-shuttlecock" v-drag>
            <div class="shuttlecock" />
        </div>
        <WebsiteIntroduction
            @scrollToBottom="scrollToCarousel"
            v-show="isShowWebsiteIntroduction"
        />

        <div
            @click="backToTop"
            class="back-to-top-btn iconfont icon-top"
            :class="{ show: !isShowWebsiteIntroduction }"
        />
        <ContentSection  />

        <div class="title" v-slide-in>
            翎翼毽球是你网页上的专属踢毽伙伴
            <p>
                提供基础课程、综合提升、动作分解演示和AI动作分析，助你从入门到精通，随时踢出精彩每一刻！
            </p>
        </div>
        <div class="box-container" v-slide-in>
            <!--                基础入门    -->
            <CourseCenter
                class="box"
                :style="{
                    transform: `rotate(${b1.rotate}deg) scale(${b1.scale})`,
                }"
            />
            <MotionDemo
                class="box"
                :style="{
                    transform: `rotate(${b2.rotate}deg) scale(${b2.scale}) translateY(${b2.translateY}px)`,
                }"
            />
            <MotionAnalysis
                class="box"
                :style="{
                    transform: `rotate(${b3.rotate}deg) scale(${b3.scale}) translateY(${b3.translateY}px)`,
                }"
            />
        </div>

        <div class="title" v-slide-in>
            我们可以协助您训练
            <p>
                <Text>
                    单击一下就能启动AI学习伴侣，智能问答为你排忧解难。毽球基本动作分为哪几大类？
                </Text>
            </p>
        </div>

        <QuestionsAndAnswers v-slide-in />
        <Footer v-slide-in />
    </div>
</template>

<style scoped lang="scss">
.home-view {
    height: 100vh;
    width: 100vw;
    overflow-y: auto;
    overflow-x: hidden;
    scroll-behavior: smooth;
    background: linear-gradient(45deg, #e2fff8, #dadfff);

    @keyframes slide-in {
        from {
            transform: translateY(100%);
            opacity: 0;
        }
        to {
            transform: translateY(0);
            opacity: 1;
        }
    }

    .container-with-scrollbar {
        flex: 1;
    }

    .back-to-top-btn {
        $size: 40px;
        position: fixed;
        bottom: 30px;
        right: -$size;
        width: $size;
        height: $size;
        border-radius: 50%;
        background-color: #fff;
        color: #000;
        font-size: 20px;
        z-index: 100;
        cursor: pointer;
        display: grid;
        place-items: center;
        transform: translateX(0);
        transition:
            transform 0.3s linear,
            opacity 0.3s linear;
        opacity: 0;
        box-shadow: $box-shadow;

        &.show {
            transform: translateX(-80px);
            opacity: 1;
        }
    }

    .header {
        animation: header 0.3s;
        @keyframes header {
            from {
                transform: translateY(-100%);
                opacity: 0;
            }
            to {
                transform: translateY(0);
                opacity: 1;
            }
        }
    }

    .home-view-shuttlecock {
        position: fixed;
        bottom: 0;
        left: 10px;
        overflow: hidden;
        height: 63px;
        transform-origin: center bottom;
        user-select: none;
        z-index: 100;

        > .shuttlecock {
            background-image: url('@/assets/home_view/毽球_4.png');
            background-repeat: no-repeat;
            transform: rotate(-15deg);
            height: 60px;
            width: 38px;
            background-size: contain;
            background-position: center;
        }
    }

    .title {
        margin-top: $header-height;
        height: 140px;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        font-size: 2.3rem;
        padding: 0 270px;
        overflow: hidden;
        font-weight: bold;
        margin-bottom: 5px;

        p {
            font-size: 1.2rem;
            text-indent: 2em;
            font-weight: normal;
            color: #666;
        }
    }

    .website-introduction {
        height: calc(100vh - $header-height);
    }

    .carousel {
        height: calc(100vh - $header-height);
    }

    .box-container {
        position: relative;
        display: flex;
        flex-direction: column;
        justify-content: space-evenly;

        .box {
            margin: 3vh auto;
            height: 70vh;
            width: 140vh;
            border-radius: 30px;
            transition: transform 0.1s ease;
            transform-origin: center center;
            overflow: hidden;
            box-shadow:
                inset 0 0 10px 1px rgb(228, 228, 228, 0.1),
                0 0 1px #d3d3d3,
                $box-shadow;

            &:nth-child(1) {
                background-image: linear-gradient(15deg, #f4f7fa, #f4f7fa);
            }

            &:nth-child(2) {
                background-image: linear-gradient(15deg, #c8b7ff, #ffdd9d);
            }

            &:nth-child(3) {
                background-image: linear-gradient(135deg, #a7c9f4, #659ee8);
            }
        }
    }
}
</style>

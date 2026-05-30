<script setup lang="ts">
/**
 * 下拉框
 */
import { ref } from 'vue';

const {
    triggeringCondition = 'click',
    showArrow = false,
    arrowMargin = '0 auto',
    autoOff = false,
    showBoxShadow = true,
} = defineProps<{
    triggeringCondition?: 'click' | 'hover';
    showArrow?: boolean;
    arrowMargin?: string;
    autoOff?: boolean; // 点击下拉框后自动关闭
    showBoxShadow?: boolean;
}>();

const open = ref(false);
const show = () => {
    window.addEventListener(
        'click',
        () => {
            open.value = false;
        },
        {
            once: true,
        },
    );
    open.value = true;
};

const onclickCard = (e: MouseEvent) => {
    if (!autoOff) {
        // 阻止默认事件
        e.stopPropagation();
    }
};

const close = () => {
    if (!autoOff) return;
    open.value = false;
};

const mouseEnter = () => {
    if (triggeringCondition == 'hover') {
        show();
    }
};
</script>

<template>
    <div
        class="dropdown"
        ref="dropdown"
        @mouseenter="mouseEnter"
        @mouseleave="close"
    >
        <div class="trigger" @click.stop="show">
            <slot name="trigger"></slot>
        </div>
        <transition name="card">
            <div
                class="card"
                v-show="open"
                @click="onclickCard"
                :style="{
                    filter: showBoxShadow
                        ? 'drop-shadow(0 0 3px #33335599)'
                        : 'none',
                }"
            >
                <div
                    class="arrow"
                    v-if="showArrow"
                    :style="{ margin: arrowMargin }"
                ></div>
                <slot />
            </div>
        </transition>
    </div>
</template>

<style lang="scss" scoped>
.dropdown {
    position: relative;
    cursor: pointer;

    .card {
        position: absolute;
        top: 100%;
        right: 0;
        z-index: 1;
        transform-origin: top left;

        &-enter-active {
            animation: open 0.2s;
        }

        &-leave-active {
            animation: open 0.2s reverse;
        }

        @keyframes open {
            0% {
                opacity: 0;
                height: 0;
                transform: scaleY(0);
            }
            100% {
                opacity: 1;
                height: auto;
                transform: scaleY(1);
            }
        }

        .arrow {
            $arrow-w: 12px; // 箭头宽
            $arrow-h: 10px; // 箭头高
            width: $arrow-w;
            height: $arrow-h;
            background-color: #fff;
            clip-path: polygon(
                50% 0,
                calc(50% + $arrow-w / 2) 100%,
                calc(50% - $arrow-w / 2) 100%
            );
        }
    }
}
</style>

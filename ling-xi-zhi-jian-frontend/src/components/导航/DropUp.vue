<script setup lang="ts">
/**
 * 上拉框
 */
import { onMounted, ref, useTemplateRef } from 'vue';
const dropdownRef = useTemplateRef<HTMLDivElement>('drop-up');
const {
    triggeringCondition = 'click',
    // showArrow = false,
    // arrowMargin = '0 auto',
    autoOff = false,
    showBoxShadow = true,
} = defineProps<{
    triggeringCondition?: 'click' | 'hover';
    // showArrow?: boolean;
    // arrowMargin?: string;
    autoOff?: boolean; // 点击下拉框后自动关闭
    showBoxShadow?: boolean;
}>();

const open = ref(false);
const show = () => {
    window.addEventListener('click', close);
    open.value = true;
};

const onclickCard = (e: MouseEvent) => {
    if (!autoOff) {
        // 阻止默认事件
        e.stopPropagation();
    }
};

const close = () => {
    if (triggeringCondition == 'click') {
        window.removeEventListener('click', close);
    }
    open.value = false;
};

const mouseEnter = () => {
    if (triggeringCondition == 'hover') {
        show();
    }
};
const left = ref(0);
// 监听card的宽度变化，当card的宽度变化时，重新设置card的left值
const ob = new ResizeObserver((entries) => {
    const entry = entries[0];
    const card = entry.target as HTMLDivElement;
    left.value = -card.offsetWidth / 2;
});

onMounted(() => {
    const card = dropdownRef.value?.querySelector('.card');
    ob.observe(card!);
});
</script>

<template>
    <div
        class="drop-up"
        ref="drop-up"
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
                    left: left + 'px',
                }"
            >
                <slot />
            </div>
        </transition>
    </div>
</template>

<style lang="scss" scoped>
.drop-up {
    position: relative;
    cursor: pointer;

    .card {
        position: absolute;
        width: min-content;
        bottom: 100%;
        z-index: 1;
        transform-origin: bottom center;

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
    }
}
</style>

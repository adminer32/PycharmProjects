<script setup lang="ts">
/**
 * 右键菜单
 */
import { onMounted, onUnmounted, ref } from 'vue';

const x = ref(0);
const y = ref(0);
const { menu } = defineProps<{
    menu: {
        label: string;
        onClick?: () => void;
    }[];
}>();

const isShowMenu = ref(false);
const handleContextMenu = (e: MouseEvent) => {
    isShowMenu.value = true;
    x.value = e.clientX;
    y.value = e.clientY;
};

const closeMenu = () => {
    isShowMenu.value = false;
};

// 第三个参数的作用
// addEventListener 方法的第三个参数可以是一个布尔值或一个对象，用于控制事件监听器的触发阶段：
// true：表示事件监听器在捕获阶段触发。也就是说，事件从 window 向下传递到目标元素的过程中，会触发该监听器。
// false（默认值）：表示事件监听器在冒泡阶段触发。也就是说，事件从目标元素向上冒泡到 window 的过程中，会触发该监听器。

onMounted(() => {
    window.addEventListener('click', closeMenu, true);
    window.addEventListener('contextmenu', closeMenu, true);
});

onUnmounted(() => {
    window.removeEventListener('click', closeMenu, true);
    window.removeEventListener('contextmenu', closeMenu, true);
});

const handleBeforeEnter = (el: Element) => {
    (el as HTMLElement).style.height = 0 + 'px';
};

const handleEnter = (el: Element) => {
    const dom = el as HTMLElement;
    dom.style.height = 'auto';
    const height = dom.clientHeight;
    dom.style.height = 0 + 'px';
    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            dom.style.height = height + 'px';
            dom.style.transition = '0.3s';
        });
    });
};

const handleAfterEnter = (el: Element) => {
    (el as HTMLElement).style.transition = 'none';
};
</script>

<template>
    <div class="context-menu" @contextmenu.stop.prevent="handleContextMenu">
        <slot />
        <Teleport to="body">
            <Transition
                @beforeEnter="handleBeforeEnter"
                @enter="handleEnter"
                @afterEnter="handleAfterEnter"
            >
                <div class="menu" v-if="isShowMenu">
                    <slot name="menu" :menu="menu">
                        <div
                            class="menu-item"
                            v-for="item of menu"
                            :key="item.label"
                            @click="item.onClick?.()"
                        >
                            {{ item.label }}
                        </div>
                    </slot>
                </div>
            </Transition>
        </Teleport>
    </div>
</template>

<style lang="scss">
.context-menu {
    width: min-content;
}

.menu {
    position: fixed;
    top: v-bind('`${x}px`');
    left: v-bind('`${y}px`');
    background-color: white;
    border-radius: 5px;
    overflow: hidden;
    box-shadow: 0 0 5px rgba(0, 0, 0, 0.2);

    .menu-item {
        padding: 5px 10px;
        cursor: pointer;

        &:hover {
            background-color: #99bef6;
        }
    }
}
</style>

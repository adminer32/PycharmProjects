<script lang="ts" setup>
import { onMounted, onUpdated, provide, reactive, useTemplateRef } from 'vue';

// 定义 props 和 emits
const { activeKey, centered = false } = defineProps<{
    activeKey: string | number;
    centered?: boolean;
}>();

const emits = defineEmits<{
    'update:activeKey': [key: string | number];
    change: [key: string | number];
}>();
const tabsRef = useTemplateRef<HTMLElement>('tabs');

// 存储所有的 TabPane 子组件
const paneList = reactive<
    {
        id: string | number;
        tab: string;
        show: () => void;
        close: () => void;
    }[]
>([]);

// 注册子组件
const register = (pane: {
    id: string | number;
    tab: string;
    show: () => void;
    close: () => void;
}) => paneList.push(pane);

// 注销子组件
const unregister = (key: string | number) => {
    paneList.splice(paneList.findIndex((pane) => pane.id == key));
};

// 提供注册和注销方法给子组件
provide('registerTabPane', register);
provide('unregisterTabPane', unregister);

onMounted(() => {
    // 此时所有的子组件已经 挂载完毕，可以获取到所有的 TabPane 子组件
    // console.log('paneList', paneList);
    paneList.forEach((pane) => {
        if (pane.id === activeKey) {
            pane.show();
        } else {
            pane.close();
        }
    });
});

onUpdated(() => {
    paneList.forEach((pane) => {
        if (pane.id === activeKey) {
            pane.show();
        } else {
            pane.close();
        }
    });

    const activeTabTitle = tabsRef.value!.querySelector(
        '.teb-title.active',
    ) as HTMLElement;
    const selectedItemBg = tabsRef.value!.querySelector(
        '.selected-item-bg',
    ) as HTMLElement;
    selectedItemBg.style.left = `${activeTabTitle.offsetLeft}px`;
    selectedItemBg.style.width = `${activeTabTitle.offsetWidth}px`;

    emits('update:activeKey', activeKey);
    emits('change', activeKey);
});
</script>
<template>
    <div class="tabs" ref="tabs">
        <!-- 标签头部 -->
        <div
            class="tabs-header"
            :style="{ justifyContent: centered ? 'center' : 'flex-start' }"
        >
            <div
                v-for="(pane, index) of paneList"
                :key="index"
                :class="['teb-title', { active: activeKey === pane.id }]"
                @click="() => emits('update:activeKey', pane.id)"
            >
                {{ pane.tab }}
            </div>
            <div class="selected-item-bg" />
        </div>
        <!-- 标签内容 -->
        <div class="tabs-content">
            <slot></slot>
        </div>
    </div>
</template>
<style scoped lang="scss">
.tabs {
    .tabs-header {
        display: flex;
        border-bottom: 1px solid #ddd;
        position: relative;

        .selected-item-bg {
            position: absolute;
            height: 5px;
            bottom: 0;
            background-color: #1890ff;
            z-index: -1;
            transition: all 0.3s;
            border-radius: 10px;
        }

        .teb-title {
            padding: 8px 16px;
            cursor: pointer;
            transition: all 0.3s;
            color: #000;

            &.active {
                color: #000;
            }

            .tabs-content {
            }
        }
    }
}
</style>

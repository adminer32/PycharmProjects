<script setup lang="ts">
import { ref, useTemplateRef } from 'vue';

defineProps<{
    header: string;
}>();
const isOpen = ref(false);
const collapseRef = useTemplateRef<HTMLElement>('collapse');
const onClick = () => {
    const bottom = collapseRef.value?.childNodes[1] as HTMLElement;
    if (!isOpen.value) {
        // 去打开
        bottom.style.height = 'auto';
        const { height } = bottom.getBoundingClientRect();
        bottom.style.height = '0';
        bottom.offsetHeight;
        bottom.style.height = `${height}px`;
    } else {
        bottom.style.height = '0';
    }
    isOpen.value = !isOpen.value;
};
</script>

<template>
    <div class="collapse" ref="collapse">
        <div class="header" @click="onClick">
            <div class="title">
                {{ header }}
            </div>
            <span
                class="iconfont icon-xuanzeqixiayige_o"
                :style="{ transform: `rotate(${isOpen ? 90 : 0}deg)` }"
            />
        </div>
        <div class="bottom">
            <slot></slot>
        </div>
    </div>
</template>

<style scoped lang="scss">
@import '../iconfont/iconfont.css';

.collapse {
    border: 0.1mm solid rgba(174, 174, 174, 0.6);
    font-size: 14px;
    user-select: none;
    border-radius: 5px;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);

    .header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 10px 10px 10px 12px;
        border-radius: 5px;
        cursor: pointer;

        .title {
            font-size: 16px;
        }

        .iconfont {
            transition: transform 0.2s ease;
        }
    }

    .bottom {
        height: 0;
        transition: height 0.3s;
        overflow: hidden;
    }
}
</style>

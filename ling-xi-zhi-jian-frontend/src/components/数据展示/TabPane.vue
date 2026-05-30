<script lang="ts" setup>
import { inject, onBeforeUnmount, onMounted, ref } from 'vue';

// 定义 props
const { tab, id } = defineProps<{
    tab: string;
    id: string | number;
}>();

// 定义是否激活
const isActive = ref(false);

// 获取父组件提供的注册和注销方法
const register =
    inject<
        (pane: {
            id: string | number;
            tab: string;
            show: () => void;
            close: () => void;
        }) => void
    >('registerTabPane');

const unregister = inject<(id: string | number) => void>('unregisterTabPane');

// 注册当前 TabPane 到父组件
onMounted(() => {
    register &&
        register({
            id,
            tab,
            show: () => (isActive.value = true),
            close: () => (isActive.value = false),
        });
});

// 组件销毁时注销
onBeforeUnmount(() => {
    if (unregister) {
        unregister(id);
    }
});
</script>

<template>
    <div v-if="isActive" class="tab-pane">
        <slot />
    </div>
</template>

<style scoped lang="scss">
.tab-pane {
}
</style>

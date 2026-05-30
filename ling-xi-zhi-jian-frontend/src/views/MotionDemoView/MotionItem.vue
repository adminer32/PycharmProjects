<script setup lang="ts">
const { selectedMotion } = defineProps<{
    item: string[];
    selectedMotion: string;
}>();

const emits = defineEmits<{
    (e: 'update:selectedMotion', value: string): void;
    (e: 'change', value: string): void;
}>();
const onClick = (item: string) => {
    if (selectedMotion == item) return; // 防止重复点击
    emits('update:selectedMotion', item);
    emits('change', item);
};
</script>

<template>
    <div
        class="motion-item"
        style="cursor: pointer"
        v-for="(motionName, index) of item"
        :key="index"
        :class="{ active: selectedMotion == motionName }"
        @click="onClick(motionName)"
    >
        <!--        <i class="iconfont icon-duigoux" v-show="selectedMotion==motionName"/>-->
        {{ motionName }}
    </div>
</template>

<style scoped lang="scss">
.motion-item {
    padding: 3px 5px;
    border-radius: 3px;

    .iconfont {
        font-weight: bold;
        color: #1a73e8;
        font-size: 14px;
    }

    &.active {
        color: #fff;
        background-color: $blue-6;
    }

    &:hover {
        color: #fff;
        background-color: $blue-6;
    }
}
</style>

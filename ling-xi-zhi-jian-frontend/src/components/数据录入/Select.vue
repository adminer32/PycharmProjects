<script setup lang="ts">
/**
 * 下拉选择框
 */
import { computed, ref } from 'vue';

const props = defineProps<{
    modelValue: string;
    items: {
        label: string;
        value: string;
    }[];
}>();

const emits = defineEmits<{
    (e: 'update:modelValue', value: string): void;
}>();

const selectOption = computed({
    get() {
        return props.modelValue;
    },
    set(value: string) {
        emits('update:modelValue', value);
    },
});

const isShowCard = ref(false);

const select = (item: string) => {
    selectOption.value = item;
    isShowCard.value = false;
};

const showCard = () => {
    isShowCard.value = !isShowCard.value;
};
</script>

<template>
    <div class="select" @click="showCard">
        {{ items.find((item) => item.value == selectOption)?.label }}
        <span class="iconfont icon-xuanzeqizhankai_o" style="margin-top: 3px" />
        <div class="card" v-show="isShowCard">
            <div
                class="option"
                v-for="(item, index) of items"
                :key="index"
                @click.stop="select(item.value)"
            >
                {{ item.label }}
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
@import '../iconfont/iconfont.css';

// 定制
$font-size: 15px;
$padding: 10px 10px;
$border-radius: 4px;
$background-color: #ededed;
$focus-background-color: #fff;
$box-shadow: 0 0 5px #5893ef;
$active-box-shadow: 0 0 5px #b2fa9e;
$height: 40px;
$gap: 3px;

.select {
    padding: $padding;
    border-radius: 4px;
    height: $height;
    background-color: transparent;
    border: 0.1mm solid $blue-10;
    cursor: pointer;
    position: relative;
    user-select: none;
    font-size: $font-size;
    display: inline-flex;
    align-items: center;
    gap: 5px;

    .iconfont {
        font-size: 13px;
    }

    .card {
        position: absolute;
        top: 100%;
        left: 0;
        min-width: 100%;
        background-color: #fff;
        border-radius: 4px;
        box-shadow: $box-shadow;
        z-index: 1;
        overflow: hidden;
        border: 0.1mm solid $blue-10;

        .option {
            white-space: nowrap;
            padding: 6px 8px;
            cursor: pointer;

            &:hover {
                background-color: $blue-2;
            }
        }
    }
}
</style>

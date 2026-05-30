<script setup lang="ts">
/**
 * 时间选择器 HH:mm
 */
import { computed, onMounted, onUnmounted, useTemplateRef } from 'vue';

const { time } = defineProps<{
    time: Date;
}>();

const emits = defineEmits<{
    (e: 'update:time', time: Date): void;
}>();
const timePicker = useTemplateRef<HTMLElement>('time-picker');
let isDragging = false; // 是否正在拖动
let startY = 0; // 鼠标按下时的初始Y坐标
let startScrollTop = 0; // 鼠标按下时的初始滚动位置（垂直）
let draggingDom: HTMLElement | null = null;
let isMoving = false;

// 鼠标按下事件
function handleMouseDown(event: MouseEvent) {
    draggingDom = event.currentTarget as HTMLElement;
    isDragging = true;
    startY = event.clientY;
    startScrollTop = draggingDom!.scrollTop;
}

// 鼠标移动事件
function handleMouseMove(event: MouseEvent) {
    if (!isDragging) return;
    isMoving = true;
    // 计算鼠标移动的距离
    const deltaY = event.clientY - startY;
    // 更新滚动位置
    draggingDom!.scrollTop = startScrollTop - deltaY;
}

// 鼠标释放事件
function handleMouseUp() {
    isDragging = false;
    setTimeout(() => {
        // 注意：不能用Promise
        isMoving = false;
    });
}

const hours = Array.from({ length: 24 }, (_, i) =>
    i.toString().padStart(2, '0'),
);
const minutes = Array.from({ length: 60 }, (_, i) =>
    i.toString().padStart(2, '0'),
);

let isInit = true;
let timer: ReturnType<typeof setTimeout>;

// 挂载事件监听
onMounted(() => {
    // 确认 .h和.m的scrollTop;
    const h = timePicker.value!.querySelector('.h') as HTMLElement;
    const m = timePicker.value!.querySelector('.m') as HTMLElement;
    function initScrollTop() {
        isInit = true;
        h.scrollTop = time.getHours() * (h.scrollHeight / (hours.length + 4));
        m.scrollTop =
            time.getMinutes() * (m.scrollHeight / (minutes.length + 4));
        setTimeout(() => {
            isInit = false;
        });
    }

    initScrollTop();

    // 没有这个计时器时，那么，当time-picker的父容器为display:none时，h和m的scrollTop会变成0
    // console.log(h.scrollHeight);   // 会一直打印出 大于零的数 也会 打印出 0， 为什么h.scrollHeight会变成0？因为h的父容器被隐藏了
    timer = setInterval(initScrollTop, 300);
    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
});

// 卸载事件监听
onUnmounted(() => {
    clearInterval(timer);
    document.removeEventListener('mousemove', handleMouseMove);
    document.removeEventListener('mouseup', handleMouseUp);
});

const scrollTOCenter = (e: MouseEvent) => {
    if (isMoving) return;
    const target = e.currentTarget as HTMLElement;
    const parent = target.parentElement as HTMLElement;
    const targetRect = target.getBoundingClientRect();
    const parentRect = parent.getBoundingClientRect();
    const targetCenter = targetRect.top + targetRect.height / 2;
    const parentCenter = parentRect.top + parentRect.height / 2;
    const scrollAmount = targetCenter - parentCenter;
    parent.scrollTop += scrollAmount;
};

const computeTimeString = computed({
    get() {
        const hour = String(time.getHours()).padStart(2, '0');
        const minute = String(time.getMinutes()).padStart(2, '0');
        return 'HH:mm'.replace('HH', hour).replace('mm', minute);
    },
    /**
     * 更新时间
     * @param value 格式为HH:mm
     */
    set(value: string) {
        const [hour, minute] = value.split(':');
        emits('update:time', new Date(0, 0, 0, Number(hour), Number(minute)));
    },
});

/**
 * 计算小时
 * 通过target的scrollTop来计算
 */
const computeHour = (e: Event) => {
    if (isInit) return;
    const target = e.currentTarget as HTMLElement;
    const hour = Math.round(
        target.scrollTop / (target.scrollHeight / (hours.length + 4)),
    );
    // 利用正则，将前面的两个数字replace为hour
    computeTimeString.value = computeTimeString.value.replace(
        /\d{2}/,
        hour.toString(),
    );
};

/**
 * 计算分钟
 */
const computeMinute = (e: Event) => {
    if (isInit) return;
    const target = e.currentTarget as HTMLElement;
    const minute = Math.round(
        target.scrollTop / (target.scrollHeight / (minutes.length + 4)),
    );
    // 利用正则，将后面的两个数字replace为minute
    computeTimeString.value = computeTimeString.value.replace(
        /\d{2}$/,
        minute.toString(),
    );
};
</script>

<template>
    <div class="time-picker" ref="time-picker">
        <div class="h" @mousedown="handleMouseDown" @scroll="computeHour">
            <div class="item" />
            <div class="item" />
            <div
                class="item"
                :style="{
                    transform: `scale(${time.getHours() - Number(h) == 0 ? 1 : 0.7})`,
                    color: time.getHours() - Number(h) == 0 ? '#000' : '#999',
                }"
                v-for="h of hours"
                :key="h"
                @click="scrollTOCenter"
            >
                {{ h }}
            </div>
            <div class="item" />
            <div class="item" />
        </div>
        <div class="m" @mousedown="handleMouseDown" @scroll="computeMinute">
            <div class="item" />
            <div class="item" />
            <div
                class="item"
                v-for="m of minutes"
                :key="m"
                :style="{
                    transform: `scale(${time.getMinutes() - Number(m) == 0 ? 1 : 0.7})`,
                    color: time.getMinutes() - Number(m) == 0 ? '#000' : '#999',
                }"
                @click="scrollTOCenter"
            >
                {{ m }}
            </div>
            <div class="item" />
            <div class="item" />
        </div>
        <div class="icon">:</div>
    </div>
</template>

<style scoped lang="scss">
.time-picker {
    $item-height: 27px;
    height: calc($item-height * 5);
    display: flex;
    justify-content: center;
    gap: 5px;
    position: relative;
    user-select: none; // 防止选中文本

    .icon {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
    }

    .h,
    .m {
        overflow-y: auto;
        overflow-x: hidden;
        scroll-snap-type: y mandatory;

        &::-webkit-scrollbar {
            display: none;
        }

        .item {
            height: $item-height;
            width: $item-height;
            display: grid;
            place-items: center;
            scroll-snap-align: center;
            scroll-snap-stop: always; // 不嫩跳过，必需要听一下
            transition:
                transform 0.3s,
                color 0.3s;
            overflow: hidden;
            cursor: pointer;
            font-size: 18px;
            transform-origin: 50% 50%;
        }
    }
}
</style>

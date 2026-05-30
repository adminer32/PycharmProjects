<script setup lang="ts">
/**
 * 模态对话框
 * 可通过内联样式修改层级
 */
import { onMounted, useTemplateRef, watch } from 'vue';

const modalRef = useTemplateRef<HTMLElement>('modal');

const {
    open,
    width = '500px',
    okText = '确定',
    cancelText = '取消',
    isWaitingOpen = false,
} = defineProps<{
    open: boolean;
    title?: string;
    width?: string;
    okText?: string;
    cancelText?: string;
    isWaitingOpen?: boolean; // 如果要做到一不打开而且 点击的位置合理 那就得控制这个变量
}>();

const lastXY = {
    x: 0,
    y: 0,
};

onMounted(() => {
    // 不可以使用 click 事件，因为使用 click 事件的话触发的时机会比 watch 晚，导致导致点击位置不能在 动画开始前被记录
    window.addEventListener(
        'mousedown',
        (e: MouseEvent) => {
            if (open || isWaitingOpen) return;
            lastXY.x = e.clientX;
            lastXY.y = e.clientY;
        },
        true,
    ); // true 表示捕获阶段
});

watch(
    () => open,
    (newVal) => {
        const centerBox = modalRef.value?.querySelector(
            '.center-box',
        ) as HTMLElement;
        if (newVal) {
            modalRef.value!.style.display = 'flex';
            /**
             * 记录最近一次点击
             */
            const animate = centerBox.animate(
                [
                    {
                        transform: `translate(${lastXY.x - window.innerWidth / 2}px, ${lastXY.y - window.innerHeight / 2}px) scale(0)`,
                    },
                    {
                        transform: 'translate(0,0) scale(1)',
                    },
                ],
                {
                    duration: 300,
                    easing: 'ease-in-out',
                },
            );
            animate.play();
        } else {
            const animate = centerBox.animate(
                [
                    {
                        transform: 'translate(0,0) scale(1)',
                    },
                    {
                        transform: `translate(${lastXY.x - window.innerWidth / 2}px, ${lastXY.y - window.innerHeight / 2}px) scale(0)`,
                    },
                ],
                {
                    duration: 300,
                    easing: 'ease-in-out',
                },
            );
            animate.play();
            animate.onfinish = () => {
                modalRef.value!.style.display = 'none';
            };
        }
        emits('update:isWaitingOpen', false);
    },
);

const emits = defineEmits<{
    (e: 'ok'): void;
    (e: 'close'): void;
    (e: 'cancel'): void;
    (e: 'update:open', open: boolean): void;
    (e: 'update:isWaitingOpen', open: boolean): void;
}>();

const close = () => {
    emits('close');
    emits('update:open', false);
};

const cancel = () => {
    emits('cancel');
    emits('update:open', false);
};
</script>

<template>
    <!--    <Teleport to="body">-->
    <div class="modal" ref="modal">
        <div class="center-box" :style="{ width: width }">
            <div class="header">
                <div class="title" v-if="title">{{ title }}</div>
                <span class="iconfont icon-guanbi_o" @click="close"></span>
            </div>
            <slot />
            <div class="btn-box">
                <button class="cancel-btn" @click="cancel">
                    {{ cancelText }}
                </button>
                <button @click="emits('ok')">
                    {{ okText }}
                </button>
            </div>
        </div>
    </div>
    <!--    </Teleport>-->
</template>

<style scoped lang="scss">
@use 'variable';
@import '../../iconfont/iconfont.css';

.modal {
    display: none;
    position: fixed;
    inset: 0;
    background-color: rgba(0, 0, 0, 0.2);
    justify-content: center;
    align-items: center;
    font-size: 15px;
    z-index: 2000; // 参考 Element Plus 官方默认配置

    .center-box {
        background-color: variable.$background-color;
        box-shadow: variable.$box-shadow;
        border-radius: variable.$border-radius;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        gap: variable.$gap;
        padding: variable.$gap;

        .header {
            display: flex;
            flex-direction: row;
            align-items: center;
            padding: 3px 5px;

            .title {
                font-size: 17px;
            }

            .iconfont {
                color: #000;
                font-weight: bold;
                font-size: 20px;
                cursor: pointer;
                margin-left: auto;
            }
        }

        .btn-box {
            display: flex;
            justify-content: flex-end;
            padding-top: 10px;
            gap: 10px;

            button {
                outline: none;
                border: none;
                padding: 5px 15px;
                background-color: rgb(48, 150, 255);
                box-shadow: 0 0 5px rgb(153, 190, 246);
                color: #fff;
                border-radius: 5px;
                display: flex;
                align-items: center;
                justify-content: center;
                transition: background-color 0.2s ease-in-out;
                font-size: 14px;
                cursor: pointer;
                user-select: none;
                overflow: hidden;
                white-space: nowrap;

                &:hover {
                    background-color: rgb(83, 168, 255);
                }
            }

            .cancel-btn {
                background-color: transparent;
                color: #000;

                &:hover {
                    background-color: rgb(244, 244, 244);
                }
            }
        }
    }
}
</style>

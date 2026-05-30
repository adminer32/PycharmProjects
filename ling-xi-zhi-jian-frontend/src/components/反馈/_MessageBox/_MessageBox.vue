<template>
    <div class="message-box">
        <span
            v-if="type == 'success'"
            class="success iconfont icon-success_fill"
        />
        <span v-if="type == 'error'" class="error iconfont icon-delete_fill" />
        <span
            v-if="type == 'warning'"
            class="warning iconfont icon-prompt_fill"
        />
        <span v-if="type == 'info'" class="info iconfont icon-warning_fill" />
        <div class="msg" :class="type">{{ msg }}</div>
    </div>
</template>

<script setup lang="ts">
defineProps<{
    type: 'success' | 'error' | 'warning' | 'info';
    msg: string;
}>();
</script>

<style lang="scss" scoped>
@import 'iconfont/iconfont.css';

.message-box {
    background-color: #fff;
    border-radius: 4px;
    padding: 5px 7px;
    box-shadow: 0 0 9px #99999977;
    display: flex;
    align-items: center;

    .iconfont {
        font-size: 20px;
        margin-right: 5px;

        &.success {
            color: #31bc31;
        }

        &.error {
            color: #fb6868;
        }

        &.warning {
            color: #ffb74a;
        }

        &.info {
            color: #2c6eff;
        }
    }

    .msg {
        font-size: 14px;
        color: #333333;
    }
}
</style>

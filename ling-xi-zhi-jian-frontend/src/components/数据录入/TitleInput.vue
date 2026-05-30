<script setup lang="ts">
/**
 * 双击可编辑的输入框
 */
import { ref } from 'vue';

const emits = defineEmits<{
    modify: [string];
}>();
const { text } = defineProps<{
    text: string;
}>();
const showInput = ref(false);
const defaultText = ref(text);

const onBlur = () => {
    showInput.value = false;
    emits('modify', defaultText.value);
};
</script>

<template>
    <div class="title-input">
        <input
            v-if="showInput"
            type="text"
            v-model="defaultText"
            @blur="onBlur"
        />
        <div class="text" @dblclick="showInput = true" v-else>
            {{ text }}
        </div>
    </div>
</template>

<style scoped lang="scss">
.title-input {
    font-size: 14px;

    input {
        width: 100%;
        outline: none;
    }
}
</style>

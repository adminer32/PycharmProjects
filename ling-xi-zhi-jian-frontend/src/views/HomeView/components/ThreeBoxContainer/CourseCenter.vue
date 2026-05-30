<script setup lang="ts">
import router from '@/router';
import Button from '@/components/通用/Button/Button.vue';
</script>
<template>
    <!--    基础入门-->
    <div class="basic-introduction">
        <div class="left">
            <img
                class="logo"
                src="@/assets/home_view/ThreeBoxContainer/basie_introduction.png"
                alt=""
            />
            <div class="title">
                AI学习伴侣<br />
                让知识点在课程体系里无缝流动
            </div>
            <p>
                选择基础课程入门，综合课程提升，为你自动生成课程概括、要点总结、知识脉络图等AI课程笔记功能，从概念划词延展到综合应用，一划唤醒知识体系的深度缝合能力。
            </p>
            <Button @click="router.push({ name: 'aiLearnFriendView' })"
                >了解更多</Button
            >
        </div>
        <div class="right">
            <img
                src="@/assets/home_view/ThreeBoxContainer/comprehensive_course_right.png"
                alt=""
            />
        </div>
    </div>
</template>

<style scoped lang="scss">
@use 'variable';

.basic-introduction {
    @include variable.style;

    .right {
        border-radius: $border-radius;
        box-shadow: $box-shadow;
        overflow: hidden;

        img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }
    }
}
</style>

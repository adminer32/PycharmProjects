<script setup lang="ts">
import router from '@/router';
import Button from '@/components/通用/Button/Button.vue';
import { onMounted, onUnmounted, useTemplateRef } from 'vue';
import * as echarts from 'echarts/core';

let motionAnalysisIconChart: echarts.ECharts | null;

const motionAssessmentRef = useTemplateRef<HTMLElement>('motion-assessment');
const initMotionAnalysisIconChart = () => {
    const dom = motionAssessmentRef.value?.querySelector(
        '.right',
    ) as HTMLElement;
    motionAnalysisIconChart = echarts.init(dom);
};
const updateMotionAnalysisIconChart = () => {
    // 模拟数据生成函数（限制在60-80之间）
    const generateData = () => {
        return [
            { value: Math.floor(Math.random() * 20) + 60, name: '髋关节' }, // 60-80
            { value: Math.floor(Math.random() * 20) + 60, name: '膝关节' }, // 60-80
            { value: Math.floor(Math.random() * 20) + 60, name: '踝关节' }, // 60-80
            { value: Math.floor(Math.random() * 20) + 60, name: '抬脚高度' }, // 60-80
        ];
    };

    // 检查图表实例是否存在
    motionAnalysisIconChart?.setOption({
        radar: {
            indicator: [
                { name: '髋关节', max: 100 },
                { name: '膝关节', max: 100 },
                { name: '踝关节', max: 100 },
                { name: '抬脚高度', max: 100 },
            ],
            center: ['50%', '50%'],
            radius: '80%',
            startAngle: 90,
            splitNumber: 5,
            axisName: {
                fontSize: 12,
                color: '#ffffff',
            },
            axisLine: {
                lineStyle: {
                    color: '#ddd',
                },
            },
            splitLine: {
                lineStyle: {
                    color: '#FFF',
                },
            },
            splitArea: {},
        },
        series: [
            {
                name: '运动分析',
                type: 'radar',
                data: [
                    {
                        value: generateData().map((item) => item.value),
                        name: '实时数据',
                        areaStyle: {
                            color: new echarts.graphic.RadialGradient(
                                0.5,
                                0.5,
                                1,
                                [
                                    {
                                        offset: 0,
                                        color: 'rgba(84, 112, 198, 0.9)',
                                    },
                                    {
                                        offset: 1,
                                        color: 'rgba(84, 112, 198, 0.2)',
                                    },
                                ],
                            ),
                        },
                        lineStyle: {
                            width: 2,
                            color: '#5470c6',
                        },
                        itemStyle: {
                            color: '#5470c6',
                        },
                        symbol: 'circle',
                    },
                ],
            },
        ],
    });
};
let timer: ReturnType<typeof setTimeout>;

onMounted(() => {
    initMotionAnalysisIconChart();
    updateMotionAnalysisIconChart();
    timer = setInterval(updateMotionAnalysisIconChart, 1500);
});

onUnmounted(() => {
    clearInterval(timer);
});
</script>
<template>
    <!--    基础入门-->
    <div class="motion-assessment" ref="motion-assessment">
        <div class="left">
            <img
                class="logo"
                src="@/assets/home_view/ThreeBoxContainer/comprehensive_course_left.png"
                alt=""
            />
            <div class="title">
                动作评估<br />
                让训练动作在学习中科学进阶
            </div>
            <p>
                使用YOLO骨骼关键点检测算法，智能解析用户视频踢毽子动作，让空中人体关节动作误差无所遁形，助你建立运动力线最优解。
            </p>
            <Button @click="router.push({ name: 'motionAssessmentView' })"
                >了解更多</Button
            >
        </div>
        <div class="right" />
    </div>
</template>

<style scoped lang="scss">
@use 'variable';

.motion-assessment {
    @include variable.style;

    .right {
        padding: 20px;
        flex: 1;
        overflow: hidden;
        box-sizing: border-box;
    }
}
</style>

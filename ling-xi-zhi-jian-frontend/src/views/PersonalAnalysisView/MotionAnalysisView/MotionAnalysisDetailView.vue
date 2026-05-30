<script setup lang="ts">
/**
 * 动作分析详情页面
 */
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useMotionAssessmentStore } from '@/stores/motionAssessmentStore';
import * as echarts from 'echarts/core';
import { BarChart, LineChart, RadarChart } from 'echarts/charts';
import {
    TitleComponent,
    TooltipComponent,
    LegendComponent,
    GridComponent
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

echarts.use([
    BarChart,
    LineChart,
    RadarChart,
    TitleComponent,
    TooltipComponent,
    LegendComponent,
    GridComponent,
    CanvasRenderer
]);

const route = useRoute();
const router = useRouter();
const motionAssessmentStore = useMotionAssessmentStore();

const historyId = computed(() => Number(route.query.historyId));
const analysisData = ref<any>(null);
const loading = ref(true);

interface ChartData {
    selfLeftXData: number[];
    selfLeftYData: number[];
    selfRightXData: number[];
    selfRightYData: number[];
    motionName: string;
    videoUrl: string;
    taskId: string;
    result: {
        stabilityScore: number;
        proficiencyScore: number;
        fluencyScore: number;
        overallScore: number;
        stabilityImprovement: string;
        proficiencyEnhancement: string;
        fluencyPromotion: string;
        generalAdvice: string;
    };
}

const currentAnalysis = ref<ChartData>({
    selfLeftXData: [],
    selfLeftYData: [],
    selfRightXData: [],
    selfRightYData: [],
    motionName: '',
    videoUrl: '',
    taskId: '',
    result: {
        stabilityScore: 0,
        proficiencyScore: 0,
        fluencyScore: 0,
        overallScore: 0,
        stabilityImprovement: '',
        proficiencyEnhancement: '',
        fluencyPromotion: '',
        generalAdvice: ''
    }
});

const initCharts = () => {
    if (!analysisData.value) return;

    // 1. 抬腿高度图表
    initElevationChart();
    // 2. 本次平均评分维度（雷达图）
    initScoreRadarChart();
    // 3. 该动作本班级近期评分分布
    initClassScoreDistributionChart();
    // 4. 本班大部分不足的点
    initClassWeakPointsChart();
    // 5. 个人该动作的历史分布情况
    initPersonalHistoryChart();
};

// 1. 抬腿高度图表
const initElevationChart = () => {
    const chartDom = document.getElementById('elevation-chart');
    if (!chartDom) return;
    
    const myChart = echarts.init(chartDom);
    const leftData = currentAnalysis.value.selfLeftXData;
    const rightData = currentAnalysis.value.selfRightXData;
    
    const option = {
        title: {
            text: '抬腿高度变化',
            left: 'center',
            top: 10,
            textStyle: { fontSize: 16, fontWeight: 600 }
        },
        tooltip: { trigger: 'axis' },
        legend: {
            data: ['左腿', '右腿'],
            top: 45
        },
        grid: {
            left: '10%',
            right: '10%',
            bottom: '15%',
            top: '20%'
        },
        xAxis: {
            type: 'category',
            data: leftData.map((_, i) => `第${i + 1}次`)
        },
        yAxis: {
            type: 'value',
            name: '高度(cm)'
        },
        series: [
            {
                name: '左腿',
                type: 'line',
                data: leftData,
                smooth: true,
                itemStyle: { color: '#5470c6' },
                areaStyle: { color: 'rgba(84, 112, 198, 0.3)' }
            },
            {
                name: '右腿',
                type: 'line',
                data: rightData,
                smooth: true,
                itemStyle: { color: '#ee6666' },
                areaStyle: { color: 'rgba(238, 102, 102, 0.3)' }
            }
        ]
    };
    
    myChart.setOption(option);
};

// 2. 本次平均评分维度（雷达图）
const initScoreRadarChart = () => {
    const chartDom = document.getElementById('score-radar-chart');
    if (!chartDom) return;
    
    const myChart = echarts.init(chartDom);
    const result = currentAnalysis.value.result;
    
    const option = {
        title: {
            text: '本次平均评分维度',
            left: 'center',
            top: 10,
            textStyle: { fontSize: 16, fontWeight: 600 }
        },
        tooltip: {},
        radar: {
            indicator: [
                { name: '稳定性', max: 100 },
                { name: '熟练度', max: 100 },
                { name: '流畅度', max: 100 },
                { name: '综合评分', max: 100 }
            ],
            radius: '60%'
        },
        series: [{
            type: 'radar',
            data: [{
                value: [
                    result.stabilityScore,
                    result.proficiencyScore,
                    result.fluencyScore,
                    result.overallScore
                ],
                name: '评分维度',
                areaStyle: { color: 'rgba(24, 144, 255, 0.4)' },
                lineStyle: { color: '#1890ff' },
                itemStyle: { color: '#1890ff' }
            }]
        }]
    };
    
    myChart.setOption(option);
};

// 3. 该动作本班级近期评分分布
const initClassScoreDistributionChart = () => {
    const chartDom = document.getElementById('class-score-chart');
    if (!chartDom) return;
    
    const myChart = echarts.init(chartDom);
    
    // 模拟班级数据
    const classScores = [75, 82, 68, 90, 78, 85, 72, 88, 76, 80, 83, 77];
    
    const option = {
        title: {
            text: '该动作本班级近期评分分布',
            left: 'center',
            top: 10,
            textStyle: { fontSize: 16, fontWeight: 600 }
        },
        tooltip: { trigger: 'axis' },
        grid: {
            left: '10%',
            right: '10%',
            bottom: '15%',
            top: '20%'
        },
        xAxis: {
            type: 'category',
            data: classScores.map((_, i) => `学生${i + 1}`)
        },
        yAxis: {
            type: 'value',
            name: '评分',
            max: 100
        },
        series: [{
            type: 'bar',
            data: classScores,
            itemStyle: {
                color: (params: any) => {
                    return params.value >= 80 ? '#52c41a' : 
                           params.value >= 60 ? '#ffa940' : '#ff7875';
                }
            }
        }]
    };
    
    myChart.setOption(option);
};

// 4. 本班大部分不足的点
const initClassWeakPointsChart = () => {
    const chartDom = document.getElementById('weak-points-chart');
    if (!chartDom) return;
    
    const myChart = echarts.init(chartDom);
    
    const weakPoints = [
        { name: '髋关节发力', value: 35 },
        { name: '膝关节角度', value: 28 },
        { name: '踝关节稳定性', value: 22 },
        { name: '抬脚高度', value: 15 }
    ];
    
    const option = {
        title: {
            text: '本班大部分不足的点',
            left: 'center',
            top: 10,
            textStyle: { fontSize: 16, fontWeight: 600 }
        },
        tooltip: { trigger: 'item' },
        series: [{
            type: 'pie',
            radius: ['40%', '70%'],
            data: weakPoints,
            label: {
                formatter: '{b}: {d}%'
            },
            itemStyle: {
                color: (params: any) => {
                    const colors = ['#ff7875', '#ffa940', '#1890ff', '#52c41a'];
                    return colors[params.dataIndex] || '#1890ff';
                }
            }
        }]
    };
    
    myChart.setOption(option);
};

// 5. 个人该动作的历史分布情况
const initPersonalHistoryChart = () => {
    const chartDom = document.getElementById('personal-history-chart');
    if (!chartDom) return;
    
    const myChart = echarts.init(chartDom);
    
    // 模拟个人历史数据
    const historyScores = [
        { date: '3/1', score: 72 },
        { date: '3/8', score: 75 },
        { date: '3/15', score: 78 },
        { date: '3/22', score: 76 },
        { date: '3/29', score: 82 },
        { date: '4/5', score: currentAnalysis.value.result.overallScore }
    ];
    
    const option = {
        title: {
            text: '个人该动作的历史分布情况',
            left: 'center',
            top: 10,
            textStyle: { fontSize: 16, fontWeight: 600 }
        },
        tooltip: { trigger: 'axis' },
        grid: {
            left: '10%',
            right: '10%',
            bottom: '15%',
            top: '20%'
        },
        xAxis: {
            type: 'category',
            data: historyScores.map(h => h.date)
        },
        yAxis: {
            type: 'value',
            name: '评分',
            min: 50,
            max: 100
        },
        series: [{
            type: 'line',
            data: historyScores.map(h => h.score),
            smooth: true,
            areaStyle: { color: 'rgba(24, 144, 255, 0.3)' },
            itemStyle: { color: '#1890ff' },
            markPoint: {
                data: [
                    { type: 'max', name: '最高分' },
                    { type: 'min', name: '最低分' }
                ]
            },
            markLine: {
                data: [{ type: 'average', name: '平均分' }]
            }
        }]
    };
    
    myChart.setOption(option);
};

const goBack = () => {
    router.back();
};

const loadAnalysisData = async () => {
    loading.value = true;
    try {
        const res = await motionAssessmentStore.getAnalysisHistory(historyId.value);
        if (res && res.data) {
            analysisData.value = res.data;
            currentAnalysis.value = {
                selfLeftXData: res.data.selfLeftXData || [],
                selfLeftYData: res.data.selfLeftYData || [],
                selfRightXData: res.data.selfRightXData || [],
                selfRightYData: res.data.selfRightYData || [],
                motionName: res.data.motionName || '',
                videoUrl: res.data.videoUrl || '',
                taskId: res.data.taskId || '',
                result: res.data.result || {
                    stabilityScore: 0,
                    proficiencyScore: 0,
                    fluencyScore: 0,
                    overallScore: 0,
                    stabilityImprovement: '',
                    proficiencyEnhancement: '',
                    fluencyPromotion: '',
                    generalAdvice: ''
                }
            };
            
            setTimeout(() => {
                initCharts();
            }, 100);
        }
    } catch (e) {
        console.error('加载分析数据失败:', e);
    } finally {
        loading.value = false;
    }
};

onMounted(() => {
    loadAnalysisData();
});
</script>

<template>
    <div class="motion-analysis-detail">
        <div class="detail-header">
            <button class="back-btn" @click="goBack">
                <span class="back-icon">←</span>
                返回
            </button>
            <h1 class="page-title">动作分析详情</h1>
            <div class="action-info" v-if="analysisData">
                <span class="action-tag">{{ currentAnalysis.motionName }}</span>
                <span class="score-tag">综合评分: {{ currentAnalysis.result.overallScore }}分</span>
            </div>
        </div>

        <div class="loading-container" v-if="loading">
            <div class="loading-spinner"></div>
            <p>正在加载分析数据...</p>
        </div>

        <div class="charts-container" v-else>
            <div class="charts-row">
                <div class="chart-card">
                    <div id="elevation-chart" class="chart-item"></div>
                </div>
                <div class="chart-card">
                    <div id="score-radar-chart" class="chart-item"></div>
                </div>
            </div>

            <div class="charts-row">
                <div class="chart-card">
                    <div id="class-score-chart" class="chart-item"></div>
                </div>
                <div class="chart-card">
                    <div id="weak-points-chart" class="chart-item"></div>
                </div>
            </div>

            <div class="charts-row">
                <div class="chart-card full-width">
                    <div id="personal-history-chart" class="chart-item"></div>
                </div>
            </div>

            <div class="advice-card">
                <h3 class="advice-title">📝 后续如何提高</h3>
                <div class="advice-content">
                    <div class="advice-item" v-if="currentAnalysis.result.stabilityImprovement">
                        <span class="advice-label">稳定性改进：</span>
                        <span class="advice-text">{{ currentAnalysis.result.stabilityImprovement }}</span>
                    </div>
                    <div class="advice-item" v-if="currentAnalysis.result.proficiencyEnhancement">
                        <span class="advice-label">能力增强：</span>
                        <span class="advice-text">{{ currentAnalysis.result.proficiencyEnhancement }}</span>
                    </div>
                    <div class="advice-item" v-if="currentAnalysis.result.fluencyPromotion">
                        <span class="advice-label">流畅度提升：</span>
                        <span class="advice-text">{{ currentAnalysis.result.fluencyPromotion }}</span>
                    </div>
                    <div class="advice-item" v-if="currentAnalysis.result.generalAdvice">
                        <span class="advice-label">综合建议：</span>
                        <span class="advice-text">{{ currentAnalysis.result.generalAdvice }}</span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
.motion-analysis-detail {
    min-height: 100vh;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 20px;

    .detail-header {
        display: flex;
        align-items: center;
        gap: 20px;
        margin-bottom: 24px;
        background: rgba(255, 255, 255, 0.95);
        padding: 16px 24px;
        border-radius: 16px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);

        .back-btn {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 10px 18px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border: none;
            border-radius: 25px;
            color: white;
            font-size: 14px;
            cursor: pointer;
            transition: all 0.3s;

            &:hover {
                transform: translateY(-2px);
                box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
            }

            .back-icon {
                font-size: 18px;
            }
        }

        .page-title {
            flex: 1;
            margin: 0;
            font-size: 22px;
            font-weight: 700;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
        }

        .action-info {
            display: flex;
            align-items: center;
            gap: 12px;

            .action-tag {
                padding: 8px 16px;
                background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                border-radius: 20px;
                color: white;
                font-size: 14px;
                font-weight: 600;
            }

            .score-tag {
                padding: 8px 16px;
                background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
                border-radius: 20px;
                color: white;
                font-size: 14px;
                font-weight: 600;
            }
        }
    }

    .loading-container {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 400px;
        background: rgba(255, 255, 255, 0.95);
        border-radius: 16px;

        .loading-spinner {
            width: 50px;
            height: 50px;
            border: 4px solid rgba(102, 126, 234, 0.2);
            border-top-color: #667eea;
            border-radius: 50%;
            animation: spin 1s linear infinite;
            margin-bottom: 16px;
        }

        p {
            color: #666;
            font-size: 16px;
        }

        @keyframes spin {
            to { transform: rotate(360deg); }
        }
    }

    .charts-container {
        .charts-row {
            display: flex;
            gap: 20px;
            margin-bottom: 20px;

            .chart-card {
                flex: 1;
                background: rgba(255, 255, 255, 0.95);
                border-radius: 16px;
                padding: 20px;
                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);

                &.full-width {
                    flex: none;
                    width: 100%;
                }

                .chart-item {
                    width: 100%;
                    height: 320px;
                }
            }
        }

        .advice-card {
            background: rgba(255, 255, 255, 0.95);
            border-radius: 16px;
            padding: 24px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);

            .advice-title {
                margin: 0 0 20px 0;
                font-size: 18px;
                font-weight: 700;
                color: #333;
            }

            .advice-content {
                display: flex;
                flex-direction: column;
                gap: 16px;

                .advice-item {
                    display: flex;
                    align-items: flex-start;
                    gap: 12px;
                    padding: 16px;
                    background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.08) 100%);
                    border-radius: 12px;
                    border-left: 4px solid #667eea;

                    .advice-label {
                        font-weight: 600;
                        color: #667eea;
                        white-space: nowrap;
                    }

                    .advice-text {
                        color: #666;
                        line-height: 1.6;
                    }
                }
            }
        }
    }
}
</style>

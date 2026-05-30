import { defineStore } from 'pinia';
import type { Result } from '@/types/globel';
import HttpUtil from '@/utils/HttpUtil';
import * as echarts from 'echarts';
import { getScoreByMotionNameApi } from '@/api/personal_assessment/getScoreByMotionNameApi';

interface Trend {
    analysisDate: string;
    avgOverallScore: number;
    avgStabilityScore: number;
    avgFluencyScore: number;
    avgProficiencyScore: number;
}

interface UpdateStateMap {
    [item: string]: {
        totalCount: number;
        todayCount: number;
        last30DaysCount: number;
    };
}

export interface Score {
    avgFluencyScore: number;
    avgOverallScore: number;
    avgProficiencyScore: number;
    avgStabilityScore: number;
}

let historyTrendChart: echarts.ECharts | null = null;
let uploadStatsChart: echarts.ECharts | null = null;

export const usePersonalAssessmentStore = defineStore(
    'usePersonalAssessmentStore',
    {
        state: () => {
            return {
                historyTrend: [] as Trend[],
                updateState: {} as UpdateStateMap,
                motionList: [] as {
                    name: string;
                    id: number;
                }[],
                selectedMotionIndex: 0,
            };
        },
        actions: {
            getHistoryTrendData() {
                return new Promise<void>((resolve, reject) => {
                    HttpUtil.get<Result<Trend[]>>(
                        '/api/visualization/queryHistoricalTrends',
                    ).then((res) => {
                        if (res.status == 'success') {
                            this.historyTrend = res.data;
                            resolve();
                        } else {
                            reject(res.message);
                        }
                    });
                });
            },

            getUpdateStateData() {
                return new Promise<void>((resolve, reject) => {
                    HttpUtil.get<Result<UpdateStateMap>>(
                        '/api/visualization/getStatsByCreateBy',
                    ).then((res) => {
                        if (res.status == 'success') {
                            this.updateState = res.data;
                            resolve();
                        } else {
                            reject(res.message);
                        }
                    });
                });
            },

            getAllMotion() {
                return new Promise<void>((resolve, reject) => {
                    HttpUtil.get<
                        Result<
                            {
                                name: string;
                                id: number;
                            }[]
                        >
                    >('/api/content/queryCategory').then((res) => {
                        if (res.status == 'success') {
                            this.motionList = res.data;
                            resolve();
                        } else {
                            reject(res.message);
                        }
                    });
                });
            },

            /**
             * 根据动作名称查询分数
             */
            queryScoreByMotionName(motionName: string) {
                return new Promise<Score>((resolve, reject) => {
                    getScoreByMotionNameApi(motionName).then((res) => {
                        if (res.status == 'success') {
                            resolve(res.data);
                        } else {
                            reject(res.message);
                        }
                    });
                });
            },

            initUploadStatsChart(dom: HTMLElement) {
                // 如果已经存在图表实例，先销毁它
                uploadStatsChart?.dispose();
                // 创建新的图表实例
                uploadStatsChart = echarts.init(dom);
                const option: echarts.EChartsOption = {
                    title: {
                        text: '上传与分析统计',
                        left: 'center',
                    },
                    tooltip: {
                        trigger: 'item',
                    },
                    legend: {
                        data: ['动作评估', 'AI学习伴侣'],
                        top: 'bottom',
                    },
                    xAxis: {
                        type: 'category',
                        data: ['总次数', '最近30天', '今日'],
                    },
                    yAxis: {
                        type: 'value',
                    },
                    series: [
                        {
                            name: '动作评估',
                            type: 'bar',
                            data: [],
                        },
                        {
                            name: 'AI学习伴侣',
                            type: 'bar',
                            data: [],
                        },
                    ],
                };

                // 使用刚指定的配置项和数据显示图表
                uploadStatsChart?.setOption(option);
            },
            updateUploadStatsChart() {
                const option: echarts.EChartsOption = {
                    series: [
                        {
                            name: '动作评估',
                            type: 'bar',
                            label: {
                                show: true,
                                position: 'top',
                            },
                            itemStyle: {
                                color: '#5488d6',
                            },
                            data: [
                                this.updateState['cardStats']?.totalCount,
                                this.updateState['cardStats']?.last30DaysCount,
                                this.updateState['cardStats']?.todayCount,
                            ],
                        },
                        {
                            name: 'AI学习伴侣',
                            type: 'bar',
                            label: {
                                show: true,
                                position: 'top',
                            },
                            itemStyle: {
                                color: '#CCEBFF',
                            },
                            data: [
                                this.updateState['noteStats']?.totalCount,
                                this.updateState['noteStats']?.last30DaysCount,
                                this.updateState['noteStats']?.todayCount,
                            ],
                        },
                    ],
                };
                // 使用刚指定的配置项和数据显示图表
                uploadStatsChart?.setOption(option);
            },

            initHistoryTrendChart(dom: HTMLElement) {
                // 如果已经存在图表实例，先销毁它
                historyTrendChart?.dispose();
                // 创建新的图表实例
                historyTrendChart = echarts.init(dom);

                // 配置图表选项
                const option: echarts.EChartsOption = {
                    title: {
                        text: '历史分析趋势',
                        left: 'center',
                    },
                    tooltip: {
                        trigger: 'axis',
                    },
                    toolbox: {
                        feature: {
                            saveAsImage: {},
                        },
                    },
                    legend: {
                        data: ['熟练度', '稳定性', '流畅性', '整体评分'],
                        top: 'bottom',
                    },
                    xAxis: {
                        type: 'category',
                        boundaryGap: false,
                    },
                    yAxis: {
                        type: 'value',
                    },
                    series: [
                        {
                            name: '熟练度',
                            type: 'line',
                            data: [], // 示例数据
                        },
                        {
                            name: '稳定性',
                            type: 'line',
                            data: [], // 示例数据
                        },
                        {
                            name: '流畅性',
                            type: 'line',
                            data: [], // 示例数据
                        },
                        {
                            name: '整体评分',
                            type: 'line',
                            data: [], // 示例数据
                        },
                    ],
                };
                historyTrendChart?.setOption(option);
                window.addEventListener('resize', () => {
                    historyTrendChart?.resize();
                });
            },
            updateHistoryTrendChart() {
                // 配置图表选项
                const option: echarts.EChartsOption = {
                    xAxis: {
                        data: this.historyTrend.map(
                            (item) => item.analysisDate,
                        ),
                    },
                    series: [
                        {
                            name: '熟练度',
                            type: 'line',
                            data: this.historyTrend.map(
                                (item) => item.avgProficiencyScore,
                            ),
                        },
                        {
                            name: '稳定性',
                            type: 'line',
                            data: this.historyTrend.map(
                                (item) => item.avgStabilityScore,
                            ),
                        },
                        {
                            name: '流畅性',
                            type: 'line',
                            data: this.historyTrend.map(
                                (item) => item.avgFluencyScore,
                            ),
                        },
                        {
                            name: '整体评分',
                            type: 'line',
                            data: this.historyTrend.map(
                                (item) => item.avgOverallScore,
                            ),
                        },
                    ],
                };
                historyTrendChart?.setOption(option);
            },
        },
    },
);

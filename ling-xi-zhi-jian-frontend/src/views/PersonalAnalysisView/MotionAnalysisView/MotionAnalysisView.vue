<script setup lang="ts">
/**
 * 动作评估页面 ———— 三大核心页面之一
 */

import {
    computed,
    nextTick,
    onMounted,
    onUnmounted,
    reactive,
    ref,
    useTemplateRef,
} from 'vue';
import { type ECharts, init } from 'echarts';
import { useRouter } from 'vue-router';
import {
    type History,
    useMotionAssessmentStore,
} from '@/stores/motionAssessmentStore';
import type { PracticeRecord } from '@/api/learning/learningApi';
import { getMotionAssessmentHistoryListApi } from '@/api/motion_assessment/getMotionAssessmentHistoryListApi';
import MessageUtil from '@/utils/MessageUtil';
import Modal from '@/components/反馈/Modal/Modal.vue';
import InputText from '@/components/数据录入/input/input/InputText.vue';
import ChatController from '@/views/PersonalAnalysisView/MotionAnalysisView/ChatController.vue';
import Button from '@/components/通用/Button/Button.vue';
import AICalendar from '@/views/PersonalAnalysisView/MotionAnalysisView/AICalendar.vue';
import type { ScheduleItem } from '@/stores/scheduleStore';
import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel';

import { generateTrainingPlanApi } from '@/api/motion_assessment/generateTrainingPlanApi';

const GVHMR_API_BASE_URL = '/gvhmr';
const router = useRouter();

interface ActionDetail {
    action_id: number;
    start_frame: number;
    end_frame: number;
    peak_frame: number;
    side: 'left' | 'right';
    slice_path?: string;
}

interface EvaluationResult {
    task_id: string;
    status: 'queued' | 'processing' | 'completed' | 'failed' | 'revoked';
    progress?: number;
    result?: EvaluationResultData;
    error?: string | null;
}

interface EvaluationResultData {
    student_info?: {
        student_id: string;
        action: string;
        source: string;
    };
    status: string;
    video_path?: string;
    action_count: number;
    actions: ActionDetail[];
    eval_results: EvalResult[];
    elevation_records: ElevationRecord[];
    threshold: number;
    json_path?: string;
}

interface ActionDetail {
    action_id: number;
    start_frame: number;
    end_frame: number;
    peak_frame: number;
    side: 'left' | 'right';
    slice_path?: string;
}

interface EvalResult {
    status: string;
    metrics?: {
        hip_angle_error: number;
        knee_angle_error: number;
        ankle_angle_error: number;
        foot_height_error: number;
    };
    pred_peak_frame: number;
    gt_peak_frame: number;
    action_type: string;
    side: 'left' | 'right';
    feedback?: {
        '综合评分': number;
        action_type: string;
        side: string;
        feedback_detail?: {
            '髋关节'?: { 评分: number };
            '膝关节'?: { 评分: number };
            '踝关节'?: { 评分: number };
            '抬脚高度'?: { 评分: number };
        };
    };
}

interface ElevationRecord {
    frame: number;
    left: number;
    right: number;
    max: number;
    is_raised: boolean;
}

const submitVideoForEvaluation = async (
    videoFile: File,
    action: string = '盘踢'
): Promise<{ task_id: string; status: string }> => {
    const formData = new FormData();
    formData.append('video', videoFile);
    formData.append('action', action);

    const response = await fetch(`${GVHMR_API_BASE_URL}/api/v1/evaluate`, {
        method: 'POST',
        body: formData,
    });

    if (!response.ok) {
        throw new Error(`API error: ${response.status}`);
    }

    return response.json();
};

const queryEvaluationResult = async (taskId: string): Promise<EvaluationResult> => {
    const response = await fetch(`${GVHMR_API_BASE_URL}/api/v1/result/${taskId}`);
    if (!response.ok) {
        throw new Error(`API error: ${response.status}`);
    }
    return response.json();
};

const getScoreColor = (score: number): string => {
    if (score >= 80) return '#52c41a';
    if (score >= 60) return '#faad14';
    return '#ff4d4f';
};

const onSelectRecord = (record: PracticeRecord) => {
    if (record.result_data?.elevation_records) {
        const formattedRecords: ElevationRecord[] = record.result_data.elevation_records.map((r: { frame: number; left: number; right: number }) => ({
            frame: r.frame,
            left: r.left,
            right: r.right,
            max: Math.max(r.left, r.right),
            is_raised: r.left > 0 || r.right > 0,
        }));
        elevationRecords.value = formattedRecords;
        updateX1Chart(`${record.action_type.replace('-default', '')}抬脚高度变化`, formattedRecords);
    }
    
    const actions = (record.result_data?.actions as ActionDetail[]) || [];
    const actionScores = record.result_data?.action_scores || [];
    const jointScores = record.result_data?.joint_scores || [];
    
    if (actions.length > 0) {
        currentActions.value = actions;
    }
    if (actionScores.length > 0) {
        currentScores.value = actionScores;
    }
    if (jointScores.length > 0) {
        currentJointScores.value = jointScores;
    }
    
    if (actionScores.length > 0 && actions.length > 0) {
        updateY1Chart(`${record.action_type.replace('-default', '')}动作评分`, actions, actionScores);
    } else if (record.result_data?.overall_score !== undefined) {
        const fallbackActions: ActionDetail[] = [{
            action_id: 1,
            start_frame: 0,
            end_frame: 0,
            peak_frame: 0,
            side: 'left',
        }];
        currentActions.value = fallbackActions;
        currentScores.value = [record.result_data.overall_score];
        currentJointScores.value = [{
            hip: record.result_data.hip_score || record.result_data.overall_score || 0,
            knee: record.result_data.knee_score || record.result_data.overall_score || 0,
            ankle: record.result_data.ankle_score || record.result_data.overall_score || 0,
            height: record.result_data.foot_height_score || record.result_data.overall_score || 0,
        }];
        updateY1Chart(`${record.action_type.replace('-default', '')}动作评分`, fallbackActions, [record.result_data.overall_score]);
    }
    
    updateResultChart();
};

const motionAssessmentViewRef = useTemplateRef<HTMLElement>(
    'motionAssessmentView',
);

onMounted(() => {
    initResultChart();

    initX1Chart('抬脚高度变化', []);
    updateX1Chart('抬脚高度变化', []);

    initY1Chart('动作评分', []);
    updateY1Chart('动作评分', []);

    motionAssessmentStore.getMotionAssessmentHistoryList();

    motionAssessmentStore.initWs(() => {
        nextTick(scrollToBottom);
    });

    loadPracticeRecords();

    restoreLastResult();
});

const restoreLastResult = async () => {
    if (currentAnalysis.taskId && !hasAnalysisResult.value) {
        motionAssessmentStore.isResultLoading = true;
        try {
            const response = await fetch(`${GVHMR_API_BASE_URL}/api/v1/result/${currentAnalysis.taskId}`);
            const result = await response.json();
            
            if (result.status === 'completed' && result.result) {
                const resultData = result.result;
                const actions = resultData.actions || [];
                const evalResults = resultData.eval_results || [];
                const elevationData = resultData.elevation_records || [];
                
                const scores = evalResults.map((e: { feedback?: { '综合评分': number } }) => e.feedback?.['综合评分'] ?? 0);
                
                const jointScores = evalResults.map((e: { feedback?: { feedback_detail?: { '髋关节'?: { 评分: number }; '膝关节'?: { 评分: number }; '踝关节'?: { 评分: number }; '抬脚高度'?: { 评分: number } } } }) => {
                    const detail = e.feedback?.feedback_detail;
                    if (detail) {
                        return {
                            hip: detail['髋关节']?.评分 ?? 0,
                            knee: detail['膝关节']?.评分 ?? 0,
                            ankle: detail['踝关节']?.评分 ?? 0,
                            height: detail['抬脚高度']?.评分 ?? 0,
                        };
                    }
                    return { hip: 0, knee: 0, ankle: 0, height: 0 };
                });

                currentAnalysis.selfLeftXData = elevationData.map((e: { left: any }) => e.left);
                currentAnalysis.selfRightXData = elevationData.map((e: { right: any }) => e.right);

                updateChartsWithApiData(actions, elevationData, scores, jointScores);
            } else {
                loadLastHistoryResult();
            }
        } catch (error) {
            console.error('恢复结果失败:', error);
            loadLastHistoryResult();
        } finally {
            motionAssessmentStore.isResultLoading = false;
        }
    } else if (!currentAnalysis.taskId && practiceRecords.value.length > 0) {
        loadLastHistoryResult();
    }
};

const loadLastHistoryResult = () => {
    if (practiceRecords.value.length > 0) {
        const lastRecord = practiceRecords.value[0];
        onSelectRecord(lastRecord);
    }
};

// 定义状态
const selectedAction = ref<string>('绷踢'); // 默认标题
const isHistoryExpanded = ref(false); // 控制 history-list 的展开状态
const isChatExpanded = ref(false);
const selectedKickAction = ref<string>(''); // 当前选中的踢球动作

const motionAssessmentStore = useMotionAssessmentStore();
const isShowX2Chart = ref(false); // 控制图表4的显示
const isShowY2Chart = ref(false); // 控制图表5的显示

const selectedInterval = ref<number | null>(null);
const practiceRecords = ref<PracticeRecord[]>([]);

const loadPracticeRecords = async () => {
    try {
        const result = await getMotionAssessmentHistoryListApi();
        if (result.status === 'success') {
            practiceRecords.value = result.data.records;
        }
    } catch (error) {
        console.error('加载训练历史记录失败:', error);
    }
};

const getAverageScores = () => {
    if (currentJointScores.value.length === 0) {
        return { avgHip: 0, avgKnee: 0, avgAnkle: 0, avgHeight: 0, avgOverall: 0 };
    }
    const sum = currentJointScores.value.reduce(
        (acc, s) => ({ hip: acc.hip + s.hip, knee: acc.knee + s.knee, ankle: acc.ankle + s.ankle, height: acc.height + s.height }),
        { hip: 0, knee: 0, ankle: 0, height: 0 }
    );
    const count = currentJointScores.value.length;
    return {
        avgHip: Math.round(sum.hip / count),
        avgKnee: Math.round(sum.knee / count),
        avgAnkle: Math.round(sum.ankle / count),
        avgHeight: Math.round(sum.height / count),
        avgOverall: Math.round((sum.hip + sum.knee + sum.ankle + sum.height) / (count * 4)),
    };
};

const getIntervalScores = (index: number) => {
    const data = currentJointScores.value[index];
    if (!data) return getAverageScores();
    const avgOverall = Math.round((data.hip + data.knee + data.ankle + data.height) / 4);
    return {
        avgHip: data.hip,
        avgKnee: data.knee,
        avgAnkle: data.ankle,
        avgHeight: data.height,
        avgOverall,
    };
};

const getCurrentScores = () => {
    if (selectedInterval.value !== null) {
        return getIntervalScores(selectedInterval.value);
    }
    return getAverageScores();
};

const videoSrc = ref<string | undefined>(undefined); // 视频流 URL，初始为 undefined

interface Analysis {
    // 自己的 XY 数据
    selfLeftXData: number[];
    selfLeftYData: number[];

    selfRightXData: number[];
    selfRightYData: number[];

    motionName: string;
    videoUrl: string;

    taskId: string;

    result: {
        stabilityScore: number; // 稳定性评分
        proficiencyScore: number; // 熟练度评分
        fluencyScore: number; // 流畅度评分
        overallScore: number; // 综合评分

        stabilityImprovement: string; // 稳定性改进
        proficiencyEnhancement: string; // 能力增强
        fluencyPromotion: string; // 流畅度提升
        generalAdvice: string; // 综合建议
    };
}

const currentAnalysis = reactive<Analysis>({
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

        stabilityImprovement: '等待上传',
        proficiencyEnhancement: '等待上传',
        fluencyPromotion: '等待上传',
        generalAdvice: '等待上传',
    },
});

const elevationRecords = ref<ElevationRecord[]>([]);
const currentActions = ref<ActionDetail[]>([]);
const currentScores = ref<number[]>([]);
const currentJointScores = ref<{ hip: number; knee: number; ankle: number; height: number }[]>([]);

let resultChart: ECharts | null = null;
let x1Chart: ECharts | null = null;
let y1Chart: ECharts | null = null;

const handleDrop = (e: DragEvent) => {
    const fileList = e.dataTransfer?.files;
    if (fileList) {
        const file = fileList.item(0);
        if (!file || !file.type.startsWith('video')) {
            MessageUtil.error('请选择视频文件！');
            return;
        }
        selectedVideoFile.value = file;
    }
};

const isVideoVisible = ref(false);

const selectedVideoFile = ref<File | null>(null);

// 选择主动作
const selectAction = (action: string) => {
    selectedAction.value = action;
    selectedKickAction.value = 'default';
    selectedInterval.value = null;
    updateX1Chart(`${action.replace('-default', '')}抬脚高度变化`);
    updateY1Chart(`${action.replace('-default', '')}动作评分`);
    updateResultChart();
};

/**
 * 开始分析
 */
const onStartAnalysis = async () => {
    if (motionAssessmentStore.isAnalyzing) {
        MessageUtil.info('正在分析中，请稍后！');
        return;
    }

    if (!selectedVideoFile.value) {
        MessageUtil.error('请先选择视频文件！');
        return;
    }

    if (!selectedAction.value) {
        MessageUtil.error('请先选择主动作！');
        return;
    }

    const motionName = MotionNameUtil.stringify(
        selectedAction.value,
        selectedKickAction.value || 'default',
    );

    motionAssessmentStore.isAnalyzing = true;
    motionAssessmentStore.isResultLoading = true;
    MessageUtil.success('已开启分析任务');

    try {
        // 使用 selectedAction.value 作为标题，而不是 motionName
        motionAssessmentStore.createHistory(selectedAction.value);

        const { task_id } = await submitVideoForEvaluation(
            selectedVideoFile.value,
            selectedAction.value
        );

        currentAnalysis.taskId = task_id;
        currentAnalysis.motionName = motionName;
        currentAnalysis.videoUrl = URL.createObjectURL(selectedVideoFile.value!);

        const evaluationResult = await pollGVHMRResult(task_id);
        console.log('GVHMR API 返回:', evaluationResult);

        if (evaluationResult.result && evaluationResult.result.elevation_records) {
            const resultData = evaluationResult.result;
            console.log('resultData:', resultData);
            
            const actions = resultData.actions || [];
            const evalResults = resultData.eval_results || [];
            console.log('actions:', actions);
            console.log('evalResults:', evalResults);
            
            const elevationData = resultData.elevation_records || [];
            console.log('elevationData:', elevationData);
            
            const threshold = resultData.threshold || -0.09;
            console.log('threshold:', threshold);
            
            const scores = evalResults.map(e => e.feedback?.['综合评分'] ?? 0);
            console.log('scores:', scores);
            
            const jointScores = evalResults.map(e => {
                const detail = e.feedback?.feedback_detail;
                if (detail) {
                    return {
                        hip: detail['髋关节']?.评分 ?? 0,
                        knee: detail['膝关节']?.评分 ?? 0,
                        ankle: detail['踝关节']?.评分 ?? 0,
                        height: detail['抬脚高度']?.评分 ?? 0,
                    };
                }
                return { hip: 0, knee: 0, ankle: 0, height: 0 };
            });
            console.log('jointScores:', jointScores);

            currentAnalysis.selfLeftXData = elevationData.map(e => e.left);
            currentAnalysis.selfRightXData = elevationData.map(e => e.right);

            updateChartsWithApiData(actions, elevationData, scores, jointScores);
        } else {
            updateChartsWithZeroData();
        }

        motionAssessmentStore
            .saveAnalysisData({
                taskId: currentAnalysis.taskId,
                motionName: currentAnalysis.motionName,
                selfLeftXData: currentAnalysis.selfLeftXData,
                selfLeftYData: currentAnalysis.selfLeftYData,
                selfRightXData: currentAnalysis.selfRightXData,
                selfRightYData: currentAnalysis.selfRightYData,
                result: currentAnalysis.result,
                gvhResult: evaluationResult.result,
            })
            .then(() => {
                MessageUtil.success('分析数据已保存到后端！');
            });

    } catch (error) {
        console.error('Analysis failed:', error);
        MessageUtil.error('分析失败，请稍后重试！');
        updateChartsWithZeroData();
    } finally {
        motionAssessmentStore.isAnalyzing = false;
        motionAssessmentStore.isResultLoading = false;
    }
};

const pollGVHMRResult = async (taskId: string): Promise<EvaluationResult> => {
    return new Promise((resolve, reject) => {
        const maxAttempts = 120;
        let attempts = 0;

        const poll = async () => {
            try {
                const result = await queryEvaluationResult(taskId);

                if (result.status === 'completed') {
                    resolve(result);
                } else if (result.status === 'failed') {
                    reject(new Error('Evaluation failed'));
                } else if (attempts >= maxAttempts) {
                    reject(new Error('Timeout waiting for result'));
                } else {
                    attempts++;
                    setTimeout(poll, 1000);
                }
            } catch (error) {
                if (attempts >= maxAttempts) {
                    reject(error);
                } else {
                    attempts++;
                    setTimeout(poll, 1000);
                }
            }
        };

        poll();
    });
};

const updateChartsWithApiData = (
    actions: ActionDetail[], 
    elevationData: ElevationRecord[], 
    scores: number[],
    jointScores?: { hip: number; knee: number; ankle: number; height: number }[]
) => {
    currentActions.value = actions;
    currentScores.value = scores;
    
    elevationRecords.value = elevationData;
    currentJointScores.value = jointScores || actions.map((_, idx) => ({
        hip: scores[idx] || 0,
        knee: scores[idx] || 0,
        ankle: scores[idx] || 0,
        height: scores[idx] || 0,
    }));
    
    updateX1Chart(`${selectedAction.value.replace('-default', '')}抬脚高度变化`, elevationData);
    updateY1Chart(`${selectedAction.value.replace('-default', '')}动作评分`, actions, scores);
    updateResultChart();
};

const updateChartsWithZeroData = () => {
    currentActions.value = [];
    currentScores.value = [];
    elevationRecords.value = [];
    currentJointScores.value = [];
    
    updateX1Chart(`${selectedAction.value.replace('-default', '')}抬脚高度变化`, []);
    updateY1Chart(`${selectedAction.value.replace('-default', '')}动作评分`, [], []);
    updateResultChartWithData(0);
};

/**
 * 重新分析
 */
const reanalyze = async () => {
    if (motionAssessmentStore.currentShowHistoryId == null) {
        MessageUtil.error('请先选择一个历史记录！');
        return;
    }

    if (!currentAnalysis.videoUrl) {
        MessageUtil.error('没有可重新分析的视频！');
        return;
    }

    const motionName = currentAnalysis.motionName;
    resetPageData(`${motionName}X1轴`, `${motionName}Y1轴`);
    currentAnalysis.motionName = motionName;
    [selectedAction.value, selectedKickAction.value] =
        MotionNameUtil.parse(motionName);

    MessageUtil.success('开始重新分析...');
    motionAssessmentStore.isAnalyzing = true;
    motionAssessmentStore.isResultLoading = true;

    try {
        const response = await fetch(currentAnalysis.videoUrl);
        const blob = await response.blob();
        const videoFile = new File([blob], 'reanalysis.mp4', { type: 'video/mp4' });

        const { task_id } = await submitVideoForEvaluation(
            videoFile,
            selectedAction.value
        );

        currentAnalysis.taskId = task_id;
        currentAnalysis.videoUrl = URL.createObjectURL(blob);

        const evaluationResult = await pollGVHMRResult(task_id);
        console.log('GVHMR API 返回:', evaluationResult);

        if (evaluationResult.result && evaluationResult.result.elevation_records) {
            const resultData = evaluationResult.result;
            console.log('resultData:', resultData);
            
            const actions = resultData.actions || [];
            const evalResults = resultData.eval_results || [];
            console.log('actions:', actions);
            console.log('evalResults:', evalResults);
            
            const elevationData = resultData.elevation_records || [];
            console.log('elevationData:', elevationData);
            
            const threshold = resultData.threshold || -0.09;
            console.log('threshold:', threshold);
            
            const scores = evalResults.map(e => e.feedback?.['综合评分'] ?? 0);
            console.log('scores:', scores);
            
            const jointScores = evalResults.map(e => {
                const detail = e.feedback?.feedback_detail;
                if (detail) {
                    return {
                        hip: detail['髋关节']?.评分 ?? 0,
                        knee: detail['膝关节']?.评分 ?? 0,
                        ankle: detail['踝关节']?.评分 ?? 0,
                        height: detail['抬脚高度']?.评分 ?? 0,
                    };
                }
                return { hip: 0, knee: 0, ankle: 0, height: 0 };
            });
            console.log('jointScores:', jointScores);

            currentAnalysis.selfLeftXData = elevationData.map(e => e.left);
            currentAnalysis.selfRightXData = elevationData.map(e => e.right);

            updateChartsWithApiData(actions, elevationData, scores, jointScores);

            motionAssessmentStore.saveAnalysisData(currentAnalysis).then(() => {
                MessageUtil.success('分析数据已保存到后端！');
            });
        } else {
            updateChartsWithZeroData();
        }
    } catch (error) {
        console.error('Reanalysis failed:', error);
        MessageUtil.error('重新分析失败，请稍后重试！');
        updateChartsWithZeroData();
    } finally {
        motionAssessmentStore.isAnalyzing = false;
        motionAssessmentStore.isResultLoading = false;
    }
};

const hasAnalysisResult = computed<boolean>(
    () => currentAnalysis.taskId !== '' && !motionAssessmentStore.isResultLoading,
);

const actionContext = computed(() => {
    if (!hasAnalysisResult.value) return undefined;
    const scores = getCurrentScores();
    return {
        action_type: selectedAction.value,
        hip_score: scores.avgHip,
        knee_score: scores.avgKnee,
        ankle_score: scores.avgAnkle,
        foot_height_score: scores.avgHeight,
        overall_score: scores.avgOverall,
    };
});
/**
 * 重置页面上的数据，
 * 可以设置 x1ChartTitle 和 y1ChartTitle （可选）
 * @param x1ChartTitle
 * @param y1ChartTitle
 */
const resetPageData = (
    x1ChartTitle: string = '未进行视频上传分析',
    y1ChartTitle: string = '未进行视频上传分析',
) => {
    currentAnalysis.selfLeftXData = [];
    currentAnalysis.selfLeftYData = [];
    currentAnalysis.selfRightXData = [];
    currentAnalysis.selfRightYData = [];
    currentAnalysis.motionName = '';
    currentAnalysis.videoUrl = '';
    currentAnalysis.taskId = '';

    currentAnalysis.result.proficiencyScore = 0;
    currentAnalysis.result.stabilityScore = 0;
    currentAnalysis.result.fluencyScore = 0;
    currentAnalysis.result.overallScore = 0;

    currentAnalysis.result.stabilityImprovement = '等待上传';
    currentAnalysis.result.proficiencyEnhancement = '等待上传';
    currentAnalysis.result.fluencyPromotion = '等待上传';
    currentAnalysis.result.generalAdvice = '等待上传';

    currentActions.value = [];
    currentScores.value = [];
    elevationRecords.value = [];
    currentJointScores.value = [];
    selectedInterval.value = null;

    updateResultChart();

    updateX1Chart(x1ChartTitle, []);
    updateY1Chart(y1ChartTitle, [], []);
    isShowX2Chart.value = false;
    isShowY2Chart.value = false;
    resetAction();
    selectedVideoFile.value = null;
    motionAssessmentStore.messageList = [];
};

// 跳转到历史记录详情页
const goToHistoryDetail = (historyId: number) => {
    router.push({
        path: '/motion_analysis_detail',
        query: { historyId: historyId.toString() }
    });
};

// 选择历史记录的方法
const onSelectHistory = (historyId: number) => {
    motionAssessmentStore.isAnalyzing = false; // 这是必要的，因为用户切换记录时可能还没分析结束
    motionAssessmentStore.isResultLoading = false;
    if (motionAssessmentStore.currentShowHistoryId !== historyId) {
        motionAssessmentStore.currentShowHistoryId = historyId;
        motionAssessmentStore.closeWs();
    }
    resetPageData();

    // 根据历史记录ID获取对应的历史记录
    motionAssessmentStore.getAnalysisHistory(historyId).then((res) => {
        if (res.status == 'success') {
            // 更新视图
            currentAnalysis.selfLeftXData = res.data.selfLeftXData;
            currentAnalysis.selfLeftYData = res.data.selfLeftYData;
            currentAnalysis.selfRightXData = res.data.selfRightXData;
            currentAnalysis.selfRightYData = res.data.selfRightYData;
            currentAnalysis.motionName = res.data.motionName;
            currentAnalysis.videoUrl = res.data.videoUrl;
            currentAnalysis.taskId = res.data.taskId;
            currentAnalysis.result = res.data.result;

            [selectedAction.value, selectedKickAction.value] =
                MotionNameUtil.parse(currentAnalysis.motionName);

            const historicalElevationRecords: ElevationRecord[] = currentAnalysis.selfLeftXData.map((_, idx) => ({
                frame: idx,
                left: currentAnalysis.selfLeftXData[idx] || 0,
                right: currentAnalysis.selfRightXData[idx] || 0,
                max: Math.max(currentAnalysis.selfLeftXData[idx] || 0, currentAnalysis.selfRightXData[idx] || 0),
                is_raised: (currentAnalysis.selfLeftXData[idx] || 0) > 0 || (currentAnalysis.selfRightXData[idx] || 0) > 0,
            }));
            elevationRecords.value = historicalElevationRecords;
            
            const historicalActions: ActionDetail[] = [];
            const historicalScores: number[] = [];
            currentJointScores.value = [];
            
            for (let i = 0; i < currentAnalysis.selfLeftYData.length; i++) {
                historicalActions.push({
                    action_id: i + 1,
                    start_frame: i,
                    end_frame: i,
                    side: 'left',
                    peak_frame: i,
                });
                historicalScores.push(Math.round((currentAnalysis.selfLeftYData[i] || 0) * 100));
                currentJointScores.value.push({
                    hip: Math.round((currentAnalysis.selfLeftYData[i] || 0) * 100),
                    knee: Math.round((currentAnalysis.selfLeftYData[i] || 0) * 100),
                    ankle: Math.round((currentAnalysis.selfLeftYData[i] || 0) * 100),
                    height: Math.round((currentAnalysis.selfLeftYData[i] || 0) * 100),
                });
            }
            currentActions.value = historicalActions;
            currentScores.value = historicalScores;

            updateResultChart();
            updateX1Chart(
                `${currentAnalysis.motionName}X1轴`,
                historicalElevationRecords,
            );
            updateY1Chart(
                `${currentAnalysis.motionName.replace('-default', '')}动作评分`,
                historicalActions,
                historicalScores,
            );

            motionAssessmentStore.getChatMessageHistory(historyId);
        }
    });
};

const closeX2Y2Chart = () => {
    isShowX2Chart.value = false;
    isShowY2Chart.value = false;
};

const inputMessage = ref('');
const messagesContainer = useTemplateRef<HTMLElement>('messagesContainer');

const sendMessage = () => {
    if (inputMessage.value.trim()) {
        // 有内容ssssssssssssssssssssssssss

        const _send = () => {
            // 将用户输入的消息添加到本地消息列表
            motionAssessmentStore.sendMessage(inputMessage.value);
            inputMessage.value = '';
            nextTick(scrollToBottom);
        };

        if (motionAssessmentStore.isOpenWs()) {
            _send();
        } else {
            motionAssessmentStore
                .initWs(() => {
                    nextTick(scrollToBottom);
                })
                .then(_send);
        }
    }
};

const scrollToBottom = () =>
    messagesContainer.value &&
    messagesContainer.value.scrollTo({
        top: messagesContainer.value.scrollHeight,
        behavior: 'smooth',
    });

// 删除历史记录的方法
const onDeleteHistory = (historyId: number) => {
    // 调用删除接口
    motionAssessmentStore.deleteHistory([historyId]).then(() => {
        isShowDeleteModal.value = false;

        // 被删除的那条历史记录的索引
        const index = motionAssessmentStore.historyList.indexOf(
            motionAssessmentStore.historyList.find(
                (item) => item.id === historyId,
            )!,
        );
        // 删除历史记录
        motionAssessmentStore.historyList.splice(index, 1);

        // 检查是否删除的是当前选中的历史记录
        if (motionAssessmentStore.currentShowHistoryId === historyId) {
            motionAssessmentStore.currentShowHistoryId = null;

            // 清空页面数据
            resetPageData();
        }
    });
};

const groupedHistoryRecords = computed<
    {
        date: string;
        records: History[];
    }[]
>(() => {
    const grouped: { date: string; records: History[] }[] = [];
    const map: { [key: string]: History[] } = {};

    motionAssessmentStore.historyList.forEach((record) => {
        const date = new Date(record.createTime).toLocaleDateString();
        if (!map[date]) {
            map[date] = [];
            grouped.push({ date, records: map[date] });
        }
        map[date].push(record);
    });

    return grouped;
});

// 创建历史记录的方法
const onCreateHistory = () => {
    motionAssessmentStore.currentShowHistoryId = null; // 不选择任何一个记录，只在点击了开始分析才会真正的去创建记录
    // 清空页面数据
    resetPageData();
};

// 切换展开状态
const toggleHistory = () => {
    isHistoryExpanded.value = !isHistoryExpanded.value;
};

const toggleChat = () => {
    isChatExpanded.value = !isChatExpanded.value;
};

const resetAction = () => {
    selectedAction.value = '绷踢'; // 重置标题
    selectedKickAction.value = '';
    closeX2Y2Chart();
};
let willDeleteId = 0;

// 图表初始化
// X1轴表 - 显示抬脚高度数据和可点击区间
const initX1Chart = (title: string, records: ElevationRecord[] = []) => {
    const chartDom = motionAssessmentViewRef.value!.querySelector(
        '.chart.x1',
    ) as HTMLElement;
    x1Chart = init(chartDom);
    
    const frames = records.map((r) => r.frame);
    const leftData = records.map((r) => r.left);
    const rightData = records.map((r) => r.right);
    
    const allValues = [...leftData, ...rightData];
    const dataMin = allValues.length > 0 ? Math.min(...allValues) : 0;
    const dataMax = allValues.length > 0 ? Math.max(...allValues) : 1;
    
    const option = {
        title: { text: title },
        tooltip: {
            trigger: 'axis',
            formatter: (params: { dataIndex: number }[]) => {
                const idx = params[0].dataIndex;
                const record = records[idx];
                if (!record) return '';
                return `帧: ${record.frame}<br/>左侧: ${record.left.toFixed(4)}<br/>右侧: ${record.right.toFixed(4)}`;
            }
        },
        legend: {
            data: ['左脚', '右脚'],
            top: 30,
        },
        xAxis: {
            type: 'category',
            data: frames,
            name: '帧',
        },
        yAxis: {
            type: 'value',
            name: '抬脚高度',
            min: dataMin,
            max: dataMax,
        },
        grid: {
            left: 50,
            right: 30,
            top: 70,
            containLabel: true,
        },
        dataZoom: [
            {
                type: 'inside',
                start: 0,
                end: 100,
            },
            {
                type: 'slider',
                start: 0,
                end: 100,
            },
        ],
        series: [
            {
                name: '左脚',
                type: 'line',
                data: leftData,
                smooth: 0.6,
                lineStyle: { color: '#FFD700' },
                areaStyle: { color: 'rgba(255, 215, 0, 0.3)' },
            },
            {
                name: '右脚',
                type: 'line',
                data: rightData,
                smooth: 0.6,
                lineStyle: { color: '#4169E1' },
                areaStyle: { color: 'rgba(65, 105, 225, 0.3)' },
            },
        ],
    };
    
    x1Chart.setOption(option);
    
    x1Chart.off('click');
    x1Chart.on('click', (params: { componentType: string; dataIndex?: number }) => {
        if (params.componentType === 'series' && params.dataIndex !== undefined) {
            const dataIndex = params.dataIndex;
            for (let i = 0; i < currentActions.value.length; i++) {
                const action = currentActions.value[i];
                if (dataIndex >= action.start_frame && dataIndex <= action.end_frame) {
                    selectedInterval.value = i;
                    updateResultChart();
                    break;
                }
            }
        }
    });
    
    window.addEventListener('resize', () => {
        x1Chart?.resize();
    });
};

const updateX1Chart = (title: string, records: ElevationRecord[] = []) => {
    if (!x1Chart) return;
    
    const frames = records.map((r) => r.frame);
    const leftData = records.map((r) => r.left);
    const rightData = records.map((r) => r.right);
    
    const allValues = [...leftData, ...rightData];
    const dataMin = allValues.length > 0 ? Math.min(...allValues) : 0;
    const dataMax = allValues.length > 0 ? Math.max(...allValues) : 1;
    
    x1Chart.setOption({
        title: { text: title },
        legend: {
            data: ['左脚', '右脚'],
            top: 30,
        },
        xAxis: {
            type: 'category',
            data: frames,
            name: '帧',
        },
        yAxis: {
            type: 'value',
            name: '抬脚高度',
            min: dataMin,
            max: dataMax,
        },
        series: [
            {
                name: '左脚',
                type: 'line',
                data: leftData,
                smooth: 0.6,
                lineStyle: { color: '#FFD700' },
                areaStyle: { color: 'rgba(255, 215, 0, 0.3)' },
            },
            {
                name: '右脚',
                type: 'line',
                data: rightData,
                smooth: 0.6,
                lineStyle: { color: '#4169E1' },
                areaStyle: { color: 'rgba(65, 105, 225, 0.3)' },
            },
        ],
    });
};

// Y轴表 - 显示检测到的动作区间
const initY1Chart = (title: string, actions: ActionDetail[] = [], scores: number[] = []) => {
    const chartDom = motionAssessmentViewRef.value!.querySelector(
        '.chart.y1',
    ) as HTMLElement;
    y1Chart = init(chartDom);
    
    const categories = actions.map((a) => `动作${a.action_id}`);
    
    const option = {
        title: { 
            text: title,
            textStyle: { fontSize: 14, fontWeight: 'bold' },
            left: 'center',
            top: 10,
        },
        tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(50, 50, 50, 0.9)',
            borderColor: '#333',
            borderWidth: 1,
            textStyle: { color: '#fff' },
            formatter: (params: { name: string; value: number; dataIndex: number }[]) => {
                const idx = params[0].dataIndex;
                const action = actions[idx];
                const score = scores[idx];
                if (!action) return '';
                return `
                    <div style="padding: 5px;">
                        <strong>动作${action.action_id}</strong> (${action.side === 'left' ? '左脚' : '右脚'})<br/>
                        <hr style="margin: 5px 0; border-color: #555;"/>
                        综合评分: <span style="color: #FFD700; font-weight: bold;">${score || 0}</span><br/>
                        帧区间: ${action.start_frame} - ${action.end_frame}
                    </div>
                `;
            }
        },
        xAxis: {
            type: 'category',
            data: categories,
            name: '动作',
            nameLocation: 'middle',
            nameGap: 30,
            nameTextStyle: { fontWeight: 'bold' },
            axisLine: { lineStyle: { color: '#ddd' } },
            axisTick: { show: false },
            axisLabel: { color: '#666' },
        },
        yAxis: {
            type: 'value',
            name: '评分',
            min: 0,
            max: 100,
            nameTextStyle: { fontWeight: 'bold' },
            axisLine: { show: false },
            axisTick: { show: false },
            splitLine: { lineStyle: { color: '#eee', type: 'dashed' } },
            axisLabel: { color: '#666' },
        },
        grid: {
            left: 50,
            right: 30,
            top: 60,
            bottom: 40,
            containLabel: true,
        },
        series: [
            {
                type: 'bar',
                data: scores,
                barWidth: '50%',
                barGap: '30%',
                itemStyle: {
                    borderRadius: [8, 8, 0, 0],
                    color: (params: { dataIndex: number }) => {
                        const action = actions[params.dataIndex];
                        return action && action.side === 'left' ? '#FFD700' : '#4169E1';
                    },
                },
                emphasis: {
                    itemStyle: {
                        shadowBlur: 10,
                        shadowColor: 'rgba(0, 0, 0, 0.3)',
                    },
                },
                label: {
                    show: true,
                    position: 'top',
                    distance: 8,
                    formatter: (params: { value: number }) => params.value,
                    color: '#333',
                    fontWeight: 'bold',
                    fontSize: 12,
                },
                animationDuration: 1500,
                animationEasing: 'elasticOut',
            },
        ],
    };
    
    y1Chart.setOption(option);
    
    y1Chart.off('click');
    y1Chart.on('click', (params: { dataIndex: number }) => {
        selectedInterval.value = params.dataIndex;
        updateResultChart();
    });
    
    window.addEventListener('resize', () => {
        y1Chart?.resize();
    });
};

// Y轴表更新
const updateY1Chart = (title: string, actions: ActionDetail[] = [], scores: number[] = []) => {
    if (!y1Chart) return;
    
    const categories = actions.map((a) => `动作${a.action_id}`);
    
    y1Chart.setOption({
        title: { text: title },
        xAxis: {
            type: 'category',
            data: categories,
            axisLine: { lineStyle: { color: '#ddd' } },
            axisTick: { show: false },
            axisLabel: { color: '#666' },
        },
        yAxis: {
            type: 'value',
            min: 0,
            max: 100,
            axisLine: { show: false },
            axisTick: { show: false },
            splitLine: { lineStyle: { color: '#eee', type: 'dashed' } },
            axisLabel: { color: '#666' },
        },
        series: [
            {
                type: 'bar',
                data: scores,
                barWidth: '50%',
                barGap: '30%',
                itemStyle: {
                    borderRadius: [8, 8, 0, 0],
                    color: (params: { dataIndex: number }) => {
                        const action = actions[params.dataIndex];
                        return action && action.side === 'left' ? '#FFD700' : '#4169E1';
                    },
                },
                label: {
                    show: true,
                    position: 'top',
                    distance: 8,
                    formatter: (params: { value: number }) => params.value,
                    color: '#333',
                    fontWeight: 'bold',
                    fontSize: 12,
                },
            },
        ],
    });
};

const initResultChart = () => {
    resultChart = init(
        motionAssessmentViewRef.value!.querySelector(
            '.result-chart',
        ) as HTMLElement,
    );
    const scores = getCurrentScores();
    const option = {
        title: {
            text: selectedInterval.value !== null ? `动作${selectedInterval.value + 1}评分维度` : '平均评分维度',
        },
        tooltip: {
            trigger: 'item',
            formatter: (params: { value: number[] }) => {
                const values = params.value;
                return `
                        <div>
                            <strong>评分详情</strong><br/>
                            髋关节: ${values[0]}<br/>
                            膝关节: ${values[1]}<br/>
                            踝关节: ${values[2]}<br/>
                            抬脚高度: ${values[3]}
                        </div>
                    `;
            },
        },
        radar: {
            indicator: [
                { name: '髋关节', max: 100 },
                { name: '膝关节', max: 100 },
                { name: '踝关节', max: 100 },
                { name: '抬脚高度', max: 100 },
            ],
            axisName: {
                color: '#252525',
            },
        },
        series: [
            {
                name: selectedInterval.value !== null ? `动作${selectedInterval.value + 1}` : '平均分',
                type: 'radar',
                data: [
                    {
                        value: [
                            scores.avgHip,
                            scores.avgKnee,
                            scores.avgAnkle,
                            scores.avgHeight,
                        ],
                        name: selectedInterval.value !== null ? `动作${selectedInterval.value + 1}` : '平均分',
                    },
                ],
                areaStyle: {},
            },
        ],
    };
    resultChart.setOption(option);
    
    resultChart.off('click');
    resultChart.on('click', () => {
        selectedInterval.value = null;
        updateResultChart();
    });
};

// 分数表
const updateResultChart = () => {
    const scores = getCurrentScores();
    resultChart!.setOption({
        title: {
            text: selectedInterval.value !== null ? `动作${selectedInterval.value + 1}评分维度` : '平均评分维度',
        },
        series: [
            {
                name: selectedInterval.value !== null ? `动作${selectedInterval.value + 1}` : '平均分',
                type: 'radar',
                data: [
                    {
                        value: [
                            scores.avgHip,
                            scores.avgKnee,
                            scores.avgAnkle,
                            scores.avgHeight,
                        ],
                        name: selectedInterval.value !== null ? `动作${selectedInterval.value + 1}` : '平均分',
                    },
                ],
                areaStyle: {},
            },
        ],
    });
};

const updateResultChartWithData = (overallScore: number) => {
    currentJointScores.value = currentActions.value.map(() => ({
        hip: overallScore,
        knee: overallScore,
        ankle: overallScore,
        height: overallScore,
    }));
    updateResultChart();
};

class MotionNameUtil {
    public static parse(name: string): string[] {
        return name.split('-');
    }

    public static stringify(key: string, name: string): string {
        return `${key}-${name}`;
    }
}

const poseImageUrl = computed<string>(() => {
    return (
        actionDemoImgMap[selectedAction.value]?.[selectedKickAction.value]
            ?.pose ?? ''
    );
});

const motion3DViewerRef = ref<InstanceType<typeof Motion3DViewer> | null>(null);

const motionCSVUrl = computed<string>(() => {
    const csvMap: Record<string, string> = {
        '绷踢': '/motion_capture_bengti.csv',
    };
    return csvMap[selectedAction.value] || '';
});

const isShowDeleteModal = ref(false);
const showDeleteModal = (id: number) => {
    isShowDeleteModal.value = true;
    willDeleteId = id;
};

const actionDemoImgMap = {
    头触球: {
        default: {
            demo: '',
            pose: '',
        },
    },
    对踢: {
        default: {
            demo: '',
            pose: '',
        },
    },
    拐踢: {
        default: {
            demo: '',
            pose: '',
        },
    },
    盘踢: {
        default: {
            demo: '',
            pose: '',
        },
    },
    磕踢: {
        default: {
            demo: '',
            pose: '',
        },
    },
    胸触球: {
        default: {
            demo: '',
            pose: '',
        },
    },
    跳踢: {
        default: {
            demo: '',
            pose: '',
        },
    },
    绷踢: {
        default: {
            demo: '',
            pose: '',
        },
    },
} as Record<
    string,
    {
        [motionName: string]: {
            demo: string;
            pose: string;
        };
    }
>;

const actionList = [
    { name: '盘踢', description: '基础踢法' },
    { name: '绷踢', description: '跳起踢法' },
    { name: '拐踢', description: '侧向踢法' },
    { name: '磕踢', description: '膝盖踢法' },
    { name: '跳踢', description: '连续跳跃踢' },
    { name: '头触球', description: '头部触球' },
    { name: '胸触球', description: '胸部触球' },
    { name: '对踢', description: '双人互踢' },
];

const getActionSuggestions = (actionName: string) => {
    const suggestionsMap: Record<string, Array<{key: string; icon: string; colorClass: string; tip: string}>> = {
        '盘踢': [
            { key: '髋关节', icon: '🦵', colorClass: 'pink', tip: '抬腿时髋关节适度外旋，保持腿部灵活性' },
            { key: '膝关节', icon: '🦴', colorClass: 'blue', tip: '膝盖微屈，踢球瞬间快速伸展发力' },
            { key: '踝关节', icon: '🦶', colorClass: 'green', tip: '踝关节保持适度紧绷，踢球部位要准' },
            { key: '抬脚高度', icon: '📏', colorClass: 'yellow', tip: '毽球高度约在小腿中部为最佳击球点' },
        ],
        '绷踢': [
            { key: '髋关节', icon: '🦵', colorClass: 'pink', tip: '起跳时髋关节提前发力，带动腿部上抬' },
            { key: '膝关节', icon: '🦴', colorClass: 'blue', tip: '空中膝盖弯曲缓冲，落地时注意缓冲' },
            { key: '踝关节', icon: '🦶', colorClass: 'green', tip: '空中踝关节灵活调整，寻找最佳击球点' },
            { key: '抬脚高度', icon: '📏', colorClass: 'yellow', tip: '跳起高度足够，确保毽球在视线下方' },
        ],
        '拐踢': [
            { key: '髋关节', icon: '🦵', colorClass: 'pink', tip: '髋关节外展幅度要大，身体侧倾配合' },
            { key: '膝关节', icon: '🦴', colorClass: 'blue', tip: '膝盖沿身体外侧画弧，动作要圆滑' },
            { key: '踝关节', icon: '🦶', colorClass: 'green', tip: '踝关节外侧击球，触球面积要大' },
            { key: '抬脚高度', icon: '📏', colorClass: 'yellow', tip: '侧抬腿高度不低于膝盖水平线' },
        ],
        '磕踢': [
            { key: '髋关节', icon: '🦵', colorClass: 'pink', tip: '大腿抬起与身体成钝角，髋部稳定' },
            { key: '膝关节', icon: '🦴', colorClass: 'blue', tip: '膝盖上抬，用膝关节内侧击球' },
            { key: '踝关节', icon: '🦶', colorClass: 'green', tip: '踝关节放松，随膝盖惯性自然摆动' },
            { key: '抬脚高度', icon: '📏', colorClass: 'yellow', tip: '膝盖抬起高度约与髋关节平齐' },
        ],
        '跳踢': [
            { key: '髋关节', icon: '🦵', colorClass: 'pink', tip: '连续跳跃时髋关节保持弹性，节奏稳定' },
            { key: '膝关节', icon: '🦴', colorClass: 'blue', tip: '每次跳跃后膝盖缓冲落地，准备下一次起跳' },
            { key: '踝关节', icon: '🦶', colorClass: 'green', tip: '节奏感要强，踝关节在节拍点击球' },
            { key: '抬脚高度', icon: '📏', colorClass: 'yellow', tip: '保持稳定高度，避免忽高忽低' },
        ],
        '头触球': [
            { key: '颈部', icon: '🧠', colorClass: 'pink', tip: '颈部放松，用额头区域触球' },
            { key: '身体协调', icon: '🤸', colorClass: 'blue', tip: '身体微微后仰，卸掉毽球冲击力' },
            { key: '时机把握', icon: '⏱️', colorClass: 'green', tip: '准确判断毽球落点，把握击球时机' },
            { key: '平衡', icon: '⚖️', colorClass: 'yellow', tip: '单脚站立时保持身体平衡稳定' },
        ],
        '胸触球': [
            { key: '胸部', icon: '💪', colorClass: 'pink', tip: '挺胸收腹，用胸部正中位置触球' },
            { key: '身体协调', icon: '🤸', colorClass: 'blue', tip: '身体后仰缓冲，减少冲击力' },
            { key: '时机把握', icon: '⏱️', colorClass: 'green', tip: '胸部迎球，在胸前约一掌距离击球' },
            { key: '平衡', icon: '⚖️', colorClass: 'yellow', tip: '双脚站稳，重心略微前倾' },
        ],
        '对踢': [
            { key: '髋关节', icon: '🦵', colorClass: 'pink', tip: '双方髋关节活动范围要匹配协调' },
            { key: '节奏配合', icon: '🎵', colorClass: 'blue', tip: '双方保持节奏同步，动作协调' },
            { key: '预判', icon: '👀', colorClass: 'green', tip: '观察对方动作，提前判断毽球方向' },
            { key: '站位', icon: '🧍', colorClass: 'yellow', tip: '保持适当距离，约2-3米为宜' },
        ],
    };
    return suggestionsMap[actionName] || suggestionsMap['盘踢'];
};

// ——————————————————————————————————————— 日程功能 ————————————————————————————————————————————
const isShowCalendarModal = ref(false);

interface _ScheduleMap {
    [day: string]: Omit<ScheduleItem, 'id'>[];
}

const scheduleMap = ref<_ScheduleMap>({});
const isPlanGenerating = ref(false); // 是否正在生成计划
const showCalendarModal = () => {
    isPlanGenerating.value = true;
    scheduleMap.value = {};
    // 获取最近3天的计划
    generateTrainingPlanApi(motionAssessmentStore.currentShowHistoryId!)
        .then((res) => {
            if (res.status == 'success') {
                for (const item of res.data) {
                    if (!scheduleMap.value[item.scheduleDate]) {
                        scheduleMap.value[item.scheduleDate] = [];
                    }
                    scheduleMap.value[item.scheduleDate].push({
                        time: item.time.substring(0, 5),
                        type: item.type,
                        content: item.content,
                    });
                }
                isShowCalendarModal.value = true;
                MessageUtil.success(res.message);
            } else {
                MessageUtil.error(res.message);
            }
        })
        .finally(() => {
            isPlanGenerating.value = false;
        });
};

/**
 * 添加训练计划
 */
const addSchedule = () => {
    //  添加训练计划
    const tempList = [];
    for (const scheduleDate in scheduleMap.value) {
        for (const scheduleItem of scheduleMap.value[scheduleDate]) {
            tempList.push({
                scheduleDate: scheduleDate,
                type: scheduleItem.type,
                time: scheduleItem.time,
                content: scheduleItem.content,
            });
        }
    }
    HttpUtil.post<Result<null>>('/api/schedules/batch-save', tempList).then(
        (res) => {
            if (res.status == 'success') {
                MessageUtil.success(res.message);
                isShowCalendarModal.value = false;
            } else {
                MessageUtil.error(res.message);
            }
        },
    );
};

const openFileSelectWindow = () => {
    const input = document.createElement('input') as HTMLInputElement;
    input.type = 'file';
    input.accept = 'video/*';
    input.onchange = (e) => {
        const target = e.target as HTMLInputElement;
        selectedVideoFile.value = target.files?.[0] ?? null;
    };
    input.click();
};
const currentSelectedHistory = computed<History | undefined>(() =>
    motionAssessmentStore.historyList.find(
        (item) => item.id == motionAssessmentStore.currentShowHistoryId,
    ),
);

const videoPreviewSrc = computed<string>(() =>
    selectedVideoFile.value ? URL.createObjectURL(selectedVideoFile.value) : '',
);

onUnmounted(() => {
    motionAssessmentStore.closeWs();
    resetPageData();
});
</script>

<template>
    <div class="motion-assessment-view" ref="motionAssessmentView">
        <div class="motion-analysis-main">
            <!-- 侧边栏 -->
            <aside class="aside" :class="{ expanded: isHistoryExpanded }">
                <!-- 侧边栏历史记录 -->
                <div class="history-header" @click="toggleHistory">
                    <span class="iconfont icon-fold" v-if="isHistoryExpanded" />
                    <span class="iconfont icon-expand" v-else />
                </div>

                <div
                    class="history-list"
                    :class="{ expanded: isHistoryExpanded }"
                >
                    <Modal
                        v-model:open="isShowDeleteModal"
                        @ok="onDeleteHistory(willDeleteId)"
                    >
                        确定要删除该记录吗？
                    </Modal>

                    <!-- 历史记录的列表在此处显示 -->
                    <div
                        v-for="(group, index) in groupedHistoryRecords"
                        :key="index"
                    >
                        <!-- 分组标题：日期 -->
                        <div class="history-date">
                            <span>{{ group.date }}</span>
                        </div>
                        <!-- 分组内容：历史记录 -->
                        <div
                            class="history-item"
                            @click="onSelectHistory(record.id)"
                            :class="{
                                selected:
                                    record.id ===
                                    motionAssessmentStore.currentShowHistoryId,
                            }"
                            v-for="record in group.records"
                            :key="record.id"
                        >
                            <div class="history-item-content">
                                <div class="history-title">{{ record.action_type }}</div>
                                <div class="history-actions">
                                    <a-button
                                        type="primary"
                                        size="small"
                                        class="detail-btn"
                                        @click.stop="goToHistoryDetail(record.id)"
                                    >
                                        查看详情
                                    </a-button>
                                </div>
                            </div>
                            <a-dropdown
                                :trigger="['click']"
                                placement="bottomRight"
                            >
                                <span
                                    class="iconfont icon-more"
                                    @click.stop
                                ></span>
                                <template #overlay>
                                    <a-menu>
                                        <a-menu-item
                                            @click="showDeleteModal(record.id)"
                                        >
                                            删除
                                        </a-menu-item>
                                    </a-menu>
                                </template>
                            </a-dropdown>
                        </div>
                    </div>
                </div>

                <div class="new-analysis-btn" @click="onCreateHistory">
                    <span class="iconfont icon-add" />
                    <span v-show="isHistoryExpanded">新建分析</span>
                </div>
            </aside>

            <div class="right">
                <!-- 左侧上传 -->
                <div class="upload-container">
                    <div class="upload-container-header">
                        <div>
                            {{ currentSelectedHistory?.action_type || '请选择动作类型' }}
                        </div>
                    </div>

                    <div class="upload-container-main">
                        <div
                            class="step-1-box"
                            @dragover.prevent
                            @drop.prevent="handleDrop"
                            @click="openFileSelectWindow"
                        >
                            <video
                                class="video-preview"
                                autoplay
                                muted
                                loop
                                v-if="videoPreviewSrc"
                                :src="videoPreviewSrc"
                            />
                            <template v-else>
                                <span
                                    class="iconfont icon-wenjianshangchuan"
                                    style="color: #9f9f9f"
                                />
                                <div class="tips">
                                    mp4、avi、mov、mkv<br />视频时长请小于2分钟
                                </div>
                            </template>
                            <div
                                class="select-file-name"
                                v-show="selectedVideoFile?.name"
                            >
                                {{ selectedVideoFile?.name }}
                            </div>
                        </div>
                        <div class="step-2-box">
                            <div class="step-badge">2</div>
                            <div class="section-header">
                                <span class="section-title">选择动作类型</span>
                                <span class="selected-action-tag" v-if="selectedAction">{{ selectedAction }}</span>
                            </div>
                            <div class="action-grid">
                                <div
                                    class="action-btn"
                                    v-for="(action, index) in actionList"
                                    :key="action.name"
                                    :class="{ active: selectedAction === action.name }"
                                    @click="selectAction(action.name)"
                                >
                                    <span class="action-index">{{ index + 1 }}</span>
                                    <span class="action-name">{{ action.name }}</span>
                                </div>
                            </div>
                            
                            <div class="suggestions-section" v-if="selectedAction">
                                <div class="suggestions-header">
                                    <span class="suggestions-title">{{ selectedAction }} 训练建议</span>
                                </div>
                                <div class="suggestions-grid">
                                    <div class="suggestion-item" v-for="suggestion in getActionSuggestions(selectedAction)" :key="suggestion.key">
                                        <div class="suggestion-icon" :class="suggestion.colorClass">
                                            <span class="icon">{{ suggestion.icon }}</span>
                                        </div>
                                        <div class="suggestion-content">
                                            <span class="suggestion-label">{{ suggestion.key }}</span>
                                            <p class="suggestion-text">{{ suggestion.tip }}</p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="step-3-box" @click="onStartAnalysis">
                            {{ motionAssessmentStore.isAnalyzing ? '正在分析...' : '开始分析' }}
                        </div>
                        <div class="video-feed" v-if="motionAssessmentStore.isAnalyzing">
                            <img :src="videoSrc" v-if="isVideoVisible" alt="" />
                            <div v-else class="image-skeleton">
                                <span class="iconfont icon-tupian" />
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 中间图表 -->
                <div class="chart-container">
                    <div class="chart x1" />
                    <div class="chart y1" />
                    <div class="chart x2" v-if="isShowX2Chart" />
                    <div class="chart y2" v-if="isShowY2Chart" />
                </div>

                <!-- 右侧问答 -->
                <div class="result-container">
                    <div
                        class="result-outer-box"
                        :class="{ collapse: isChatExpanded }"
                    >
                        <div class="result-inner-box">
                            <div class="result-chart-box">
                                <div class="result-chart" />
                            </div>
                        </div>
                    </div>
                    <div class="chart-box">
                        <div class="chat-header">
                            <div class="chat-header-left">
                                <span>训练历史记录</span>
                                <span
                                    class="iconfont icon-shang"
                                    v-if="!isChatExpanded"
                                    @click="toggleChat"
                                />
                                <span
                                    class="iconfont icon-xia"
                                    v-else
                                    @click="toggleChat"
                                />
                            </div>
                            <div class="right">
                                <Button
                                    class="create-schedule-btn"
                                    v-if="hasAnalysisResult"
                                    :loading="isPlanGenerating"
                                    @click="showCalendarModal"
                                >
                                    生成训练计划
                                </Button>
                                <Modal
                                    width="60vw"
                                    v-model:open="isShowCalendarModal"
                                    @ok="addSchedule"
                                    style="z-index: 1000"
                                    ok-text="添加到我的训练计划"
                                    v-model:is-waiting-open="isPlanGenerating"
                                    title="AI生成训练计划"
                                >
                                    <AICalendar
                                        v-model:schedule-map="scheduleMap"
                                    />
                                </Modal>
                                <Button
                                    class="reanalysis-btn"
                                    v-if="hasAnalysisResult"
                                    @click="reanalyze"
                                >
                                    重新分析
                                </Button>
                            </div>
                        </div>
                        <div class="chat-body" ref="messagesContainer">
                            <div class="records-list">
                                <div 
                                    v-for="record in practiceRecords" 
                                    :key="record.id"
                                    class="record-item"
                                    @click="onSelectRecord(record)"
                                >
                                    <div class="record-icon">
                                        <span class="iconfont icon-dongzuo"></span>
                                    </div>
                                    <div class="record-info">
                                        <div class="record-action">{{ record.action_type.replace('-default', '') }}</div>
                                        <div class="record-time">{{ new Date(record.created_at).toLocaleString() }}</div>
                                    </div>
                                    <div class="record-score" :style="{ color: getScoreColor(record.result_data?.overall_score || 0) }">
                                        {{ record.result_data?.overall_score || 0 }}分
                                    </div>
                                </div>
                                <div v-if="practiceRecords.length === 0" class="no-records">
                                    暂无训练记录
                                </div>
                            </div>
                        </div>
                        <ChatController
                            :action-context="actionContext"
                        />
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
.motion-assessment-view {
    display: flex;
    flex-direction: column;
    height: 100%;
    background-color: #f3f5fb;

    .motion-analysis-main {
        $aside-width: 50px;
        flex: 1;
        display: flex;
        flex-direction: row;
        background-color: #ffffff;
        overflow: hidden;
        position: relative;
        padding-left: $aside-width;

        .aside {
            width: $aside-width;
            height: 100%;
            transition: width 0.2s linear;
            display: flex;
            flex-direction: column;
            align-items: stretch;
            $expanded-width: 250px;
            overflow: hidden;
            position: absolute;
            left: 0;
            z-index: 2;
            box-shadow: $box-shadow;
            background-color: #e2f0f6;

            &.expanded {
                width: $expanded-width;
            }

            .history-header {
                background-color: $blue-3;
                display: flex;
                justify-content: center;
                align-items: center;
                cursor: pointer;
                transition: background-color 0.2s ease-in-out;

                &:hover {
                    background-color: $blue-4;
                }

                .iconfont {
                    color: white;
                    font-size: 25px;
                    margin: 10px;
                    border-radius: 5px;
                }
            }

            .history-list {
                width: $expanded-width;
                flex: 1;
                overflow-y: auto;
                opacity: 0;
                transition: opacity 0.3s ease-in-out;

                &::-webkit-scrollbar {
                    width: 5px;
                    height: 0;
                }

                &::-webkit-scrollbar-thumb {
                    background-color: $blue-3;
                }

                &.expanded {
                    opacity: 1;
                }

                .history-date {
                    padding-left: 10px;
                    margin-top: 10px;
                    border-bottom: 1px solid #ddd;
                    color: #8b8b8b;
                }

                .history-item {
                    margin: 3px;
                    height: 50px;
                    border-radius: 10px;
                    display: flex;
                    flex-direction: row;
                    align-items: center;
                    justify-content: space-between;
                    cursor: pointer;
                    padding: 0 10px;

                    .history-item-content {
                        flex: 1;
                        display: flex;
                        flex-direction: column;
                        gap: 4px;
                        overflow: hidden;
                    }

                    .history-title {
                        font-size: 15px;
                        color: #464646;
                        white-space: nowrap;
                        overflow: hidden;
                        text-overflow: ellipsis;
                    }

                    .history-actions {
                        .detail-btn {
                            height: 22px;
                            font-size: 12px;
                            padding: 0 8px;
                            border-radius: 4px;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            border: none;
                            color: #fff;
                            cursor: pointer;
                            transition: all 0.3s ease;

                            &:hover {
                                transform: scale(1.05);
                                box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
                            }
                        }
                    }

                    &:hover {
                        background-color: #d9ddf1;

                        .iconfont {
                            opacity: 1;
                        }

                        .detail-btn {
                            opacity: 1;
                        }
                    }

                    .iconfont {
                        opacity: 0;
                        font-weight: bold;
                        font-size: 25px;
                        border-radius: 10px;
                        margin-left: 8px;

                        &:hover {
                            background-color: #fff;
                        }
                    }

                    &.selected {
                        background-color: #d9ddf1;

                        .iconfont {
                            opacity: 1;
                        }
                    }
                }
            }

            .new-analysis-btn {
                background-color: $blue-3;
                display: flex;
                justify-content: center;
                align-items: center;
                gap: 10px;
                cursor: pointer;
                white-space: nowrap;
                transition: background-color 0.2s ease-in-out;
                user-select: none;

                &:hover {
                    background-color: $blue-4;
                }

                span {
                    font-size: 15px;
                    color: white;
                }

                .iconfont {
                    color: white;
                    font-size: 24px;
                    margin: 7px 0;
                }
            }
        }

        .right {
            flex: 1;
            padding: 0 $gap;
            display: flex;
            flex-direction: row;
            overflow: hidden;
            gap: $gap;

            .upload-container {
                flex: 2;
                background-color: #e2f0f6;
                border-radius: $border-radius;
                overflow: hidden;
                display: flex;
                flex-direction: column;
                margin: $gap 0;
                box-shadow: $box-shadow;

                .upload-container-header {
                    padding: 10px 15px;
                    display: flex;
                    flex-direction: row;
                    align-items: center;
                    background-color: $blue-3;
                    font-size: 20px;
                    color: #464646;
                    font-weight: bold;

                    .iconfont {
                        cursor: pointer;
                        margin-left: 5px;
                        color: #464646;
                        font-size: 20px;
                        margin-top: 1px;
                    }
                }

                .upload-container-main {
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    background-color: transparent;
                    gap: $gap;
                    padding: $gap;
                    overflow: hidden;
                    position: relative;

                    .video-feed {
                        // 上传视频后 会是有视频播放
                        position: absolute;
                        inset: 0;
                        z-index: 1;
                        background-color: #e2f0f6;

                        .image-skeleton {
                            height: 100%;
                            width: 100%;
                            overflow: hidden;
                            position: relative;
                            background-color: #f0f2f5;
                            display: grid;
                            place-items: center;

                            &::after {
                                content: '';
                                position: absolute;
                                animation: shan 1.3s ease 0s infinite;
                                top: 0;
                                width: 40%;
                                height: 100%;
                                background: linear-gradient(
                                    to left,
                                    rgba(255, 255, 255, 0) 0,
                                    rgba(255, 255, 255, 0.4) 50%,
                                    rgba(255, 255, 255, 0) 100%
                                );
                                transform: skewX(-45deg);
                                @keyframes shan {
                                    0% {
                                        left: -100%;
                                    }
                                    100% {
                                        left: 220%;
                                    }
                                }
                            }

                            .iconfont {
                                font-size: 100px;
                                color: #cecfd1;
                            }
                        }

                        img {
                            height: 100%;
                            width: 100%;
                            object-fit: contain;
                        }
                    }

                    .step-1-box {
                        position: relative;
                        border-radius: $border-radius;
                        background-color: #fff;
                        height: 190px;
                        display: flex;
                        cursor: pointer;
                        flex-direction: column;
                        justify-content: center;
                        align-items: center;
                        overflow: hidden;

                        &::after {
                            content: '1';
                            display: block;
                            position: absolute;
                            top: 5px;
                            left: 5px;
                            background-color: rgb(153, 190, 246);
                            $size: 25px;
                            width: $size;
                            height: $size;
                            line-height: $size;
                            border-radius: 50%;
                            text-align: center;
                            font-size: 16px;
                            color: #ffffff;
                        }

                        .iconfont {
                            font-size: 50px;
                            color: #ededed;
                        }

                        .video-preview {
                            height: 130px;
                            max-width: 85%;
                            object-fit: contain;
                        }

                        .select-file-name {
                            margin-top: 10px;
                            background-color: #72a4f1;
                            box-shadow: 0 0 2px #72a4f1;
                            border-radius: 2px;
                            padding: 1px 5px;
                            color: #fff;
                            font-size: 12px;
                        }

                        .tips {
                            color: #000;
                            font-size: 14px;
                            white-space: nowrap;
                            text-align: center;
                        }
                    }

                    .step-2-box {
                        flex: 1;
                        overflow: hidden;
                        display: flex;
                        flex-direction: column;
                        position: relative;
                        border-radius: $border-radius;
                        background: #ffffff;
                        box-shadow: $box-shadow;
                        padding: 16px 20px;
                        gap: 12px;

                        .step-badge {
                            position: absolute;
                            top: -1px;
                            left: -1px;
                            width: 24px;
                            height: 24px;
                            background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                            border-radius: 12px 0 12px 0;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            font-size: 12px;
                            font-weight: bold;
                            color: #ffffff;
                            z-index: 1;
                        }

                        .section-header {
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            margin-bottom: 10px;
                            padding-left: 28px;

                            .section-title {
                                font-size: 14px;
                                font-weight: 600;
                                color: #1e293b;
                            }

                            .selected-action-tag {
                                font-size: 11px;
                                background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                                color: white;
                                padding: 3px 10px;
                                border-radius: 10px;
                                font-weight: 500;
                            }
                        }

                        .action-grid {
                            display: grid;
                            grid-template-columns: repeat(4, 1fr);
                            gap: 8px;

                            .action-btn {
                                display: flex;
                                flex-direction: column;
                                align-items: center;
                                justify-content: center;
                                padding: 10px 6px;
                                background: #f8fafc;
                                border: 1.5px solid #e2e8f0;
                                border-radius: 8px;
                                cursor: pointer;
                                transition: all 0.2s ease;
                                position: relative;
                                overflow: hidden;

                                &::before {
                                    content: '';
                                    position: absolute;
                                    top: 0;
                                    left: 0;
                                    right: 0;
                                    height: 2px;
                                    background: linear-gradient(90deg, #1890ff 0%, #69c0ff 100%);
                                    transform: scaleX(0);
                                    transition: transform 0.2s ease;
                                }

                                &:hover {
                                    transform: translateY(-1px);
                                    box-shadow: 0 4px 12px rgba(24, 144, 255, 0.12);
                                    border-color: #93c5fd;
                                    background: #ffffff;
                                    
                                    &::before {
                                        transform: scaleX(1);
                                    }
                                }

                                &.active {
                                    background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                                    border-color: transparent;
                                    box-shadow: 0 3px 12px rgba(24, 144, 255, 0.3);

                                    .action-index {
                                        background: rgba(255, 255, 255, 0.3);
                                        color: white;
                                    }

                                    .action-name {
                                        color: white;
                                        font-weight: 600;
                                    }
                                }

                                .action-index {
                                    width: 20px;
                                    height: 20px;
                                    background: linear-gradient(135deg, #e6f4ff 0%, #bae0ff 100%);
                                    border-radius: 50%;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                    font-size: 10px;
                                    font-weight: 700;
                                    color: #1890ff;
                                    margin-bottom: 4px;
                                }

                                .action-name {
                                    font-size: 12px;
                                    font-weight: 500;
                                    color: #464646;
                                }
                            }
                        }

                        .suggestions-section {
                            margin-top: 8px;

                            .suggestions-header {
                                margin-bottom: 8px;
                                padding-left: 28px;

                                .suggestions-title {
                                    font-size: 13px;
                                    font-weight: 600;
                                    color: #1e293b;
                                }
                            }

                            .suggestions-grid {
                                display: grid;
                                grid-template-columns: repeat(2, 1fr);
                                gap: 8px;

                                .suggestion-item {
                                    display: flex;
                                    align-items: flex-start;
                                    gap: 10px;
                                    padding: 10px;
                                    background: #f8fafc;
                                    border-radius: 8px;
                                    border: 1px solid #e2e8f0;
                                    transition: all 0.2s ease;

                                    &:hover {
                                        box-shadow: 0 3px 10px rgba(24, 144, 255, 0.08);
                                        transform: translateY(-1px);
                                        background: #ffffff;
                                    }

                                    .suggestion-icon {
                                        width: 28px;
                                        height: 28px;
                                        border-radius: 6px;
                                        display: flex;
                                        align-items: center;
                                        justify-content: center;
                                        flex-shrink: 0;

                                        .icon {
                                            font-size: 14px;
                                        }

                                        &.pink {
                                            background: linear-gradient(135deg, #fdf2f8 0%, #fce7f3 100%);
                                        }
                                        &.blue {
                                            background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
                                        }
                                        &.green {
                                            background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
                                        }
                                        &.yellow {
                                            background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
                                        }
                                    }

                                    .suggestion-content {
                                        flex: 1;
                                        min-width: 0;

                                        .suggestion-label {
                                            font-size: 11px;
                                            font-weight: 600;
                                            color: #64748b;
                                            text-transform: uppercase;
                                            letter-spacing: 0.3px;
                                            display: block;
                                            margin-bottom: 2px;
                                        }

                                        .suggestion-text {
                                            font-size: 11px;
                                            color: #475569;
                                            line-height: 1.4;
                                            margin: 0;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    .step-3-box {
                        padding: 20px;
                        text-align: center;
                        cursor: pointer;
                        overflow: hidden;
                        display: flex;
                        flex-direction: column;
                        position: relative;
                        border-radius: $border-radius;
                        background-color: #fff;
                        user-select: none;

                        &::after {
                            content: '3';
                            display: block;
                            position: absolute;
                            top: 5px;
                            left: 5px;
                            background-color: rgb(153, 190, 246);
                            $size: 25px;
                            width: $size;
                            height: $size;
                            line-height: $size;
                            border-radius: 50%;
                            text-align: center;
                            font-size: 16px;
                            color: #ffffff;
                        }
                    }
                }
            }

            .chart-container {
                flex: 3;
                overflow-y: auto;
                overflow-x: hidden;

                &::-webkit-scrollbar {
                    width: 5px;
                    height: 0;
                }

                &::-webkit-scrollbar-thumb {
                    background-color: $blue-3;
                }

                .chart {
                    padding: 10px;
                    height: calc(50% - $gap * 1.5);
                    background-color: #e2f0f6;
                    margin: $gap 0;
                    border-radius: $border-radius;
                }
            }

            .result-container {
                flex: 3;
                overflow: hidden;
                display: flex;
                flex-direction: column;

                $result-box-height: 300px;

                .result-outer-box {
                    height: $result-box-height;
                    transition: height 0.3s linear;
                    overflow: hidden;

                    &.collapse {
                        height: 0;
                    }

                    .result-inner-box {
                        height: calc($result-box-height - $gap);
                        display: flex;
                        flex-direction: row;
                        justify-content: space-between;
                        gap: $gap;
                        margin: $gap 0 0;

                        .result-chart-box {
                            flex: 2;
                            background-color: #e2f0f6;
                            border-radius: $border-radius;
                            overflow: hidden;
                            padding: 10px;

                            .result-chart {
                                height: 100%;
                                width: 100%;
                            }
                        }

                        .pose {
                            flex: 1;
                            background-color: #e2f0f6;
                            border-radius: $border-radius;
                            overflow: hidden;
                            //display: grid;    // 这里不能使用 grid 布局，必须使用 flex 布局
                            //place-items: center;

                            display: flex;
                            align-items: center;
                            justify-content: center;

                            img {
                                flex-shrink: 0;
                                height: 100%;
                                width: 100%;
                                object-fit: contain;
                            }
                        }
                    }
                }

                .chart-box {
                    flex: 1;
                    background-color: #e2f0f6;
                    border-radius: $border-radius;
                    overflow: hidden;
                    display: flex;
                    flex-direction: column;
                    margin: $gap 0;

                    .chat-header {
                        height: 50px;
                        background: linear-gradient(to right, #c0e5f5, #fadada);
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        padding: 0 18px;

                        .chat-header-left {
                            width: 50%;
                            height: 100%;
                            display: flex;
                            align-items: center;
                            font-size: 20px;
                            font-weight: bold;
                            color: #464646;

                            .iconfont {
                                margin-left: 7px;
                                cursor: pointer;
                                font-size: 16px;
                            }
                        }

                        .right {
                            all: unset;
                            display: flex;
                            align-items: center;
                            gap: 10px;

                            .create-schedule-btn,
                            .reanalysis-btn {
                                font-size: 14px;
                                padding: 2px 6px;
                                background-color: rgba(
                                    $color: #fff,
                                    $alpha: 0.6
                                );
                                color: #444;
                                transition: background-color 0.2s ease-in-out;
                                box-shadow: none;
                                height: 27px;
                                display: flex;
                                align-items: center;

                                &:hover {
                                    background-color: #d7f4ff;
                                }
                            }
                        }
                    }

                    .chat-body {
                        flex: 1;
                        overflow-y: auto;
                        padding: 12px;
                        position: relative;

                        &::-webkit-scrollbar {
                            width: 6px;
                            height: 0;
                        }

                        &::-webkit-scrollbar-thumb {
                            background: rgba(24, 144, 255, 0.3);
                            border-radius: 3px;
                        }

                        &::-webkit-scrollbar-thumb:hover {
                            background: rgba(24, 144, 255, 0.5);
                        }

                        .records-list {
                            display: flex;
                            flex-direction: column;
                            gap: 10px;

                            .record-item {
                                display: flex;
                                align-items: center;
                                gap: 12px;
                                padding: 14px 16px;
                                background: rgba(255, 255, 255, 0.98);
                                border-radius: 12px;
                                box-shadow: 0 2px 12px rgba(24, 144, 255, 0.08);
                                border: 1px solid rgba(24, 144, 255, 0.1);
                                cursor: pointer;
                                transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

                                &:hover {
                                    transform: translateX(4px);
                                    box-shadow: 0 6px 20px rgba(24, 144, 255, 0.15);
                                    border-color: rgba(24, 144, 255, 0.25);
                                }

                                .record-icon {
                                    width: 40px;
                                    height: 40px;
                                    background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
                                    border-radius: 10px;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                    flex-shrink: 0;

                                    .iconfont {
                                        font-size: 20px;
                                        color: #fff;
                                    }
                                }

                                .record-info {
                                    flex: 1;
                                    min-width: 0;

                                    .record-action {
                                        font-size: 14px;
                                        font-weight: 600;
                                        color: #333;
                                        margin-bottom: 4px;
                                    }

                                    .record-time {
                                        font-size: 12px;
                                        color: #999;
                                    }
                                }

                                .record-score {
                                    font-size: 16px;
                                    font-weight: 700;
                                    padding: 4px 12px;
                                    background: rgba(24, 144, 255, 0.1);
                                    border-radius: 20px;
                                }
                            }

                            .no-records {
                                text-align: center;
                                padding: 40px 20px;
                                color: #999;
                                font-size: 14px;
                            }
                        }
                    }
                }
            }
        }
    }
}

@media (max-width: 1200px) {
    .motion-assessment-view {
        .motion-analysis-main {
            .right {
                .upload-container {
                    flex: 1.5;
                }
                .chart-container {
                    flex: 2;
                }
                .result-container {
                    flex: 2;
                }
            }
        }
    }
}

@media (max-width: 992px) {
    .motion-assessment-view {
        .motion-analysis-main {
            flex-direction: column;
            
            .aside {
                width: 100%;
                height: auto;
                max-height: 200px;
                position: relative;
                flex-direction: row;
                padding-left: 0;
                
                &.expanded {
                    width: 100%;
                }
                
                .history-header {
                    width: 50px;
                    min-height: 100%;
                }
                
                .history-list {
                    width: calc(100% - 50px);
                    opacity: 1;
                    
                    .history-item {
                        height: 40px;
                    }
                }
                
                .new-analysis-btn {
                    width: 50px;
                    writing-mode: vertical-rl;
                    text-orientation: mixed;
                    
                    span {
                        writing-mode: vertical-rl;
                    }
                }
            }
            
            .right {
                flex-direction: column;
                padding: $gap;
                
                .upload-container,
                .chart-container,
                .result-container {
                    flex: none;
                    height: 300px;
                    margin: 0;
                }
                
                .result-container {
                    .result-outer-box {
                        height: 250px;
                        
                        .result-inner-box {
                            height: calc(250px - $gap);
                            flex-direction: column;
                            
                            .result-chart-box,
                            .pose {
                                flex: 1;
                            }
                        }
                    }
                }
            }
        }
    }
}

@media (max-width: 768px) {
    .motion-assessment-view {
        .motion-analysis-main {
            .right {
                .upload-container,
                .chart-container {
                    height: 250px;
                }
                
                .result-container {
                    .result-outer-box {
                        height: 200px;
                        
                        .result-inner-box {
                            height: calc(200px - $gap);
                            
                            .result-chart-box {
                                flex: 1.5;
                            }
                            
                            .pose {
                                flex: 1;
                            }
                        }
                    }
                }
            }
        }
    }
}
</style>

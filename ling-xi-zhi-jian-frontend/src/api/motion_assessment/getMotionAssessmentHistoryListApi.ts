import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel.d';
import { useUserStore } from '@/stores/userStore';
import type { PracticeRecord } from '@/api/learning/learningApi';

interface HistoryRecord {
    id: number;
    student_id: number;
    analysis_data: {
        selfLeftXData?: number[];
        selfLeftYData?: number[];
        selfRightXData?: number[];
        selfRightYData?: number[];
        motionName?: string;
        taskId?: string;
        result?: {
            stabilityScore?: number;
            proficiencyScore?: number;
            fluencyScore?: number;
            overallScore?: number;
        };
        gvhResult?: {
            action_count?: number;
            actions?: Array<{
                action_id: number;
                start_frame: number;
                end_frame: number;
                peak_frame: number;
                side: 'left' | 'right';
                slice_path?: string;
            }>;
            eval_results?: Array<{
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
            }>;
            elevation_records?: Array<{
                frame: number;
                left: number;
                right: number;
                max: number;
                is_raised: boolean;
            }>;
            threshold?: number;
        };
    };
    created_at: string;
}

/**
 * 获取动作评估的历史记录
 */
export const getMotionAssessmentHistoryListApi = (): Promise<
    Result<{
        records: PracticeRecord[];
    }>
> => {
    return new Promise<
        Result<{
            records: PracticeRecord[];
        }>
    >((resolve, reject) => {
        const userStore = useUserStore();
        const studentId = Number(userStore.myInfo.userId);

        HttpUtil.get<Result<{ records: HistoryRecord[] }>>(
            '/api/analysis/history',
            { params: { student_id: studentId } }
        ).then((res) => {
            if (res.status == 'success') {
                resolve({
                    status: 'success',
                    data: {
                        records: res.data.records.map((record) => {
                            const data = record.analysis_data || {};
                            const elevationRecords = [];
                            const leftData = data.selfLeftXData || [];
                            const rightData = data.selfRightXData || [];
                            const len = Math.max(leftData.length, rightData.length);

                            for (let i = 0; i < len; i++) {
                                elevationRecords.push({
                                    frame: i,
                                    left: leftData[i] || 0,
                                    right: rightData[i] || 0
                                });
                            }

                            const result = data.result || {};
                            const gvhResult = data.gvhResult || {};
                            const evalResults = gvhResult.eval_results || [];
                            
                            const actionScores = evalResults.map((e: { feedback?: { '综合评分': number } }) => e.feedback?.['综合评分'] ?? 0);
                            const overallScore = actionScores.length > 0 
                                ? Math.round(actionScores.reduce((a: number, b: number) => a + b, 0) / actionScores.length)
                                : (result.overallScore || 0);

                            const jointScores = evalResults.map((e: { feedback?: { feedback_detail?: { '髋关节'?: { 评分: number }; '膝关节'?: { 评分: number }; '踝关节'?: { 评分: number }; '抬脚高度'?: { 评分: number } } } }) => {
                                const detail = e.feedback?.feedback_detail;
                                if (detail) {
                                    return {
                                        hip: detail['髋关节']?.评分 ?? overallScore,
                                        knee: detail['膝关节']?.评分 ?? overallScore,
                                        ankle: detail['踝关节']?.评分 ?? overallScore,
                                        height: detail['抬脚高度']?.评分 ?? overallScore,
                                    };
                                }
                                return {
                                    hip: overallScore,
                                    knee: overallScore,
                                    ankle: overallScore,
                                    height: overallScore,
                                };
                            });

                            const actions = gvhResult.actions || [];
                            const elevationRecordsGvh = gvhResult.elevation_records || [];

                            return {
                                id: record.id,
                                student_id: record.student_id,
                                action_type: data.motionName || '未知动作',
                                video_url: '',
                                result_data: {
                                    elevation_records: elevationRecordsGvh.length > 0 ? elevationRecordsGvh : elevationRecords,
                                    overall_score: overallScore,
                                    action_scores: actionScores,
                                    joint_scores: jointScores,
                                    actions: actions,
                                    hip_score: jointScores.length > 0 
                                        ? Math.round(jointScores.reduce((a: { hip: number }, b: { hip: number }) => ({ hip: a.hip + b.hip }), { hip: 0 }).hip / jointScores.length)
                                        : overallScore,
                                    knee_score: jointScores.length > 0
                                        ? Math.round(jointScores.reduce((a: { knee: number }, b: { knee: number }) => ({ knee: a.knee + b.knee }), { knee: 0 }).knee / jointScores.length)
                                        : overallScore,
                                    ankle_score: jointScores.length > 0
                                        ? Math.round(jointScores.reduce((a: { ankle: number }, b: { ankle: number }) => ({ ankle: a.ankle + b.ankle }), { ankle: 0 }).ankle / jointScores.length)
                                        : overallScore,
                                    foot_height_score: jointScores.length > 0
                                        ? Math.round(jointScores.reduce((a: { height: number }, b: { height: number }) => ({ height: a.height + b.height }), { height: 0 }).height / jointScores.length)
                                        : overallScore,
                                },
                                created_at: record.created_at,
                            };
                        }),
                    },
                    message: res.message,
                });
            } else {
                reject(res);
            }
        });
    });
};

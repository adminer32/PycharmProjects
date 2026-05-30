import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel';

/**
 * 获取最近3天的计划
 * @param historyId
 */
export const generateTrainingPlanApi = (historyId: number) =>
    HttpUtil.get<
        Result<
            {
                scheduleDate: string;
                time: string;
                type: string;
                content: string;
            }[]
        >
    >('/api/analysis/generateTrainingPlan', {
        params: {
            historyId,
            dayNum: 3,
        },
    });

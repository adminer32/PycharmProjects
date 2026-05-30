import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel';

interface Schedule {
    time: string; // HH:mm
    type: string;
    content: string;
    scheduleDate: string;
}

/**
 * 保存训练计划
 */
export const saveScheduleApi = (schedule: Schedule) =>
    HttpUtil.post<
        Result<{
            id: number;
            content: string;
            scheduleDate: string;
            time: string;
            type: string;
        }>
    >('/api/v0/student/schedule/save', schedule);

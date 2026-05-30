import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel';
import type { ScheduleMap } from '@/stores/scheduleStore';

/**
 * 获取日程列表
 * @param dateString
 */
export const getScheduleListApi = (dateString: string) =>
    HttpUtil.get<Result<ScheduleMap>>('/api/v0/student/schedule/query', {
        params: {
            date: dateString,
        },
    });

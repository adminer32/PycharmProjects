import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel';

interface AIChatNote {
    id: number;
    taskId: string;
    createTime: string;
    videoUrl: string;
    title: string;
    autoChapters: string; // 笔记   // 没分析出来时返回的是字符串分析出来返回的是JSON
    summarization: string; // json
    meetingAssistance: string; // json
    personalNote: string;
    taskStatus: '进行中' | '已完成' | '失败';
}

export const queryTaskStatusApi = (taskId: string) => {
    return HttpUtil.get<Result<AIChatNote>>('/api/ai/queryTaskStatus', {
        params: {
            taskId,
        },
    });
};

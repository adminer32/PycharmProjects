import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel';

export const submitAnalysisTaskApi = (
    historyId: number,
    motionName: string,
    file: Blob,
    onUploadProgress?: (loaded: number, total: number) => void,
) => {
    // 调用后端接口插入内容
    return HttpUtil.postForm<
        Result<{
            analysis_id: string;
        }>
    >(
        '/api/content/insert',
        {
            aiContentInfo: JSON.stringify({
                historyId,
                card: {
                    title: '',
                },
                type: 'card',
                sender: 'user',
            }),
            file,
            motionName,
        },
        {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
            onUploadProgress: (e) => {
                onUploadProgress?.(e.loaded, e.total!);
            },
        },
    );
};

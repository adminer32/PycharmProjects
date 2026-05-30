import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel';

export const updateAvatarApi = (file: File) => {
    return HttpUtil.postForm<
        Result<{
            object: string;
        }>
    >('/api/file/operation/upload', {
        file,
    });
};

import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel';

const API_BASE = '/api';

export interface Course {
    id: number;
    aiNoteId: number;
    createTime: string;
    createBy: string;
    name: string;
    categoryId: number;
    subCategoryName: string;
    content: string;
    description: string;
    videoUrl: string;
    teacherName: string;
    levelName: string;
    coverImageUrl: string;
    enabled: boolean;
    videoDuration: string;
}

export interface Category {
    createTime: string;
    categoryId: number;
    subCategoryName: string;
    subCategoryCourse: Course[];
    content: string;
    description: string;
    videoUrl: string;
    teacherName: string;
    levelName: string;
    coverImageUrl: string;
    enabled: boolean;
    videoDuration: string;
}

export const getCourseListApi = (parentCategoryName: string) => {
    return HttpUtil.post<Result<{ list: Category[] }>>(
        `${API_BASE}/course/query`,
        {
            param: {
                parent_category_name: parentCategoryName,
            },
        },
    );
};

export const getVideoProgressApi = (userId: number) => {
    return HttpUtil.get<Result<Record<string, { progress: number; updateTime: string }>>>(
        `${API_BASE}/video/progress`,
        { params: { user_id: userId } }
    );
};

export const saveVideoProgressApi = (userId: number, videoPath: string, progress: number) => {
    return HttpUtil.post<Result<void>>(
        `${API_BASE}/video/progress`,
        {
            user_id: userId,
            video_path: videoPath,
            progress: progress,
        }
    );
};

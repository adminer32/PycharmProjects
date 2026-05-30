import HttpUtil from '@/utils/HttpUtil';

const API_BASE = '/api/v0';

export interface TeacherVideo {
    id: string;
    name: string;
    videoUrl: string;
    coverImageUrl: string;
    duration: string;
    category: string;
    description: string;
    teacherId: number;
    teacherName: string;
    classId: number;
    date: string;
}

export const getTeacherVideosApi = () => {
    return HttpUtil.get<{
        status: string;
        data: TeacherVideo[];
    }>(`${API_BASE}/teacher/lessons`);
};

export const getTeacherVideoByIdApi = (videoId: string) => {
    return HttpUtil.get<{
        status: string;
        data: TeacherVideo;
    }>(`${API_BASE}/teacher/lessons/${videoId}`);
};

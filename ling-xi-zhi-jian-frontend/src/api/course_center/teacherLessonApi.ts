import HttpUtil from '@/utils/HttpUtil';

interface TeacherLesson {
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

interface Result {
    status: string;
    message?: string;
    data?: TeacherLesson[];
}

export const getTeacherLessonsApi = (classId?: number, teacherId?: number) => {
    let url = '/api/v0/teacher/lessons';
    const params: string[] = [];
    
    if (classId !== undefined) {
        params.push(`class_id=${classId}`);
    }
    if (teacherId !== undefined) {
        params.push(`teacher_id=${teacherId}`);
    }
    
    if (params.length > 0) {
        url += '?' + params.join('&');
    }
    
    return HttpUtil.get<Result>(url);
};

export const getTeacherLessonByIdApi = (lessonId: number) => {
    return HttpUtil.get<Result>(`/api/v0/teacher/lessons/${lessonId}`);
};

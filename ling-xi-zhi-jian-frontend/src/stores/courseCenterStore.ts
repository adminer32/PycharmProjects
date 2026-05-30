/**
 * 课程中心状态管理
 */
import { defineStore } from 'pinia';
import { getNoteByCourseIdApi } from '@/api/course_center/getNoteByCourseId';
import { getCourseListApi, getVideoProgressApi, saveVideoProgressApi } from '@/api/course_center/courseApi';

// 课程数据
export interface Course {
    id: number;
    aiNoteId: number;
    createTime: string;
    createBy: string;
    name: string; // 课程名称
    categoryId: number;
    subCategoryName: string; // 类别
    content: string; // 课程内容
    description: string; // 课程描述
    videoUrl: string;
    teacherName: string; // 教师名称
    levelName: string; // 等级
    coverImageUrl: string; // 封面图片URL
    enabled: boolean; // 是否可学
    videoDuration: string; // 课程时长
}

// 课程数据
export interface Category {
    createTime: string;
    categoryId: number;
    subCategoryName: string; // 类别
    subCategoryCourse: Course[];
    content: string; // 课程内容
    description: string; // 课程描述
    videoUrl: string;
    teacherName: string; // 教师名称
    levelName: string; // 等级
    coverImageUrl: string; // 封面图片URL
    enabled: boolean; // 是否可学
    videoDuration: string; // 课程时长
}

interface Note {
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

export const useCourseCenterStore = defineStore('useCourseCenterStore', {
    state: () => {
        return {
            categoryList: [] as Category[],
            note: {} as Note,
            videoProgressMap: {} as Record<string, number>,
        };
    },

    getters: {
        courseMap: (state) => {
            const map = {} as {
                [category: string]: Course[];
            };
            for (const category of state.categoryList) {
                map[category.subCategoryName] = category.subCategoryCourse;
            }
            return map;
        },
    },

    actions: {
        getNote(courseId: number) {
            getNoteByCourseIdApi(courseId).then((res) => {
                if (res.status === 'success') {
                    this.note = res.data;
                }
            });
        },

        getCourseList() {
            getCourseListApi('基本课程').then((res) => {
                if (res.status === 'success') {
                    this.categoryList = res.data.list;
                    getCourseListApi('综合课程').then((res) => {
                        if (res.status === 'success') {
                            for (const category of res.data.list) {
                                this.categoryList.push(category);
                            }
                        }
                    });
                }
            });
        },

        getVideoProgress(userId: number) {
            getVideoProgressApi(userId).then((res) => {
                if (res.status === 'success') {
                    this.videoProgressMap = {};
                    for (const [path, data] of Object.entries(res.data)) {
                        this.videoProgressMap[path] = data.progress;
                    }
                }
            });
        },

        saveVideoProgress(userId: number, videoPath: string, progress: number) {
            this.videoProgressMap[videoPath] = progress;
            saveVideoProgressApi(userId, videoPath, progress);
        },
    },
});

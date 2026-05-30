/**
 * 教师档案接口类型定义
 */
export interface TeacherProfile {
    id?: number;
    teacherId?: number;
    name: string;
    avatar?: string;
    email?: string;
    phone?: string;
    gender?: number; // 0-保密，1-男，2-女
    specialty?: string;
    bio?: string;
    active?: number; // 0-禁用，1-启用
    department?: string;
    teachingYears?: number;
    createdAt?: string;
    updatedAt?: string;
}

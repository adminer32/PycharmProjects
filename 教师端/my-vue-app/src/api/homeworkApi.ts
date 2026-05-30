import HttpUtil from '@/utils/HttpUtil';
import type { RestResponse } from '@/types/RestResponse';

// ============ 类型定义 ============

// 作业列表项
export interface HomeworkListVO {
  id: number;
  title: string;
  requirements: string;
  deadline: string;
  status: number;
  submissionCount: number;
  totalCount: number;
  pendingGradeCount: number;
  averageScore: number | null;
  createdAt: string;
}

// 作业详情
export interface HomeworkDetailVO extends HomeworkListVO {
  demoVideoUrl: string | null;
  classId: number;
  className: string;
  deadlineTime: string;
}

// 学生提交记录
export interface SubmissionVO {
  id?: number;
  studentId: number;
  studentName: string;
  studentAvatar: string;
  studentAccount?: string;
  videoUrl?: string;
  submitTime?: string;
  aiScore?: number | null;
  teacherScore?: number | null;
  feedback?: string | null;
  aiSuggestion?: string | null;
  status: number; // -1-未提交，0-待批改，1-已批改
  submitted: boolean; // 是否已提交
}

// 发布作业请求
export interface CreateHomeworkDTO {
  title: string;
  requirements: string;
  demoVideoUrl?: string;
  deadline: string; // datetime 格式
  classId: number;
}

// 批改作业请求
export interface GradeSubmissionDTO {
  teacherScore: number;
  teacherComment: string;
  aiSuggestion?: string;
}

// ============ API 函数 ============

// 获取作业列表
export const getHomeworkList = (classId?: number) => {
  const params = classId ? { classId } : {};
  return HttpUtil.get<RestResponse<HomeworkListVO[]>>('/api/homework/list', { params });
};

// 获取作业详情
export const getHomeworkDetail = (id: number) => {
  return HttpUtil.get<RestResponse<HomeworkDetailVO>>(`/api/homework/${id}`);
};

// 发布新作业
export const createHomework = (data: CreateHomeworkDTO) => {
  return HttpUtil.post<RestResponse<number>>('/api/homework', data);
};

// 更新作业
export const updateHomework = (id: number, data: Partial<CreateHomeworkDTO>) => {
  return HttpUtil.put<RestResponse<void>>(`/api/homework/${id}`, data);
};

// 删除作业
export const deleteHomework = (id: number) => {
  return HttpUtil.delete<RestResponse<void>>(`/api/homework/${id}`);
};

// 获取学生提交列表
export const getSubmissions = (homeworkId: number) => {
  return HttpUtil.get<RestResponse<SubmissionVO[]>>(`/api/homework/${homeworkId}/submissions`);
};

// 获取单个提交详情
export const getSubmissionDetail = (submissionId: number) => {
  return HttpUtil.get<RestResponse<SubmissionVO>>(`/api/homework/submission/${submissionId}`);
};

// 批改作业
export const gradeSubmission = (submissionId: number, data: GradeSubmissionDTO) => {
  return HttpUtil.put<RestResponse<void>>(`/api/homework/submission/${submissionId}/grade`, data);
};

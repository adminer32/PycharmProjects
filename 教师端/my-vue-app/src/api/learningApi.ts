import HttpUtil from '@/utils/HttpUtil'

export interface LearningOverview {
  completionRate: number
  pendingReviewCount: number
  totalStudents: number
  needAttentionCount: number
  needAttentionStudents: Array<{ id: number; name: string; reason: string }>
}

export interface StudentInfo {
  id: number
  name: string
  studentId: string
  planStatus: 'completed' | 'progress' | 'pending'
  weakness: string
  progress: number
}

export interface TrainingTask {
  name?: string
  spec?: string
  video?: string
  day?: string
  content?: string
  duration?: number
  target?: string
  action?: string
}

export interface PlanDetail {
  id: number
  goal: string
  tasks: TrainingTask[]
  progress: number
  reviewStatus?: 'pending' | 'approved' | 'rejected'
  reviewComment?: string
  generatedAt?: string
  weekNumber?: number
  totalWeeks?: number
  weekStart?: string
  weekEnd?: string
}

export interface ReviewInput {
  planId: number
  teacherId: number
  action: 'approve' | 'reject'
  comment?: string
}

export interface ActionVideoInfo {
  id: number
  title: string
  url: string
  duration: string
  category: string
  description?: string
  recommendedTo?: number[]
}

export interface AddVideoInput {
  title: string
  duration: string
  url: string
  category: string
  teacherId: number
  classId?: number
}

export interface RecommendVideoInput {
  videoId: number
  studentIds: number[]
  teacherId: number
  comment?: string
}

export const learningApi = {
  getOverview(classId: string) {
    return HttpUtil.get<{ code: number; message: string; data: LearningOverview }>('/api/learning/overview', { params: { classId } })
  },
  getStudents(classId: string, keyword?: string) {
    return HttpUtil.get<{ code: number; message: string; data: StudentInfo[] }>('/api/learning/students', { params: { classId, keyword: keyword || '' } })
  },
  getPlan(studentId: number) {
    return HttpUtil.get<{ code: number; message: string; data: PlanDetail }>('/api/learning/plan/current-week', { params: { studentId } })
  },
  getWeekPlan(studentId: number, weekNumber?: number) {
    return HttpUtil.get<{ code: number; message: string; data: PlanDetail }>('/api/learning/plan/week', { params: { studentId, weekNumber: weekNumber || 1 } })
  },
  reviewPlan(input: ReviewInput) {
    return HttpUtil.put<{ code: number; message: string; data: boolean }>('/api/learning/plan/review', input)
  },
  getVideos(classId?: string, category?: string) {
    return HttpUtil.get<{ code: number; message: string; data: ActionVideoInfo[] }>('/api/learning/videos', {
      params: { classId: classId || '', category: category || '' }
    })
  },
  addVideo(input: AddVideoInput) {
    return HttpUtil.post<{ code: number; message: string; data: ActionVideoInfo }>('/api/learning/videos', input)
  },
  recommendVideo(input: RecommendVideoInput) {
    return HttpUtil.post<{ code: number; message: string; data: boolean }>('/api/learning/videos/recommend', input)
  },
  getAllPlans(studentId: number) {
    return HttpUtil.get<{ code: number; message: string; data: PlanHistoryItem[] }>('/api/learning/plan/all', { params: { studentId } })
  },
  getPlanGroups(studentId: number) {
    return HttpUtil.get<{ code: number; message: string; data: PlanGroup[] }>('/api/learning/plan/groups', { params: { studentId } })
  },
  deletePlanGroup(studentId: number, planGroupId: number) {
    return HttpUtil.delete<{ code: number; message: string; data: boolean }>(`/api/learning/plan/group`, { params: { studentId, planGroupId } })
  }
}

export interface PlanHistoryItem {
  id: number
  weekNumber: number
  totalWeeks: number
  goal: string
  weekStart?: string
  weekEnd?: string
  progress: number
  reviewStatus: string
  createdAt: string
}

export interface PlanGroup {
  planGroupId: number
  studentId: number
  totalWeeks: number
  overallGoal: string
  createdAt: string
  weekCount: number
  weekNumbers: string[]
  approvedCount: number
  pendingCount: number
}

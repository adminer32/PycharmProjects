import HttpUtil from '@/utils/HttpUtil'

export interface AiHomeworkInput {
  subject: string
  difficulty: 'easy' | 'medium' | 'hard'
  studentCount?: number
  deadline?: string
  classId: string | number
}

export interface AiPlanInput {
  studentId: number | string
  planDuration?: number
  goal?: string
}

export interface AiHomeworkResult {
  title: string
  requirements: string
  tasks: Array<{ name: string; description: string; standard: string }>
  evaluationCriteria: string
}

export interface AiPlanResult {
  goal: string
  weeklyPlans: Array<{
    week: number
    theme: string
    focusSkills: string[]
    dailyTasks: Array<{ day: string; content: string; duration: number }>
    weeklyGoal: string
  }>
  milestones: string[]
  tips: string
}

export const aiApi = {
  generateHomework(input: AiHomeworkInput) {
    return HttpUtil.post<{ code: number; message: string; data: AiHomeworkResult }>('/api/ai/generate-homework', input, { timeout: 120000 })
  },
  generatePlan(input: AiPlanInput) {
    return HttpUtil.post<{ code: number; message: string; data: AiPlanResult }>('/api/ai/generate-plan', input, { timeout: 120000 })
  },
  savePlan(input: { studentId: number | string; classId?: string | number; totalWeeks: number; goal: string; weeklyPlans: any; milestones: any; tips: string }) {
    return HttpUtil.post<{ code: number; message: string }>('/api/ai/save-plan', input)
  }
}

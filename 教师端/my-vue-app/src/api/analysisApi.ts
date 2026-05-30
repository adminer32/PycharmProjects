import HttpUtil from '@/utils/HttpUtil'

export interface AnalysisClassDashboard {
  metrics: {
    avgStudyDuration: number
    homeworkAvg: number
    classCheckinRate: number
    avgSkillPoints: number
  }
  durationTrend: Array<{ label: string, value: number }>
  scoreTrend: Array<{ label: string, value: number }>
  radarData: Array<{ skillType: string, personalScore: number, classAvgScore: number }>
  checkinData: {
    monthRate: number
    weekRate: number
    dayCounts: Record<number, number>
  }
  scoreDistribution: {
    excellent: number
    good: number
    pass: number
    fail: number
  }
  actionDistribution: Array<{ name: string, avg: number, historyAvg: number, feedback: string, recentScores: string }>
}

export interface AnalysisStudentDashboard {
  metrics: {
    studyDuration: number
    homeworkAvg: number
    checkinRate: number
    skillPoints: number
    monthTotalCheckin: number
    continuousCheckin: number
  }
  durationTrend: Array<{ label: string, value: number }>
  scoreTrend: Array<{ label: string, value: number }>
  radarData: Array<{ skillType: string, personalScore: number, classAvgScore: number }>
  actionAnalysis: Array<{ name: string, avg: number, historyAvg: number, feedback: string, recentScores: string }>
  personalCheckinData: number[]
  tags: {
    progressTag: string
    weakTag: string
    strongTag: string
    progressPercent: number
  }
}

export interface AiAdviceItem {
  id?: number
  targetType: string
  targetId?: number
  priorityLevel: string
  title: string
  description: string
  suggestions: string[]
  weekStartDate?: string
  createdAt?: string
}

export const analysisApi = {
  getClassDashboard(classId: number, month?: string) {
    return HttpUtil.get<{ code: number, message: string, data: AnalysisClassDashboard }>(`/api/analysis/class/${classId}/dashboard`, { params: { month } })
  },
  getStudentDashboard(studentId: string | number, month?: string) {
    return HttpUtil.get<{ code: number, message: string, data: AnalysisStudentDashboard }>(`/api/analysis/student/${studentId}/dashboard`, { params: { month } })
  },
  getStudents(classId: number) {
    return HttpUtil.get<{ code: number, message: string, data: any[] }>(`/api/analysis/class/${classId}/students`)
  },
  getClassAdvice(classId: number) {
    return HttpUtil.get<{ code: number, message: string, data: AiAdviceItem[] }>(`/api/ai/advice/class/${classId}`, { timeout: 120000 })
  },
  regenerateClassAdvice(classId: number) {
    return HttpUtil.post<{ code: number, message: string, data: AiAdviceItem[] }>(`/api/ai/advice/class/${classId}/regenerate`, {}, { timeout: 120000 })
  },
  getStudentAdvice(studentId: number) {
    return HttpUtil.get<{ code: number, message: string, data: AiAdviceItem[] }>(`/api/ai/advice/student/${studentId}`, { timeout: 120000 })
  },
  regenerateStudentAdvice(studentId: number) {
    return HttpUtil.post<{ code: number, message: string, data: AiAdviceItem[] }>(`/api/ai/advice/student/${studentId}/regenerate`, {}, { timeout: 120000 })
  },
  getMotionAnalysisData(studentId: string | number) {
    return HttpUtil.get<{ code: number, message: string, data: any }>(`/api/analysis/student/${studentId}/motion-analysis`)
  },
  getStudentPhysique(studentId: string | number) {
    return HttpUtil.get<{ code: number, message: string, data: any }>(`/api/analysis/student/${studentId}/physique`)
  }
}

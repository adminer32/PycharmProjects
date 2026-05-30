export interface Homework {
  id: string
  title: string
  requirement: string
  deadline: string
  deadlineTime: string
  status: 'ongoing' | 'pending' | 'graded' | 'expired'
  submissionCount: number
  totalCount: number
  aiPreGrade: boolean
  demoVideo?: string
  averageScore?: number
  pendingGradeCount?: number
}

export interface Submission {
  id: string
  studentId: string
  studentName: string
  studentAvatar: string
  submitTime: string
  aiScore: number
  teacherScore?: number
  feedback: string
  status: 'pending' | 'graded'
  videoUrl: string
}

export interface HomeworkStats {
  ongoing: number
  pendingGrade: number
  graded: number
  averageCompletion: number
}

export interface CreateHomeworkForm {
  title: string
  requirement: string
  deadline: string
  deadlineTime: string
  demoVideo: File | null
  aiPreGrade: boolean
}

export interface GradeSubmissionForm {
  submissionId: string
  score: number
  feedback: string
}

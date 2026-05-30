export interface ClassOverview {
  totalStudents: number
  todayHomework: { submitted: number; total: number; rate: string }
  todayCheckin: { submitted: number; total: number; rate: string }
  pendingHomework: number
}

export interface ScheduleItem {
  id: string
  day: number
  period: number
  courseName: string
  location: string
  duration?: number // 连续节数，默认 1
}

export interface PendingItem {
  id: string
  icon: string
  title: string
  description: string
  actionText: string
  actionRoute: string
}

export interface Notice {
  id: string
  title: string
  content: string
  time: string
  isImportant: boolean
  readCount: number
  totalStudents: number
}

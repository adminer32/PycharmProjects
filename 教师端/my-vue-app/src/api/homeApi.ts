import HttpUtil from '@/utils/HttpUtil'

export interface HomeOverview {
  totalStudents: number
  todayHomework: { submitted: number; total: number; rate: string }
  todayCheckin: { submitted: number; total: number; rate: string }
  pendingHomework: number
}

export interface PendingItem {
  id: number | string
  icon: string
  title: string
  description: string
  actionText: string
  actionRoute: string
  itemType: string
  countTotal?: number
  countUrgent?: number
}

export interface Notice {
  id: number | string
  title: string
  content: string
  time: string
  isImportant: boolean
  readCount: number
  totalStudents: number
  notificationType: string
}

export interface NoticeInput {
  id?: number | string
  classId: number
  teacherId: number
  title: string
  content: string
  isImportant: boolean
}

export const homeApi = {
  getHomeOverview(classId: string) {
    return HttpUtil.get<{ code: number, message: string, data: HomeOverview }>(`/api/home/overview`, { params: { classId } })
  },
  getPendingItems(teacherId: number, classId?: string) {
    return HttpUtil.get<{ code: number, message: string, data: PendingItem[] }>(`/api/home/pending`, { params: { teacherId, classId } })
  },
  getNotices(classId: string, page?: number, size?: number) {
    return HttpUtil.get<{ code: number, message: string, data: { list: Notice[], total: number } }>(`/api/home/notices`, { params: { classId, page: page || 1, size: size || 5 } })
  },
  publishNotice(input: NoticeInput) {
    return HttpUtil.post<{ code: number, message: string, data: Notice }>(`/api/home/notice`, input)
  },
  updateNotice(input: NoticeInput) {
    return HttpUtil.put<{ code: number, message: string, data: Notice }>(`/api/home/notice`, input)
  },
  deleteNotice(id: number | string) {
    return HttpUtil.delete<{ code: number, message: string, data: boolean }>(`/api/home/notice/${id}`)
  }
}

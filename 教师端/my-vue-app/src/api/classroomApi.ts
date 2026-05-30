import HttpUtil from '@/utils/HttpUtil'

export interface ClassroomLesson {
  id: number | string
  title: string
  thumbnail: string
  videoUrl?: string
  duration: string
  recordedAt: string
  className: string
}

export interface ClassroomKnowledgePoint {
  id: number | string
  title: string
  description: string
  order: number
  hasVideo: boolean
  videoUrl?: string
  videoDuration?: string
  videoThumbnail?: string
  lessonId: number | string
}

export interface ClassroomNote {
  id: number | string
  content: string
  createdAt: string
  lessonId: number | string
}

export interface AddKnowledgeInput {
  lessonId: number | string
  title: string
  description: string
  demoVideoUrl?: string
}

export interface AddNoteInput {
  lessonId: number | string
  content: string
}

export interface SaveNotesInput {
  lessonId: number | string
  notes: { id?: number | string; content: string }[]
}

export const classroomApi = {
  getLessons(classId: string) {
    return HttpUtil.get<{ code: number; message: string; data: ClassroomLesson[] }>('/api/classroom/lessons', { params: { classId } })
  },
  getKnowledgePoints(lessonId: number | string) {
    return HttpUtil.get<{ code: number; message: string; data: ClassroomKnowledgePoint[] }>('/api/classroom/knowledge', { params: { lessonId } })
  },
  getNotes(lessonId: number | string) {
    return HttpUtil.get<{ code: number; message: string; data: ClassroomNote[] }>('/api/classroom/notes', { params: { lessonId } })
  },
  addKnowledge(input: AddKnowledgeInput) {
    return HttpUtil.post<{ code: number; message: string; data: ClassroomKnowledgePoint }>('/api/classroom/knowledge', input)
  },
  addNote(input: AddNoteInput) {
    return HttpUtil.post<{ code: number; message: string; data: ClassroomNote }>('/api/classroom/note', input)
  },
  updateKnowledgeVideo(id: number | string, videoUrl: string) {
    return HttpUtil.put<{ code: number; message: string }>(`/api/classroom/knowledge/${id}/video`, { videoUrl })
  },
  saveNotes(input: SaveNotesInput) {
    return HttpUtil.post<{ code: number; message: string }>('/api/classroom/notes/save', input)
  },
  analyzeVideo(videoId: number | string) {
    return HttpUtil.post<{ code: number; message: string; data: { totalStudents: number; activeStudents: number; attendanceRate: number; participationRate: number } }>('/api/classroom/analyze', { videoId })
  }
}

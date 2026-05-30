export interface VideoItem {
  id: string;
  title: string;
  thumbnail: string;
  duration: string;
  date: string;
  status: 'watched' | 'unwatched' | 'progress';
  progress?: number;
  tags: string[];
  category: string;
}

export interface Marker {
  time: number;
  timeStr: string;
  desc: string;
  marked: boolean;
}

export interface Note {
  content: string;
}

export interface Term {
  name: string;
  desc: string;
}

export interface KnowledgePoint {
  id: string;
  title: string;
  description: string;
  order: number;
  hasVideo: boolean;
  videoUrl?: string;
  videoDuration?: string;
  videoThumbnail?: string;
}

export interface KnowledgePointForm {
  title: string;
  description: string;
}

export interface LessonItem {
  id: string
  title: string
  thumbnail: string
  duration: string
  recordedAt: string
}

export interface LessonVideo {
  id: string
  title: string
  description: string
  videoUrl: string
  thumbnail: string
  duration: string
  recordedAt: string
  classId: number
  className: string
}

export interface AnalysisResult {
  id: string;
  timestamp: number;
  type: 'pose' | 'action' | 'technique';
  title: string;
  description: string;
  score?: number;
  suggestions?: string[];
  imageUrl?: string;
}

export interface VideoAnalysis {
  videoId: string;
  totalScore: number;
  poseAnalysis: AnalysisResult[];
  actionAnalysis: AnalysisResult[];
  techniqueAnalysis: AnalysisResult[];
  summary: string;
}

export interface LessonItem {
  id: string;
  title: string;
  thumbnail: string;
  duration: string;
  recordedAt: string;
  className: string;
}

export interface KnowledgePointForm {
  title: string;
  description: string;
}

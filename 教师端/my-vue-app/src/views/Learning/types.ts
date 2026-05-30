export type PlanReviewStatus = 'pending' | 'approved' | 'rejected'

export interface Student {
  id: number;
  name: string;
  studentId: string;
  planStatus: 'completed' | 'progress' | 'pending';
  weakness: string;
  progress: number;
}

export interface TrainingTask {
  name?: string;
  spec?: string;
  video?: string;
  day?: string;
  content?: string;
  duration?: number;
  target?: string;
  action?: string;
}

export interface TrainingPlan {
  goal: string;
  tasks: TrainingTask[];
  progress: number;
  reviewStatus?: PlanReviewStatus;
  reviewComment?: string;
  generatedAt?: string;
}

export interface VideoItem {
  id: string;
  title: string;
  duration: string;
  url: string;
  category: string;
  recommendedTo?: number[];
}

export interface VideoRecommendation {
  videoId: number;
  studentId: number;
  comment?: string;
  recommendedAt: string;
}

export interface VideoFormData {
  title: string;
  duration: string;
  url: string;
  category: string;
}

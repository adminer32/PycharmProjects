import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel';

interface Score {
    avgFluencyScore: number;
    avgOverallScore: number;
    avgProficiencyScore: number;
    avgStabilityScore: number;
}

export const getScoreByMotionNameApi = (motionName: string) =>
    HttpUtil.get<Result<Score>>('/api/visualization/getMotionNameAnalysis', {
        params: { motionName },
    });

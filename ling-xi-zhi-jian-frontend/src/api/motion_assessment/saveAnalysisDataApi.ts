import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel';

export const saveAnalysisDataApi = (
    studentId: number,
    analysisData: Record<string, any>,
) =>
    HttpUtil.post<Result<null>>('/api/analysis/save', {
        student_id: studentId,
        analysis_data: analysisData,
    });

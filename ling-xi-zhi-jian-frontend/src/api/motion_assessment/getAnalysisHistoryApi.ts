import HttpUtil from '../../utils/HttpUtil';
import type { Result } from '@/types/globel.d';

interface Analysis {
    // 自己的 XY 数据
    selfLeftXData: number[];
    selfLeftYData: number[];

    selfRightXData: number[];
    selfRightYData: number[];

    motionName: string;

    videoUrl: string;

    taskId: string;

    result: {
        // 评分
        stabilityScore: number; // 稳定性评分
        proficiencyScore: number; // 熟练度评分
        fluencyScore: number; // 流畅度评分
        overallScore: number; // 综合评分

        // 建议
        stabilityImprovement: string; // 稳定性改进
        proficiencyEnhancement: string; // 能力增强
        fluencyPromotion: string; // 流畅度提升
        generalAdvice: string; // 综合建议
    };
}

/**
 * 根据 historyId 获取分析记录
 */
export const getAnalysisHistoryApi = (historyId: number) => {
    return new Promise<Result<Analysis>>((resolve) => {
        HttpUtil.get<
            Result<{
                contentList: [
                    {
                        id: string;
                        type: 'card' | 'text';
                        text: string;
                        card: {
                            title: string;
                            videoUrl: string;
                            analysis: {
                                selfLeftXData: number[];
                                selfLeftYData: number[];
                                selfRightXData: number[];
                                selfRightYData: number[];
                                standardLeftXData: number[];
                                standardLeftYData: number[];
                                standardRightXData: number[];
                                standardRightYData: number[];
                                motionName: string;
                                taskId: string;
                                stabilityScore: number;
                                proficiencyScore: number;
                                fluencyScore: number;
                                overallScore: number;
                                stabilityImprovement: string;
                                proficiencyEnhancement: string;
                                fluencyPromotion: string;
                                generalAdvice: string;
                            };
                        };
                        sender: 'ai' | 'user';
                        time: string;
                    },
                ];
            }>
        >('/video/history/detail', {
            params: {
                historyId,
            },
        }).then((res) => {
            if (res.status == 'success') {
                const content = res.data?.contentList?.filter(
                    (content) => content.type === 'card',
                )[0];
                resolve({
                    status: 'success',
                    data: {
                        selfLeftXData: content?.card?.analysis?.selfLeftXData,
                        selfLeftYData: content?.card?.analysis?.selfLeftYData,
                        selfRightXData: content?.card?.analysis?.selfRightXData,
                        selfRightYData: content?.card?.analysis?.selfRightYData,
                        motionName: content?.card?.analysis?.motionName,
                        videoUrl: content?.card?.videoUrl,
                        taskId: content?.card?.analysis?.taskId,
                        result: {
                            stabilityScore: content?.card?.analysis?.stabilityScore,
                            proficiencyScore: content?.card?.analysis?.proficiencyScore,
                            fluencyScore: content?.card?.analysis?.fluencyScore,
                            overallScore: content?.card?.analysis?.overallScore,
                            stabilityImprovement: content?.card?.analysis?.stabilityImprovement,
                            proficiencyEnhancement: content?.card?.analysis?.proficiencyEnhancement,
                            fluencyPromotion: content?.card?.analysis?.fluencyPromotion,
                            generalAdvice: content?.card?.analysis?.generalAdvice,
                        },
                    } as Analysis,
                    message: res.message,
                });
            }
        });
    });
};

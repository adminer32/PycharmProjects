import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel.d';

/**
 * 根据 historyId 获取聊天记录记录
 */
export const getChatHistoryApi = (historyId: number) => {
    return new Promise<
        Result<
            {
                text: string;
                sender: 'ai' | 'user';
            }[]
        >
    >((resolve) => {
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
                                // 自己的 XY 数据
                                selfLeftXData: number[];
                                selfLeftYData: number[];

                                selfRightXData: number[];
                                selfRightYData: number[];

                                // 标准的 XY 数据
                                standardLeftXData: number[];
                                standardLeftYData: number[];

                                standardRightXData: number[];
                                standardRightYData: number[];

                                motionName: string;

                                taskId: string;

                                stabilityScore: number; // 稳定性评分
                                proficiencyScore: number; // 熟练度评分
                                fluencyScore: number; // 流畅度评分
                                overallScore: number; // 综合评分

                                stabilityImprovement: string; // 稳定性改进
                                proficiencyEnhancement: string; // 能力增强
                                fluencyPromotion: string; // 流畅度提升
                                generalAdvice: string; // 综合建议
                            };
                        };
                        sender: 'ai' | 'user';
                        time: string;
                    },
                ];
            }>
        >('/api/video/history/detail', {
            params: {
                historyId,
            },
        }).then((res) => {
            if (res.status == 'success') {
                resolve({
                    status: 'success',
                    data: res.data.contentList
                        .filter((content) => content.type === 'text')
                        .map((content) => {
                            return {
                                text: content.text,
                                sender: content.sender,
                            };
                        }),
                    message: res.message,
                });
            }
        });
    });
};

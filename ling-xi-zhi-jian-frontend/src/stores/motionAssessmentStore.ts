import { defineStore } from 'pinia';
import { type Result } from '@/types/globel.d';
import HttpUtil from '@/utils/HttpUtil';
import TimeUtil from '@/utils/TimeUtil';
import { SystemConstant } from '@/constants/SystemConstant';
import MessageUtil from '@/utils/MessageUtil';
import { getAnalysisHistoryApi } from '@/api/motion_assessment/getAnalysisHistoryApi';
import { getMotionAssessmentHistoryListApi } from '@/api/motion_assessment/getMotionAssessmentHistoryListApi';
import { getChatHistoryApi } from '@/api/motion_assessment/getChatHistoryApi';
import { submitAnalysisTaskApi } from '@/api/motion_assessment/submitAnalysisTaskApi';
import { saveAnalysisDataApi } from '@/api/motion_assessment/saveAnalysisDataApi';
import { useUserStore } from './userStore';

export const motionOption = {
    // "准备姿势": [
    //     "左右开位站立",
    //     "前后开位站立"
    // ],
    // "步法移动": [
    //     "前上步",
    //     "后撤步",
    //     "滑步",
    //     "交叉步",
    //     "并步",
    //     "跨步",
    //     "转体上步",
    //     "跑动步"
    // ],

    踢球动作: [
        '脚背踢球', // 已有模型
        '单脚内侧踢球', // 已有模型
        '双脚内侧踢球', // 已有模型
        '脚外侧踢球', // 已有模型
    ],
    单人专项动作: [
        '绷踢',
        '盘踢',
        '磕踢',
        '对踢',
        '拐踢',
        '头触球',
        '跳踢',
        '胸触球',
    ],
    发球动作: [
        '脚外侧发球', // 已有模型
        '正脚背发球', // 已有模型
        '脚内侧发球',
    ],
    触球动作: [
        '大腿触球', // 已有模型
        '单腿触踢球',
        '腹部触踢球',
        '胸部触踢球',
        '头部触踢球',
        '头球',
        // "倒钩球",
        // "脚踏球"
    ],
};

export interface History {
    id: number;
    action_type: string;
    createTime: string;
}

interface ChatMessage {
    text: string;
    sender: 'user' | 'ai';
}

interface AnalysisResult {
    stabilityScore: number;
    proficiencyScore: number;
    fluencyScore: number;
    overallScore: number;

    taskStatus: '已完成' | '失败' | string;

    stabilityImprovement: string;
    proficiencyEnhancement: string;
    fluencyPromotion: string;
    generalAdvice: string;
}

let abortController: AbortController | null = null;
let ws: WebSocket | null = null;
const wsBaseUrl = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8001';
export const useMotionAssessmentStore = defineStore(
    'useMotionAssessmentStore',
    {
        state: () => ({
            /**
             * 默认跟随系统
             */
            uploadProgress: 0,
            analysisProgress: 0,
            historyList: [] as History[],
            currentShowHistoryId: null as number | null,
            messageList: [] as ChatMessage[],
            isAnalyzing: false,
            isResultLoading: false,
        }),

        actions: {
            initWs(onMessage: () => void) {
                return new Promise<void>((resolve) => {
                    if (!wsBaseUrl) {
                        console.warn('WebSocket baseUrl not configured');
                        resolve();
                        return;
                    }
                    const webSocketUrl = wsBaseUrl + '/ws/ai?token=' + localStorage.getItem(SystemConstant.TOKEN);
                    ws = new WebSocket(webSocketUrl);
                    ws.onerror = () => {
                        console.warn('WebSocket connection error');
                    };
                    ws.onclose = () => {
                        console.log('WebSocket closed');
                    };
                    ws.onopen = () => resolve();
                    ws.onmessage = (event) => {
                        const msg = event.data as string;
                        if (
                            this.messageList[this.messageList.length - 1]
                                .sender !== 'ai'
                        ) {
                            this.messageList.push({
                                text: msg,
                                sender: 'ai',
                            } as ChatMessage);
                        } else {
                            this.messageList[
                                this.messageList.length - 1
                            ].text += msg;
                        }
                        onMessage();
                    };
                });
            },

            isOpenWs() {
                return ws !== null && ws.readyState === WebSocket.OPEN;
            },

            closeWs() {
                ws?.close();
                ws = null;
            },

            /**
             * 开始分析
             */
            startAnalysis(motionName: string, file: Blob) {
                return new Promise<void>((resolve, reject) => {
                    if (this.currentShowHistoryId == null) {
                        MessageUtil.error('请先选择历史记录！');
                        return;
                    }
                    submitAnalysisTaskApi(
                        this.currentShowHistoryId,
                        motionName,
                        file,
                        (loaded, total) => {
                            this.uploadProgress = parseFloat(
                                ((loaded / total) * 100).toFixed(2),
                            );
                        },
                    ).then((res) => {
                        if (res.status === 'success') {
                            resolve(); // 返回成功消息
                        } else {
                            MessageUtil.error(res.message);
                            reject(new Error(res.message)); // 返回错误消息
                        }
                    });
                });
            },

            /**
             * 根据 historyId 获取分析记录
             */
            getAnalysisHistory(historyId: number) {
                return getAnalysisHistoryApi(historyId);
            },

            /**
             * 获取动作评估的历史记录
             */
            getMotionAssessmentHistoryList() {
                getMotionAssessmentHistoryListApi().then((res) => {
                    if (res.status == 'success') {
                        this.historyList = res.data.records;
                    } else {
                        MessageUtil.error(res.message);
                    }
                });
            },

            /**
             * 获取聊天历史记录
             */
            getChatMessageHistory(historyId: number) {
                getChatHistoryApi(historyId).then((res) => {
                    if (res.status == 'success') {
                        this.messageList = res.data;
                    }
                });
            },

            /**
             * 取消上传
             */
            cancelUpload() {
                abortController?.abort();
                abortController = null;
            },

            /**
             * 创建一个新的历史记录
             */
            createHistory(motionName: string) {
                return new Promise<void>((resolve) => {
                    // 获取当前时间并格式化为 HH:mm
                    HttpUtil.post<Result<{ history_id: number }>>(
                        '/api/video/history/save',
                        {
                            id: 0,
                            content: '',
                        },
                    ).then((res) => {
                        if (res.status === 'success') {
                            // 将新建的历史记录添加到 historyList 的开头
                            this.historyList.unshift({
                                id: res.data.history_id, // 使用返回的 history_id
                                action_type: motionName,
                                createTime: TimeUtil.getCurrentTime(),
                            });

                            // 设置当前显示的历史记录 ID 为新创建的 ID
                            this.currentShowHistoryId = res.data.history_id;

                            // 返回新创建的历史记录 ID
                            resolve();
                        }
                    });
                });
            },

            deleteHistory(ids: number[]) {
                return new Promise<void>((resolve, reject) => {
                    // 将多个 ID 用逗号分隔拼接成字符串
                    const idString = ids.join(',');
                    HttpUtil.delete<Result<null>>(
                        `/api/video/history/del?ids=${idString}`,
                    ) // 直接将字符串拼接到 URL 中
                        .then((res) => {
                            if (res.status === 'success') {
                                // 从本地的 historyList 中移除对应的历史记录
                                this.historyList = this.historyList.filter(
                                    (history) => !ids.includes(history.id),
                                );
                                MessageUtil.success('删除历史记录成功');
                                resolve();
                            } else {
                                MessageUtil.error(res.message);
                                reject();
                            }
                        });
                });
            },

            sendMessage(text: string) {
                this.messageList.push({ text, sender: 'user' });
                // 创建 FormData 对象
                const formData = new FormData();
                formData.append(
                    'aiContentInfo',
                    JSON.stringify({
                        historyId: this.currentShowHistoryId,
                        type: 'text',
                        card: null,
                        text,
                        sender: 'user',
                    }),
                ); // 参数名为 aiContentInfo，值为 JSON 字符串
                // 调用后端接口
                HttpUtil.postForm<Result<null>>(
                    '/api/content/insert',
                    formData,
                    {
                        headers: {
                            'Content-Type': 'multipart/form-data', // 设置请求头
                        },
                    },
                ).then((res) => {
                    if (res.status === 'error') {
                        MessageUtil.error(res.message);
                    }
                });
            },

            /**
             * 保存分析数据
             */
            saveAnalysisData(
                analysisData: Record<string, any>,
            ): Promise<void> {
                return new Promise<void>((resolve, reject) => {
                    const userStore = useUserStore();
                    const studentId = Number(userStore.myInfo.userId);
                    saveAnalysisDataApi(
                        studentId,
                        analysisData,
                    ).then((res) => {
                        if (res.status === 'success') {
                            resolve();
                        } else {
                            MessageUtil.error(res.message);
                            reject(new Error(res.message || '保存分析数据失败'));
                        }
                    });
                });
            },

            queryAnalysisResultByTaskId(taskId: string) {
                return new Promise<AnalysisResult>((resolve, reject) => {
                    // 调用后端接口
                    HttpUtil.get<Result<AnalysisResult>>(
                        '/api/analysis/queryByTaskId',
                        {
                            params: { taskId }, // 通过 params 传递参数
                        },
                    )
                        .then((res) => {
                            if (res.status === 'success') {
                                // 如果查询成功，调用 resolve 并返回数据
                                resolve(res.data);
                            } else {
                                // 如果后端返回错误，调用 reject 并传递错误信息
                                MessageUtil.error(res.message);
                                reject(
                                    new Error(
                                        res.message || '查询分析数据失败',
                                    ),
                                );
                            }
                        })
                        .catch((error) => {
                            // 捕获网络或其他错误
                            MessageUtil.error('查询分析数据失败:');
                            reject(error);
                        });
                });
            },
        },
    },
);

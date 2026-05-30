/**
 * AI 分析本地缓存管理
 * 使用 localStorage 持久化存储 AI 分析结果
 */
import { defineStore } from 'pinia';
import HttpUtil from '@/utils/HttpUtil';
import MessageUtil from '@/utils/MessageUtil';

export interface AILocalCache {
    videoPath: string;
    taskId: string;
    autoChapters: string;
    summarization: string;
    meetingAssistance: string;
    taskStatus: '进行中' | '已完成' | '失败';
    updateTime: number;
}

const CACHE_KEY = 'ai_analysis_cache';
const MAX_CACHE_SIZE = 100;

export const useAiAnalysisCacheStore = defineStore('useAiAnalysisCacheStore', {
    state: () => ({
        cacheMap: new Map<string, AILocalCache>(),
        isAnalyzingMap: new Map<string, boolean>(),
    }),

    actions: {
        init() {
            this.loadFromLocalStorage();
        },

        loadFromLocalStorage() {
            try {
                const data = localStorage.getItem(CACHE_KEY);
                if (data) {
                    const cacheArray: AILocalCache[] = JSON.parse(data);
                    this.cacheMap.clear();
                    for (const item of cacheArray) {
                        this.cacheMap.set(item.videoPath, item);
                    }
                }
            } catch (e) {
                console.error('加载AI分析缓存失败:', e);
            }
        },

        saveToLocalStorage() {
            try {
                const cacheArray = Array.from(this.cacheMap.values());
                if (cacheArray.length > MAX_CACHE_SIZE) {
                    const sorted = cacheArray.sort((a: AILocalCache, b: AILocalCache) => b.updateTime - a.updateTime);
                    const toKeep = sorted.slice(0, MAX_CACHE_SIZE);
                    this.cacheMap.clear();
                    for (const item of toKeep) {
                        this.cacheMap.set(item.videoPath, item);
                    }
                }
                localStorage.setItem(CACHE_KEY, JSON.stringify(Array.from(this.cacheMap.values())));
            } catch (e) {
                console.error('保存AI分析缓存失败:', e);
            }
        },

        getCache(videoPath: string): AILocalCache | null {
            return this.cacheMap.get(videoPath) || null;
        },

        setCache(videoPath: string, cache: Omit<AILocalCache, 'videoPath' | 'updateTime'>) {
            const newCache: AILocalCache = {
                videoPath,
                ...cache,
                updateTime: Date.now(),
            };
            this.cacheMap.set(videoPath, newCache);
            this.saveToLocalStorage();
        },

        isAnalyzing(videoPath: string): boolean {
            return this.isAnalyzingMap.get(videoPath) || false;
        },

        setAnalyzing(videoPath: string, analyzing: boolean) {
            if (analyzing) {
                this.isAnalyzingMap.set(videoPath, true);
            } else {
                this.isAnalyzingMap.delete(videoPath);
            }
        },

        async triggerAnalysis(videoPath: string, videoUrl: string): Promise<AILocalCache | null> {
            if (this.isAnalyzing(videoPath)) {
                MessageUtil.info('该视频正在分析中，请稍候...');
                return null;
            }

            const existingCache = this.getCache(videoPath);
            if (existingCache && existingCache.taskStatus === '已完成') {
                return existingCache;
            }

            this.setAnalyzing(videoPath, true);

            try {
                const title = videoPath.split('/').pop()?.replace('.mp4', '') || '未命名课程';

                const addTaskRes = await HttpUtil.post<{ status: string; message?: string; data: { taskId: string } }>('/api/ai/addTranscriptionTask', {
                    title,
                    video_url: videoUrl,
                });

                if (addTaskRes.status !== 'success') {
                    throw new Error(addTaskRes.message);
                }

                const taskId = addTaskRes.data.taskId;

                const fullProcessRes = await HttpUtil.post<{ status: string; message?: string }>('/api/v0/transcribe/full-process', {
                    task_id: taskId,
                    video_url: videoUrl,
                });

                if (fullProcessRes.status !== 'success') {
                    throw new Error(fullProcessRes.message);
                }

                this.setCache(videoPath, {
                    taskId,
                    autoChapters: '',
                    summarization: '',
                    meetingAssistance: '',
                    taskStatus: '进行中',
                });

                return await this.pollTaskStatus(videoPath, taskId);

            } catch (error: unknown) {
                console.error('触发AI分析失败:', error);
                const errorMessage = error instanceof Error ? error.message : '未知错误';
                MessageUtil.error('AI分析启动失败: ' + errorMessage);
                this.setAnalyzing(videoPath, false);
                return null;
            }
        },

        async pollTaskStatus(videoPath: string, taskId: string): Promise<AILocalCache | null> {
            return new Promise((resolve) => {
                let pollCount = 0;
                const maxPolls = 200; // 最多轮询200次，约10分钟
                
                const poll = async () => {
                    if (!this.isAnalyzing(videoPath)) {
                        resolve(null);
                        return;
                    }

                    pollCount++;
                    
                    // 超时处理
                    if (pollCount > maxPolls) {
                        console.error('AI分析超时');
                        this.setAnalyzing(videoPath, false);
                        this.setCache(videoPath, {
                            taskId,
                            autoChapters: '',
                            summarization: '',
                            meetingAssistance: '',
                            taskStatus: '失败',
                        });
                        MessageUtil.error('AI分析超时，请重试');
                        resolve(null);
                        return;
                    }

                    try {
                        const res = await HttpUtil.get<{ status: string; data: { taskStatus: '进行中' | '已完成' | '失败'; autoChapters?: string; summarization?: string; meetingAssistance?: string } }>('/api/ai/queryTaskStatus', {
                            params: { taskId },
                        });

                        if (res.status === 'success' && res.data) {
                            const taskStatus = res.data.taskStatus;

                            this.setCache(videoPath, {
                                taskId,
                                autoChapters: res.data.autoChapters || '',
                                summarization: res.data.summarization || '',
                                meetingAssistance: res.data.meetingAssistance || '',
                                taskStatus,
                            });

                            if (taskStatus !== '进行中') {
                                this.setAnalyzing(videoPath, false);
                                if (taskStatus === '已完成') {
                                    MessageUtil.success('AI分析完成！');
                                } else {
                                    MessageUtil.error('AI分析失败');
                                }
                                resolve(this.getCache(videoPath));
                                return;
                            }
                        }
                    } catch (e) {
                        console.error('查询任务状态失败:', e);
                    }

                    setTimeout(poll, 3000);
                };

                poll();
            });
        },

        clearCache(videoPath?: string) {
            if (videoPath) {
                this.cacheMap.delete(videoPath);
            } else {
                this.cacheMap.clear();
            }
            this.saveToLocalStorage();
        },
    },
});

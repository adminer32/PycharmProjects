import { defineStore } from 'pinia';
import type { Result } from '@/types/globel';
import HttpUtil from '@/utils/HttpUtil';
import { message } from 'ant-design-vue';
import { queryTaskStatusApi } from '@/api/ai_learn_friend/queryTaskStatusApi';
import MessageUtil from '@/utils/MessageUtil';
import axios from 'axios';

interface AIChatNote {
    id: number;
    taskId: string;
    createTime: string;
    videoUrl: string;
    title: string;
    autoChapters: string; // 笔记   // 没分析出来时返回的是字符串分析出来返回的是JSON
    summarization: string; // json
    meetingAssistance: string; // json
    personalNote: string;
    taskStatus: '进行中' | '已完成' | '失败';
    _isSelected: boolean; // 下划线代表只在前端存在
}

let abortController: AbortController | null = null;
export const useAiNoteStore = defineStore('useAiNoteStore', {
    state: () => {
        return {
            historyList: [] as AIChatNote[],
            selectedIndex: null as null | number,
            showUploadPage: true,
            isVideoUploading: false,
            uploadProgress: 0,
        };
    },

    getters: {
        computedCurrentNote(state) {
            return state.historyList[state.selectedIndex!];
        },
    },

    actions: {
        queryTaskStatus(taskId: string) {
            return new Promise<Omit<AIChatNote, '_isSelected'>>(
                (resolve, reject) => {
                    queryTaskStatusApi(taskId)
                        .then((res) => {
                            if (res.status === 'success') {
                                resolve(res.data);
                            } else {
                                reject();
                                message.error(res.message).then();
                            }
                        })
                        .catch(() => {
                            reject();
                        });
                },
            );
        },

        /**
         * AI 润色
         * @param text
         */
        notePolishing(text: string) {
            return new Promise<string>((resolve, reject) => {
                HttpUtil.post<Result<string>>('/api/ai/NotePolishing', {
                    message: text,
                })
                    .then((res) => {
                        if (res.status === 'success') {
                            message.success(res.message).then();
                            resolve(res.data);
                        } else {
                            reject();
                            message.error(res.message).then();
                        }
                    })
                    .catch((err) => {
                        message.error(err.message).then();
                        reject();
                    });
            });
        },

        /**
         * 保存笔记
         * @param text
         */
        saveNote(text: string) {
            HttpUtil.post<Result<null>>('/api/ai/updateNote', {
                id: this.historyList[this.selectedIndex!].id,
                title: '',
                personal_note: text,
            }).then((res) => {
                if (res.status === 'success') {
                    this.historyList[this.selectedIndex!]!.personalNote = text;
                    // message.success(res.message).then();
                } else {
                    message.error(res.message).then();
                }
            });
        },

        cancelUpload() {
            abortController?.abort();
            abortController = null;
            this.isVideoUploading = false;
        },
        getHistoryList() {
            HttpUtil.post<
                Result<{
                    list: AIChatNote[];
                }>
            >('/api/ai/queryPersonalNotes', {
                param: {
                    title: '',
                    id: '',
                    task_status: '',
                    task_id: '',
                },
            }).then((res) => {
                if (res.status === 'success') {
                    this.historyList = res.data.list.map((item) => {
                        return {
                            ...item,
                            _isSelected: false,
                        };
                    });
                } else {
                    message.error(res.message).then();
                }
            });
        },

        editHistory(title: string, personalNote: string, id?: number) {
            return new Promise<void>((resolve, reject) => {
                if (!id) {
                    id = this.historyList[this.selectedIndex!].id;
                }
                HttpUtil.post<Result<null>>('/api/ai/updateNote', {
                    id,
                    title,
                    personal_note: personalNote,
                }).then((res) => {
                    if (res.status === 'success') {
                        this.getHistoryList();
                        message.success(res.message).then();
                        resolve();
                    } else {
                        message.error(res.message).then();
                        reject();
                    }
                });
            });
        },

        upLoadFile(file: File) {
            this.isVideoUploading = true;
            this.uploadProgress = 0;
            abortController = new AbortController();
            return new Promise<{
                url: string;
                src: string;
            }>((resolve, reject) => {
                const uploadAxios = axios.create();
                const formData = new FormData();
                formData.append('file', file);
                uploadAxios.postForm<Result<{ object: string }>>(
                    '/api/v0/file/operation/upload',
                    formData,
                    {
                        onUploadProgress: (e) => {
                            this.uploadProgress = parseFloat(
                                ((e.loaded / e.total!) * 100).toFixed(2),
                            );
                        },
                        signal: abortController?.signal,
                    },
                )
                    .then((res) => {
                        if (res.data.status === 'success') {
                            this.uploadProgress = 100;
                            resolve({
                                url: res.data.data.object,
                                src: URL.createObjectURL(file),
                            });
                        } else {
                            message.error(res.data.message).then();
                            reject();
                        }
                    })
                    .catch((err) => {
                        message.error('上传失败').then();
                        reject(err);
                    })
                    .finally(() => {
                        this.isVideoUploading = false;
                    });
            });
        },

        remove(indexList: number[]) {
            HttpUtil.delete<Result<null>>('/api/ai/del', {
                params: {
                    ids: indexList
                        .map((index) => this.historyList[index].id)
                        .join(','),
                },
            }).then((res) => {
                if (res.status === 'success') {
                    this.getHistoryList();
                    this.showUploadPage = true;
                    if (indexList.includes(this.selectedIndex!)) {
                        this.selectedIndex = null;
                    }
                } else {
                    message.error(res.message).then();
                }
            });
        },

        upLoadHistory(title: string, fileUrl: string, fileSrc: string) {
            HttpUtil.post<Result<AIChatNote>>('/api/ai/addTranscriptionTask', {
                title: title,
                video_url: fileUrl,
            }).then(async (res) => {
                if (res.status === 'success') {
                    this.historyList.unshift({
                        ...res.data,
                        videoUrl: fileSrc,
                        _isSelected: false,
                    });
                    this.selectedIndex = 0;
                    this.showUploadPage = false;
                    
                    // 启动完整视频处理流程（转录+分析）
                    try {
                        await HttpUtil.post<Result<any>>('/api/v0/transcribe/full-process', {
                            task_id: res.data.taskId,
                            video_url: fileUrl,
                        });
                    } catch (e) {
                        console.error('视频处理失败:', e);
                    }
                    
                    // 定时查询任务状态
                    const t = setInterval(() => {
                        this.queryTaskStatus(res.data.taskId)
                            .then((note) => {
                                if (note.taskStatus !== '进行中') {
                                    for (
                                        let i = 0;
                                        i < this.historyList.length;
                                        i++
                                    ) {
                                        if (
                                            this.historyList[i].taskId ==
                                            note.taskId
                                        ) {
                                            this.historyList[i] = {
                                                ...note,
                                                _isSelected: false,
                                            };
                                            break;
                                        }
                                    }
                                    clearInterval(t);
                                }
                            })
                            .catch(() => {
                                clearInterval(t);
                            });
                    }, 3000);
                } else {
                    MessageUtil.error(res.message);
                }
            });
        },

        createNote() {
            this.selectedIndex = null;
            this.showUploadPage = true;
        },
    },
});

/**
 * 流式 AI 状态管理
 */

import { defineStore } from 'pinia';
import { SystemConstant } from '@/constants/SystemConstant';

export interface Message {
    text: string;
    sender: 'user' | 'ai';
}

let ws: WebSocket | null = null;
const baseUrl = import.meta.env.VITE_WS_BASE_URL;

/**
 * 只用于 课程中心 和 AI学习伴侣
 */
export const useAiChatStore = defineStore('useAiChatStore', {
    state: () => {
        return {
            messageList: [] as Message[],
        };
    },

    getters: {},

    actions: {
        clearMessageList() {
            this.messageList = [
                {
                    text: '您有什么问题就向我提问吧，我可以为你解答视频知识的相关问题！',
                    sender: 'ai',
                },
            ];
        },

        wsInit(onMessage: () => void, taskId?: string) {
            this.clearMessageList();
            
            if (!baseUrl) {
                console.warn('WebSocket baseUrl not configured, skipping connection');
                return;
            }
            
            const wsUrl = baseUrl + '/ws/ai?token=' + localStorage.getItem(SystemConstant.TOKEN);
            ws = new WebSocket(wsUrl);
            
            ws.onerror = () => {
                console.warn('WebSocket connection error, AI chat may not work');
            };
            
            ws.onclose = () => {
                console.log('WebSocket closed');
            };
            
            ws.onopen = () => {
                // 发送初始化消息，传入 task_id
                if (taskId) {
                    ws?.send(JSON.stringify({ type: 'init', task_id: taskId }));
                }
            };
            ws.onmessage = (event) => {
                const data = JSON.parse(event.data as string);
                if (data.type === 'init') {
                    // 初始化响应
                    console.log('WebSocket initialized, has_context:', data.has_context);
                    return;
                }
                if (data.type === 'chat' && data.content) {
                    this.messageList[this.messageList.length - 1].text += data.content;
                    onMessage();
                }
            };
        },

        sendMessage(
            text: string,
            note?: {
                taskId: string;
                createTime: string;
                videoUrl: string;
                title: string;
                autoChapters: string;
                summarization: string;
                meetingAssistance: string;
                personalNote: string;
                taskStatus: '进行中' | '已完成' | '失败';
            },
        ) {
            const mySendMessage = {
                text,
                sender: 'user',
            } as Message;
            this.messageList.push(mySendMessage);
            this.messageList.push({
                text: '',
                sender: 'ai',
            });
            
            // 发送聊天消息
            ws?.send(
                JSON.stringify({
                    type: 'chat',
                    content: text,
                }),
            );
        },

        closeWs() {
            ws?.close();
        },
    },
});

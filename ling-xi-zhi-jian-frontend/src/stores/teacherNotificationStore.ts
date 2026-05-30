import { defineStore } from 'pinia';
import { SystemConstant } from '@/constants/SystemConstant';

export interface TeacherNotification {
    id: string;
    type: 'info' | 'warning' | 'success' | 'error';
    title: string;
    message: string;
    timestamp: number;
}

let notificationWs: WebSocket | null = null;
let heartbeatTimer: ReturnType<typeof setInterval> | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 10;
const HEARTBEAT_INTERVAL = 30000;
const RECONNECT_DELAY = 5000;

export const useTeacherNotificationStore = defineStore(
    'useTeacherNotificationStore',
    {
        state: () => ({
            notificationList: [] as TeacherNotification[],
            isConnected: false,
        }),

        actions: {
            initWebSocket() {
                const token = localStorage.getItem(SystemConstant.TOKEN);
                if (!token) {
                    console.log('未登录，不建立教师提醒 WebSocket 连接');
                    return;
                }

                if (notificationWs && notificationWs.readyState === WebSocket.OPEN) {
                    return;
                }

                const webSocketUrl =
                    import.meta.env.VITE_BACK_END_BASE_URL +
                    '/ws/teacher/notification?token=' +
                    token;

                console.log('正在连接教师提醒 WebSocket...');
                notificationWs = new WebSocket(webSocketUrl);

                notificationWs.onopen = () => {
                    this.isConnected = true;
                    reconnectAttempts = 0;
                    console.log('教师提醒 WebSocket 已连接');
                    this.startHeartbeat();
                };

                notificationWs.onmessage = (event) => {
                    try {
                        const rawData = JSON.parse(event.data);
                        
                        if (rawData.type === 'pong') {
                            return;
                        }

                        const data = rawData as {
                            id?: string;
                            type?: 'info' | 'warning' | 'success' | 'error';
                            title?: string;
                            message: string;
                        };

                        const notification: TeacherNotification = {
                            id: data.id || Date.now().toString(),
                            type: data.type || 'info',
                            title: data.title || '教师提醒',
                            message: data.message,
                            timestamp: Date.now(),
                        };

                        this.notificationList.push(notification);

                        if (typeof window.showNotice === 'function') {
                            window.showNotice(data.message, data.type || 'info', 5000);
                        }
                    } catch (error) {
                        console.error('解析教师提醒消息失败:', error);
                    }
                };

                notificationWs.onerror = (error) => {
                    console.error('教师提醒 WebSocket 错误:', error);
                };

                notificationWs.onclose = () => {
                    this.isConnected = false;
                    this.stopHeartbeat();
                    this.scheduleReconnect();
                };
            },

            startHeartbeat() {
                this.stopHeartbeat();
                heartbeatTimer = setInterval(() => {
                    if (notificationWs && notificationWs.readyState === WebSocket.OPEN) {
                        notificationWs.send(JSON.stringify({ type: 'ping' }));
                    }
                }, HEARTBEAT_INTERVAL);
            },

            stopHeartbeat() {
                if (heartbeatTimer) {
                    clearInterval(heartbeatTimer);
                    heartbeatTimer = null;
                }
            },

            scheduleReconnect() {
                if (reconnectTimer) {
                    clearTimeout(reconnectTimer);
                    reconnectTimer = null;
                }

                if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                    console.log('已达到最大重连次数，停止重连');
                    return;
                }

                const token = localStorage.getItem(SystemConstant.TOKEN);
                if (!token) {
                    console.log('未登录，停止重连');
                    return;
                }

                reconnectAttempts++;
                console.log(`${RECONNECT_DELAY / 1000}秒后尝试第${reconnectAttempts}次重连...`);
                reconnectTimer = setTimeout(() => {
                    this.initWebSocket();
                }, RECONNECT_DELAY);
            },

            closeWebSocket() {
                this.stopHeartbeat();
                if (reconnectTimer) {
                    clearTimeout(reconnectTimer);
                    reconnectTimer = null;
                }
                reconnectAttempts = MAX_RECONNECT_ATTEMPTS;

                if (notificationWs) {
                    notificationWs.close();
                    notificationWs = null;
                    this.isConnected = false;
                }
            },

            removeNotification(id: string) {
                const index = this.notificationList.findIndex(n => n.id === id);
                if (index > -1) {
                    this.notificationList.splice(index, 1);
                }
            },

            clearAllNotifications() {
                this.notificationList = [];
            },
        },
    },
);

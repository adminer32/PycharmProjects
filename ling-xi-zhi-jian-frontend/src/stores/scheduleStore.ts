import { defineStore } from 'pinia';
import MessageUtil from '@/utils/MessageUtil';
import { saveScheduleApi, getScheduleListApi } from '@/api/personal_homepage/scheduleApi';

export interface ScheduleItem {
    id: number; // 唯一标识
    time: string; // HH:mm
    type: string;
    content: string;
}

export interface ScheduleMap {
    [date: string]: ScheduleItem[];
}

export const useScheduleStore = defineStore('useScheduleStore', {
    state: () => {
        return {
            scheduleMap: {} as ScheduleMap,
        };
    },

    actions: {
        /**
         * 按月份获取日程
         * @param dateString YYYY-MM
         */
        getScheduleList(dateString: string) {
            getScheduleListApi(dateString).then((res) => {
                if (res.status == 'success') {
                    this.scheduleMap = res.data;
                } else {
                    MessageUtil.error(res.message);
                }
            });
        },

        /**
         * 添加日程
         * dataString  YYYY-MM-DD
         */
        addEvent(newEvent: Omit<ScheduleItem, 'id'>, dataString: string) {
            return new Promise<void>((resolve, reject) => {
                saveScheduleApi({
                    time: newEvent.time, // HH:mm
                    type: newEvent.type,
                    content: newEvent.content,
                    scheduleDate: dataString,
                }).then((res) => {
                    if (res.status == 'success') {
                        if (!this.scheduleMap[dataString]) {
                            this.scheduleMap[dataString] = [];
                        }
                        this.scheduleMap[dataString].push({
                            id: res.data.id,
                            time: newEvent.time,
                            type: newEvent.type,
                            content: newEvent.content,
                        });
                        MessageUtil.success(res.message);
                        resolve();
                    } else {
                        MessageUtil.error(res.message);
                        reject();
                    }
                });
            });
        },

        editEvent(newEvent: ScheduleItem, dataString: string) {
            return new Promise<void>((resolve, reject) => {
                saveScheduleApi({
                    id: newEvent.id,
                    time: newEvent.time,
                    type: newEvent.type,
                    content: newEvent.content,
                    scheduleDate: dataString,
                }).then((res: any) => {
                    if (res.status == 'success') {
                        for (const date in this.scheduleMap) {
                            for (const event of this.scheduleMap[date]) {
                                if (event.id == newEvent.id) {
                                    event.time = newEvent.time;
                                    event.content = newEvent.content;
                                    event.type = newEvent.type;
                                    MessageUtil.success(res.message);
                                    resolve();
                                    return;
                                }
                            }
                        }
                    } else {
                        MessageUtil.error(res.message);
                        reject();
                    }
                });
            });
        },
    },
});

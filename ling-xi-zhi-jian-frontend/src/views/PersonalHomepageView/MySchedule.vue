<script lang="ts" setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import TimeUtil from '@/utils/TimeUtil';
import Modal from '@/components/反馈/Modal/Modal.vue';
import MessageUtil from '@/utils/MessageUtil';
import { type ScheduleItem, useScheduleStore } from '@/stores/scheduleStore';
import { deleteScheduleApi } from '@/api/personal_homepage/scheduleApi';
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import dayjs, { type Dayjs } from 'dayjs';

import EditBox from '@/views/PersonalAnalysisView/MotionAnalysisView/EditBox.vue';

const scheduleStore = useScheduleStore();

let tempDate = TimeUtil.getCurrentTime('YYYY-MM-DD'); // 正在操作的日期
const willDelete = {
    // 记录将要删除的 ID
    isDeleteOneDay: false,
    id: 0,
};

const isShowAddEventModal = ref(false);
const isShowDeleteEventModal = ref(false);
const isShowEditEventModal = ref(false);

const deleteEvent = () => {
    let ids: string;
    if (willDelete.isDeleteOneDay) {
        ids = scheduleStore.scheduleMap[tempDate]
            ? scheduleStore.scheduleMap[tempDate]
                  .map((item) => item.id)
                  .join(',')
            : '';
    } else {
        ids = willDelete.id.toString();
    }
    if (!ids.length) {
        isShowDeleteEventModal.value = false;
        return;
    }
    deleteScheduleApi(ids)
        .then((res: any) => {
            if (res.status == 'success') {
                if (willDelete.isDeleteOneDay) {
                    delete scheduleStore.scheduleMap[tempDate];
                } else {
                    scheduleStore.scheduleMap[tempDate].splice(
                        scheduleStore.scheduleMap[tempDate].findIndex(
                            (item) => item.id === willDelete.id,
                        ),
                        1,
                    );
                }
                MessageUtil.success(res.message);
            } else {
                MessageUtil.error(res.message);
            }
        })
        .finally(() => {
            isShowDeleteEventModal.value = false;
        });
};

/**
 * 打开删除提示框
 */
const showDeleteEventModal = (
    dateString: string,
    isDeleteOneDay: boolean,
    id?: number,
) => {
    tempDate = dateString;
    isShowDeleteEventModal.value = true;
    willDelete.isDeleteOneDay = isDeleteOneDay;
    willDelete.id = id ? id : 0;
};

onMounted(() => {
    scheduleStore.getScheduleList(TimeUtil.getCurrentTime('YYYY-MM'));
});

/**
 * 添加日程
 */
const addEvent = () => {
    scheduleStore
        .addEvent(
            {
                time: TimeUtil.format(tempEvent.time, 'HH:mm'),
                type: tempEvent.type,
                content: tempEvent.content,
            },
            date.value.format('YYYY-MM-DD'),
        )
        .then(() => {
            isShowAddEventModal.value = false;
        });
};

const tempEvent = reactive({
    id: 0,
    time: new Date(),
    type: 'warning',
    content: '',
});

const showAddEventModal = () => {
    tempEvent.type = 'warning';
    tempEvent.content = '';
    tempEvent.time = new Date();
    isShowAddEventModal.value = true;
};

/**
 * 一键生成训练计划
 */
const generateSchedule = () => {
    // 生成未来7天的训练计划
    const exercises = ['盘踢', '磕踢', '外摆踢', '里合踢', '绷踢', '拐踢'];
    
    // 每天的训练时间
    const trainingTimes = ['08:00', '16:00', '19:00'];
    
    // 为未来7天生成训练计划
    for (let i = 0; i < 7; i++) {
        const targetDate = date.value.add(i, 'day');
        const dateString = targetDate.format('YYYY-MM-DD');
        
        // 每天生成2个训练项目
        for (let j = 0; j < 2; j++) {
            const randomExercise = exercises[Math.floor(Math.random() * exercises.length)];
            const randomTime = trainingTimes[Math.floor(Math.random() * trainingTimes.length)];
            
            // 创建训练计划
            const scheduleItem = {
                time: randomTime,
                type: 'warning',
                content: `${randomExercise}训练`
            };
            
            // 添加到计划中
            scheduleStore.addEvent(scheduleItem, dateString);
        }
    }
    
    MessageUtil.success('训练计划生成成功！');
};

const timeString2Date = (timeString: string): Date => {
    TimeUtil.getCurrentTime('YYYY-MM-DD');
    return new Date(
        TimeUtil.getCurrentTime('YYYY-MM-DD') + 'T' + timeString + ':00',
    );
};

const showEditEventModal = (dateString: string, id: number) => {
    tempDate = dateString;
    const selectedEvent = scheduleStore.scheduleMap[dateString]!.find(
        (item) => item.id === id,
    )!;
    tempEvent.id = id;
    tempEvent.type = selectedEvent.type;
    tempEvent.content = selectedEvent.content;
    tempEvent.time = timeString2Date(selectedEvent.time);
    isShowEditEventModal.value = true;
};

const editEvent = (newEvent: {
    id: number;
    time: Date;
    type: string;
    content: string;
}) => {
    scheduleStore
        .editEvent(
            {
                id: newEvent.id,
                time: TimeUtil.format(newEvent.time, 'HH:mm'),
                type: newEvent.type,
                content: newEvent.content,
            },
            tempDate,
        )
        .then(() => {
            isShowEditEventModal.value = false;
        });
};

const colorList = [
    '#f8df97',
    '#8592ef',
    '#e898f4',
    '#97f8e9',
    '#99bef6',
    '#dabbfa',
    '#f4a398',
    '#8aea92',
];

const date = ref(dayjs(TimeUtil.getCurrentTime('YYYY-MM-DD')));

watch(date, (newVal, oldVal) => {
    if (newVal.format('YYYY-MM') != oldVal.format('YYYY-MM')) {
        scheduleStore.getScheduleList(newVal.format('YYYY-MM'));
    }
});

const onSelect = (value: Dayjs) => {
    date.value = value;
};

const preMonth = () => {
    date.value = date.value.subtract(1, 'month');
};
const nextMonth = () => {
    date.value = date.value.add(1, 'month');
};

interface ScheduleItemWithColor extends ScheduleItem {
    color: string;
}

const scheduleMapWithColor = computed(() => {
    const map = {} as {
        [date: string]: ScheduleItemWithColor[];
    };
    for (const date in scheduleStore.scheduleMap) {
        map[date] = scheduleStore.scheduleMap[date].map((item) => {
            return {
                ...item,
                color: colorList[
                    Math.trunc(Math.random() * 100) % colorList.length
                ],
            };
        });
    }
    return map;
});

// ---------------------------------------------
</script>

<template>
    <div class="my-schedule">
        <div class="left">
            <div class="title">我的训练计划</div>
            <button class="add-schedule-btn" @click="generateSchedule">
                <span class="iconfont icon-add1" />
                一键生成训练计划
            </button>
            <Modal
                v-model:open="isShowAddEventModal"
                @ok="addEvent"
                :title="'添加训练计划->' + date.format('YYYY-MM-DD')"
            >
                <EditBox
                    v-model:content="tempEvent.content"
                    v-model:time="tempEvent.time"
                />
            </Modal>
            <a-config-provider :locale="zhCN">
                <a-calendar
                    :fullscreen="false"
                    v-model:value="date"
                    @select="onSelect"
                >
                    <template #headerRender="{ value: current }">
                        <div style="padding: 10px">
                            <a-row type="flex" justify="space-between">
                                {{ current.format('YYYY年MM月') }}
                                <div
                                    style="
                                        display: flex;
                                        align-items: center;
                                        gap: 10px;
                                    "
                                >
                                    <span
                                        style="
                                            font-size: 11px;
                                            color: #666;
                                            cursor: pointer;
                                        "
                                        class="iconfont icon-zuo"
                                        @click="preMonth"
                                    />
                                    <span
                                        style="
                                            font-size: 11px;
                                            color: #666;
                                            cursor: pointer;
                                        "
                                        class="iconfont icon-you"
                                        @click="nextMonth"
                                    />
                                </div>
                            </a-row>
                        </div>
                    </template>
                </a-calendar>
            </a-config-provider>
            <button
                class="danger-button"
                @click="showDeleteEventModal(date.format('YYYY-MM-DD'), true)"
            >
                删除训练计划
            </button>
            <Modal
                v-model:open="isShowDeleteEventModal"
                @ok="deleteEvent"
                title="删除训练计划"
            >
                <div class="text" style="margin-left: 5px; color: red">
                    确认删除{{
                        willDelete.isDeleteOneDay
                            ? date.format('YYYY年MM月DD日') + '的'
                            : ''
                    }}训练计划？
                </div>
            </Modal>
        </div>

        <Modal
            v-model:open="isShowEditEventModal"
            @ok="editEvent(tempEvent)"
            title="编辑训练计划"
        >
            <EditBox
                v-model:content="tempEvent.content"
                v-model:time="tempEvent.time"
            />
        </Modal>

        <div class="right">
            <div class="top">
                <div class="header">
                    <button
                        @click="
                            date = dayjs(TimeUtil.getCurrentTime('YYYY-MM-DD'))
                        "
                    >
                        今天
                    </button>
                </div>
                <div class="days">
                    <div class="time" />
                    <div class="day">
                        <div class="dot">
                            {{ date.subtract(1, 'day').format('DD') }}
                        </div>
                    </div>
                    <div class="day">
                        <div class="dot today">
                            {{ date.format('DD') }}
                        </div>
                    </div>
                    <div class="day">
                        <div class="dot">
                            {{ date.add(1, 'day').format('DD') }}
                        </div>
                    </div>
                    <div class="day">
                        <div class="dot">
                            {{ date.add(2, 'day').format('DD') }}
                        </div>
                    </div>
                </div>
            </div>
            <div class="content">
                <div
                    class="grid-item"
                    :class="{ time: (item - 1) % 5 == 0 }"
                    v-for="item of 121"
                    :key="item"
                >
                    <div v-if="(item - 1) % 5 == 0" class="time">
                        {{ ((item - 1) / 5).toString().padStart(2, '0') }}:00
                    </div>
                    <!-- 计划内容 -->
                    <template
                        v-else-if="
                            scheduleMapWithColor[
                                date
                                    .add(((item - 1) % 5) - 2, 'day')
                                    .format('YYYY-MM-DD')
                            ] &&
                            scheduleMapWithColor[
                                date
                                    .add(((item - 1) % 5) - 2, 'day')
                                    .format('YYYY-MM-DD')
                            ]?.filter(
                                (scheduleItem) =>
                                    scheduleItem.time.substring(0, 2) ==
                                    Math.trunc((item - 1) / 5)
                                        .toString()
                                        .padStart(2, '0'),
                            )
                        "
                    >
                        <a-popover
                            trigger="click"
                            class="popover"
                            v-for="schedule of scheduleMapWithColor[
                                date
                                    .add(((item - 1) % 5) - 2, 'day')
                                    .format('YYYY-MM-DD')
                            ]?.filter(
                                (scheduleItem) =>
                                    scheduleItem.time.substring(0, 2) ==
                                    Math.trunc((item - 1) / 5)
                                        .toString()
                                        .padStart(2, '0'),
                            )"
                            :key="schedule.id"
                            :title="schedule.time"
                        >
                            <div
                                class="event"
                                style="font-size: 14px; word-break: break-all"
                                :style="{ backgroundColor: schedule.color }"
                            >
                                {{ schedule.time }} {{ schedule.content }}
                            </div>
                            <template #content>
                                <div class="card" style="width: 300px">
                                    <div
                                        class="content"
                                        style="
                                            padding: 4px 0;
                                            word-break: break-all;
                                        "
                                    >
                                        {{ schedule.content }}
                                    </div>
                                    <div
                                        class="btn-box"
                                        style="
                                            display: flex;
                                            margin-top: 10px;
                                            justify-content: flex-end;
                                            gap: 5px;
                                        "
                                    >
                                        <a-button
                                            @click="
                                                showEditEventModal(
                                                    date
                                                        .add(
                                                            ((item - 1) % 5) -
                                                                2,
                                                            'day',
                                                        )
                                                        .format('YYYY-MM-DD'),
                                                    schedule.id,
                                                )
                                            "
                                        >
                                            编辑
                                        </a-button>
                                        <a-button
                                            danger
                                            @click="
                                                showDeleteEventModal(
                                                    date
                                                        .add(
                                                            ((item - 1) % 5) -
                                                                2,
                                                            'day',
                                                        )
                                                        .format('YYYY-MM-DD'),
                                                    false,
                                                    schedule.id,
                                                )
                                            "
                                        >
                                            删除
                                        </a-button>
                                    </div>
                                </div>
                            </template>
                        </a-popover>
                    </template>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
.my-schedule {
    margin: $gap;
    display: flex;
    flex-direction: row;
    overflow: hidden;

    .left {
        width: 300px;
        background-color: #fff;
        overflow: hidden;
        padding: 15px;

        .title {
            font-size: 20px;
            font-weight: bold;
        }

        .add-schedule-btn {
            background-color: #fefefe;
            outline: none;
            $height: 45px;
            height: $height;
            padding: 0 13px;
            margin: 20px 10px;
            border-radius: 10px;
            box-shadow:
                -1px 1px 7px 1px rgba(0, 0, 0, 0.1),
                inset 0 0 1px 1px rgba(255, 255, 255, 0.1);
            cursor: pointer;
            border: 0.01mm solid #33333377;

            .iconfont {
                font-size: 16px;
            }
        }

        .danger-button {
            width: 100%;
            margin-top: 10px;
            background-color: #f56c6c;
            border: none;
            font-size: 14px;
            color: #fff;
            border-radius: 3px;
            cursor: pointer;
            transition: background-color 0.1s;

            &:hover {
                background-color: #f89898;
            }
        }
    }

    .right {
        flex: 1;
        background-color: #fff;
        display: flex;
        flex-direction: column;

        .header {
            button {
                background-color: #fefefe;
                outline: none;
                $height: 25px;
                height: $height;
                padding: 0 13px;
                border-radius: 10px;
                box-shadow:
                    -1px 1px 2px 1px rgba(0, 0, 0, 0.1),
                    inset 0 0 1px 1px rgba(255, 255, 255, 0.1);
                cursor: pointer;
                border: 0.01mm solid #33333377;
            }
        }

        $time-area-width: 50px;

        .top {
            padding: 15px 15px 0;
            font-size: 17px;
            box-shadow:
                -1px 1px 7px 1px rgba(0, 0, 0, 0.1),
                inset 0 0 1px 1px rgba(255, 255, 255, 0.1);
            border-bottom-left-radius: 10px;
            position: sticky;
            border: 0.01mm solid #33333377;
            border-top: none;
            border-right: none;
            z-index: 1;

            .days {
                display: flex;
                flex-direction: row;

                .time {
                    width: $time-area-width;
                }

                .day {
                    flex: 1;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    padding: 10px;

                    .dot {
                        &.today {
                            $size: 30px;
                            height: $size;
                            width: $size;
                            background-color: $blue-9;
                            border-radius: calc($size / 2);
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            font-size: 14px;
                            box-shadow:
                                1px 1px 7px 1px rgba(0, 0, 0, 0.1),
                                inset 0 0 1px 1px rgba(255, 255, 255, 0.1);
                            color: #fff;
                        }
                    }
                }
            }
        }

        .content {
            flex: 1;
            display: grid;
            grid-template-columns: $time-area-width repeat(4, 1fr);
            padding: 15px 10px 0;
            overflow-y: scroll;
            background-color: inherit;

            &::-webkit-scrollbar {
                display: none;
            }

            .grid-item {
                border-left: 1px solid #e6e6e6;
                border-bottom: 1px solid #e6e6e6;
                background-color: inherit;

                &:first-child {
                    border-left: none;
                    border-top: 1px solid #e6e6e6;
                }

                &:nth-child(2),
                &:nth-child(3),
                &:nth-child(4),
                &:nth-child(5) {
                    border-top: 1px solid #e6e6e6;
                }

                &:last-child {
                    border: none;
                }

                &.time {
                    border-left: none;
                    align-items: flex-start;
                    background-color: inherit;
                    overflow: visible;

                    .time {
                        width: 38px;
                        transform: translateY(-50%);
                        text-align: right;
                        font-size: 12px;
                        border-radius: 3px;
                        background-color: inherit;
                        padding: 20px 5px 20px 0;
                    }
                }

                .popover {
                    margin: 5px;
                    padding: 4px;
                    border-radius: 5px;
                    cursor: pointer;
                }
            }
        }
    }
}
</style>

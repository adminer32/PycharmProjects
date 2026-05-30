<script lang="ts" setup>
import { computed, reactive, ref } from 'vue';
import TimeUtil from '@/utils/TimeUtil';
import {
    ElButton,
    ElCalendar,
    ElConfigProvider,
    ElEmpty,
    ElTimeline,
    ElTimelineItem,
} from 'element-plus';
import 'element-plus/dist/index.css';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import Modal from '@/components/反馈/Modal/Modal.vue';
import EditBox from '@/views/PersonalAnalysisView/MotionAnalysisView/EditBox.vue';
import MessageUtil from '@/utils/MessageUtil';

interface ScheduleMap {
    [day: string]: {
        time: string; // HH:mm，是唯一值
        type: string;
        content: string;
    }[];
}

const { scheduleMap } = defineProps<{
    scheduleMap: ScheduleMap;
}>();

const emits = defineEmits<{
    (e: 'update:scheduleMap', scheduleMap: ScheduleMap): void;
}>();

const updateScheduleMap = (scheduleMap: ScheduleMap) => {
    emits('update:scheduleMap', scheduleMap);
};

const willDelete = {
    // 记录将要删除的 ID
    isDeleteOneDay: false,
    index: 0,
};

const isShowAddEventModal = ref(false);
const isShowDeleteEventModal = ref(false);
const isShowEditEventModal = ref(false);

const deleteEvent = () => {
    const tempMap = { ...scheduleMap };
    if (willDelete.isDeleteOneDay) {
        delete tempMap[TimeUtil.format(currentDate.value, 'YYYY-MM-DD')];
    } else {
        tempMap[TimeUtil.format(currentDate.value, 'YYYY-MM-DD')].splice(
            willDelete.index,
            1,
        );
    }
    updateScheduleMap(tempMap);
    isShowDeleteEventModal.value = false;
};

/**
 * 打开删除提示框
 */
const showDeleteEventModal = (isDeleteOneDay: boolean, index?: number) => {
    isShowDeleteEventModal.value = true;
    willDelete.isDeleteOneDay = isDeleteOneDay;
    willDelete.index = index ?? 0;
};

/**
 * 添加日程
 */
const addEvent = () => {
    if (
        scheduleMap[TimeUtil.format(currentDate.value, 'YYYY-MM-DD')].find(
            (item) => item.time === TimeUtil.format(tempEvent.time, 'HH:mm'),
        )
    ) {
        MessageUtil.info('该时间段已存在日程');
        return;
    }
    const tempMap = { ...scheduleMap };
    if (!tempMap[TimeUtil.format(currentDate.value, 'YYYY-MM-DD')]) {
        tempMap[TimeUtil.format(currentDate.value, 'YYYY-MM-DD')] = [];
    }
    tempMap[TimeUtil.format(currentDate.value, 'YYYY-MM-DD')].push({
        time: TimeUtil.format(tempEvent.time, 'HH:mm'),
        type: tempEvent.type,
        content: tempEvent.content,
    });
    updateScheduleMap(tempMap);
    isShowAddEventModal.value = false;
};

const currentDate = ref(new Date());

const tempEvent = reactive({
    index: 0,
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

const timeString2Date = (timeString: string): Date => {
    return new Date(
        TimeUtil.getCurrentTime('YYYY-MM-DD') + 'T' + timeString + ':00',
    );
};

const showEditEventModal = (index: number) => {
    const selectedEvent =
        scheduleMap[TimeUtil.format(currentDate.value, 'YYYY-MM-DD')][index];
    tempEvent.type = selectedEvent.type;
    tempEvent.content = selectedEvent.content;
    tempEvent.time = timeString2Date(selectedEvent.time);
    tempEvent.index = index; // 正在编辑的 index
    isShowEditEventModal.value = true;
};

const editEvent = () => {
    // const tempMap = {...scheduleMap};

    // 深拷贝
    const tempMap = JSON.parse(JSON.stringify(scheduleMap));
    tempMap[TimeUtil.format(currentDate.value, 'YYYY-MM-DD')][
        tempEvent.index
    ].time = TimeUtil.format(tempEvent.time, 'HH:mm');
    tempMap[TimeUtil.format(currentDate.value, 'YYYY-MM-DD')][
        tempEvent.index
    ].content = tempEvent.content;
    tempMap[TimeUtil.format(currentDate.value, 'YYYY-MM-DD')][
        tempEvent.index
    ].type = tempEvent.type;

    const calcMaxCount = (
        list: {
            time: string;
            type: string;
            content: string;
        }[],
    ) => {
        // 创建一个对象来统计每个时间出现的次数
        const timeCount: Record<string, number> = {};

        // 遍历数组，统计每个时间的出现次数
        list.forEach((item) => {
            if (timeCount[item.time]) {
                timeCount[item.time]++;
            } else {
                timeCount[item.time] = 1;
            }
        });

        // 找出出现次数最多的那个时间的数量
        let maxCount = 0;
        for (let time in timeCount) {
            if (timeCount[time] > maxCount) {
                maxCount = timeCount[time];
            }
        }
        return maxCount;
    };

    // 统计 tempMap[TimeUtil.format(currentDate.value, 'YYYY-MM-DD')] 中 time 字段相同的元素个数
    if (
        calcMaxCount(
            tempMap[TimeUtil.format(currentDate.value, 'YYYY-MM-DD')],
        ) > 1
    ) {
        MessageUtil.info('该时间段已存在日程');
        return;
    }
    updateScheduleMap(tempMap);
    isShowEditEventModal.value = false;
};

const computedSelectedEventList = computed(() => {
    const sortedList =
        scheduleMap[TimeUtil.format(currentDate.value, 'YYYY-MM-DD')];
    sortedList?.sort((item1, item2) => {
        return (
            TimeUtil.parse(item1.time, 'HH:mm').getTime() -
            TimeUtil.parse(item2.time, 'HH:mm').getTime()
        );
    });
    return sortedList;
});
</script>

<template>
    <div class="ai-schedule">
        <ElConfigProvider :locale="zhCn">
            <div class="left">
                <ElCalendar v-model="currentDate">
                    <template #date-cell="{ data }">
                        <div class="date-cell">
                            <div class="day">
                                {{ data.day.split('-')[2] }}
                            </div>
                            <template v-if="scheduleMap[data.day]">
                                <div
                                    class="event-item"
                                    v-for="(item, index) of scheduleMap[
                                        data.day
                                    ]"
                                    :key="index"
                                >
                                    <div class="dot" :class="item.type" />
                                    {{ item.time }}
                                </div>
                            </template>
                        </div>
                    </template>
                </ElCalendar>
            </div>
            <div class="right">
                <div class="header">
                    {{ TimeUtil.format(currentDate, 'YYYY年MM月DD日') }}
                    <div class="right">
                        <ElButton
                            type="primary"
                            size="small"
                            @click="showAddEventModal"
                            >添加</ElButton
                        >
                        <ElButton
                            type="danger"
                            size="small"
                            @click="showDeleteEventModal(true)"
                        >
                            删除
                        </ElButton>
                        <Modal
                            v-model:open="isShowDeleteEventModal"
                            @ok="deleteEvent"
                        >
                            确认删除？
                        </Modal>
                        <Modal
                            v-model:open="isShowAddEventModal"
                            @ok="addEvent"
                            title="添加训练计划"
                        >
                            <EditBox
                                v-model:content="tempEvent.content"
                                v-model:time="tempEvent.time"
                            />
                        </Modal>
                    </div>
                </div>
                <div class="content">
                    <Modal
                        v-model:open="isShowEditEventModal"
                        @ok="editEvent"
                        title="编辑训练计划"
                    >
                        <EditBox
                            v-model:content="tempEvent.content"
                            v-model:time="tempEvent.time"
                        />
                    </Modal>

                    <ElTimeline
                        class="timeline"
                        v-if="
                            computedSelectedEventList &&
                            computedSelectedEventList.length
                        "
                    >
                        <ElTimelineItem
                            v-for="item of computedSelectedEventList"
                            :key="item.time"
                            :timestamp="item.time"
                            placement="top"
                        >
                            <div class="event-item">
                                <div class="event-content">
                                    {{ item.content }}
                                </div>
                                <a-dropdown
                                    :trigger="['click']"
                                    placement="bottomRight"
                                >
                                    <span class="iconfont icon-more"></span>
                                    <template #overlay>
                                        <a-menu>
                                            <a-menu-item
                                                @click="
                                                    showEditEventModal(
                                                        computedSelectedEventList.findIndex(
                                                            (event) =>
                                                                event.time ===
                                                                item.time,
                                                        ),
                                                    )
                                                "
                                            >
                                                编辑
                                            </a-menu-item>
                                            <a-menu-item
                                                @click="
                                                    showDeleteEventModal(
                                                        false,
                                                        computedSelectedEventList.findIndex(
                                                            (event) =>
                                                                event.time ==
                                                                item.time,
                                                        ),
                                                    )
                                                "
                                            >
                                                删除
                                            </a-menu-item>
                                        </a-menu>
                                    </template>
                                </a-dropdown>
                            </div>
                        </ElTimelineItem>
                    </ElTimeline>
                    <ElEmpty v-else style="height: 100%" :image-size="200" />
                </div>
            </div>
        </ElConfigProvider>
    </div>
</template>

<style scoped lang="scss">
.ai-schedule {
    display: flex;
    flex-direction: row;
    overflow: hidden;

    .left {
        flex: 3;
        background-color: #fff;
        overflow: hidden;
        border-right: solid 0.03mm rgba(230, 230, 230, 0.81);

        .date-cell {
            height: 100%;
            width: 100%;
            overflow-y: auto;
            overflow-x: hidden;

            &::-webkit-scrollbar {
                width: 3px;
            }

            &::-webkit-scrollbar-thumb {
                background-color: $blue-3;
            }

            .event-item {
                white-space: nowrap;
                font-size: 13px;
                display: flex;
                align-items: center;
                gap: 5px;

                .dot {
                    width: 7px;
                    height: 7px;
                    border-radius: 50%;
                    background-color: #e4e7ed;
                }
            }
        }
    }

    .right {
        flex: 2;
        background-color: #fff;
        display: flex;
        flex-direction: column;
        gap: $gap;
        overflow: hidden;

        .header {
            padding: 15px;
            font-size: 17px;
            font-weight: bold;
            display: flex;
            flex-direction: row;
            justify-content: space-between;

            .right {
                all: unset;
                box-shadow: none;
                display: flex;
                flex-direction: row;
            }
        }

        .content {
            flex: 1;
            overflow: auto;

            .timeline {
                margin-right: 40px;

                .event-item {
                    padding: 10px;
                    display: flex;
                    flex-direction: row;
                    justify-content: space-between;
                    box-shadow: $box-shadow;
                    gap: 7px;
                    align-items: flex-start;

                    .event-content {
                        word-break: break-word;
                    }

                    .iconfont {
                        font-size: 25px;
                        font-weight: bold;
                        cursor: pointer;

                        &:hover {
                            background-color: #f3f3f3;
                            border-radius: 5px;
                        }
                    }
                }
            }
        }
    }
}
</style>

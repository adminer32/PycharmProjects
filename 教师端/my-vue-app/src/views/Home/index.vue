<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useClassStore } from '@/store/class'
import { storeToRefs } from 'pinia'
import ClassOverview from './components/ClassOverview.vue'
import ScheduleCard from './components/ScheduleCard.vue'
import PendingCard from './components/PendingCard.vue'
import NoticeCard from './components/NoticeCard.vue'
import type { ScheduleItem } from './types'
import { homeApi, type HomeOverview, type PendingItem, type Notice, type NoticeInput } from '@/api/homeApi'
import { ElMessage } from 'element-plus'

const classStore = useClassStore()
const { currentClass } = storeToRefs(classStore)

const loading = ref(false)

// 班级概览 - 从 API 获取
const classOverview = ref<HomeOverview>({
  totalStudents: 0,
  todayHomework: { submitted: 0, total: 0, rate: '0%' },
  todayCheckin: { submitted: 0, total: 0, rate: '0%' },
  pendingHomework: 0
})

// 待处理事项 - 从 API 获取
const pendingItems = ref<PendingItem[]>([])

// 通知公告 - 从 API 获取
const notices = ref<Notice[]>([])

const scheduleData = ref<ScheduleItem[]>([
  { id: '1', day: 1, period: 1, courseName: '盘踢基础', location: '体育馆 A 区', duration: 2 },
  { id: '2', day: 1, period: 5, courseName: '磕踢进阶', location: '体育馆 B 区', duration: 2 },
  { id: '3', day: 2, period: 7, courseName: '体能训练', location: '操场', duration: 2 },
  { id: '4', day: 2, period: 3, courseName: '外摆踢专项', location: '体育馆 A 区' , duration: 2},
  { id: '5', day: 3, period: 2, courseName: '里合踢训练', location: '体育馆 A 区', duration: 2 },
  { id: '6', day: 4, period: 1, courseName: '综合练习', location: '体育馆 B 区', duration: 2 },
  { id: '7', day: 5, period: 2, courseName: '考核测试', location: '体育馆 A 区' , duration: 2},
  { id: '8', day: 4, period: 5, courseName: '薄弱突破', location: '体育馆 C 区' , duration: 2},
])

const loadHomeData = async () => {
  if (!classStore.currentClass?.id) return
  console.log('🔍 正在加载首页数据，当前班级ID:', classStore.currentClass.id, '类型:', typeof classStore.currentClass.id)
  loading.value = true
  try {
    const [overviewRes, pendingRes, noticeRes] = await Promise.all([
      homeApi.getHomeOverview(String(classStore.currentClass.id)),
      homeApi.getPendingItems(1, String(classStore.currentClass.id)),
      homeApi.getNotices(String(classStore.currentClass.id))
    ])
    console.log('📊 班级概览数据:', overviewRes.data)
    console.log('⏰ 待处理事项:', pendingRes.data)
    console.log('📢 通知公告:', noticeRes.data)
    if (overviewRes.data) classOverview.value = overviewRes.data
    if (pendingRes.data) pendingItems.value = pendingRes.data
    if (noticeRes.data?.list) notices.value = (noticeRes.data as any).list
  } catch (e) {
    console.error('❌ 加载首页数据失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadHomeData()
})

watch(() => classStore.currentClass?.id, () => {
  loadHomeData()
})

const handleSaveSchedule = (schedule: ScheduleItem) => {
  const index = scheduleData.value.findIndex(s => s.id === schedule.id)
  if (index > -1) {
    scheduleData.value[index] = schedule
  } else {
    scheduleData.value.push(schedule)
  }
}

const handleDeleteSchedule = (id: string) => {
  const index = scheduleData.value.findIndex(s => s.id === id)
  if (index > -1) {
    scheduleData.value.splice(index, 1)
  }
}

const handlePublishNotice = async (notice: Notice) => {
  if (!classStore.currentClass?.id) return
  try {
    const input: NoticeInput = {
      classId: Number(classStore.currentClass.id),
      teacherId: 2,
      title: notice.title,
      content: notice.content,
      isImportant: notice.isImportant
    }
    const res = await homeApi.publishNotice(input)
    if (res.data) {
      notices.value.unshift(res.data)
      ElMessage.success('通知发布成功')
    }
  } catch (e) {
    console.error('发布通知失败', e)
    ElMessage.error('发布通知失败')
  }
}

const handleEditNotice = async (notice: Notice) => {
  if (!classStore.currentClass?.id) return
  try {
    const input: NoticeInput = {
      id: notice.id,
      classId: Number(classStore.currentClass.id),
      teacherId: 2,
      title: notice.title,
      content: notice.content,
      isImportant: notice.isImportant
    }
    const res = await homeApi.updateNotice(input)
    if (res.data) {
      const index = notices.value.findIndex(n => n.id === notice.id)
      if (index > -1) {
        notices.value[index] = res.data
      }
      ElMessage.success('通知更新成功')
    }
  } catch (e) {
    console.error('更新通知失败', e)
    ElMessage.error('更新通知失败')
  }
}

const handleDeleteNotice = async (id: string) => {
  try {
    const res = await homeApi.deleteNotice(id)
    if (res.data) {
      const index = notices.value.findIndex(n => n.id === id)
      if (index > -1) {
        notices.value.splice(index, 1)
      }
      ElMessage.success('通知删除成功')
    }
  } catch (e) {
    console.error('删除通知失败', e)
    ElMessage.error('删除通知失败')
  }
}
</script>

<template>
  <div class="home-container" v-loading="loading">
    <ClassOverview :data="classOverview" />

    <ScheduleCard
      :schedule-data="scheduleData"
      @save="handleSaveSchedule"
      @delete="handleDeleteSchedule"
    />

    <div class="two-column">
      <PendingCard :items="pendingItems" />
      <NoticeCard
        :notices="notices"
        @publish="handlePublishNotice"
        @edit="handleEditNotice"
        @delete="handleDeleteNotice"
      />
    </div>
  </div>
</template>

<style scoped>
.home-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.two-column {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

@media (max-width: 1200px) {
  .two-column {
    grid-template-columns: 1fr;
  }
}
</style>

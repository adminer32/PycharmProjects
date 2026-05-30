<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import HomeworkList from './components/HomeworkList.vue'
import CreateHomeworkDialog from './components/CreateHomeworkDialog.vue'
import EditHomeworkDialog from './components/EditHomeworkDialog.vue'
import SubmissionsDetailDialog from './components/SubmissionsDetailDialog.vue'
import { getHomeworkList, createHomework, deleteHomework, type HomeworkListVO, type CreateHomeworkDTO } from '@/api/homeworkApi'
import { useClassStore } from '@/store/class'
import { storeToRefs } from 'pinia'

const router = useRouter()
const classStore = useClassStore()
const { currentClass } = storeToRefs(classStore)

const filterStatus = ref<string>('all')
const searchKeyword = ref('')
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const showSubmissionsDialog = ref(false)
const currentHomework = ref<HomeworkListVO | null>(null)

const homeworkList = ref<HomeworkListVO[]>([])
const loading = ref(false)



const loadHomeworkList = async () => {
  const classId = currentClass.value?.id
  if (!classId) {
    homeworkList.value = []
    return
  }
  loading.value = true
  try {
    const res = await getHomeworkList(classId)
    if (res.data) {
      homeworkList.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载作业列表失败')
  } finally {
    loading.value = false
  }
}

watch(currentClass, () => {
  loadHomeworkList()
})

onMounted(async () => {
  if (classStore.classList.length === 0) {
    await classStore.fetchClassList()
  }
  if (classStore.classList.length > 0 && !currentClass.value) {
    classStore.setCurrentClass(classStore.classList[0])
  }
  loadHomeworkList()
})

const stats = computed(() => ({
  ongoing: homeworkList.value.filter(h => h.status === 1).length,
  graded: homeworkList.value.filter(h => h.status === 2).length,
  averageCompletion: homeworkList.value.length > 0
    ? Math.round(homeworkList.value.reduce((sum, h) => sum + (h.submissionCount / h.totalCount) * 100, 0) / homeworkList.value.length)
    : 0
}))

const statusMap: Record<string, number> = {
  'ongoing': 1,
  'graded': 2,
  'expired': 3
}

const filteredHomework = computed(() => {
  let result = homeworkList.value
  
  if (filterStatus.value !== 'all') {
    const statusValue = statusMap[filterStatus.value]
    result = result.filter(h => h.status === statusValue)
  }
  
  if (searchKeyword.value) {
    result = result.filter(h => 
      h.title.toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
  }
  
  return result
})

const handleCreateHomework = async (data: CreateHomeworkDTO) => {
  try {
    const res = await createHomework(data)
    if (res.data) {
      ElMessage.success('作业发布成功')
      showCreateDialog.value = false
      loadHomeworkList()
    }
  } catch (error) {
    ElMessage.error('发布作业失败')
  }
}

const handleGradeHomework = (homework: HomeworkListVO) => {
  router.push({
    path: '/homework/grade',
    query: { homeworkId: homework.id }
  })
}

const handleViewDetail = (homework: HomeworkListVO) => {
  currentHomework.value = homework
  showSubmissionsDialog.value = true
}

const handleEditHomework = (homework: HomeworkListVO) => {
  currentHomework.value = homework
  showEditDialog.value = true
}

const handleDeleteHomework = async (homework: HomeworkListVO) => {
  try {
    await deleteHomework(homework.id)
    ElMessage.success('作业已删除')
    loadHomeworkList()
  } catch (error) {
    ElMessage.error('删除作业失败')
  }
}

const handleRemind = (homework: HomeworkListVO) => {
  ElMessage.success(`已提醒未提交"${homework.title}"的学生`)
}

const handleExport = (homework: HomeworkListVO) => {
  ElMessage.info('正在导出成绩表...')
  setTimeout(() => {
    ElMessage.success('成绩表已导出')
  }, 1500)
}
</script>

<template>
  <div class="homework-container">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">作业管理</h2>
      </div>
      <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">
        发布新作业
      </el-button>
    </div>

    <div class="homework-stats">
      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon">📋</div>
          <div class="stat-info">
            <span class="stat-label">进行中作业</span>
            <span class="stat-value">{{ stats.ongoing }}</span>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon">✅</div>
          <div class="stat-info">
            <span class="stat-label">已批改</span>
            <span class="stat-value">{{ stats.graded }}</span>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon">📊</div>
          <div class="stat-info">
            <span class="stat-label">平均完成率</span>
            <span class="stat-value">{{ stats.averageCompletion }}</span>
            <span class="stat-unit">%</span>
          </div>
        </div>
      </el-card>
    </div>

    <div class="action-bar">
      <div class="filter-group">
        <el-button
          :type="filterStatus === 'all' ? 'primary' : 'default'"
          round
          @click="filterStatus = 'all'"
        >
          全部
        </el-button>
        <el-button
          :type="filterStatus === 'ongoing' ? 'primary' : 'default'"
          round
          @click="filterStatus = 'ongoing'"
        >
          进行中
        </el-button>
        <el-button
          :type="filterStatus === 'graded' ? 'primary' : 'default'"
          round
          @click="filterStatus = 'graded'"
        >
          已批改
        </el-button>
        <el-button
          :type="filterStatus === 'expired' ? 'primary' : 'default'"
          round
          @click="filterStatus = 'expired'"
        >
          已截止
        </el-button>
      </div>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索作业名称"
        clearable
        style="width: 240px"
      >
        <template #prefix>
          <span>🔍</span>
        </template>
      </el-input>
    </div>

    <div v-loading="loading">
      <HomeworkList
        :homework-list="filteredHomework"
        @grade="handleGradeHomework"
        @view-detail="handleViewDetail"
        @edit="handleEditHomework"
        @delete="handleDeleteHomework"
        @remind="handleRemind"
        @export="handleExport"
      />
    </div>

    <CreateHomeworkDialog
      v-model="showCreateDialog"
      @submit="handleCreateHomework"
    />

    <EditHomeworkDialog
      v-model="showEditDialog"
      :homework="currentHomework"
      @updated="loadHomeworkList"
    />

    <SubmissionsDetailDialog
      v-model="showSubmissionsDialog"
      :homework="currentHomework"
    />
  </div>
</template>

<style scoped>
.homework-container {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.class-selector {
  width: 180px;
}

.class-selector :deep(.el-input__wrapper) {
  border-radius: 12px;
  background-color: #f8fafc;
  box-shadow: none;
  border: 1px solid #e2e8f0;
}

.empty-select {
  padding: 8px;
  text-align: center;
  color: #94a3b8;
  font-size: 14px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0;
}

.class-badge {
  font-size: 0.875rem;
  font-weight: 500;
  color: #0ea5e9;
  background: #e0f2fe;
  padding: 4px 12px;
  border-radius: 12px;
}

.homework-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 16px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  font-size: 2rem;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e0f2fe;
  border-radius: 50%;
}

.stat-info {
  flex: 1;
}

.stat-label {
  display: block;
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
}

.stat-unit {
  font-size: 1rem;
  color: #64748b;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.filter-group {
  display: flex;
  gap: 8px;
}

@media (max-width: 1000px) {
  .homework-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .homework-stats {
    grid-template-columns: 1fr;
  }
  
  .action-bar {
    flex-direction: column;
    gap: 12px;
  }
  
  .filter-group {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>

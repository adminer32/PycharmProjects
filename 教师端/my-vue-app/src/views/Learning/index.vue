<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useClassStore } from '@/store/class'
import { storeToRefs } from 'pinia'
import { learningApi, type LearningOverview, type StudentInfo, type PlanDetail as PlanDetailType, type ReviewInput, type PlanGroup } from '@/api/learningApi'
import StudentList from './components/StudentList.vue'
import PlanDetail from './components/PlanDetail.vue'
import VideoLibrary from './components/VideoLibrary.vue'
import EditPlanDialog from './components/EditPlanDialog.vue'
import ReviewPlanDialog from './components/ReviewPlanDialog.vue'
import RecommendVideoDialog from './components/RecommendVideoDialog.vue'
import AddVideoDialog from './components/AddVideoDialog.vue'
import type { VideoItem, VideoFormData, VideoRecommendation } from './types'
import { aiApi, type AiPlanInput, type AiPlanResult } from '@/api/aiApi'
import { MagicStick, Loading } from '@element-plus/icons-vue'

const searchKeyword = ref('')
const showEditDialog = ref(false)
const showReviewDialog = ref(false)
const showRecommendDialog = ref(false)
const showAddVideoDialog = ref(false)

const showAiPlanDialog = ref(false)
const aiPlanLoading = ref(false)
const aiPlanDuration = ref(4)
const aiSaving = ref(false)
const generatedPlan = ref<AiPlanResult | null>(null)

const showHistoryDialog = ref(false)
const planGroups = ref<PlanGroup[]>([])
const historyLoading = ref(false)

const classStore = useClassStore()
const { currentClass } = storeToRefs(classStore)
const loading = ref(false)

const currentStudent = ref<StudentInfo | null>(null)
const currentPlan = ref<PlanDetailType | null>(null)
const selectedVideo = ref<VideoItem | null>(null)

const students = ref<StudentInfo[]>([])

const videoLibrary = ref<VideoItem[]>([])

const currentWeekNumber = ref(1)
const totalWeeksCount = ref(0)

const stats = ref<LearningOverview>({
  completionRate: 0,
  pendingReviewCount: 0,
  totalStudents: 0,
  needAttentionCount: 0,
  needAttentionStudents: []
})

const loadLearningData = async () => {
  if (!classStore.currentClass?.id) return
  loading.value = true
  try {
    const [overviewRes, studentsRes] = await Promise.all([
      learningApi.getOverview(String(classStore.currentClass.id)),
      learningApi.getStudents(String(classStore.currentClass.id))
    ])
    if (overviewRes.data) stats.value = overviewRes.data
    if (studentsRes.data) students.value = studentsRes.data
  } catch (e) {
    console.error('加载学习管理数据失败', e)
  } finally {
    loading.value = false
  }
}

const loadVideos = async () => {
  try {
    const res = await learningApi.getVideos(classStore.currentClass?.id ? String(classStore.currentClass.id) : undefined)
    if (res.data) {
      const categoryMap: Record<string, string> = {
        '盘踢': 'panti',
        '磕踢': 'keti',
        '绷踢': 'bengti',
        '拐踢': 'guaiti',
        '踏踢': 'tati',
        '跳踢': 'tiaoti',
        '外摆': 'waibai',
        '里合': 'lihe'
      }
      videoLibrary.value = res.data.map(v => ({
        ...v,
        category: categoryMap[v.category] || v.category
      }))
    }
  } catch (e) {
    console.error('加载视频列表失败', e)
  }
}

onMounted(() => { loadLearningData(); loadVideos() })
watch(() => classStore.currentClass?.id, () => {
  if (classStore.currentClass?.id) {
    currentStudent.value = null
    currentPlan.value = null
    loadLearningData()
    loadVideos()
  }
})

const filteredStudents = computed(() => {
  if (!searchKeyword.value) return students.value
  const keyword = searchKeyword.value.toLowerCase()
  return students.value.filter(s =>
    s.name.toLowerCase().includes(keyword) ||
    s.studentId.includes(keyword)
  )
})

const handleSelectStudent = (student: StudentInfo) => {
  currentStudent.value = student
  loadStudentPlan(student.id)
}

const loadStudentPlan = async (studentId: number, weekNumber?: number) => {
  try {
    const res = await learningApi.getWeekPlan(studentId, weekNumber || 1)
    console.log('loadStudentPlan response:', res)
    console.log('loadStudentPlan tasks:', res.data?.tasks)
    if (res.data) {
      currentPlan.value = res.data
      currentWeekNumber.value = res.data.weekNumber || weekNumber || 1
      totalWeeksCount.value = res.data.totalWeeks || 1
    } else {
      currentPlan.value = null
      currentWeekNumber.value = 1
      totalWeeksCount.value = 0
    }
  } catch (e) {
    console.error('加载训练计划失败', e)
    ElMessage.error('加载训练计划失败')
  }
}

const handleChangeWeek = (week: number) => {
  if (currentStudent.value) {
    loadStudentPlan(currentStudent.value.id, week)
  }
}

const handleEditPlan = () => {
  showEditDialog.value = true
}

const handleReviewPlan = () => {
  showReviewDialog.value = true
}

const handleApprovePlan = async () => {
  if (!currentPlan.value) return
  try {
    await learningApi.reviewPlan({
      planId: currentPlan.value.id,
      teacherId: 2,
      action: 'approve'
    })
    currentPlan.value.reviewStatus = 'approved'
    currentPlan.value.reviewComment = ''
    ElMessage.success('计划已批准')
    loadLearningData()
  } catch (e) {
    console.error('审批失败', e)
    ElMessage.error('审批失败')
  }
}

const handleRejectPlan = async (reason: string) => {
  if (!currentPlan.value) return
  try {
    await learningApi.reviewPlan({
      planId: currentPlan.value.id,
      teacherId: 2,
      action: 'reject',
      comment: reason
    })
    currentPlan.value.reviewStatus = 'rejected'
    currentPlan.value.reviewComment = reason
    ElMessage.success('计划已驳回')
    loadLearningData()
  } catch (e) {
    console.error('驳回失败', e)
    ElMessage.error('驳回失败')
  }
}

const handleSavePlan = (plan: TrainingPlan) => {
  currentPlan.value = plan
  ElMessage.success(`已为${currentStudent.value?.name}保存训练计划`)
}

const handleRefresh = () => {
  loadLearningData()
  ElMessage.success('数据已刷新')
}

const showVideoDialog = ref(false)
const selectedVideoForPreview = ref<VideoItem | null>(null)

const handleVideoSelect = (video: VideoItem) => {
  selectedVideoForPreview.value = video
  showVideoDialog.value = true
}

const handleRecommendVideo = (video: VideoItem) => {
  selectedVideo.value = video
  showRecommendDialog.value = true
}

const handleConfirmRecommend = async (studentIds: number[], videoId: number, comment: string) => {
  try {
    await learningApi.recommendVideo({
      videoId,
      studentIds,
      teacherId: 2,
      comment
    })
    const video = videoLibrary.value.find(v => v.id === videoId)
    if (video) {
      if (!video.recommendedTo) {
        video.recommendedTo = []
      }
      video.recommendedTo = [...new Set([...video.recommendedTo, ...studentIds])]
    }
    ElMessage.success(`已将视频推荐给 ${studentIds.length} 名学生`)
  } catch (e) {
    console.error('推荐视频失败', e)
    ElMessage.error('推荐视频失败')
  }
}

const handleAddVideo = () => {
  showAddVideoDialog.value = true
}

const handleConfirmAddVideo = async (videoData: VideoFormData) => {
  try {
    const res = await learningApi.addVideo({
      ...videoData,
      teacherId: 2,
      classId: classStore.currentClass?.id
    })
    if (res.data) {
      const categoryMap: Record<string, string> = {
        '盘踢': 'panti',
        '磕踢': 'keti',
        '绷踢': 'bengti',
        '拐踢': 'guaiti',
        '踏踢': 'tati',
        '跳踢': 'tiaoti'
      }
      videoLibrary.value.push({
        ...res.data,
        category: categoryMap[res.data.category] || res.data.category,
        recommendedTo: []
      })
      ElMessage.success('视频添加成功')
    }
  } catch (e) {
    console.error('添加视频失败', e)
    ElMessage.error('添加视频失败')
  }
}

const MOCK_AI_PLAN = {
  goal: "在2周内提升该学生的毽球综合技能，重点加强盘踢、绷踢和拐踢的稳定性和准确性，使其各项技能得分均超过班级平均分10分以上，并培养持续训练的兴趣。",
  weeklyPlans: [
    {
      week: 1,
      theme: "基础巩固与动作规范",
      focusSkills: ["盘踢", "绷踢", "拐踢"],
      weeklyGoal: "掌握盘踢、绷踢、拐踢的基本动作要领，能完成简单的组合动作，各项技能在练习中连续次数较初始有明显提升。",
      dailyTasks: [
        { day: "周一", content: "热身5分钟（慢跑、关节活动）。盘踢专项：对墙练习，目标连续20次不失误，共3组。绷踢基础：原地单脚绷踢，每组15次，左右脚各2组。拐踢体验：尝试用外脚背踢毽，每组10次，共2组。放松拉伸5分钟。", duration: 35 },
        { day: "周三", content: "热身5分钟。盘踢进阶：移动中盘踢（前后左右小范围移动），每组15次，共3组。绷踢应用：两人一组对传绷踢（或对墙），目标连续10次成功，共3组。拐踢巩固：结合盘踢，进行盘踢-拐踢组合练习，每组8次组合，共2组。趣味挑战：尝试用不同部位（膝、肩）接毽2分钟。放松拉伸5分钟。", duration: 40 },
        { day: "周五", content: `热身5分钟。综合循环训练：设置3个站点（盘踢对墙、绷踢计数、拐踢过障碍），每个站点练习5分钟，循环2轮。技能游戏：进行"毽球保龄球"（用毽球踢倒水瓶）游戏10分钟。放松拉伸5分钟。`, duration: 45 },
        { day: "周日", content: "热身5分钟。复习与测试：分别测试盘踢、绷踢、拐踢的1分钟最高连续次数并记录。针对弱项（如盘踢稳定性）进行补偿训练3组。自由练习：尝试自创小套路（如盘踢+拐踢+接住）5分钟。放松拉伸5分钟。", duration: 40 }
      ]
    },
    {
      week: 2,
      theme: "技能整合与实战应用",
      focusSkills: ["盘踢", "绷踢", "拐踢"],
      weeklyGoal: "熟练整合盘踢、绷踢和拐踢，在移动和组合动作中保持稳定，初步具备在简单对抗中运用技能的能力，各项技能得分预估超过班级平均分10分以上。",
      dailyTasks: [
        { day: "周二", content: "热身5分钟。盘踢强化：在移动中完成盘踢并控制毽子落点（设定地面目标区域），每组15次，共3组。绷踢强化：提高绷踢高度和速度，对墙快速反弹练习，每组20次，共3组。拐踢强化：在跑动中完成拐踢变向，每组10次，共2组。协调性练习：左右脚交替盘踢1分钟，共2组。放松拉伸5分钟。", duration: 40 },
        { day: "周四", content: `热身5分钟。组合技能训练：练习"盘踢-绷踢-拐踢"三联动作，力求流畅，每组5次完整组合，共4组。实战模拟：与搭档（或对墙）进行小范围对踢，融入三种技能，持续8分钟。反应游戏："听指令踢法"（随机喊盘、绷、拐）练习5分钟。放松拉伸5分钟。`, duration: 45 },
        { day: "周六", content: "热身5分钟。综合耐力训练：不间断完成盘踢50次+绷踢30次+拐踢20次为一轮，完成2轮，中间休息2分钟。精准度挑战：设定不同距离和角度的目标进行三种踢法的精准传球练习10分钟。放松拉伸5分钟。", duration: 40 },
        { day: "周日", content: "热身5分钟。最终评估与巩固：全面测试盘踢、绷踢、拐踢的1分钟连续次数及准确性，与第一周对比。针对评估结果进行30分钟强化补偿训练。趣味展示：录制一段30秒的个人技能展示视频。放松拉伸5分钟，总结两周进步。", duration: 45 }
      ]
    }
  ],
  milestones: [
    "第一周末：三种基本踢法动作规范，连续次数较训练前提升20%以上。",
    "第二周末：能流畅完成至少一套包含盘、绷、拐的组合动作，并在模拟实战中有效运用。"
  ],
  tips: "你好！你的基础不错，尤其是拐踢有优势。这两周的计划旨在帮你全面提升。请记住：1. 训练前务必热身，防止受伤。2. 不要急于求成，关注动作质量而非单纯次数。3. 遇到瓶颈时，回顾动作要点或休息一下再继续。4. 把游戏环节当作放松和发现乐趣的机会。相信通过科学和有趣的训练，你的综合技能一定会更上一层楼！坚持就是胜利，期待你的进步！"
}

const handleAiGeneratePlan = () => {
  showAiPlanDialog.value = true
  aiPlanLoading.value = false
  generatedPlan.value = null
}

const handleStartGeneratePlan = async () => {
  if (!currentStudent.value) {
    ElMessage.warning('请先选择一个学生')
    return
  }

  aiPlanLoading.value = true
  generatedPlan.value = null

  try {
    await new Promise(resolve => setTimeout(resolve, 5000))
    generatedPlan.value = { ...MOCK_AI_PLAN }
    ElMessage.success('AI训练计划生成成功！')
  } catch (e) {
    ElMessage.error('生成失败')
  } finally {
    aiPlanLoading.value = false
  }
}

const handleSaveAiPlan = async () => {
  if (!generatedPlan.value || !currentStudent.value) return

  aiSaving.value = true
  try {
    await aiApi.savePlan({
      studentId: currentStudent.value.id,
      classId: currentClass.value?.id,
      totalWeeks: aiPlanDuration.value,
      goal: generatedPlan.value.goal,
      weeklyPlans: generatedPlan.value.weeklyPlans,
      milestones: generatedPlan.value.milestones,
      tips: generatedPlan.value.tips
    })
    ElMessage.success(`✅ 计划已保存！共 ${aiPlanDuration.value} 周训练计划已存入数据库`)
    showAiPlanDialog.value = false
    loadStudentPlan(currentStudent.value.id)
    loadLearningData()
  } catch (e) {
    ElMessage.error('保存失败：' + (e instanceof Error ? e.message : '未知错误'))
  } finally {
    aiSaving.value = false
  }
}

const handleShowHistory = async () => {
  if (!currentStudent.value) return
  showHistoryDialog.value = true
  historyLoading.value = true
  try {
    const res = await learningApi.getPlanGroups(currentStudent.value.id)
    planGroups.value = res.data || []
  } catch (e) {
    console.error('加载历史计划失败', e)
    ElMessage.error('加载历史计划失败')
  } finally {
    historyLoading.value = false
  }
}

const handleViewHistoryGroup = async (groupId: number, weekNumber: number) => {
  showHistoryDialog.value = false
  if (currentStudent.value) {
    loadStudentPlan(currentStudent.value.id, weekNumber)
  }
}

const handleDeleteHistoryGroup = async (groupId: number) => {
  try {
    await learningApi.deletePlanGroup(currentStudent.value!.id, groupId)
    ElMessage.success('计划组已删除')
    handleShowHistory()
    loadLearningData()
    if (currentStudent.value) {
      loadStudentPlan(currentStudent.value.id)
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}
</script>

<template>
  <div class="learning-container" v-loading="loading">
    <div class="page-header">
      <h2 class="page-title">AI训练计划管理</h2>
    </div>

    <div class="plan-stats">
      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon completion"></div>
          <div class="stat-info">
            <span class="stat-label">班级计划完成率</span>
            <span class="stat-value">{{ stats.completionRate ?? 0 }}%</span>
            <span v-if="stats.completionRate >= 80" class="stat-trend up">优秀</span>
            <span v-else-if="stats.completionRate >= 50" class="stat-trend up">↑ 提升</span>
            <span v-else class="stat-trend down">需关注</span>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon plans"></div>
          <div class="stat-info">
            <span class="stat-label">待审批计划</span>
            <span class="stat-value">{{ stats.pendingReviewCount ?? 0 }}</span>
            <span class="stat-unit">/{{ stats.totalStudents ?? 0 }}人</span>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon attention"></div>
          <div class="stat-info">
            <span class="stat-label">重点关注</span>
            <span class="stat-value">{{ stats.needAttentionCount ?? 0 }}</span>
            <span class="stat-unit">人</span>
          </div>
        </div>
        <div v-if="stats.needAttentionStudents?.length > 0" class="attention-list">
          <el-tooltip
            v-for="student in stats.needAttentionStudents.slice(0, 5)"
            :key="student.id"
            :content="`${student.name} - ${student.reason}`"
            placement="bottom"
          >
            <div class="attention-avatar">{{ student.name[0] }}</div>
          </el-tooltip>
          <span v-if="(stats.needAttentionCount ?? 0) > 5" class="more-text">+{{ stats.needAttentionCount - 5 }}</span>
        </div>
      </el-card>
    </div>

    <div class="action-bar">
      <div class="action-left">
        <el-button @click="handleRefresh">
          刷新数据
        </el-button>
        <el-button
          type="success"
          plain
          :icon="MagicStick"
          :disabled="!currentStudent"
          @click="handleAiGeneratePlan"
        >
          🤖 AI智能规划
        </el-button>
      </div>
      <div class="action-right">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索学生姓名/学号"
          clearable
          style="width: 240px"
        >
          <template #prefix>
            <span></span>
          </template>
        </el-input>
      </div>
    </div>

    <div class="plan-management">
      <StudentList
        :student-list="filteredStudents"
        :selected-id="currentStudent?.id || null"
        @select="handleSelectStudent"
      />

      <PlanDetail
        :student="currentStudent"
        :plan="currentPlan"
        :current-week="currentWeekNumber"
        :total-weeks="totalWeeksCount"
        @edit="handleEditPlan"
        @review="handleReviewPlan"
        @change-week="handleChangeWeek"
        @show-history="handleShowHistory"
      />
    </div>

    <VideoLibrary
      :video-list="videoLibrary"
      @select="handleVideoSelect"
      @recommend="handleRecommendVideo"
      @add="handleAddVideo"
    />

    <EditPlanDialog
      v-model="showEditDialog"
      :student-name="currentStudent?.name || ''"
      :plan="currentPlan"
      :video-list="videoLibrary"
      @save="handleSavePlan"
    />

    <ReviewPlanDialog
      v-model="showReviewDialog"
      :student="currentStudent"
      :plan="currentPlan"
      @approve="handleApprovePlan"
      @reject="handleRejectPlan"
    />

    <RecommendVideoDialog
      v-model="showRecommendDialog"
      :video="selectedVideo"
      :students="students"
      @recommend="handleConfirmRecommend"
    />

    <AddVideoDialog
      v-model="showAddVideoDialog"
      @add="handleConfirmAddVideo"
    />

    <!-- 视频预览弹窗 -->
    <el-dialog
      v-model="showVideoDialog"
      :title="selectedVideoForPreview?.title || '视频预览'"
      width="800px"
      destroy-on-close
    >
      <div v-if="selectedVideoForPreview" class="video-preview-container">
        <video
          v-if="selectedVideoForPreview.url"
          controls
          autoplay
          style="width: 100%; max-height: 500px;"
        >
          <source :src="selectedVideoForPreview.url" type="video/mp4">
          您的浏览器不支持视频播放
        </video>
        <div class="video-info-panel">
          <p><strong>标题：</strong>{{ selectedVideoForPreview.title }}</p>
          <p><strong>时长：</strong>{{ selectedVideoForPreview.duration }}</p>
          <p><strong>分类：</strong>{{ selectedVideoForPreview.category }}</p>
          <p v-if="selectedVideoForPreview.description"><strong>描述：</strong>{{ selectedVideoForPreview.description }}</p>
        </div>
      </div>
    </el-dialog>

    <!-- AI 智能训练计划弹窗 -->
    <el-dialog
      v-model="showAiPlanDialog"
      :title="`🤖 AI智能训练计划 - ${currentStudent?.name || '学生'}`"
      width="750px"
      destroy-on-close
    >
      <div class="ai-plan-container">
        <div class="ai-plan-header">
          <p>AI 将根据 <strong>{{ currentStudent?.name }}</strong> 的技能弱项数据，
             自动生成个性化训练计划</p>
          <div class="plan-duration-selector">
            <span>计划周期：</span>
            <el-select v-model="aiPlanDuration" style="width: 100px">
              <el-option :value="2" label="2周" />
              <el-option :value="4" label="4周（推荐）" />
              <el-option :value="8" label="8周" />
              <el-option :value="12" label="12周" />
            </el-select>
            <el-button
              type="primary"
              :loading="aiPlanLoading"
              @click="handleStartGeneratePlan"
              style="margin-left: 12px"
            >
              {{ aiPlanLoading ? 'AI正在思考...' : '🚀 生成计划' }}
            </el-button>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="aiPlanLoading && !generatedPlan" class="ai-loading-state">
          <el-icon class="is-loading" :size="40" color="#10b981"><Loading /></el-icon>
          <p>AI 正在分析学生数据并生成训练计划...</p>
          <p class="loading-hint">这可能需要10-30秒，请耐心等待</p>
        </div>

        <!-- 计划展示区域 -->
        <div v-if="generatedPlan" class="generated-plan-content">
          <div class="plan-goal-card">
            <h4>🎯 总体目标</h4>
            <p>{{ generatedPlan.goal }}</p>
          </div>

          <div v-if="generatedPlan.weeklyPlans?.length" class="weekly-plans">
            <h4>📅 周计划详情</h4>
            <div
              v-for="(week, idx) in generatedPlan.weeklyPlans"
              :key="idx"
              class="week-card"
            >
              <div class="week-header">
                <span class="week-number">第{{ week.week }}周</span>
                <span class="week-theme">{{ week.theme }}</span>
              </div>
              <div class="focus-skills">
                <span v-for="skill in week.focusSkills" :key="skill" class="skill-tag">
                  {{ skill }}
                </span>
              </div>
              <div class="weekly-goal">本周目标：{{ week.weeklyGoal }}</div>
              <div class="daily-tasks">
                <div
                  v-for="task in week.dailyTasks"
                  :key="task.day"
                  class="daily-task"
                >
                  <span class="task-day">{{ task.day }}</span>
                  <span class="task-content">{{ task.content }}</span>
                  <span class="task-duration">{{ task.duration }}分钟</span>
                </div>
              </div>
            </div>
          </div>

          <div v-if="generatedPlan.milestones?.length" class="milestones">
            <h4>🏆 里程碑</h4>
            <ul>
              <li v-for="(ms, idx) in generatedPlan.milestones" :key="idx">{{ ms }}</li>
            </ul>
          </div>

          <div v-if="generatedPlan.tips" class="tips-card">
            <h4>💡 教练建议</h4>
            <p>{{ generatedPlan.tips }}</p>
          </div>
        </div>

        <!-- 保存操作区 -->
        <div v-if="generatedPlan" class="ai-plan-actions">
          <el-button @click="showAiPlanDialog = false">取消</el-button>
          <el-button type="primary" :loading="aiSaving" @click="handleSaveAiPlan">
            💾 保存此计划（{{ aiPlanDuration }}周）
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 历史计划弹窗 -->
    <el-dialog
      v-model="showHistoryDialog"
      :title="`📋 ${currentStudent?.name || '学生'} - 历史训练计划`"
      width="700px"
      destroy-on-close
    >
      <div v-loading="historyLoading" class="history-content">
        <div v-if="planGroups.length === 0" class="empty-history">
          <p>暂无历史训练计划</p>
        </div>
        <div v-else class="history-list">
          <div
            v-for="group in planGroups"
            :key="group.planGroupId"
            class="history-group-card"
          >
            <div class="group-header">
              <div class="group-info">
                <span class="group-date">{{ new Date(group.createdAt).toLocaleDateString('zh-CN') }}</span>
                <span class="group-meta">{{ group.totalWeeks }}周计划 · {{ group.weekCount }}个周次</span>
              </div>
              <div class="group-actions">
                <el-button
                  type="primary"
                  size="small"
                  @click="handleViewHistoryGroup(group.planGroupId, 1)"
                >
                  查看
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  plain
                  @click="handleDeleteHistoryGroup(group.planGroupId)"
                >
                  删除
                </el-button>
              </div>
            </div>
            <div class="group-goal">
              <strong>总体目标：</strong>{{ group.overallGoal }}
            </div>
            <div class="group-status">
              <el-tag v-if="group.pendingCount > 0" type="warning" size="small">
                待审核 {{ group.pendingCount }}周
              </el-tag>
              <el-tag v-if="group.approvedCount > 0" type="success" size="small">
                已批准 {{ group.approvedCount }}周
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.learning-container {
  padding: 0;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary, #1e293b);
}

.plan-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 20px;
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
  border-radius: 50%;
  flex-shrink: 0;
}

.stat-icon.completion {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
}

.stat-icon.completion::before {
  content: '📊';
}

.stat-icon.plans {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
}

.stat-icon.plans::before {
  content: '📝';
}

.stat-icon.attention {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
}

.stat-icon.attention::before {
  content: '⚠️';
}

.stat-info {
  flex: 1;
}

.stat-label {
  display: block;
  font-size: 0.85rem;
  color: var(--text-secondary, #64748b);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 1.3rem;
  font-weight: 600;
  color: var(--text-primary, #1e293b);
}

.stat-unit {
  font-size: 0.9rem;
  color: var(--text-secondary, #64748b);
  margin-left: 4px;
}

.stat-trend {
  font-size: 0.75rem;
  margin-left: 8px;
  padding: 2px 6px;
  border-radius: 30px;
}

.stat-trend.up {
  background: rgba(16, 185, 129, 0.1);
  color: var(--success, #10b981);
}

.stat-trend.down {
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger, #ef4444);
}

.attention-list {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color, #e2e8f0);
}

.attention-avatar {
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #ef4444 0%, #fca5a5 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s;
}

.attention-avatar:hover {
  transform: scale(1.1);
}

.more-text {
  font-size: 0.75rem;
  color: var(--text-secondary, #64748b);
  font-weight: 500;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.action-left {
  display: flex;
  gap: 12px;
}

.plan-management {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 24px;
  margin-bottom: 24px;
  min-height: 500px;
  align-items: stretch;
}

@media (max-width: 1000px) {
  .plan-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .plan-management {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .plan-stats {
    grid-template-columns: 1fr;
  }

  .action-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .action-left {
    justify-content: center;
  }
}

.video-preview-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.video-info-panel {
  background: var(--bg-primary, #f8fafc);
  padding: 16px;
  border-radius: 8px;
}

.video-info-panel p {
  margin: 8px 0;
  font-size: 0.9rem;
  color: var(--text-primary, #1e293b);
}

.ai-plan-container {
  padding: 0;
}

.ai-plan-header {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.ai-plan-header p {
  margin: 0 0 12px 0;
  color: #065f46;
  font-size: 0.95rem;
}

.plan-duration-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  color: #374151;
}

.ai-loading-state {
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
}

.ai-loading-state .el-icon {
  margin-bottom: 12px;
}

.ai-loading-state p {
  margin: 4px 0;
}

.loading-hint {
  font-size: 0.85rem;
  color: #9ca3af;
}

.generated-plan-content {
  max-height: 500px;
  overflow-y: auto;
  padding-right: 4px;
}

.plan-goal-card {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  padding: 16px;
  border-radius: 10px;
  margin-bottom: 20px;
  border-left: 4px solid #f59e0b;
}

.plan-goal-card h4 {
  margin: 0 0 8px 0;
  color: #92400e;
  font-size: 1rem;
}

.plan-goal-card p {
  margin: 0;
  color: #78350f;
  line-height: 1.6;
}

.weekly-plans h4,
.milestones h4,
.tips-card h4 {
  margin: 16px 0 12px 0;
  font-size: 1rem;
  color: #1e293b;
}

.week-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 12px;
  transition: box-shadow 0.2s;
}

.week-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

.week-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.week-number {
  font-weight: 700;
  font-size: 1.05rem;
  color: #2563eb;
  background: #dbeafe;
  padding: 4px 12px;
  border-radius: 20px;
}

.week-theme {
  font-weight: 600;
  color: #475569;
}

.focus-skills {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.skill-tag {
  background: #ede9fe;
  color: #5b21b6;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 0.8rem;
}

.weekly-goal {
  font-size: 0.9rem;
  color: #059669;
  margin-bottom: 10px;
  padding: 8px;
  background: #ecfdf5;
  border-radius: 6px;
}

.daily-tasks {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.daily-task {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 0.88rem;
}

.task-day {
  font-weight: 600;
  color: #0369a1;
  min-width: 36px;
}

.task-content {
  flex: 1;
  color: #334155;
}

.task-duration {
  color: #64748b;
  font-size: 0.82rem;
}

.milestones ul {
  padding-left: 20px;
  margin: 0;
  color: #475569;
}

.milestones li {
  margin-bottom: 6px;
  line-height: 1.5;
}

.tips-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  padding: 16px;
  border-radius: 10px;
  border-left: 4px solid #0ea5e9;
}

.tips-card p {
  margin: 8px 0 0 0;
  color: #075985;
  line-height: 1.6;
}

.ai-plan-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.history-content {
  min-height: 200px;
}

.empty-history {
  text-align: center;
  padding: 40px;
  color: #94a3b8;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-group-card {
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e2e8f0;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.group-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.group-date {
  font-weight: 600;
  color: #1e293b;
  font-size: 0.95rem;
}

.group-meta {
  font-size: 0.82rem;
  color: #64748b;
}

.group-actions {
  display: flex;
  gap: 8px;
}

.group-goal {
  font-size: 0.88rem;
  color: #334155;
  margin-bottom: 8px;
  line-height: 1.5;
}

.group-status {
  display: flex;
  gap: 8px;
}
</style>

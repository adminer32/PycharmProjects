<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import 'echarts-liquidfill'
import { useRouter } from 'vue-router'
import { getLearningStatsApi, getActionScoresApi, getWeeklyTrendApi, getCheckinRecordsApi, getShuttlecockSkillsApi } from '@/api/learning/learningApi'
import { useUserStore } from '@/stores/userStore'

const router = useRouter();
const userStore = useUserStore();

// 学生个人分析数据 - 从用户Store获取
const selectedStudent = ref({
  id: userStore.myInfo.userId || '1',
  name: userStore.myInfo.name || '加载中...',
  code: userStore.myInfo.code || '',
  score: 0,
  avatar: userStore.myInfo.name?.charAt(0) || '?'
})

const studentMetrics = ref({
  learningHours: 0,
  homeworkScore: 0,
  checkinRate: 0,
  skillPoints: 3
})

const activeTrendType = ref<'learningHours' | 'homeworkScores'>('learningHours');

interface StudentTrendData {
  weeks: string[];
  learningHours: number[];
  homeworkScores: number[];
}

const studentTrendData = ref<StudentTrendData>({
  weeks: ['第1周', '第2周', '第3周', '第4周'],
  learningHours: [0, 0, 0, 0],
  homeworkScores: [0, 0, 0, 0]
});

const checkinChartData = ref<{
  dates: string[];
  durations: number[];
}>({
  dates: [],
  durations: []
});

const studentActionData = ref<any[]>([])
const checkinData = ref(new Set<number>())

const shuttlecockSkillsData = ref<{
  personal: number[];
  classAvg: number[];
  indicators: { name: string; max: number }[];
}>({
  personal: [],
  classAvg: [],
  indicators: []
})

const trendTypes: { value: 'learningHours' | 'homeworkScores'; label: string }[] = [
  { value: 'learningHours', label: '学习时长' },
  { value: 'homeworkScores', label: '作业平均分' }
];

const getTrendData = () => {
  return studentTrendData.value[activeTrendType.value];
};

const getTrendLabel = () => {
  return trendTypes.find(type => type.value === activeTrendType.value)?.label || '';
};

const getYAxisRange = () => {
  if (activeTrendType.value === 'learningHours') {
    return { min: 0, max: 50 };
  } else {
    return { min: 60, max: 100 };
  }
};

// 加载数据
const loadData = async () => {
  console.log('loadData called, studentId:', selectedStudent.value.id)
  const studentId = parseInt(selectedStudent.value.id as string) || 1
  
  try {
    // 获取学习统计
    const statsRes = await getLearningStatsApi(studentId)
    if (statsRes) {
      studentMetrics.value = {
        learningHours: statsRes.learning_hours || 0,
        homeworkScore: statsRes.homework_avg_score || 0,
        checkinRate: statsRes.checkin_rate || 0,
        skillPoints: statsRes.skill_points || 0
      }
    }
    
    // 获取动作评分
    const actionRes = await getActionScoresApi(studentId)
    if (actionRes && Array.isArray(actionRes)) {
      studentActionData.value = actionRes.map((s: any) => ({
        name: s.action_type,
        score: s.current_score,
        historyScore: s.history_avg_score,
        recentScores: s.recent_scores || [],
        feedback: s.feedback || []
      }))
    }
    
    // 获取周趋势
    const trendRes = await getWeeklyTrendApi(studentId)
    if (trendRes && Array.isArray(trendRes)) {
      studentTrendData.value = {
        weeks: trendRes.map((t: any) => t.week),
        learningHours: trendRes.map((t: any) => t.learning_hours),
        homeworkScores: trendRes.map((t: any) => t.homework_score)
      }
      // 更新趋势图表
      updateTrendChart()
    }
    
    // 获取打卡记录
    const checkinRes = await getCheckinRecordsApi(studentId)
    if (checkinRes && Array.isArray(checkinRes)) {
      checkinData.value = new Set(
        checkinRes.map((r: any) => new Date(r.checkin_date).getDate())
      )
      // 设置学习时长图表数据，按日期升序排列，时间早的在左边
      const sortedCheckins = [...checkinRes].sort((a, b) => 
        new Date(a.checkin_date).getTime() - new Date(b.checkin_date).getTime()
      )
      checkinChartData.value = {
        dates: sortedCheckins.map((r: any) => r.checkin_date).slice(0, 14), // 最早的14条
        durations: sortedCheckins.map((r: any) => Number(r.duration) || 0).slice(0, 14)
      }
      updateCheckinStats()
    }
    
    // 获取毽球技能数据
    const skillsRes = await getShuttlecockSkillsApi(studentId)
    if (skillsRes && skillsRes.status === 'success' && skillsRes.data) {
      shuttlecockSkillsData.value = {
        personal: skillsRes.data.personal || [],
        classAvg: skillsRes.data.class_avg || [],
        indicators: skillsRes.data.indicators || [
          { name: '盘踢', max: 100 },
          { name: '绷踢', max: 100 },
          { name: '拐踢', max: 100 },
          { name: '磕踢', max: 100 },
          { name: '踏踢', max: 100 },
          { name: '跳踢', max: 100 }
        ]
      }
      initShuttlecockRadarChart()
    }
    
    // 更新图表
    updateTrendChart()
    initSunburstChart()
    
    console.log('学情数据加载完成', {
      metrics: studentMetrics.value,
      actions: studentActionData.value,
      trend: studentTrendData.value,
      checkins: checkinData.value
    })
  } catch (error) {
    console.error('加载学情数据失败:', error)
  }
}

// 在组件挂载时加载数据
const selectedRecentScore = ref<{ actionName: string; score: number; index: number } | null>(null);
const expandedAction = ref<string | null>(null);

const selectedAction = ref<{
  name: string;
  score: number;
  feedback: string[];
} | null>(null);

const selectRecentScore = (actionName: string, score: number, index: number) => {
  selectedRecentScore.value = { actionName, score, index };
};

const selectActionDetail = (action: typeof studentActionData.value[0]) => {
  selectedAction.value = {
    name: action.name,
    score: action.score,
    feedback: action.feedback || []
  };
};

const toggleActionHistory = (actionName: string) => {
  if (expandedAction.value === actionName) {
    expandedAction.value = null;
  } else {
    expandedAction.value = actionName;
  }
};

const getJointScore = (joint: 'hip' | 'knee' | 'ankle' | 'height') => {
  const baseScore = selectedAction.value?.score || 85;
  const variations: Record<string, number> = {
    hip: Math.min(100, baseScore + 5),
    knee: Math.min(100, baseScore + 2),
    ankle: Math.min(100, baseScore - 3),
    height: Math.min(100, baseScore + 8)
  };
  return variations[joint] || baseScore;
};

// 监听动作数据加载，自动选中第一个
watch(studentActionData, (newData) => {
  if (newData.length > 0 && !selectedAction.value) {
    selectActionDetail(newData[0])
  }
}, { immediate: true })

onMounted(async () => {
  // 获取用户信息
  await userStore.getMyInfo()
  
  // 更新学生信息
  selectedStudent.value = {
    id: userStore.myInfo.userId || '1',
    name: userStore.myInfo.name || '加载中...',
    code: userStore.myInfo.code || '',
    score: 0,
    avatar: userStore.myInfo.name?.charAt(0) || '?'
  }
  
  loadData()
  initCharts()
  
  // 初始化打卡日历
  daysInMonth.value = getDaysInMonth(currentYear.value, currentMonth.value)
  emptyDays.value = calculateEmptyDays(currentYear.value, currentMonth.value)
  updateCheckinStats()
})

const getDisplayScore = (action: typeof studentActionData.value[0]) => {
  if (selectedRecentScore.value && selectedRecentScore.value.actionName === action.name) {
    return selectedRecentScore.value.score;
  }
  return action.historyScore;
};

const getDisplayLabel = (action: typeof studentActionData.value[0]) => {
  if (selectedRecentScore.value && selectedRecentScore.value.actionName === action.name) {
    return `第${selectedRecentScore.value.index + 1}次练习`;
  }
  return '历史平均';
};

// 打卡相关数据
const currentDate = new Date()
const currentYear = ref(currentDate.getFullYear())
const currentMonth = ref(currentDate.getMonth() + 1) // 月份从1开始
const checkinStats = ref({
  totalDays: 0,
  consecutiveDays: 0
})
const daysInMonth = ref(31) // 初始值，后续会根据月份更新
const emptyDays = ref(0) // 用于日历对齐的空白天数

// 导出报告
const exportReport = () => {
  showToast(`正在生成 ${selectedStudent.value.name} 的学情报告...`, 'info')
  setTimeout(() => {
    showToast('报告已导出，请查看下载文件夹', 'success')
  }, 1500)
}

// 分享给家长
const shareReport = () => {
  showToast(`已将 ${selectedStudent.value.name} 的学情报告发送给家长`, 'success')
}

// 计算指定月份的天数
const getDaysInMonth = (year: number, month: number) => {
  return new Date(year, month, 0).getDate()
}

// 计算日历开始的空白天数（用于对齐周一）
const calculateEmptyDays = (year: number, month: number) => {
  const firstDay = new Date(year, month - 1, 1)
  const dayOfWeek = firstDay.getDay() // 0-6, 0是周日
  // 调整为周一为第一天，0表示周一，6表示周日
  return dayOfWeek === 0 ? 6 : dayOfWeek - 1
}

// 检查某一天是否已打卡
const isChecked = (day: number) => {
  return checkinData.value.has(day)
}

// 悬浮窗相关
const showTimerModal = ref(false);
const showCheckinOptionsModal = ref(false);
const selectedDay = ref(0);
const timerSeconds = ref(0);
const isTimerRunning = ref(false);
const requiredDuration = ref(1800); // 指定时长：30分钟（1800秒）
let timerInterval: number | null = null;

// 切换打卡状态
const toggleCheckin = (day: number) => {
  if (checkinData.value.has(day)) {
    checkinData.value.delete(day)
    updateCheckinStats()
  } else {
    // 显示打卡选项悬浮窗
    selectedDay.value = day;
    showCheckinOptionsModal.value = true;
  }
};

// 开始计时
const startTimer = () => {
  if (timerInterval) {
    clearInterval(timerInterval);
  }
  timerInterval = window.setInterval(() => {
    timerSeconds.value++;
  }, 1000);
  isTimerRunning.value = true;
};

// 暂停计时
const pauseTimer = () => {
  if (timerInterval) {
    clearInterval(timerInterval);
    timerInterval = null;
  }
  isTimerRunning.value = false;
};

// 停止计时并完成打卡
const stopTimer = () => {
  if (timerInterval) {
    clearInterval(timerInterval);
    timerInterval = null;
  }
  isTimerRunning.value = false;
  // 完成打卡
  checkinData.value.add(selectedDay.value);
  updateCheckinStats();
  showTimerModal.value = false;
};

// 取消打卡
const cancelCheckin = () => {
  if (timerInterval) {
    clearInterval(timerInterval);
    timerInterval = null;
  }
  isTimerRunning.value = false;
  showTimerModal.value = false;
};

// 去学习AI课堂
const goToAIClassroom = () => {
  showCheckinOptionsModal.value = false;
  // 跳转到AI课堂页面
  router.push('/smart_class');
};

// 去完成AI训练计划
const goToAITraining = () => {
  showCheckinOptionsModal.value = false;
  // 显示时钟悬浮窗
  showTimerModal.value = true;
  // 重置计时器
  timerSeconds.value = 0;
};

// 取消打卡选项
const cancelCheckinOptions = () => {
  showCheckinOptionsModal.value = false;
};

// 格式化时间
const formatTime = (seconds: number) => {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;
  return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
};



// 更新打卡统计数据
const updateCheckinStats = () => {
  // 优先使用从API获取的数据，如果没有则使用本地计算
  if (checkinData.value.size > 0) {
    // 计算总打卡天数
    checkinStats.value.totalDays = checkinData.value.size
    
    // 计算连续打卡天数
    let consecutive = 0
    let currentDay = daysInMonth.value
    while (checkinData.value.has(currentDay) && currentDay > 0) {
      consecutive++
      currentDay--
    }
    checkinStats.value.consecutiveDays = consecutive
  }
}

// 月份切换
const changeMonth = (direction: string) => {
  if (direction === 'prev') {
    if (currentMonth.value === 1) {
      currentMonth.value = 12
      currentYear.value--
    } else {
      currentMonth.value--
    }
  } else {
    if (currentMonth.value === 12) {
      currentMonth.value = 1
      currentYear.value++
    } else {
      currentMonth.value++
    }
  }
  
  // 更新月份天数和空白天数
  daysInMonth.value = getDaysInMonth(currentYear.value, currentMonth.value)
  emptyDays.value = calculateEmptyDays(currentYear.value, currentMonth.value)
  
  showToast(`${currentYear.value}年${currentMonth.value}月`, 'info')
}

// 显示提示
const showToast = (message: string, type: string) => {
  // 这里可以集成 Element Plus 的 Message 组件
  console.log(`${type}: ${message}`)
}



// 初始化图表
let studentTrendChart: ECharts | null = null;

const initCharts = () => {
  // 学生分数趋势图
  const studentTrendChartDom = document.getElementById('studentTrendChart')
  if (studentTrendChartDom) {
    studentTrendChart = echarts.init(studentTrendChartDom)
    updateTrendChart()

    // 响应式调整
    window.addEventListener('resize', () => {
      studentTrendChart.resize()
    })
  }

  // 初始化毽球运动雷达图
  initShuttlecockRadarChart()

  // 初始化旭日图
  initSunburstChart()

  // 初始化仪表盘
  initGaugeChart()
};

const updateTrendChart = () => {
  if (!studentTrendChart) return;
  
  const isLearningHours = activeTrendType.value === 'learningHours';
  const xAxisData = isLearningHours ? checkinChartData.value.dates : studentTrendData.value.weeks;
  const chartData = isLearningHours ? checkinChartData.value.durations : getTrendData();
  const trendLabel = getTrendLabel();
  const yRange = isLearningHours 
    ? { min: 0, max: Math.max(...checkinChartData.value.durations, 10) }
    : getYAxisRange();
  
  const studentTrendOption = {
    backgroundColor: 'transparent',
    grid: {
      left: '5%',
      right: '5%',
      top: '15%',
      bottom: '15%',
      containLabel: true,
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: '#eee',
      borderWidth: 1,
      padding: [12, 16],
      textStyle: { color: '#333' },
      extraCssText: 'box-shadow: 0 4px 20px rgba(0,0,0,0.08);border-radius:12px;',
      axisPointer: {
        type: 'line',
        lineStyle: { color: '#FF6B6B', type: 'dashed', width: 2 }
      }
    },
    xAxis: {
      type: 'category',
      data: xAxisData,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { 
        color: '#888',
        fontSize: 12,
        margin: 12
      }
    },
    yAxis: {
      type: 'value',
      min: yRange.min,
      max: yRange.max,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: {
        lineStyle: { color: '#f5f5f5', type: 'dashed' }
      },
      axisLabel: { 
        color: '#888',
        fontSize: 11,
        margin: 10
      }
    },
    series: [
      {
        name: trendLabel,
        type: 'line',
        smooth: 0.4,
        data: chartData,
        symbol: 'circle',
        symbolSize: 10,
        itemStyle: {
          color: '#FF6B6B',
          borderColor: '#fff',
          borderWidth: 3,
          shadowBlur: 10,
          shadowColor: 'rgba(255, 107, 107, 0.3)'
        },
        lineStyle: {
          color: '#FF6B6B',
          width: 4,
          shadowBlur: 8,
          shadowColor: 'rgba(255, 107, 107, 0.3)'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 107, 107, 0.25)' },
            { offset: 1, color: 'rgba(255, 107, 107, 0.02)' }
          ]),
        },
        emphasis: {
          scale: true,
          itemStyle: {
            symbolSize: 14
          }
        }
      },
    ],
  };

  studentTrendChart.setOption(studentTrendOption);
};

const handleTrendTypeChange = (type: 'learningHours' | 'homeworkScores') => {
  activeTrendType.value = type;
  updateTrendChart();
};

const initShuttlecockRadarChart = () => {
  const chartDom = document.getElementById('shuttlecock-radar-chart');
  if (chartDom) {
    const myChart = echarts.init(chartDom);
    
    const defaultIndicators = [
      { name: '盘踢', max: 100 },
      { name: '绷踢', max: 100 },
      { name: '拐踢', max: 100 },
      { name: '磕踢', max: 100 },
      { name: '踏踢', max: 100 },
      { name: '跳踢', max: 100 }
    ];
    
    const personalData = shuttlecockSkillsData.value.personal.length > 0 
      ? shuttlecockSkillsData.value.personal 
      : [0, 0, 0, 0, 0, 0];
    const classAvgData = shuttlecockSkillsData.value.classAvg.length > 0 
      ? shuttlecockSkillsData.value.classAvg 
      : [0, 0, 0, 0, 0, 0];
    const indicators = shuttlecockSkillsData.value.indicators.length > 0 
      ? shuttlecockSkillsData.value.indicators 
      : defaultIndicators;
    
    const option = {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(255,255,255,0.98)',
        borderColor: '#eee',
        borderWidth: 1,
        padding: [12, 16],
        textStyle: { color: '#333' },
        extraCssText: 'box-shadow: 0 4px 20px rgba(0,0,0,0.08);border-radius:12px;',
      },
      legend: {
        bottom: 0,
        itemWidth: 14,
        itemHeight: 14,
        textStyle: { fontSize: 12, color: '#666' },
        data: ['个人水平', '班级平均']
      },
      radar: {
        center: ['50%', '45%'],
        radius: '70%',
        indicator: indicators,
        axisName: {
          color: '#666',
          fontSize: 12,
          padding: [8, 12]
        },
        splitArea: {
          areaStyle: {
            color: ['rgba(255,107,107,0.02)', 'rgba(255,107,107,0.04)', 'rgba(255,107,107,0.06)', 'rgba(255,107,107,0.08)', 'rgba(255,107,107,0.1)']
          }
        },
        axisLine: {
          lineStyle: { color: '#f0f0f0' }
        },
        splitLine: {
          lineStyle: { color: '#f0f0f0', type: 'dashed' }
        }
      },
      series: [
        {
          name: '毽球技能',
          type: 'radar',
          data: [
            {
              value: personalData,
              name: '个人水平',
              lineStyle: {
                color: '#FF6B6B',
                width: 3
              },
              areaStyle: {
                color: 'rgba(255, 107, 107, 0.3)'
              },
              itemStyle: {
                color: '#FF6B6B'
              },
              symbol: 'circle',
              symbolSize: 8
            },
            {
              value: classAvgData,
              name: '班级平均',
              lineStyle: {
                color: '#4ECDC4',
                width: 3
              },
              areaStyle: {
                color: 'rgba(78, 205, 196, 0.2)'
              },
              itemStyle: {
                color: '#4ECDC4'
              },
              symbol: 'circle',
              symbolSize: 8
            }
          ]
        }
      ]
    };
    
    myChart.setOption(option);
    
    window.addEventListener('resize', () => {
      myChart.resize();
    });
  }
}

// 旭日图 - 动作分布
const sunburstColors = [
  '#FF6B6B',  // 珊瑚红
  '#4ECDC4',  // 青绿
  '#45B7D1',  // 天蓝
  '#96CEB4',  // 薄荷绿
  '#FFEAA7',  // 柠檬黄
  '#DDA0DD',  // 梅红
  '#98D8C8',  // 浅绿
  '#F7DC6F',  // 金黄
];
const selectedSunburstAction = ref<any>(null);
const averageScore = computed(() => {
  if (studentActionData.value.length === 0) return 0;
  const sum = studentActionData.value.reduce((acc, a) => acc + a.score, 0);
  return Math.round(sum / studentActionData.value.length);
});

let sunburstChart: ECharts | null = null;
const initSunburstChart = () => {
  const chartDom = document.getElementById('sunburst-chart');
  if (chartDom) {
    if (sunburstChart) {
      sunburstChart.dispose();
    }
    sunburstChart = echarts.init(chartDom);
    
    // 构建数据：默认显示所有动作的历史评分，点击后显示详细
    const sunburstData = {
      name: '毽球技能',
      children: studentActionData.value.map((action, index) => {
        const color = sunburstColors[index % sunburstColors.length];
        if (selectedSunburstAction.value?.name === action.name) {
          // 选中状态：显示详细评分
          return {
            name: action.name,
            value: action.score,
            itemStyle: { color: color },
            children: [
              { 
                name: '当前评分', 
                value: action.score,
                itemStyle: { color: color }
              },
              { 
                name: '历史平均', 
                value: action.historyScore,
                itemStyle: { color: '#e0e0e0' }
              },
            ]
          };
        } else {
          // 默认状态：只显示历史评分
          return {
            name: action.name,
            value: action.historyScore,
            itemStyle: { color: color },
          };
        }
      })
    };

    const option = {
      tooltip: {
        trigger: 'item',
        formatter: (params: any) => {
          if (params.treePathInfo && params.treePathInfo.length === 1) {
            // 外层 - 动作名称
            const idx = params.dataIndex;
            const action = studentActionData.value[idx];
            if (action) {
              return `<div style="padding:8px 12px">
                <div style="font-size:14px;font-weight:600;color:#333;margin-bottom:6px">${action.name}</div>
                <div style="color:#666;font-size:12px">历史评分: <span style="color:${sunburstColors[idx % sunburstColors.length]};font-weight:bold">${action.historyScore}分</span></div>
                ${selectedSunburstAction.value?.name !== action.name ? '<div style="color:#999;font-size:11px;margin-top:4px">点击查看详情</div>' : ''}
              </div>`;
            }
          }
          // 内层 - 当前评分/历史平均
          const parentIdx = params.treePathInfo?.[0]?.dataIndex ?? params.dataIndex;
          const parentAction = studentActionData.value[parentIdx];
          if (parentAction) {
            return `<div style="padding:8px 12px">
              <div style="font-size:12px;color:#666;margin-bottom:4px">${parentAction.name}</div>
              <div style="font-size:14px;font-weight:600">${params.name}: <span style="color:${params.name === '当前评分' ? sunburstColors[parentIdx % sunburstColors.length] : '#999'}">${params.value}分</span></div>
            </div>`;
          }
          return '';
        },
        backgroundColor: 'rgba(255,255,255,0.98)',
        borderColor: '#eee',
        borderWidth: 1,
        padding: [12, 16],
        textStyle: { color: '#333', fontSize: 13 },
        extraCssText: 'box-shadow: 0 8px 32px rgba(0,0,0,0.12);border-radius:12px;'
      },
      series: [{
        type: 'sunburst',
        data: sunburstData.children,
        radius: ['15%', '90%'],
        sort: undefined,
        emphasis: {
          focus: 'ancestor',
          itemStyle: {
            shadowBlur: 30,
            shadowColor: 'rgba(0,0,0,0.25)'
          }
        },
        label: {
          rotate: 'tangential',
          fontSize: 11,
          color: '#fff',
          fontWeight: '600',
          minAngle: 15,
          textShadowBlur: 3,
          textShadowColor: 'rgba(0,0,0,0.3)',
        },
        levels: [
          { r0: '15%', r: '40%', label: { fontSize: 12, fontWeight: 'bold', color: '#333', show: false } },
          { r0: '40%', r: '70%', label: { fontSize: 11, color: '#fff' } },
          { r0: '70%', r: '90%', label: { fontSize: 10, position: 'outside', color: '#666', show: !!selectedSunburstAction.value }, itemStyle: { borderWidth: 2, borderColor: '#fff' } }
        ],
        itemStyle: {
          borderRadius: 6,
          borderWidth: 3,
          borderColor: '#ffffff',
        },
        gapWidth: 3,
      }],
      animation: true,
      animationDuration: 1200,
      animationEasing: 'cubicOut',
    };

    sunburstChart.setOption(option);
    
    sunburstChart.on('click', (params: any) => {
      if (params.data) {
        const actionName = params.data.name;
        const action = studentActionData.value.find(a => a.name === actionName);
        if (action) {
          if (selectedSunburstAction.value?.name === action.name) {
            // 已选中，点击取消
            selectedSunburstAction.value = null;
          } else {
            // 选中该动作
            selectedSunburstAction.value = action;
          }
          initSunburstChart();
        }
      }
    });

    window.addEventListener('resize', () => sunburstChart?.resize());
  }
};

watch(selectedSunburstAction, () => {
  initSunburstChart();
});

watch(() => studentMetrics.value.homeworkScore, () => {
  initGaugeChart();
});

// 点击中心圆圈清除选中
const clearSunburstSelection = () => {
  selectedSunburstAction.value = null;
  initSunburstChart();
};

// 弧形仪表盘 - 总体掌握度
let gaugeChart: ECharts | null = null;
const initGaugeChart = () => {
  const chartDom = document.getElementById('gauge-chart');
  if (chartDom) {
    if (gaugeChart) {
      gaugeChart.dispose();
    }
    gaugeChart = echarts.init(chartDom);
    
    const score = studentMetrics.value.homeworkScore || 78;

    const option = {
      backgroundColor: 'transparent',
      series: [
        // 外圈装饰
        {
          type: 'gauge',
          startAngle: 200,
          endAngle: -20,
          radius: '110%',
          pointer: { show: false },
          progress: { show: false },
          axisLine: { lineStyle: { width: 0, color: [[1, '#f0f0f0']] } },
          splitLine: { show: false },
          axisTick: { show: false },
          axisLabel: { show: false },
          detail: { show: false },
          data: [{ value: 0 }]
        },
        // 主进度弧
        {
          type: 'gauge',
          startAngle: 200,
          endAngle: -20,
          radius: '85%',
          pointer: { show: false },
          progress: {
            show: true,
            width: 20,
            roundCap: true,
            itemStyle: {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 1, y2: 0,
                colorStops: [
                  { offset: 0, color: '#FF6B6B' },
                  { offset: 0.5, color: '#FFB347' },
                  { offset: 1, color: '#4ECDC4' }
                ]
              },
              shadowBlur: 20,
              shadowColor: 'rgba(255, 107, 107, 0.4)'
            }
          },
          axisLine: {
            lineStyle: { width: 20, color: [[1, '#f0f0f0']], roundCap: true }
          },
          axisTick: { show: false },
          splitLine: { show: false },
          axisLabel: { show: false },
          detail: { show: false },
          data: [{ value: score }]
        },
        // 内圈装饰
        {
          type: 'gauge',
          startAngle: 200,
          endAngle: -20,
          radius: '60%',
          pointer: { show: false },
          progress: { show: false },
          axisLine: { lineStyle: { width: 0, color: [[1, '#f0f0f0']] } },
          axisTick: { show: false },
          splitLine: { show: false },
          axisLabel: { show: false },
          detail: { show: false },
          data: [{ value: 0 }]
        },
        // 中心文字
        {
          type: 'pie',
          radius: ['0%', '50%'],
          startAngle: 0,
          endAngle: 360,
          silent: true,
          label: { show: false },
          labelLine: { show: false },
          data: [{
            value: 100,
            itemStyle: {
              color: 'transparent'
            }
          }]
        }
      ]
    };

    gaugeChart.setOption(option);
    window.addEventListener('resize', () => gaugeChart?.resize());
  }
}
</script>

<template>
  <div class="learning-analysis-view">
    <!-- ==================== 学生个人分析视图 ==================== -->
    <div class="analysis-view">
      <!-- 学生个人学情内容 -->
      <div class="student-analysis-content">
        <!-- 个人信息卡片 -->
        <div class="student-profile-card">
          <div class="student-avatar-large">{{ selectedStudent.avatar }}</div>
          <div class="student-detail">
            <h2 class="student-fullname">{{ selectedStudent.name }}</h2>
            <p class="student-basic-info">学号: {{ selectedStudent.code }} | 进阶级 | 总学情分 {{ selectedStudent.score }}</p>
            <div class="student-badges">
              <span class="badge success">本周进步 +5%</span>
              <span class="badge warning">外摆踢待提升</span>
              <span class="badge success">盘踢优秀</span>
            </div>
          </div>
        </div>

        <!-- 个人核心指标 -->
        <div class="metrics-grid">
          <div class="metric-card">
            <div class="metric-icon">⏰</div>
            <div class="metric-info">
              <span class="metric-label">学习时长</span>
              <span class="metric-value">{{ studentMetrics.learningHours }}</span>
              <span class="metric-unit">小时</span>
              <span class="metric-trend up">↑ 35小时</span>
            </div>
          </div>
          <div class="metric-card">
            <div class="metric-icon">📝</div>
            <div class="metric-info">
              <span class="metric-label">作业平均分</span>
              <span class="metric-value">{{ studentMetrics.homeworkScore }}</span>
              <span class="metric-trend up">↑ 3分</span>
            </div>
          </div>
          <div class="metric-card">
            <div class="metric-icon">✅</div>
            <div class="metric-info">
              <span class="metric-label">打卡率</span>
              <span class="metric-value">{{ Number(studentMetrics.checkinRate).toFixed(1) }}</span>
              <span class="metric-unit">%</span>
              <span class="metric-trend up">↑ 8%</span>
            </div>
          </div>
          <div class="metric-card">
            <div class="metric-icon">⭐</div>
            <div class="metric-info">
              <span class="metric-label">技能点</span>
              <span class="metric-value">{{ studentMetrics.skillPoints }}</span>
              <span class="metric-trend up">↑ 12点</span>
            </div>
          </div>
        </div>

        <!-- 个人趋势 -->
        <div class="charts-row">
          <div class="chart-card-large sunburst-card">
            <div class="chart-header">
              <h3>🔥 动作技能分布</h3>
              <div class="sunburst-legend">
                <span v-for="(action, idx) in studentActionData.slice(0, 4)" :key="action.name" class="legend-item">
                  <span class="legend-dot" :style="{ background: sunburstColors[idx] }"></span>
                  {{ action.name }}
                </span>
              </div>
            </div>
            <div class="sunburst-container">
              <div id="sunburst-chart" class="chart"></div>
              <div class="sunburst-center clickable" v-if="selectedSunburstAction" @click="clearSunburstSelection">
                <div class="center-score">{{ selectedSunburstAction.score }}</div>
                <div class="center-label">{{ selectedSunburstAction.name }}</div>
                <div class="center-sub">
                  <span class="score-type">当前</span> / {{ selectedSunburstAction.historyScore }} <span class="score-type">历史</span>
                </div>
                <div class="center-hint">点击返回</div>
              </div>
              <div class="sunburst-center" v-else-if="studentActionData.length">
                <div class="center-score">{{ averageScore }}</div>
                <div class="center-label">历史平均</div>
                <div class="center-sub">{{ studentActionData.length }}项技能</div>
              </div>
            </div>
          </div>
          <div class="chart-card-large gauge-card">
            <div class="chart-header">
              <h3>📊 学习掌握度</h3>
            </div>
            <div class="gauge-container">
              <div class="gauge-chart-wrapper">
                <div id="gauge-chart" class="chart"></div>
                <div class="gauge-center-text">
                  <div class="gauge-score">{{ studentMetrics.homeworkScore }}</div>
                  <div class="gauge-label-text">综合评分</div>
                </div>
              </div>
              <div class="gauge-details">
                <div class="gauge-item">
                  <span class="gauge-label">学习时长</span>
                  <span class="gauge-value secondary">{{ studentMetrics.learningHours }}h</span>
                </div>
                <div class="gauge-item">
                  <span class="gauge-label">打卡率</span>
                  <span class="gauge-value secondary">{{ Number(studentMetrics.checkinRate).toFixed(1) }}%</span>
                </div>
                <div class="gauge-item">
                  <span class="gauge-label">技能点</span>
                  <span class="gauge-value secondary">{{ studentMetrics.skillPoints }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 新增图表区域 -->
        <div class="charts-row">
          <div class="chart-card-large">
            <div class="chart-header">
              <h3>📈 近期学习趋势</h3>
              <div class="trend-toggle">
                <button 
                  v-for="type in trendTypes" 
                  :key="type.value"
                  :class="['toggle-btn', { active: activeTrendType === type.value }]"
                  @click="handleTrendTypeChange(type.value)"
                >
                  {{ type.label }}
                </button>
              </div>
            </div>
            <div class="chart-container">
              <div id="studentTrendChart" class="chart"></div>
            </div>
          </div>
          <!-- 个人数据雷达图 -->
          <div class="chart-card-large">
            <div class="chart-header">
              <h3>🎯 毽球运动技能评估</h3>
              <div class="chart-legend">
                <span><span class="legend-dot self"></span> 个人水平</span>
                <span><span class="legend-dot avg"></span> 班级平均</span>
              </div>
            </div>
            <div class="chart-container">
              <div id="shuttlecock-radar-chart" class="chart"></div>
            </div>
          </div>
        </div>

        <!-- 个人动作能力详细分析 -->
        <div class="action-detail-analysis">
          <div class="analysis-header">
            <div class="header-left">
              <h3>动作能力详细分析</h3>
              <p class="subtitle">点击动作卡片查看专业训练建议</p>
            </div>
            <button class="generate-plan-btn" @click="showToast('正在生成训练计划...', 'info')">
              <span class="btn-icon">+</span>
              生成训练计划
            </button>
          </div>
          
          <div class="action-analysis-container">
            <!-- 左侧：动作选择网格 -->
            <div class="action-grid-panel">
              <div class="action-grid">
                <div 
                  v-for="(action, index) in studentActionData" 
                  :key="action.name"
                  class="action-card"
                  :class="{ active: selectedAction && selectedAction.name === action.name }"
                >
                  <div class="action-card-main" @click="selectActionDetail(action)">
                    <div class="action-rank">{{ index + 1 }}</div>
                    <div class="action-info">
                      <span class="action-name">{{ action.name }}</span>
                      <span class="action-score">{{ action.score }}分</span>
                    </div>
                    <div class="action-indicator">
                      <div class="indicator-bar" :style="{ width: action.score + '%' }"></div>
                    </div>
                  </div>
                  <div 
                    class="action-history-toggle"
                    v-if="action.recentScores && action.recentScores.length > 0"
                    @click.stop="toggleActionHistory(action.name)"
                  >
                    <span class="history-label">历史记录 ({{ action.recentScores.length }})</span>
                    <span class="iconfont" :class="expandedAction === action.name ? 'icon-up' : 'icon-down'"></span>
                  </div>
                  <div class="action-history-list" v-if="expandedAction === action.name">
                    <div 
                      v-for="(score, idx) in action.recentScores" 
                      :key="idx"
                      class="history-item"
                      :class="{ active: selectedRecentScore && selectedRecentScore.actionName === action.name && selectedRecentScore.index === idx }"
                      @click.stop="selectRecentScore(action.name, score, idx)"
                    >
                      <span class="history-index">第{{ idx + 1 }}次</span>
                      <span class="history-score">{{ score }}分</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 右侧：动作建议面板 -->
            <div class="suggestion-panel" v-if="selectedAction">
              <div class="selected-action-header">
                <div class="action-badge">{{ selectedAction.name }}</div>
                <div class="action-total-score">
                  <span class="score-label">{{ getDisplayLabel(selectedAction) }}</span>
                  <span class="score-value">{{ getDisplayScore(selectedAction) }}</span>
                </div>
              </div>
              
              <div class="suggestion-grid">
                <div class="suggestion-card hip">
                  <div class="suggestion-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="12" r="10"/>
                      <path d="M12 6v6l4 2"/>
                    </svg>
                  </div>
                  <div class="suggestion-content">
                    <span class="suggestion-label">髋关节</span>
                    <p class="suggestion-text">{{ selectedAction.feedback[0] || '保持髋关节灵活，注意抬腿时髋部发力配合' }}</p>
                  </div>
                  <div class="suggestion-score">{{ getJointScore('hip') }}</div>
                </div>
                
                <div class="suggestion-card knee">
                  <div class="suggestion-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 2v20M2 12h20"/>
                      <circle cx="12" cy="12" r="4"/>
                    </svg>
                  </div>
                  <div class="suggestion-content">
                    <span class="suggestion-label">膝关节</span>
                    <p class="suggestion-text">{{ selectedAction.feedback[1] || '膝盖微屈缓冲落地冲击，保持关节稳定性' }}</p>
                  </div>
                  <div class="suggestion-score">{{ getJointScore('knee') }}</div>
                </div>
                
                <div class="suggestion-card ankle">
                  <div class="suggestion-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M4 20l8-8 8 8"/>
                      <path d="M8 16l4-4 4 4"/>
                    </svg>
                  </div>
                  <div class="suggestion-content">
                    <span class="suggestion-label">踝关节</span>
                    <p class="suggestion-text">{{ selectedAction.feedback[2] || '踝关节适度紧绷，踢球瞬间快速发力' }}</p>
                  </div>
                  <div class="suggestion-score">{{ getJointScore('ankle') }}</div>
                </div>
                
                <div class="suggestion-card height">
                  <div class="suggestion-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 2v22M7 7l5 5-5 5M17 7l-5 5 5 5"/>
                    </svg>
                  </div>
                  <div class="suggestion-content">
                    <span class="suggestion-label">抬脚高度</span>
                    <p class="suggestion-text">{{ selectedAction.feedback[3] || '提高抬腿高度，增强踢毽的稳定性和准确性' }}</p>
                  </div>
                  <div class="suggestion-score">{{ getJointScore('height') }}</div>
                </div>
              </div>
              
              <div class="action-tip">
                <span class="tip-icon">💡</span>
                <span class="tip-text">点击上方动作卡片切换查看不同动作的详细建议</span>
              </div>
            </div>
            
            <!-- 空状态 -->
            <div class="empty-state" v-else>
              <div class="empty-icon">🎯</div>
              <p class="empty-text">选择一个动作查看详细分析</p>
            </div>
          </div>
        </div>

        <!-- 个人打卡详情 -->
        <div class="personal-checkin-card">
          <div class="card-header">
            <h3>📅 个人打卡记录</h3>
            <span class="checkin-summary-text">最近30天打卡 <strong>{{ checkinStats.totalDays }}</strong> 天，连续打卡 <strong>{{ checkinStats.consecutiveDays }}</strong> 天</span>
          </div>
          <div class="checkin-calendar">
            <div class="calendar-header">
              <button class="month-nav prev personal-prev" @click="changeMonth('prev')">←</button>
              <h4 class="current-month">{{ currentYear }}年{{ currentMonth }}月</h4>
              <button class="month-nav next personal-next" @click="changeMonth('next')">→</button>
            </div>
            <div class="calendar-weekdays">
              <span>一</span><span>二</span><span>三</span><span>四</span><span>五</span><span>六</span><span>日</span>
            </div>
            <div class="calendar-days">
              <div 
                v-for="day in daysInMonth" 
                :key="day"
                class="calendar-day"
                :class="{ checked: isChecked(day) }"
                @click="toggleCheckin(day)"
              >
                {{ day }}
              </div>
              <div 
                v-for="emptyDay in emptyDays" 
                :key="'empty-' + emptyDay"
                class="calendar-day empty"
              ></div>
            </div>
          </div>
        </div>

        <!-- 动作组 -->
        <div class="analysis-card action-groups">
            <div class="card-header">
                <h3>🏃 动作组</h3>
            </div>

          <div class="suggestion-list">
            <div class="suggestion-item high-priority">
              <div class="suggestion-header">
                <span class="priority-badge">必做</span>
                <span class="suggestion-title">盘踢</span>
              </div>
              <p>今日计划：</p>
              <ul>
                <li>完成3组，每组20次</li>
                <li>保持动作标准，膝盖微屈</li>
                <li>注意节奏感和稳定性</li>
              </ul>
            </div>
            <div class="suggestion-item medium-priority">
              <div class="suggestion-header">
                <span class="priority-badge">必做</span>
                <span class="suggestion-title">磕踢</span>
              </div>
              <p>今日计划：</p>
              <ul>
                <li>完成3组，每组15次</li>
                <li>配合节拍器练习，保持节奏稳定</li>
                <li>注意膝盖高度和踢毽部位</li>
              </ul>
            </div>
            <div class="suggestion-item low-priority">
              <div class="suggestion-header">
                <span class="priority-badge">选做</span>
                <span class="suggestion-title">外摆踢</span>
              </div>
              <p>今日计划：</p>
              <ul>
                <li>完成2组，每组10次</li>
                <li>侧腰充分拉伸，保持身体平衡</li>
                <li>循序渐进，注重动作质量</li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 打卡选项悬浮窗 -->
        <div v-if="showCheckinOptionsModal" class="timer-modal">
          <div class="timer-modal-content">
            <div class="timer-modal-header">
              <h3>选择打卡方式</h3>
              <button class="close-btn" @click="cancelCheckinOptions">&times;</button>
            </div>
            <div class="timer-modal-body">
              <div class="checkin-options">
                <button class="option-btn ai-classroom-btn" @click="goToAIClassroom">去学习AI课堂</button>
                <button class="option-btn ai-training-btn" @click="goToAITraining">去完成AI训练计划</button>
              </div>
              <div class="option-info">
                <p>选择一种方式完成今日打卡</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 打卡计时悬浮窗 -->
        <div v-if="showTimerModal" class="timer-modal">
          <div class="timer-modal-content">
            <div class="timer-modal-header">
              <h3>打卡计时</h3>
              <button class="close-btn" @click="cancelCheckin">&times;</button>
            </div>
            <div class="timer-modal-body">
              <div class="timer-display">{{ formatTime(timerSeconds) }}</div>
              <div class="timer-target">目标时长：{{ formatTime(requiredDuration) }}</div>
              <div class="timer-progress">
                <div class="progress-bar" :style="{ width: Math.min((timerSeconds / requiredDuration) * 100, 100) + '%' }"></div>
              </div>
              <div class="timer-controls">
                <button v-if="!isTimerRunning" class="timer-btn start-btn" @click="startTimer">开始</button>
                <button v-else class="timer-btn pause-btn" @click="pauseTimer">暂停</button>
                <button class="timer-btn stop-btn" @click="stopTimer">完成打卡</button>
                <button class="timer-btn cancel-btn" @click="cancelCheckin">取消</button>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
/* ===== 全局变量 ===== */
:root {
  /* 浅色模式 */
  --bg-primary: #f5f9fe;
  --bg-secondary: #ffffff;
  --bg-card: #ffffff;
  --bg-sidebar: #ffffff;
  --text-primary: #1e293b;
  --text-secondary: #64748b;
  --text-light: #94a3b8;
  --border-color: #e2e8f0;
  --primary-blue: #0a66c2;
  --primary-light: #e6f0fa;
  --shadow-color: rgba(0, 0, 0, 0.05);
  --hover-bg: #f1f5f9;
  --success: #10b981;
  --warning: #f59e0b;
  --danger: #ef4444;
  --dropdown-bg: #ffffff;
  --dropdown-shadow: 0 10px 25px rgba(0,0,0,0.1);
}

/* 深色模式 */
body.dark-mode {
  --bg-primary: #0f172a;
  --bg-secondary: #1e293b;
  --bg-card: #1e293b;
  --bg-sidebar: #1e293b;
  --text-primary: #f1f5f9;
  --text-secondary: #cbd5e1;
  --text-light: #94a3b8;
  --border-color: #334155;
  --primary-blue: #38bdf8;
  --primary-light: #2d3a5e;
  --shadow-color: rgba(0, 0, 0, 0.3);
  --hover-bg: #2d3a5e;
  --dropdown-bg: #1e293b;
  --dropdown-shadow: 0 10px 25px rgba(0,0,0,0.3);
}

/* ===== 基础样式 ===== */
.learning-analysis-view {
  min-height: 100%;
  background-color: #f5f9fe;
  padding: 20px;
  box-sizing: border-box;
}

.analysis-view {
  max-width: 1200px;
  margin: 0 auto;
}

/* ===== 个人信息卡片 ===== */
.student-profile-card {
  display: flex;
  align-items: center;
  background-color: #ffffff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
}

.student-avatar-large {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background-color: #1890ff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: bold;
  margin-right: 20px;
}

.student-detail {
  flex: 1;
}

.student-fullname {
  font-size: 24px;
  font-weight: bold;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.student-basic-info {
  color: #64748b;
  margin: 0 0 12px 0;
  font-size: 14px;
}

.student-badges {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.badge {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
}

.badge.success {
  background-color: rgba(82, 196, 26, 0.1);
  color: #52c41a;
  border: 1px solid #52c41a;
}

.badge.warning {
  background-color: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
  border: 1px solid #f59e0b;
}

/* ===== 核心指标 ===== */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.metric-card {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  gap: 16px;
}

.metric-icon {
  font-size: 32px;
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.metric-info {
  flex: 1;
}

.metric-label {
  display: block;
  color: #000000;
  font-size: 14px;
  margin-bottom: 4px;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #1e293b;
}

.metric-unit {
  font-size: 16px;
  color: #64748b;
  margin-left: 4px;
}

.metric-trend {
  font-size: 12px;
  font-weight: 500;
  margin-left: 8px;
}

.metric-trend.up {
  color: #52c41a;
}

.metric-trend.down {
  color: #ff4d4f;
}

/* ===== 图表区域 ===== */
.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.chart-card-large {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
}

.sunburst-card {
  position: relative;
  overflow: hidden;
  
  .sunburst-legend {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    
    .legend-item {
      display: flex;
      align-items: center;
      gap: 5px;
      font-size: 12px;
      color: #555;
      background: #f8f9fa;
      padding: 4px 10px;
      border-radius: 20px;
      transition: all 0.2s;
      
      &:hover {
        background: #eee;
      }
      
      .legend-dot {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        box-shadow: 0 2px 4px rgba(0,0,0,0.15);
      }
    }
  }
}

.sunburst-container {
  position: relative;
  height: 340px;
  
  .chart {
    width: 100%;
    height: 100%;
  }
  
  .sunburst-center {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    text-align: center;
    pointer-events: none;
    background: linear-gradient(145deg, #ffffff 0%, #f8f9fa 100%);
    border-radius: 50%;
    width: 130px;
    height: 130px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    box-shadow: 
      0 8px 32px rgba(0, 0, 0, 0.08),
      inset 0 -2px 8px rgba(0, 0, 0, 0.02);
    border: 3px solid #fff;
    
    .center-score {
      font-size: 36px;
      font-weight: 800;
      background: linear-gradient(135deg, #FF6B6B 0%, #4ECDC4 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      line-height: 1.1;
      letter-spacing: -1px;
    }
    
    .center-label {
      font-size: 14px;
      font-weight: 600;
      color: #333;
      margin-top: 4px;
    }
    
    .center-sub {
      font-size: 11px;
      color: #888;
      margin-top: 4px;
      
      .score-type {
        font-size: 10px;
        color: #aaa;
      }
    }
    
    .center-hint {
      font-size: 10px;
      color: #bbb;
      margin-top: 6px;
      padding: 2px 8px;
      background: #f5f5f5;
      border-radius: 10px;
    }
  }
  
  .sunburst-center.clickable {
    cursor: pointer;
    transition: transform 0.2s;
    
    &:hover {
      transform: translate(-50%, -50%) scale(1.05);
    }
  }
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-header h3 {
  font-size: 16px;
  font-weight: bold;
  color: #1e293b;
  margin: 0;
}

.chart-legend {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #64748b;
}

.trend-toggle {
  display: flex;
  gap: 8px;
}

.toggle-btn {
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  background-color: #ffffff;
  color: #64748b;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background-color: #f1f5f9;
    color: #1e293b;
  }

  &.active {
    background-color: #1890ff;
    color: #ffffff;
    border-color: #1890ff;
  }
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.legend-dot.class {
  background-color: #1890ff;
}

.legend-dot.homework {
  background-color: #52c41a;
}

.legend-dot.self {
  background-color: #409EFF;
}

.legend-dot.avg {
  background-color: #F56C6C;
}

.chart-container {
  height: 300px;
  position: relative;
}

.chart {
  width: 100%;
  height: 100%;
}

.gauge-card {
  .gauge-container {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 300px;
    gap: 24px;
    
    .gauge-chart-wrapper {
      position: relative;
      flex: 1;
      height: 100%;
      
      .chart {
        width: 100%;
        height: 100%;
      }
      
      .gauge-center-text {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        text-align: center;
        pointer-events: none;
        
        .gauge-score {
          font-size: 48px;
          font-weight: 800;
          background: linear-gradient(135deg, #FF6B6B 0%, #FFB347 50%, #4ECDC4 100%);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
          line-height: 1;
          letter-spacing: -2px;
        }
        
        .gauge-label-text {
          font-size: 13px;
          color: #999;
          margin-top: 6px;
          font-weight: 500;
        }
      }
    }
    
    .gauge-details {
      display: flex;
      flex-direction: column;
      gap: 24px;
      padding: 28px 24px;
      background: linear-gradient(145deg, #ffffff 0%, #f8f9fa 100%);
      border-radius: 20px;
      min-width: 180px;
      box-shadow: 
        0 8px 32px rgba(0, 0, 0, 0.06),
        inset 0 1px 0 rgba(255, 255, 255, 0.8);
      
      .gauge-item {
        display: flex;
        flex-direction: column;
        gap: 6px;
        padding-bottom: 16px;
        border-bottom: 1px solid #f0f0f0;
        
        &:last-child {
          border-bottom: none;
          padding-bottom: 0;
        }
        
        .gauge-label {
          font-size: 12px;
          color: #888;
          font-weight: 500;
          text-transform: uppercase;
          letter-spacing: 0.5px;
        }
        
        .gauge-value {
          font-size: 22px;
          font-weight: 700;
          background: linear-gradient(135deg, #FF6B6B 0%, #FFB347 50%, #4ECDC4 100%);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
          
          &.secondary {
            font-size: 18px;
          }
        }
      }
    }
  }
}

/* ===== 动作能力详细分析 - 新设计 ===== */
.action-detail-analysis {
  background-color: #ffffff;
  border-radius: 16px;
  padding: 28px;
  margin-bottom: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border: 1px solid #e2e8f0;
}

.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  
  .header-left {
    h3 {
      font-size: 18px;
      font-weight: 600;
      color: #1e293b;
      margin: 0 0 6px 0;
      letter-spacing: 0.5px;
    }
    
    .subtitle {
      font-size: 13px;
      color: #94a3b8;
      margin: 0;
    }
  }
  
  .generate-plan-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 18px;
    background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);
    color: white;
    border: none;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
    
    .btn-icon {
      font-size: 16px;
      font-weight: bold;
    }
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgba(24, 144, 255, 0.4);
    }
  }
}

.action-analysis-container {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 28px;
  min-height: 360px;
}

.action-grid-panel {
  .action-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 14px;
  }
}

.action-card {
  position: relative;
  background: linear-gradient(145deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1.5px solid #e2e8f0;
  border-radius: 14px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  
  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(24, 144, 255, 0.12);
    border-color: #93c5fd;
  }
  
  &.active {
    background: linear-gradient(145deg, #eff6ff 0%, #dbeafe 100%);
    border-color: #3b82f6;
    box-shadow: 0 4px 16px rgba(59, 130, 246, 0.2);
    
    .action-rank {
      background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
      color: white;
    }
    
    .indicator-bar {
      background: linear-gradient(90deg, #3b82f6 0%, #60a5fa 100%);
    }
  }
  
  .action-card-main {
    padding: 16px;
    cursor: pointer;
  }
  
  .action-rank {
    position: absolute;
    top: 10px;
    left: 12px;
    width: 22px;
    height: 22px;
    background: linear-gradient(135deg, #cbd5e1 0%, #94a3b8 100%);
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 11px;
    font-weight: 700;
    color: white;
  }
  
  .action-info {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding-top: 8px;
    gap: 4px;
    
    .action-name {
      font-size: 15px;
      font-weight: 600;
      color: #1e293b;
    }
    
    .action-score {
      font-size: 20px;
      font-weight: 700;
      background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  }
  
  .action-indicator {
    margin-top: 12px;
    height: 4px;
    background-color: #e2e8f0;
    border-radius: 2px;
    overflow: hidden;
    
    .indicator-bar {
      height: 100%;
      background: linear-gradient(90deg, #94a3b8 0%, #cbd5e1 100%);
      border-radius: 2px;
      transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
    }
  }
  
  .action-history-toggle {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 16px;
    background: rgba(24, 144, 255, 0.08);
    cursor: pointer;
    font-size: 12px;
    color: #1890ff;
    border-top: 1px solid rgba(24, 144, 255, 0.1);
    transition: all 0.2s ease;
    
    &:hover {
      background: rgba(24, 144, 255, 0.15);
    }
    
    .history-label {
      font-weight: 500;
    }
    
    .iconfont {
      font-size: 12px;
      transition: transform 0.2s ease;
    }
  }
  
  .action-history-list {
    padding: 8px;
    background: #ffffff;
    border-top: 1px solid #e2e8f0;
    
    .history-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 12px;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s ease;
      margin-bottom: 4px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      &:hover {
        background: #f0f7ff;
      }
      
      &.active {
        background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
        border: 1px solid #93c5fd;
        
        .history-score {
          color: #1890ff;
          font-weight: 600;
        }
      }
      
      .history-index {
        font-size: 12px;
        color: #64748b;
      }
      
      .history-score {
        font-size: 13px;
        font-weight: 600;
        color: #1e293b;
      }
    }
  }
}

.suggestion-panel {
  background: linear-gradient(165deg, #fafbfc 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 24px;
  
  .selected-action-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid #e2e8f0;
    
    .action-badge {
      font-size: 16px;
      font-weight: 600;
      color: #1e293b;
      background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
      padding: 8px 20px;
      border-radius: 20px;
      border: 1px solid #bfdbfe;
    }
    
    .action-total-score {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      
      .score-label {
        font-size: 11px;
        color: #94a3b8;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }
      
      .score-value {
        font-size: 28px;
        font-weight: 700;
        background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }
    }
  }
}

.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.suggestion-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 4px;
    height: 100%;
  }
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  }
  
  &.hip::before { background: linear-gradient(180deg, #f472b6 0%, #ec4899 100%); }
  &.knee::before { background: linear-gradient(180deg, #60a5fa 0%, #3b82f6 100%); }
  &.ankle::before { background: linear-gradient(180deg, #34d399 0%, #10b981 100%); }
  &.height::before { background: linear-gradient(180deg, #fbbf24 0%, #f59e0b 100%); }
  
  .suggestion-icon {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    
    svg {
      width: 20px;
      height: 20px;
    }
  }
  
  &.hip .suggestion-icon {
    background: linear-gradient(135deg, #fdf2f8 0%, #fce7f3 100%);
    color: #ec4899;
  }
  &.knee .suggestion-icon {
    background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
    color: #3b82f6;
  }
  &.ankle .suggestion-icon {
    background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
    color: #10b981;
  }
  &.height .suggestion-icon {
    background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
    color: #f59e0b;
  }
  
  .suggestion-content {
    flex: 1;
    min-width: 0;
    
    .suggestion-label {
      font-size: 12px;
      font-weight: 600;
      color: #64748b;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      display: block;
      margin-bottom: 4px;
    }
    
    .suggestion-text {
      font-size: 13px;
      color: #475569;
      line-height: 1.5;
      margin: 0;
    }
  }
  
  .suggestion-score {
    font-size: 18px;
    font-weight: 700;
    color: #1e293b;
    flex-shrink: 0;
  }
}

.action-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 20px;
  padding: 12px;
  background: linear-gradient(135deg, #fefce8 0%, #fef9c3 100%);
  border-radius: 10px;
  border: 1px solid #fef08a;
  
  .tip-icon {
    font-size: 16px;
  }
  
  .tip-text {
    font-size: 12px;
    color: #854d0e;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 300px;
  background: linear-gradient(165deg, #fafbfc 0%, #f8fafc 100%);
  border: 2px dashed #e2e8f0;
  border-radius: 16px;
  
  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
    opacity: 0.6;
  }
  
  .empty-text {
    font-size: 15px;
    color: #94a3b8;
    margin: 0;
  }
}

/* ===== 个人打卡记录 ===== */
.personal-checkin-card {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h3 {
  font-size: 16px;
  font-weight: bold;
  color: #1e293b;
  margin: 0;
}

.checkin-summary-text {
  font-size: 14px;
  color: #64748b;
}

.checkin-calendar {
  width: 100%;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.current-month {
  font-size: 14px;
  font-weight: bold;
  color: #1e293b;
  margin: 0;
}

.month-nav {
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  color: #64748b;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.month-nav:hover {
  background-color: #f1f5f9;
  color: #1e293b;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.calendar-weekdays span {
  text-align: center;
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10px;
}

.calendar-day {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #64748b;
  border-radius: 8px;
  transition: all 0.2s ease;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  background-color: #ffffff;
}

.calendar-day:hover {
  background-color: #f8fafc;
  transform: scale(1.05);
}

.calendar-day.checked {
  background-color: #dcfce7;
  color: #166534;
  font-weight: bold;
  border-color: #86efac;
  box-shadow: 0 2px 4px rgba(34, 197, 94, 0.2);
  animation: checkinPulse 0.3s ease-in-out;
}

.calendar-day.empty {
  border: none;
  background: transparent;
  cursor: default;
}

.calendar-day.empty:hover {
  transform: none;
  background: transparent;
}

@keyframes checkinPulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
  }
}

/* ===== 动作组 ===== */
.action-groups {
  margin-bottom: 24px;
}

.btn-toStudy{
    display: flex;
    width: 90px;
    height: 30px;
    padding: 5px;
    font-size: 16px;
    background-color: #1890ff;

}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.suggestion-item {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  background-color: #ffffff;
  border-left: 4px solid;
}

.suggestion-item.high-priority {
  border-left-color: #ff4d4f;
}

.suggestion-item.medium-priority {
  border-left-color: #faad14;
}

.suggestion-item.low-priority {
  border-left-color: #52c41a;
}

.suggestion-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.priority-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.suggestion-item.high-priority .priority-badge {
  background-color: rgba(255, 77, 79, 0.1);
  color: #ff4d4f;
}

.suggestion-item.medium-priority .priority-badge {
  background-color: rgba(250, 173, 20, 0.1);
  color: #faad14;
}

.suggestion-item.low-priority .priority-badge {
  background-color: rgba(82, 196, 26, 0.1);
  color: #52c41a;
}

.suggestion-title {
  font-weight: bold;
  color: #1e293b;
  font-size: 14px;
}

.suggestion-item p {
  font-size: 14px;
  color: #1e293b;
  margin: 0 0 12px 0;
}

.suggestion-item ul {
  margin: 0;
  padding-left: 20px;
  font-size: 14px;
  color: #1e293b;
}

.suggestion-item li {
  margin-bottom: 4px;
}

/* ===== 导出报告按钮 ===== */
.export-section {
  display: flex;
  gap: 16px;
  margin-bottom: 40px;
}

.btn-export,
.btn-share {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.btn-export {
  background-color: #1890ff;
  color: white;
}

.btn-export:hover {
  background-color: #40a9ff;
}

.btn-share {
  background-color: #ffffff;
  color: #1e293b;
  border: 1px solid #e2e8f0;
}

.btn-share:hover {
  background-color: var(--hover-bg);
}

.export-icon,
.toStudy-icon,
.share-icon {
  font-size: 16px;
}

/* ===== 响应式设计 ===== */
@media (max-width: 768px) {
  .learning-analysis-view {
    padding: 12px;
  }
  
  .metrics-grid {
    grid-template-columns: 1fr;
  }
  
  .charts-row {
    grid-template-columns: 1fr;
  }
  
  .action-analysis-container {
    grid-template-columns: 1fr;
  }
  
  .action-grid-panel .action-grid {
    grid-template-columns: repeat(4, 1fr);
  }
  
  .suggestion-grid {
    grid-template-columns: 1fr;
  }
  
  .student-profile-card {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }
  
  .student-avatar-large {
    margin-right: 0;
  }
  
  .export-section {
    flex-direction: column;
  }
  
  .btn-export,
  .btn-share {
    justify-content: center;
  }
  
  .calendar-day {
    font-size: 12px;
  }
  
  .calendar-days {
    gap: 6px;
  }
  
  .calendar-weekdays span {
    font-size: 10px;
  }
  
  .current-month {
    font-size: 12px;
  }
  
  .month-nav {
    font-size: 14px;
    padding: 2px 6px;
  }
}

@media (max-width: 480px) {
  .calendar-day {
    font-size: 10px;
  }
  
  .calendar-days {
    gap: 4px;
  }
  
  .checkin-summary-text {
    font-size: 12px;
  }
  
  .card-header h3 {
    font-size: 14px;
  }
}

/* ===== 打卡计时悬浮窗 ===== */
.timer-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.timer-modal-content {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  width: 90%;
  max-width: 400px;
  overflow: hidden;
}

.timer-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
  background-color: #f8fafc;
  
  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: bold;
    color: #1e293b;
  }
  
  .close-btn {
    background: none;
    border: none;
    font-size: 24px;
    cursor: pointer;
    color: #64748b;
    padding: 0;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 4px;
    transition: all 0.2s ease;
    
    &:hover {
      background-color: #f1f5f9;
      color: #1e293b;
    }
  }
}

.timer-modal-body {
    padding: 32px 20px;
    
    .timer-display {
      font-size: 48px;
      font-weight: bold;
      text-align: center;
      margin-bottom: 16px;
      color: #1890ff;
      font-family: monospace;
    }
    
    .timer-target {
      text-align: center;
      font-size: 14px;
      color: #64748b;
      margin-bottom: 16px;
    }
    
    .timer-progress {
      width: 100%;
      height: 8px;
      background-color: #f0f0f0;
      border-radius: 4px;
      overflow: hidden;
      margin-bottom: 32px;
      
      .progress-bar {
        height: 100%;
        background-color: #1890ff;
        border-radius: 4px;
        transition: width 0.3s ease;
      }
    }
    
    .timer-controls {
      display: flex;
      gap: 12px;
      justify-content: center;
      
      .timer-btn {
        padding: 10px 20px;
        border: none;
        border-radius: 8px;
        font-size: 14px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s ease;
        
        &.start-btn {
          background-color: #52c41a;
          color: #ffffff;
          
          &:hover {
            background-color: #73d13d;
          }
        }
        
        &.pause-btn {
          background-color: #faad14;
          color: #ffffff;
          
          &:hover {
            background-color: #ffc53d;
          }
        }
        
        &.stop-btn {
          background-color: #1890ff;
          color: #ffffff;
          
          &:hover {
            background-color: #40a9ff;
          }
          
          &.disabled {
            background-color: #d9d9d9;
            color: #bfbfbf;
            cursor: not-allowed;
            
            &:hover {
              background-color: #d9d9d9;
            }
          }
        }
        
        &.cancel-btn {
          background-color: #f5f5f5;
          color: #64748b;
          
          &:hover {
            background-color: #e2e8f0;
            color: #1e293b;
          }
        }
      }
    }
    
    .checkin-options {
      display: flex;
      flex-direction: column;
      gap: 16px;
      margin-bottom: 24px;
      
      .option-btn {
        padding: 16px 20px;
        border: none;
        border-radius: 8px;
        font-size: 16px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s ease;
        text-align: center;
        
        &.ai-classroom-btn {
          background-color: #1890ff;
          color: #ffffff;
          
          &:hover {
            background-color: #40a9ff;
          }
        }
        
        &.ai-training-btn {
          background-color: #52c41a;
          color: #ffffff;
          
          &:hover {
            background-color: #73d13d;
          }
        }
      }
    }
    
    .option-info {
      text-align: center;
      
      p {
        font-size: 14px;
        color: #64748b;
        margin: 0;
      }
    }
  }
</style>
import { ref, onMounted } from 'vue';

export function useHomeData() {
  const loading = ref(false);
  const teacherInfo = ref({
    name: '王老师',
    level: '高级教练 · LV5',
    totalStudents: 145,
    avgFitnessScore: 88.5,           // 平均体能分
    fitnessPassRate: 98,             // 体测达标率
    avgSpecialtyScore: 85,           // 专项得分
    highRiskCount: 2                 // 运动风险（疲劳/受伤）
  });

  const notifications = ref([
    { time: '09:30', content: '高一(1)班：李明颠球动作轨迹分析已出，右侧膝盖易损警报', type: 'danger' },
    { time: '10:15', content: '作业《左右脚交替盘踢100次》批改进度达到 90%', type: 'success' },
    { time: '14:20', content: '系统识别到3名学生下肢过度疲劳，已自动调整为拉伸恢复课', type: 'warning' },
    { time: '16:00', content: '校队省赛特训名单（花式动作组）已更新，请审核', type: 'info' }
  ]);

  const todaySchedule = ref([
    { time: '08:00 - 09:30', class: '高一(1)班', content: '盘踢与外摆基础教学', room: '二楼室内馆A区' },
    { time: '10:00 - 11:30', class: '高一(2)班', content: '体能储备与步法移动', room: '室外操场塑胶跑道' },
    { time: '14:00 - 15:30', class: '校队', content: '花式连击组合与防守站位', room: '一楼战术室/器械区' }
  ]);

  // 折线图配置 (近4周体能与专项成绩趋势)
  const lineChartOption = ref({
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255,255,255,0.9)', textStyle: { color: '#334155' }, borderColor: '#e2e8f0' },
    legend: { data: ['平均体能水平', '毽球专项成绩'], textStyle: { color: '#64748b' }, right: 10, top: 10 },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: ['第1周', '第2周', '第3周', '第4周'], axisLabel: { color: '#64748b' }, axisLine: { lineStyle: { color: '#cbd5e1' } } },
    yAxis: { type: 'value', min: 75, max: 100, axisLabel: { color: '#64748b' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    series: [
      { name: '平均体能水平', type: 'line', smooth: true, data: [82, 84, 86, 88.5], itemStyle: { color: '#10b981' } },
      { name: '毽球专项成绩', type: 'line', smooth: true, data: [78, 80, 83, 85], itemStyle: { color: '#3b82f6' }, areaStyle: { color: 'rgba(59, 130, 246, 0.1)' } }
    ]
  });

  // 雷达图配置 (毽球专项指标)
  const radarChartOption = ref({
    tooltip: { backgroundColor: 'rgba(255,255,255,0.9)', textStyle: { color: '#334155' }, borderColor: '#e2e8f0' },
    legend: { data: ['本班平均', '年级平均'], bottom: 0, textStyle: { color: '#64748b' } },
    radar: {
      indicator: [
        { name: '颠球稳定性', max: 100 },
        { name: '花式动作熟练度', max: 100 },
        { name: '左右脚协调性', max: 100 },
        { name: '移动步法', max: 100 },
        { name: '击球力量控制', max: 100 },
        { name: '接球成功率', max: 100 }
      ],
      axisName: { color: '#475569' },
      splitArea: { areaStyle: { color: ['#f8fafc', '#ffffff'] } },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
      axisLine: { lineStyle: { color: '#e2e8f0' } }
    },
    series: [
      {
        name: '能力对比',
        type: 'radar',
        data: [
          { value: [92, 78, 85, 88, 82, 90], name: '本班平均', itemStyle: { color: '#3b82f6' }, areaStyle: { color: 'rgba(59, 130, 246, 0.2)' } },
          { value: [85, 80, 82, 85, 80, 85], name: '年级平均', itemStyle: { color: '#94a3b8' }, lineStyle: { type: 'dashed' } }
        ]
      }
    ]
  });

  const fetchHomeData = async () => {
    loading.value = true;
    await new Promise(resolve => setTimeout(resolve, 500));
    loading.value = false;
  };

  onMounted(() => {
    fetchHomeData();
  });

  return {
    loading,
    teacherInfo,
    notifications,
    todaySchedule,
    lineChartOption,
    radarChartOption
  };
}

import { ref, onMounted } from 'vue';

export interface StudentAnalysis {
  id: string;
  name: string;
  score: number;
  level: '优' | '良' | '中' | '差';
  strength: string;
  weakness: string;
  improvement: string;
}

export function useAnalysis() {
  const loading = ref(false);
  const classSummary = ref({
    averageScore: 88.5,
    topScore: 98,
    passRate: 100,
    activeParticipation: 95
  });

  const students = ref<StudentAnalysis[]>([
    { id: '1', name: '张旭', score: 92, level: '优', strength: '爆发力强，发球角度刁钻', weakness: '连续对跳耐力稍弱', improvement: '建议加强体能储备训练' },
    { id: '2', name: '李华', score: 85, level: '良', strength: '接球稳健，预判准确', weakness: '由于步伐原因导致进攻机会流失', improvement: '建议侧重交叉步与并步切换' },
    { id: '3', name: '周晓', score: 78, level: '中', strength: '协调性好', weakness: '核心力量薄弱，扣球不稳定', improvement: '建议每日进行平板支撑和腰腹训练' },
  ]);

  const fetchAnalysis = async () => {
    loading.value = true;
    await new Promise(resolve => setTimeout(resolve, 800));
    loading.value = false;
  };

  onMounted(() => {
    fetchAnalysis();
  });

  return {
    loading,
    classSummary,
    students
  };
}

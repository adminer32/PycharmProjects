import { ref } from 'vue';

export interface StudentProgress {
  id: string;
  name: string;
  condition: string;
  aiSuggestedCourse: string;
  practicePlan: string;
}

export function useLearningManagement() {
  const isGenerating = ref(false);
  const students = ref<StudentProgress[]>([
    { id: '1', name: '张旭', condition: '发球力量不足，落点漂浮', aiSuggestedCourse: '发球力量强化与落点控制', practicePlan: '每日100次深蹲 + 50次对墙发球' },
    { id: '2', name: '李华', condition: '接球反应较慢，移动步伐乱', aiSuggestedCourse: '接发球反应时与步伐专项', practicePlan: '30米往返跑 + 交叉步练习' },
    { id: '3', name: '周晓', condition: '进攻缺乏爆发力，扣杀角度单调', aiSuggestedCourse: '核心力量爆发与扣杀角度', practicePlan: '仰卧起坐 + 跳起大腿触胸' },
  ]);

  const generateCourse = async (studentId: string) => {
    isGenerating.value = true;
    // 模拟 AI 生成过程
    await new Promise(resolve => setTimeout(resolve, 1500));
    const student = students.value.find(s => s.id === studentId);
    if (student) {
      student.aiSuggestedCourse = "AI 重新生成的定制课程：" + student.condition.split('，')[0] + "进阶课";
    }
    isGenerating.value = false;
  };

  return {
    isGenerating,
    students,
    generateCourse
  };
}

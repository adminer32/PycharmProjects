import { ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';

export interface Homework {
  id: string;
  title: string;
  targetClass: string;
  deadline: string;
  submissionCount: number;
  totalCount: number;
  status: 'published' | 'draft' | 'closed';
}

export function useHomework() {
  const homeworkList = ref<Homework[]>([
    { id: '1', title: '每日稳球对墙练习 5分钟视频', targetClass: '高一(1)班', deadline: '2026-03-25', submissionCount: 45, totalCount: 50, status: 'published' },
    { id: '2', title: '脚内侧踢球动作自查', targetClass: '高一(2)班', deadline: '2026-03-26', submissionCount: 12, totalCount: 48, status: 'published' },
    { id: '3', title: '体能加练: 波比跳 30个', targetClass: '校队', deadline: '2026-03-24', submissionCount: 30, totalCount: 30, status: 'closed' },
  ]);

  const publishHomework = async (form: any) => {
    // 模拟发布作业逻辑
    const newHw: Homework = {
      id: Date.now().toString(),
      title: form.title,
      targetClass: form.targetClass,
      deadline: form.deadline,
      submissionCount: 0,
      totalCount: 50,
      status: 'published'
    };
    homeworkList.value.unshift(newHw);
    ElMessage.success('作业发布成功！');
  };

  const deleteHomework = (id: string) => {
    ElMessageBox.confirm('确定要撤回该作业吗？学生将无法再提交。', '警告', {
      type: 'warning'
    }).then(() => {
      homeworkList.value = homeworkList.value.filter(h => h.id !== id);
      ElMessage.success('作业已成功撤回');
    });
  };

  return {
    homeworkList,
    publishHomework,
    deleteHomework
  };
}

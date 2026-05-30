import { defineStore } from 'pinia';
import { ref } from 'vue';
import { getClassList, type ClassListVO } from '@/api/classApi';

export const useClassStore = defineStore('class', () => {
  const classList = ref<ClassListVO[]>([]);
  const currentClass = ref<ClassListVO | null>(null);
  const loading = ref(false);

  async function fetchClassList() {
    loading.value = true;
    try {
      const res = await getClassList();
      if (res.data) {
        classList.value = res.data;
      }
    } catch (e) {
      console.error('获取班级列表失败', e);
    } finally {
      loading.value = false;
    }
  }

  function setCurrentClass(cls: ClassListVO) {
    currentClass.value = cls;
  }

  return {
    classList,
    currentClass,
    loading,
    fetchClassList,
    setCurrentClass
  };
});

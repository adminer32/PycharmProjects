import HttpUtil from '@/utils/HttpUtil';
import type { RestResponse } from '@/types/RestResponse';

export interface ClassListVO {
  id: number;
  name: string;
  grade: number;
  studentCount: number;
}

export const getClassList = () => {
  return HttpUtil.get<RestResponse<ClassListVO[]>>('/api/class/list');
};

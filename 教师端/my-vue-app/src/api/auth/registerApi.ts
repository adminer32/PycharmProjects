import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel';

export const registerApi = (username: string, email: string, password: string) => {
  return HttpUtil.post<Result>(
    '/api/system/user/register',
    { username, email, password }
  );
};
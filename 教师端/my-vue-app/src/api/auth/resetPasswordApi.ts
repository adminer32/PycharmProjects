import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel';

export const resetPasswordApi = (
  username: string,
  captcha: string,
  newPassword: string
) => {
  return HttpUtil.post<Result>(
    '/api/system/user/reset-password',
    { username, captcha, newPassword }
  );
};
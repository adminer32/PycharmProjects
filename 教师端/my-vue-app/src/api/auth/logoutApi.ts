import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel';

export const logoutApi = () =>
    HttpUtil.post<Result<null>>('/api/system/user/logout');

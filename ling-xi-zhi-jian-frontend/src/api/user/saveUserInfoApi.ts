import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel';

interface User {
    userId: string; //
    avatar: string;
    name: string; // 昵称
    username: string; // 用户名
    code: string; // 身份标识
    telephone: string;
    email: string;
}
export const saveUserInfoApi = (user: User) => {
    return HttpUtil.post<Result<null>>(
        '/api/system/user/personalCenterSave',
        user,
    );
};

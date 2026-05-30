import HttpUtil from '@/utils/HttpUtil';
import type { RestResponse } from '@/types/RestResponse';

/**
 * 账号管理接口
 */
export default class UserApi {
    /**
     * 获取当前登录账号的信息
     */
    static getUserInfo() {
        return HttpUtil.get<RestResponse<any>>('/api/system/user/me');
    }
}

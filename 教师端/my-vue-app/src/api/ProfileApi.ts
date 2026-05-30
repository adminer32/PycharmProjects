import HttpUtil from '@/utils/HttpUtil';
import type { TeacherProfile } from '@/types/TeacherProfile';
import type { RestResponse } from '@/types/RestResponse';

/**
 * 教师档案管理相关接口封装
 */
export default class ProfileApi {

    /**
     * 获取个人档案
     */
    static getMyProfile() {
        return HttpUtil.get<RestResponse<TeacherProfile>>('/api/profile/me');
    }

    /**
     * 保存档案信息
     */
    static saveProfile(profile: TeacherProfile) {
        return HttpUtil.put<RestResponse<string>>('/api/profile/me', profile);
    }

    /**
     * 上传头像文件
     * @param file 文件对象
     */
    static uploadAvatar(file: File) {
        const formData = new FormData();
        formData.append('file', file);
        return HttpUtil.postForm<RestResponse<string>>('/api/file/upload', formData);
    }
}

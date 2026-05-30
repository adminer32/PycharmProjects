import request from '@/utils/request';

// 登录请求 DTO
export interface LoginRequestDTO {
  username?: string;
  password?: string;
  turnstileToken?: string;
}

// 认证 API
export const AuthAPI = {
  /**
   * 用户登录 (带 Turnstile)
   */
  login(data: LoginRequestDTO) {
    return request.post<any, string>('/api/system/user/login', data);
  },

  /**
   * 用户注销
   */
  logout() {
    return request.post('/api/system/user/logout');
  }
};

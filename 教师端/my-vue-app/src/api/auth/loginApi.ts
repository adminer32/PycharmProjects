import HttpUtil from '@/utils/HttpUtil';
import type { Result } from '@/types/globel';

/**
 * 登录请求类
 */
export interface LoginRequestDTO {
  username: string
  password: string
  turnstileToken: string
}

/**
 * 用户登录
 * @param config 登录参数
 */
export const loginApi = (config: LoginRequestDTO) => {
  return HttpUtil.post<Result<string>>('/api/system/user/login', config)
}

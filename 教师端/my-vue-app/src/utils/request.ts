import axios, { type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import { useUserStore } from '@/store/user';
import { ElMessage } from 'element-plus';

// Sa-token 对应的 Header 键名，根据后端 application.yml 中的 token-name 设置
const TOKEN_NAME = 'auth-token'; 

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '', // 使用 Vite 代理，所以这里置空即可请求同源 /api
  timeout: 10000,
});

// 请求拦截器
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore();
    if (userStore.token) {
      // 自动注入 Sa-token 到请求头
      config.headers[TOKEN_NAME] = userStore.token;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data;
    
    // 适配后端的 RestResponse<T> 结构: { code: 200, message: 'ok', data: ... }
    if (res.code && res.code !== 200) {
      ElMessage.error(res.message || '发生错误');
      
      // 处理特定的业务状态码，例如 401 未授权 / Token 失效
      if (res.code === 401) {
        const userStore = useUserStore();
        userStore.logout();
        window.location.href = '/login'; 
      }
      return Promise.reject(new Error(res.message || 'Error'));
    }
    
    // 如果 code === 200，默认直接返回 data 部分
    // 如果没有 code 字段（例如普通的 Mock 结构），则直接返回整个 response
    return res.data !== undefined ? res.data : res;
  },
  (error) => {
    ElMessage.error(error.message || '网络错误');
    return Promise.reject(error);
  }
);

export default request;

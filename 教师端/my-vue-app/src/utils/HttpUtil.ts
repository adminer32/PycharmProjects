import axios, { type AxiosError, type AxiosRequestConfig } from 'axios';
import { SystemConstant } from '@/constants/SystemConstant';
import { ElMessage } from 'element-plus';

const httpInstance = axios.create({
    baseURL: '',
    timeout: 30000,
});

let isRedirecting = false;

function handleUnauthorized() {
    if (isRedirecting) return;
    isRedirecting = true;
    
    ElMessage.warning('登录失效或未登录，请先登录');
    
    localStorage.removeItem(SystemConstant.TOKEN);
    localStorage.removeItem(SystemConstant.USER_INFO);
    localStorage.removeItem('sa-token');
    
    window.location.href = '/login';
}

httpInstance.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem(SystemConstant.TOKEN);
        if (token) {
            config.headers['auth-token'] = token;
        }
        config.headers['locale'] =
            localStorage.getItem(SystemConstant.Locale) || 'zh-CN';
        return config;
    },
    (error) => {
        if (error.request) {
            ElMessage.error('请求未收到回应！！！');
        } else {
            ElMessage.error('请求未成功发出，' + error.message);
        }
        return Promise.reject(error);
    },
);

httpInstance.interceptors.response.use(
    (response) => {
        const data = response.data as { code?: number; message?: string };
        if (data && typeof data.code === 'number') {
            if (data.code === 401 || data.code === 403) {
                handleUnauthorized();
                return Promise.reject(new Error(data.message || '登录已过期'));
            }
            if (data.code === 500) {
                ElMessage.error(data.message || '服务器内部错误');
                return Promise.reject(new Error(data.message || '服务器内部错误'));
            }
        }
        return response;
    },
    (error: AxiosError) => {
        switch (error.response?.status) {
            case 400:
                ElMessage.error((error.config?.url || '接口') + '：错误的请求');
                break;
            case 401:
                handleUnauthorized();
                break;
            case 402:
                ElMessage.error(
                    (error.config?.url || '接口') + '：用户名或密码错误！！！',
                );
                break;
            case 403:
                ElMessage.error((error.config?.url || '接口') + '：没有权限访问！！！');
                break;
            case 404:
                ElMessage.error(
                    (error.config?.url || '接口') + '：请求的资源不存在！！！',
                );
                break;
            case 405:
                ElMessage.error((error.config?.url || '接口') + '：请求方法不允许！！！');
                break;
            case 415:
                ElMessage.error(
                    (error.config?.url || '接口') + '：Unsupported Media Type！！！',
                );
                break;
            case 500:
                ElMessage.error((error.config?.url || '接口') + '：服务器内部错误！！！');
                break;
            case 502:
                ElMessage.error((error.config?.url || '接口') + '：服务器后端未启动或网关异常！！！');
                break;
            case 503:
                ElMessage.error(
                    (error.config?.url || '接口') + '：Service Unavailable！！！',
                );
                break;
            default:
                ElMessage.error('网络请求发生未知异常');
        }
        return Promise.reject(error);
    },
);

export default class HttpUtil {
    static get<T>(url: string, config?: AxiosRequestConfig<any>) {
        return new Promise<T>((resolve, reject) => {
            httpInstance
                .get<T>(url, config)
                .then((res) => resolve(res.data))
                .catch((err) => reject(err));
        });
    }

    static post<T>(
        url: string,
        data?: Record<string, any>,
        config?: AxiosRequestConfig<any>,
    ) {
        return new Promise<T>((resolve, reject) => {
            httpInstance
                .post<T>(url, data, config)
                .then((res) => resolve(res.data))
                .catch((err) => reject(err));
        });
    }

    static put<T>(url: string, data?: Record<string, any>) {
        return new Promise<T>((resolve, reject) => {
            httpInstance
                .put<T>(url, data)
                .then((res) => resolve(res.data))
                .catch((err) => reject(err));
        });
    }

    static delete<T>(url: string, config?: AxiosRequestConfig<any>) {
        return new Promise<T>((resolve, reject) => {
            httpInstance
                .delete<T>(url, config)
                .then((res) => resolve(res.data))
                .catch((err) => reject(err));
        });
    }

    static postForm<T>(
        url: string,
        data: object,
        config?: AxiosRequestConfig<any>,
    ) {
        return new Promise<T>((resolve, reject) => {
            const finalConfig = {
                ...config,
                timeout: config?.timeout ?? 60000,
            };
            httpInstance
                .postForm<T>(url, data, finalConfig)
                .then((res) => resolve(res.data))
                .catch((err) => reject(err));
        });
    }
}

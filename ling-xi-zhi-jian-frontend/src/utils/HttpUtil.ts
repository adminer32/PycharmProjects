import axios, { type AxiosError, type AxiosRequestConfig } from 'axios';
import { SystemConstant } from '@/constants/SystemConstant';
import MessageUtil from '@/utils/MessageUtil';

const httpInstance = axios.create({
    baseURL: import.meta.env.VITE_BACK_END_BASE_URL,
    timeout: 30000,
});

httpInstance.interceptors.request.use(
    (config) => {
        config.headers['locale'] =
            localStorage.getItem(SystemConstant.Locale) || 'zh-CN';
        const access_token = localStorage.getItem('access_token');
        if (access_token) {
            config.headers['access_token'] = access_token;
        }
        return config;
    },
    (error) => {
        // 对请求错误做些什么
        if (error.request) {
            MessageUtil.error('请求未收到回应！！！');
        } else {
            MessageUtil.error('请求未成功发出，' + error.message);
        }
        return Promise.reject(error);
    },
);

httpInstance.interceptors.response.use(
    (response) => response,
    (error: AxiosError) => {
        switch (error.response?.status) {
            case 400:
                MessageUtil.error(error.config!.url + '错误的请求');
                break;
            case 403:
                MessageUtil.error(error.config!.url + '，没有权限访问！！！');
                break;
            case 404:
                MessageUtil.error(
                    error.config!.url + '，请求的资源不存在！！！',
                );
                break;
            case 405:
                MessageUtil.error(error.config!.url + '，请求方法不允许！！！');
                break;
            case 415:
                MessageUtil.error(
                    error.config!.url + '，Unsupported Media Type！！！',
                );
                break;
            case 500:
                MessageUtil.error(error.config!.url + '，服务器内部错误！！！');
                break;
            case 502:
                MessageUtil.error(error.config!.url + '，服务器未启动！！！');
                break;
            case 503:
                MessageUtil.error(
                    error.config!.url + '，Service Unavailable！！！',
                );
                break;
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
                timeout: config?.timeout ?? 60000, // 如果未设置 timeout，则使用默认值 60000 毫秒
            };

            httpInstance
                .postForm<T>(url, data, finalConfig)
                .then((res) => resolve(res.data))
                .catch((err) => reject(err));
        });
    }
}

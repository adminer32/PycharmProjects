/**
 * 通用响应结构
 */
export interface RestResponse<T = any> {
    httpCode: number;
    status: 'success' | 'error' | 'warning' | 'info';
    message: string;
    data: T;
}

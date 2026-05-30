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

export const getUserInfoApi = async (): Promise<Result<User>> => {
    const access_token = localStorage.getItem('access_token');
    const headers: Record<string, string> = {
        'Content-Type': 'application/json',
    };
    if (access_token) {
        headers['access_token'] = access_token;
    }

    try {
        const response = await fetch('/api/v0/student/users/', {
            method: 'GET',
            headers,
        });
        const result = await response.json();
        
        if (result.success && result.data) {
            return {
                status: 'success',
                message: '获取成功',
                data: {
                    userId: String(result.data.id),
                    avatar: result.data.avatar || '/src/assets/avatar.svg',
                    name: result.data.real_name || result.data.username,
                    username: result.data.username,
                    code: result.data.student_id || '',
                    telephone: '',
                    email: result.data.email || '',
                }
            };
        }
        return { status: 'error', message: result.message || '获取用户信息失败', data: null as any };
    } catch (error) {
        return { status: 'error', message: '网络错误', data: null as any };
    }
};

const API_BASE = '/api';

export interface TokenValidationResult {
    success: boolean;
    access_token_valid: boolean;
    refresh_token_valid: boolean;
}

export const verifyTokenApi = async (): Promise<TokenValidationResult> => {
    const access_token = localStorage.getItem('access_token');
    const refresh_token = localStorage.getItem('refresh_token');

    try {
        const headers: Record<string, string> = {
            'Content-Type': 'application/json',
        };
        if (access_token) {
            headers['access_token'] = access_token;
        }

        const response = await fetch(`${API_BASE}/v0/student/auth/token`, {
            method: 'GET',
            headers,
        });
        return await response.json();
    } catch (error) {
        console.error('Token verification failed:', error);
        return { success: false, access_token_valid: false, refresh_token_valid: false };
    }
};

export const clearTokens = () => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
};
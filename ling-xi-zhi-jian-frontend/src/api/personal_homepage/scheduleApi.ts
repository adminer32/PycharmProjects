const API_BASE = '/api';

const getHeaders = () => {
    const headers: Record<string, string> = {
        'Content-Type': 'application/json',
    };
    const access_token = localStorage.getItem('access_token');
    if (access_token) {
        headers['access_token'] = access_token;
    }
    return headers;
};

export const getScheduleListApi = (dateString: string) =>
    fetch(`${API_BASE}/v0/student/schedule/query?date=${dateString}`, {
        headers: getHeaders()
    }).then(res => res.json());

export interface ScheduleSaveData {
    id?: number;
    time: string;
    type: string;
    content: string;
    scheduleDate: string;
}

export const saveScheduleApi = (data: ScheduleSaveData) =>
    fetch(`${API_BASE}/v0/student/schedule/save`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(data)
    }).then(res => res.json());

export const deleteScheduleApi = (ids: string) =>
    fetch(`${API_BASE}/v0/student/schedule/del?ids=${ids}`, {
        method: 'DELETE',
        headers: getHeaders()
    }).then(res => res.json());
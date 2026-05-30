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

export const getLearningStatsApi = (studentId: number) =>
    fetch(`${API_BASE}/v0/student/learning/stats/${studentId}`, {
        headers: getHeaders()
    }).then(res => res.json());

export const getActionScoresApi = (studentId: number) =>
    fetch(`${API_BASE}/v0/student/learning/action-scores/${studentId}`, {
        headers: getHeaders()
    }).then(res => res.json());

export const getShuttlecockSkillsApi = (studentId: number) =>
    fetch(`${API_BASE}/v0/student/learning/shuttlecock-skills/${studentId}`, {
        headers: getHeaders()
    }).then(res => res.json());

export const getWeeklyTrendApi = (studentId: number) =>
    fetch(`${API_BASE}/v0/student/learning/weekly-trend/${studentId}`, {
        headers: getHeaders()
    }).then(res => res.json());

export const getCheckinRecordsApi = (studentId: number, month?: number) => {
    const url = month 
        ? `${API_BASE}/v0/student/learning/checkin-records/${studentId}?month=${month}`
        : `${API_BASE}/v0/student/learning/checkin-records/${studentId}`;
    return fetch(url, { headers: getHeaders() }).then(res => res.json());
};

export interface PracticeRecord {
    id: number;
    student_id: number;
    action_type: string;
    video_url: string;
    result_data: {
        elevation_records?: Array<{ frame: number; left: number; right: number }>;
        overall_score?: number;
        hip_score?: number;
        knee_score?: number;
        ankle_score?: number;
        foot_height_score?: number;
        feedback?: string[];
        [key: string]: any;
    };
    created_at: string;
}

export const savePracticeRecordApi = (data: {
    student_id: number;
    action_type: string;
    video_url: string;
    result_data: any;
}): Promise<{ success: boolean; record_id?: number }> =>
    fetch(`${API_BASE}/v0/student/learning/practice-record`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(data)
    }).then(res => res.json());

export const getPracticeRecordsApi = (studentId: number): Promise<PracticeRecord[]> =>
    fetch(`${API_BASE}/v0/student/learning/practice-records/${studentId}`, {
        headers: getHeaders()
    }).then(res => res.json());

export const getPracticeRecordApi = (recordId: number): Promise<PracticeRecord> =>
    fetch(`${API_BASE}/v0/student/learning/practice-record/${recordId}`, {
        headers: getHeaders()
    }).then(res => res.json());

export const deletePracticeRecordApi = (recordId: number): Promise<{ success: boolean }> =>
    fetch(`${API_BASE}/v0/student/learning/practice-record/${recordId}`, {
        method: 'DELETE',
        headers: getHeaders()
    }).then(res => res.json());
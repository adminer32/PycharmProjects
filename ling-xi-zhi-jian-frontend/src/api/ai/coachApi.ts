const API_BASE = '/api';

interface ActionContext {
    action_type?: string;
    hip_score?: number;
    knee_score?: number;
    ankle_score?: number;
    foot_height_score?: number;
    overall_score?: number;
    feedback?: string[];
}

interface CoachChatRequest {
    message: string;
    action_context?: ActionContext;
    model?: string;
}

interface CoachChatResponse {
    success: boolean;
    message: string;
    model: string;
}

export const coachChatApi = (data: CoachChatRequest): Promise<CoachChatResponse> =>
    fetch(`${API_BASE}/v0/ai/coach/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    }).then(res => res.json());

export const listOllamaModelsApi = (): Promise<{ success: boolean; models: string[] }> =>
    fetch(`${API_BASE}/v0/ai/models`).then(res => res.json());

export const checkOllamaHealthApi = (): Promise<{ success: boolean; status: string }> =>
    fetch(`${API_BASE}/v0/ai/health`).then(res => res.json());
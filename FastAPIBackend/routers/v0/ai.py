from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
import httpx
import json
import os
import json as json_util
from datetime import datetime
from database.operation import query, change, insert_and_get_id

router = APIRouter(tags=["ai"])

DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1"


def get_deepseek_api_key():
    config_path = os.path.join(
        os.path.dirname(__file__), "..", "..", "configs", "deepseek.json"
    )
    try:
        with open(config_path, "r") as f:
            config = json_util.load(f)
            return config.get("deepseek_api_key", "")
    except:
        return os.getenv("DEEPSEEK_API_KEY", "")


class ChatMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    messages: List[ChatMessage]
    model: str = "deepseek-chat"
    stream: bool = False


class ActionContext(BaseModel):
    action_type: Optional[str] = None
    hip_score: Optional[float] = None
    knee_score: Optional[float] = None
    ankle_score: Optional[float] = None
    foot_height_score: Optional[float] = None
    overall_score: Optional[float] = None
    feedback: Optional[List[str]] = None


class CoachChatRequest(BaseModel):
    message: str
    action_context: Optional[ActionContext] = None
    model: str = "deepseek-chat"


@router.post("/chat")
async def chat(request: ChatRequest):
    """通用DeepSeek聊天接口"""
    api_key = get_deepseek_api_key()
    if not api_key:
        raise HTTPException(status_code=500, detail="DeepSeek API密钥未配置")

    try:
        async with httpx.AsyncClient(timeout=120.0) as client:
            response = await client.post(
                f"{DEEPSEEK_BASE_URL}/chat/completions",
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": request.model,
                    "messages": [m.model_dump() for m in request.messages],
                    "stream": False,
                },
            )
            if response.status_code != 200:
                raise HTTPException(
                    status_code=500, detail=f"DeepSeek API错误: {response.text}"
                )
            return response.json()
    except httpx.ConnectError:
        raise HTTPException(status_code=503, detail="无法连接到DeepSeek服务")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/coach/chat")
async def coach_chat(request: CoachChatRequest):
    """AI教练聊天接口 - 根据动作分析上下文回答问题"""

    system_prompt = """你是一位专业的毽球运动教练，名字叫"翎析"。你的职责是：
1. 根据用户的动作分析数据提供专业的指导建议
2. 用通俗易懂的语言解释动作要领
3. 给出具体的改进方法
4. 鼓励用户坚持练习
5. 回答要简洁有力，不啰嗦
6. 如果用户没有提供动作数据，引导用户提供或描述自己的动作问题

你的回复应该专业、亲切、有耐心，像一位经验丰富的教练在面对面指导学生。"""

    messages = [{"role": "system", "content": system_prompt}]

    if request.action_context:
        context_parts = []
        if request.action_context.action_type:
            context_parts.append(f"当前练习动作：{request.action_context.action_type}")
        if request.action_context.overall_score:
            context_parts.append(f"综合评分：{request.action_context.overall_score}分")
        if request.action_context.hip_score:
            context_parts.append(f"髋关节评分：{request.action_context.hip_score}分")
        if request.action_context.knee_score:
            context_parts.append(f"膝关节评分：{request.action_context.knee_score}分")
        if request.action_context.ankle_score:
            context_parts.append(f"踝关节评分：{request.action_context.ankle_score}分")
        if request.action_context.foot_height_score:
            context_parts.append(
                f"抬脚高度评分：{request.action_context.foot_height_score}分"
            )
        if request.action_context.feedback:
            context_parts.append(
                f"详细反馈：{'；'.join(request.action_context.feedback)}"
            )

        if context_parts:
            context_info = "【用户动作分析数据】\n" + "\n".join(context_parts)
            messages.append({"role": "system", "content": context_info})

    messages.append({"role": "user", "content": request.message})

    api_key = get_deepseek_api_key()
    if not api_key:
        raise HTTPException(status_code=500, detail="DeepSeek API密钥未配置")

    try:
        async with httpx.AsyncClient(timeout=120.0) as client:
            response = await client.post(
                f"{DEEPSEEK_BASE_URL}/chat/completions",
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={"model": request.model, "messages": messages, "stream": False},
            )
            if response.status_code != 200:
                raise HTTPException(
                    status_code=500, detail=f"DeepSeek API错误: {response.text}"
                )

            result = response.json()
            return {
                "success": True,
                "message": result.get("choices", [{}])[0]
                .get("message", {})
                .get("content", ""),
                "model": request.model,
            }
    except httpx.ConnectError:
        raise HTTPException(status_code=503, detail="无法连接到DeepSeek服务")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/models")
async def list_models():
    """列出可用的DeepSeek模型"""
    return {
        "success": True,
        "models": ["deepseek-chat", "deepseek-coder"],
    }


@router.get("/health")
async def health_check():
    """检查DeepSeek服务状态"""
    api_key = get_deepseek_api_key()
    if not api_key:
        return {"success": False, "status": "offline", "reason": "API密钥未配置"}

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.post(
                f"{DEEPSEEK_BASE_URL}/chat/completions",
                headers={"Authorization": f"Bearer {api_key}"},
                json={
                    "model": "deepseek-chat",
                    "messages": [{"role": "user", "content": "hi"}],
                    "max_tokens": 1,
                },
            )
            if response.status_code == 200:
                return {"success": True, "status": "online"}
            return {"success": False, "status": "error"}
    except:
        return {"success": False, "status": "offline"}


class QueryNoteParam(BaseModel):
    title: Optional[str] = ""
    id: Optional[str] = ""
    task_status: Optional[str] = ""
    task_id: Optional[str] = ""


class QueryNoteRequest(BaseModel):
    param: QueryNoteParam


class AIChatNote(BaseModel):
    id: int
    taskId: str
    createTime: str
    videoUrl: str
    title: str
    autoChapters: str = ""
    summarization: str = ""
    meetingAssistance: str = ""
    personalNote: str = ""
    taskStatus: str = "进行中"


@router.post("/queryPersonalNotes")
async def query_personal_notes(request: QueryNoteRequest):
    """查询个人笔记列表"""
    try:
        sql = "SELECT id, task_id, create_time, video_url, title, auto_chapters, summarization, meeting_assistance, personal_note, task_status FROM ai_transcription_task WHERE 1=1"
        params = []

        if request.param.title:
            sql += " AND title LIKE %s"
            params.append(f"%{request.param.title}%")
        if request.param.id:
            sql += " AND id = %s"
            params.append(request.param.id)
        if request.param.task_status:
            sql += " AND task_status = %s"
            params.append(request.param.task_status)
        if request.param.task_id:
            sql += " AND task_id = %s"
            params.append(request.param.task_id)

        sql += " ORDER BY create_time DESC"

        rows = query(sql, tuple(params))
        notes = []
        for row in rows:
            notes.append(
                {
                    "id": row["id"],
                    "taskId": row["task_id"],
                    "createTime": str(row["create_time"]) if row["create_time"] else "",
                    "videoUrl": row["video_url"] or "",
                    "title": row["title"] or "",
                    "autoChapters": row["auto_chapters"] or "",
                    "summarization": row["summarization"] or "",
                    "meetingAssistance": row["meeting_assistance"] or "",
                    "personalNote": row["personal_note"] or "",
                    "taskStatus": row["task_status"] or "进行中",
                }
            )

        return {"status": "success", "data": {"list": notes}}
    except Exception as e:
        return {"status": "error", "message": str(e), "data": {"list": []}}


@router.post("/addTranscriptionTask")
async def add_transcription_task(request: dict):
    """添加转录任务"""
    try:
        title = request.get("title", "未命名任务")
        video_url = request.get("video_url", "")
        task_id = f"task_{datetime.now().strftime('%Y%m%d%H%M%S')}"

        sql = "INSERT INTO ai_transcription_task (task_id, title, video_url, task_status, create_time) VALUES (%s, %s, %s, %s, NOW())"
        task_id = insert_and_get_id(sql, (task_id, title, video_url, "进行中"))

        if task_id:
            return {
                "status": "success",
                "data": {
                    "id": task_id,
                    "taskId": sql.split("VALUES")[0].split("task_id")[1]
                    if task_id
                    else "",
                    "createTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    "videoUrl": video_url,
                    "title": title,
                    "autoChapters": "",
                    "summarization": "",
                    "meetingAssistance": "",
                    "personalNote": "",
                    "taskStatus": "进行中",
                },
            }
        return {"status": "error", "message": "创建任务失败"}
    except Exception as e:
        return {"status": "error", "message": str(e)}


@router.post("/updateNote")
async def update_note(request: dict):
    """更新笔记"""
    try:
        note_id = request.get("id")
        title = request.get("title")
        personal_note = request.get("personal_note", "")

        if note_id and title:
            sql = "UPDATE ai_transcription_task SET title = %s, personal_note = %s WHERE id = %s"
            success = change(sql, (title, personal_note, note_id))
            if success:
                return {"status": "success", "message": "更新成功"}

        return {"status": "error", "message": "更新失败"}
    except Exception as e:
        return {"status": "error", "message": str(e)}


@router.delete("/del")
async def delete_notes(ids: str):
    """删除笔记"""
    try:
        id_list = [int(x.strip()) for x in ids.split(",") if x.strip().isdigit()]
        if not id_list:
            return {"status": "error", "message": "无效的ID"}

        placeholders = ",".join(["%s"] * len(id_list))
        sql = f"DELETE FROM ai_transcription_task WHERE id IN ({placeholders})"
        success = change(sql, tuple(id_list))

        if success:
            return {"status": "success", "message": "删除成功"}
        return {"status": "error", "message": "删除失败"}
    except Exception as e:
        return {"status": "error", "message": str(e)}


@router.get("/queryTaskStatus")
async def query_task_status(taskId: str):
    """查询任务状态"""
    try:
        sql = "SELECT id, task_id, task_status, auto_chapters, summarization, meeting_assistance FROM ai_transcription_task WHERE task_id = %s"
        rows = query(sql, (taskId,))

        if rows:
            row = rows[0]
            return {
                "status": "success",
                "data": {
                    "id": row["id"],
                    "taskId": row["task_id"],
                    "taskStatus": row["task_status"] or "进行中",
                    "autoChapters": row["auto_chapters"] or "",
                    "summarization": row["summarization"] or "",
                    "meetingAssistance": row["meeting_assistance"] or "",
                },
            }
        return {"status": "error", "message": "任务不存在"}
    except Exception as e:
        return {"status": "error", "message": str(e)}


@router.post("/NotePolishing")
async def note_polishing(request: dict):
    """AI笔记润色"""
    api_key = get_deepseek_api_key()
    if not api_key:
        return {"status": "error", "message": "DeepSeek API密钥未配置"}

    note_content = request.get("note_content", "")
    if not note_content:
        return {"status": "error", "message": "笔记内容不能为空"}

    try:
        messages = [
            {
                "role": "system",
                "content": "你是一位专业的文字润色助手，请将用户提供的笔记内容进行润色，使其更加通顺、有条理、简洁有力。直接返回润色后的内容，不要添加其他说明。",
            },
            {"role": "user", "content": note_content},
        ]

        async with httpx.AsyncClient(timeout=60.0) as client:
            response = await client.post(
                f"{DEEPSEEK_BASE_URL}/chat/completions",
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={"model": "deepseek-chat", "messages": messages, "stream": False},
            )

            if response.status_code != 200:
                return {"status": "error", "message": "AI服务错误"}

            result = response.json()
            polished = (
                result.get("choices", [{}])[0].get("message", {}).get("content", "")
            )
            return {"status": "success", "data": {"polished_note": polished}}
    except Exception as e:
        return {"status": "error", "message": str(e)}

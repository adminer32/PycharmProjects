from fastapi import APIRouter, HTTPException, BackgroundTasks
from pydantic import BaseModel
from typing import Optional
import os
import json
import httpx
import subprocess
import uuid
from datetime import datetime

router = APIRouter(tags=["transcribe"])

DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1"


def get_deepseek_api_key():
    config_path = os.path.join(
        os.path.dirname(__file__), "..", "..", "configs", "deepseek.json"
    )
    try:
        with open(config_path, "r") as f:
            config = json.load(f)
            return config.get("deepseek_api_key", "")
    except:
        return os.getenv("DEEPSEEK_API_KEY", "")


class TranscribeRequest(BaseModel):
    task_id: str
    video_path: str


class AnalysisRequest(BaseModel):
    task_id: str
    transcription: str


def extract_audio(video_path: str, output_path: str) -> bool:
    """从视频提取音频"""
    try:
        cmd = [
            "ffmpeg",
            "-i",
            video_path,
            "-vn",
            "-acodec",
            "pcm_s16le",
            "-ar",
            "16000",
            "-ac",
            "1",
            "-y",
            output_path,
        ]
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=300)
        return result.returncode == 0
    except Exception as e:
        print(f"音频提取失败: {e}")
        return False


_whisper_model = None


def get_whisper_model():
    """获取或初始化 openai-whisper 模型（单例）"""
    global _whisper_model
    if _whisper_model is None:
        import whisper

        print("Loading whisper model...")
        _whisper_model = whisper.load_model("base", device="cpu")
        print("Model loaded!")
    return _whisper_model


def transcribe_audio(audio_path: str) -> Optional[str]:
    """使用 openai-whisper 转录音频"""
    try:
        model = get_whisper_model()
        result = model.transcribe(audio_path, language="zh", fp16=False)
        return result.get("text", "").strip() if result else None
    except Exception as e:
        print(f"转录失败: {e}")
        return None


def analyze_with_deepseek(transcription: str, api_key: str) -> dict:
    """使用 DeepSeek 分析转录文本，生成章节、关键词、总结和知识图谱"""
    try:
        system_prompt = """你是一位专业的毽球学习内容分析助手。用户提供的是一段毽球（踢毽子）教学视频的转录文本。

注意事项：
1. 转录文本可能包含语音识别错误，你需要根据上下文进行合理修正
2. 视频内容主要涉及毽球运动，包括：盘踢、绷踢、拐踢、磕踢、踏踢、跳踢等技术动作
3. 如果转录内容明显与毽球无关或识别错误，请基于毽球教学内容进行修正
4. 毽球的正确名称是"毽球"或"踢毽子"，不是"建球"

请分析转录文本并严格按照以下JSON格式返回：
{
    "keywords": ["毽球", "盘踢", "技术动作"...],  // 5-8个毽球相关的关键词
    "chapters": [
        {"headline": "章节标题", "summary": "章节摘要"}
    ],  // 3-5个学习章节
    "overall_summary": "整体总结",  // 一段总结
    "mind_map": {  // 知识图谱结构
        "title": "视频主题",
        "children": [
            {"title": "子主题1", "children": [{"title": "详细内容1"}, {"title": "详细内容2"}]},
            {"title": "子主题2", "children": [{"title": "详细内容3"}]}
        ]
    }
}"""

        from opencc import OpenCC

        cc = OpenCC("t2s")

        def to_simplified(text):
            return cc.convert(text) if text else text

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": f"以下是视频转录文本：\n{transcription}"},
        ]

        response_text = ""
        with httpx.Client(timeout=120.0) as client:
            response = client.post(
                f"{DEEPSEEK_BASE_URL}/chat/completions",
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={"model": "deepseek-chat", "messages": messages, "stream": False},
            )

            if response.status_code == 200:
                result = response.json()
                response_text = (
                    result.get("choices", [{}])[0].get("message", {}).get("content", "")
                )
            else:
                return None

        # 解析 JSON 响应
        try:
            from opencc import OpenCC

            cc = OpenCC("t2s")

            def to_simplified(text):
                return cc.convert(text) if text else text

            def convert_mind_map(node, converter):
                """递归转换知识图谱节点"""
                if not node:
                    return node
                converted = {
                    "Title": converter(node.get("title", node.get("Title", ""))),
                    "Topic": [],
                }
                children = node.get("children", [])
                for child in children:
                    converted["Topic"].append(convert_mind_map(child, converter))
                return converted

            json_str = response_text
            if "```json" in json_str:
                json_str = json_str.split("```json")[1].split("```")[0]
            elif "```" in json_str:
                json_str = json_str.split("```")[1].split("```")[0]

            result = json.loads(json_str.strip())

            # 转换为简体中文
            result["overall_summary"] = to_simplified(result.get("overall_summary", ""))
            result["keywords"] = [to_simplified(k) for k in result.get("keywords", [])]
            result["chapters"] = [
                {
                    "headline": to_simplified(c.get("headline", "")),
                    "summary": to_simplified(c.get("summary", "")),
                }
                for c in result.get("chapters", [])
            ]

            # 转换知识图谱
            if result.get("mind_map"):
                result["mind_map"] = convert_mind_map(result["mind_map"], to_simplified)

            return result
        except:
            return None

    except Exception as e:
        print(f"DeepSeek 分析失败: {e}")
        return None


@router.post("/transcribe")
async def transcribe_video(request: TranscribeRequest):
    """转录视频（提取音频+语音转文字）"""
    video_path = request.video_path

    if not os.path.exists(video_path):
        raise HTTPException(status_code=404, detail="视频文件不存在")

    # 生成音频文件路径
    audio_path = f"/tmp/audio_{uuid.uuid4().hex[:8]}.wav"

    try:
        # 1. 提取音频
        if not extract_audio(video_path, audio_path):
            return {"status": "error", "message": "音频提取失败"}

        # 2. 转录
        transcription = transcribe_audio(audio_path)
        if not transcription:
            return {"status": "error", "message": "转录失败"}

        return {
            "status": "success",
            "data": {"task_id": request.task_id, "transcription": transcription},
        }

    finally:
        # 清理临时音频文件
        if os.path.exists(audio_path):
            os.remove(audio_path)


@router.post("/analyze")
async def analyze_transcription(request: AnalysisRequest):
    """分析转录文本（生成章节、关键词、总结）"""
    api_key = get_deepseek_api_key()
    if not api_key:
        return {"status": "error", "message": "DeepSeek API 密钥未配置"}

    result = analyze_with_deepseek(request.transcription, api_key)

    if result:
        return {
            "status": "success",
            "data": {
                "task_id": request.task_id,
                "keywords": result.get("keywords", []),
                "chapters": result.get("chapters", []),
                "overall_summary": result.get("overall_summary", ""),
                "transcription": request.transcription,
            },
        }

    return {"status": "error", "message": "分析失败"}


class FullProcessRequest(BaseModel):
    task_id: str
    video_url: str


def process_video_task(task_id: str, video_path: str):
    """后台任务：处理视频转录和分析"""
    from database.operation import change

    api_key = get_deepseek_api_key()
    if not api_key:
        print(f"Task {task_id}: DeepSeek API 密钥未配置")
        return

    # 1. 提取音频
    audio_path = f"/tmp/audio_{uuid.uuid4().hex[:8]}.wav"
    try:
        if not extract_audio(video_path, audio_path):
            print(f"Task {task_id}: 音频提取失败")
            return

        # 2. 转录
        transcription = transcribe_audio(audio_path)
        if not transcription:
            print(f"Task {task_id}: 转录失败")
            return

        # 3. DeepSeek 分析
        analysis = analyze_with_deepseek(transcription, api_key)

        # 4. 保存到数据库
        keywords_json = (
            json.dumps({"Keywords": analysis.get("keywords", [])}) if analysis else ""
        )
        chapters_json = json.dumps(
            [
                {"Headline": c.get("headline", ""), "Summary": c.get("summary", "")}
                for c in (analysis.get("chapters", []) if analysis else [])
            ]
        )
        # 保存总结和知识图谱
        summary_data = {"Summary": analysis.get("overall_summary", "")}
        if analysis.get("mind_map"):
            summary_data["MindMapSummary"] = [analysis["mind_map"]]
        summary_json = json.dumps(summary_data) if analysis else ""

        sql = """UPDATE ai_transcription_task 
                 SET auto_chapters = %s, summarization = %s, meeting_assistance = %s, 
                     transcription = %s, task_status = '已完成' 
                 WHERE task_id = %s"""

        success = change(
            sql, (chapters_json, summary_json, keywords_json, transcription, task_id)
        )

        if success:
            print(f"Task {task_id}: 处理完成")
        else:
            print(f"Task {task_id}: 保存失败")

    finally:
        if os.path.exists(audio_path):
            os.remove(audio_path)


@router.post("/full-process")
async def full_process_video(
    request: FullProcessRequest, background_tasks: BackgroundTasks
):
    """完整流程：上传视频 -> 转录 -> 分析 -> 保存结果（后台异步执行）"""
    task_id = request.task_id
    video_url = request.video_url

    # 获取绝对路径
    video_path = video_url
    if video_url.startswith("/videos/"):
        video_path = video_url.replace(
            "/videos/", "/home/mrliao/下载/GVHMR/InputVideo/视频/"
        )
    elif video_url.startswith("/uploads/"):
        base_dir = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))
        video_path = os.path.join(base_dir, "data", video_url.lstrip("/"))
    elif video_url.startswith("/"):
        base_dir = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))
        video_path = os.path.join(base_dir, video_url.lstrip("/"))

    if not os.path.exists(video_path):
        return {"status": "error", "message": f"视频文件不存在: {video_path}"}

    # 立即返回，让后台任务处理耗时的转录和分析
    background_tasks.add_task(process_video_task, task_id, video_path)

    return {
        "status": "success",
        "message": "任务已加入队列，正在后台处理",
        "data": {"task_id": task_id},
    }


@router.get("/health")
async def health_check():
    """检查转录服务状态"""
    try:
        import whisper

        model = whisper.load_model("base", device="cpu")
        return {"status": "success", "whisper": "available"}
    except Exception as e:
        return {"status": "error", "whisper": str(e)}

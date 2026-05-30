from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from routers import api
import uvicorn
import os
import json
import httpx

DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1"

app = FastAPI()
app.include_router(api.router)

app.mount(
    "/videos",
    StaticFiles(directory="/home/mrliao/下载/GVHMR/InputVideo/视频"),
    name="videos",
)

app.mount(
    "/uploads",
    StaticFiles(directory=os.path.join(os.path.dirname(__file__), "data", "uploads")),
    name="uploads",
)

# 允许前端 origin（开发环境允许所有）
origins = [
    "http://localhost:5173",
    "http://127.0.0.1:5173",
    "http://localhost:5500",
    "http://127.0.0.1:5500",
    "http://localhost:5010",
    "http://127.0.0.1:5010",
    "null",
    "file://",
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.websocket("/ws/ai")
async def websocket_ai(websocket: WebSocket):
    """AI对话WebSocket端点 - 使用视频转录作为上下文"""
    await websocket.accept()

    api_key = None
    transcription_context = ""

    try:
        from database.operation import query

        while True:
            data = await websocket.receive_text()
            message = json.loads(data)

            if message.get("type") == "init":
                # 初始化，获取视频转录上下文
                task_id = message.get("task_id", "")
                if task_id:
                    sql = "SELECT transcription FROM ai_transcription_task WHERE task_id = %s"
                    rows = query(sql, (task_id,))
                    if rows and rows[0][0]:
                        transcription_context = rows[0][0]

                await websocket.send_json(
                    {
                        "type": "init",
                        "status": "success",
                        "has_context": bool(transcription_context),
                    }
                )

            elif message.get("type") == "chat":
                user_message = message.get("content", "")

                # 加载 API key
                if not api_key:
                    config_path = os.path.join(
                        os.path.dirname(__file__), "configs", "deepseek.json"
                    )
                    try:
                        with open(config_path, "r") as f:
                            config = json.load(f)
                            api_key = config.get("deepseek_api_key", "")
                    except:
                        pass

                if not api_key:
                    await websocket.send_json(
                        {
                            "type": "chat",
                            "content": "DeepSeek API 密钥未配置",
                            "status": "error",
                        }
                    )
                    continue

                # 构建消息
                system_prompt = """你是一位专业的毽球运动教练，名字叫"翎析"。你的职责是：
1. 根据用户的动作分析数据提供专业的指导建议
2. 用通俗易懂的语言解释动作要领
3. 给出具体的改进方法
4. 鼓励用户坚持练习
5. 回答要简洁有力，不啰嗦
6. 如果用户没有提供动作数据，引导用户提供或描述自己的动作问题

你的回复应该专业、亲切、有耐心，像一位经验丰富的教练在面对面指导学生。"""

                messages = [{"role": "system", "content": system_prompt}]

                # 添加视频转录上下文
                if transcription_context:
                    context_prompt = f"【视频转录内容】\n{transcription_context}\n\n请基于以上视频转录内容回答用户的问题。"
                    messages.append({"role": "system", "content": context_prompt})

                messages.append({"role": "user", "content": user_message})

                # 调用 DeepSeek
                try:
                    async with httpx.AsyncClient(timeout=60.0) as client:
                        response = await client.post(
                            f"{DEEPSEEK_BASE_URL}/chat/completions",
                            headers={
                                "Authorization": f"Bearer {api_key}",
                                "Content-Type": "application/json",
                            },
                            json={
                                "model": "deepseek-chat",
                                "messages": messages,
                                "stream": False,
                            },
                        )

                        if response.status_code == 200:
                            result = response.json()
                            ai_response = (
                                result.get("choices", [{}])[0]
                                .get("message", {})
                                .get("content", "")
                            )
                            await websocket.send_json(
                                {
                                    "type": "chat",
                                    "content": ai_response,
                                    "status": "success",
                                }
                            )
                        else:
                            await websocket.send_json(
                                {
                                    "type": "chat",
                                    "content": "AI 服务暂时不可用",
                                    "status": "error",
                                }
                            )
                except Exception as e:
                    await websocket.send_json(
                        {
                            "type": "chat",
                            "content": f"发生错误: {str(e)}",
                            "status": "error",
                        }
                    )

    except WebSocketDisconnect:
        pass
    except Exception as e:
        try:
            await websocket.send_json({"type": "error", "content": str(e)})
        except:
            pass
    except Exception as e:
        try:
            await websocket.send_json({"type": "error", "content": str(e)})
        except:
            pass


if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8001)

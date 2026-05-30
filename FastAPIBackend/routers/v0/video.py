from fastapi import APIRouter
from pydantic import BaseModel
from database.operation import query, change, insert_and_get_id
import json
from typing import Optional, List
from datetime import datetime

router = APIRouter(prefix="/video/history", tags=["video"])


class ContentItem(BaseModel):
    id: str
    type: str
    text: Optional[str] = None
    card: Optional[dict] = None
    sender: str
    time: str


class HistoryItem(BaseModel):
    id: int
    title: str
    contentList: Optional[List[ContentItem]] = None
    createTime: str


class QueryRequest(BaseModel):
    param: dict


@router.post("/query")
async def query_video_history(request: QueryRequest):
    """查询视频历史记录"""
    try:
        sql = """
            SELECT id, title, content, created_at
            FROM video_history
            ORDER BY created_at DESC
            LIMIT 100
        """
        rows = query(sql, ())

        records = []
        for row in rows:
            history_id, title, content_str, created_at = row

            content_list = None
            if content_str:
                try:
                    content_list = json.loads(content_str)
                except:
                    content_list = []

            records.append(
                {
                    "id": history_id,
                    "title": title or "",
                    "contentList": content_list,
                    "createTime": created_at.isoformat() if created_at else "",
                }
            )

        return {
            "status": "success",
            "message": "查询成功",
            "data": {"records": records},
        }
    except Exception as e:
        return {"status": "error", "message": str(e), "data": {"records": []}}


class SaveVideoHistoryRequest(BaseModel):
    id: int
    content: str = ""


@router.post("/save")
async def save_video_history(request: SaveVideoHistoryRequest):
    """保存视频历史记录"""
    try:
        if request.id == 0:
            sql = """
                INSERT INTO video_history (content, created_at)
                VALUES (%s, %s)
            """
            now = datetime.now()
            new_id = insert_and_get_id(sql, (request.content, now))
            if new_id:
                return {
                    "status": "success",
                    "message": "保存成功",
                    "data": {"history_id": new_id},
                }
            return {"status": "error", "message": "保存失败"}
        else:
            sql = """
                UPDATE video_history SET content = %s
                WHERE id = %s
            """
            success = change(sql, (request.content, request.id))
            if success:
                return {
                    "status": "success",
                    "message": "保存成功",
                    "data": {"history_id": request.id},
                }
            return {"status": "error", "message": "保存失败"}
    except Exception as e:
        return {"status": "error", "message": str(e)}


@router.delete("/del")
async def delete_video_history(ids: str):
    """删除视频历史记录"""
    try:
        id_list = ids.split(",")
        id_tuple = tuple(int(i) for i in id_list)

        placeholders = ",".join(["%s"] * len(id_tuple))
        sql = f"DELETE FROM video_history WHERE id IN ({placeholders})"

        success = change(sql, id_tuple)

        if success:
            return {"status": "success", "message": "删除成功"}
        else:
            return {"status": "error", "message": "删除失败"}
    except Exception as e:
        return {"status": "error", "message": str(e)}


@router.get("/detail")
async def query_history_detail(historyId: int):
    """查询历史记录详情"""
    try:
        sql = """
            SELECT id, title, content, created_at
            FROM video_history
            WHERE id = %s
        """
        rows = query(sql, (historyId,))

        if not rows:
            return {"status": "error", "message": "记录不存在", "data": None}

        history_id, title, content_str, created_at = rows[0]

        content_list = None
        if content_str:
            try:
                content_list = json.loads(content_str)
            except:
                content_list = []

        return {
            "status": "success",
            "message": "查询成功",
            "data": {
                "id": history_id,
                "title": title or "",
                "contentList": content_list,
                "createTime": created_at.isoformat() if created_at else "",
            },
        }
    except Exception as e:
        return {"status": "error", "message": str(e), "data": None}

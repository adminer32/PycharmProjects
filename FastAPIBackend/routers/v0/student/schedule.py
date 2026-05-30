from fastapi import APIRouter, HTTPException
from fastapi.requests import Request
from fastapi.responses import JSONResponse
from utils.jwt_manager import JWTManager
import database

router = APIRouter()
jwt_manager = JWTManager()


def get_student_id_from_request(request: Request) -> int:
    """从请求中获取学生ID"""
    access_token = request.headers.get("access_token")
    ip_address = request.headers.get("X-Real-IP")

    if not access_token:
        raise HTTPException(status_code=401, detail="未登录")

    user_id = jwt_manager.verify_access_token(access_token, ip_address, "student")
    if not user_id:
        raise HTTPException(status_code=401, detail="登录已过期")

    return user_id


@router.get("/query")
async def query_schedules(request: Request, date: str):
    """获取指定月份的日程"""
    try:
        student_id = get_student_id_from_request(request)
        result = database.get_schedules_by_month(student_id, date)
        return {"status": "success", "data": result.get("data", {})}
    except HTTPException as e:
        return {"status": "error", "message": e.detail}
    except Exception as e:
        return {"status": "error", "message": str(e)}


@router.post("/save")
async def save_schedule(request: Request):
    """保存日程"""
    try:
        student_id = get_student_id_from_request(request)
        data = await request.json()

        schedule_date = data.get("scheduleDate") or data.get("schedule_date")
        time = data.get("time")
        schedule_type = data.get("type", "warning")
        content = data.get("content", "")

        if not all([schedule_date, time]):
            return {"status": "error", "message": "参数不完整"}

        result = database.save_schedule(
            student_id, schedule_date, time, schedule_type, content
        )
        if result.get("success"):
            return {
                "status": "success",
                "message": "保存成功",
                "data": {"id": result.get("id")},
            }
        return {"status": "error", "message": "保存失败"}
    except HTTPException as e:
        return {"status": "error", "message": e.detail}
    except Exception as e:
        return {"status": "error", "message": str(e)}


@router.delete("/del")
async def delete_schedules(request: Request, ids: str):
    """删除日程"""
    try:
        get_student_id_from_request(request)

        if not ids:
            return {"status": "error", "message": "参数不完整"}

        success = database.delete_schedules(ids)
        if success:
            return {"status": "success", "message": "删除成功"}
        return {"status": "error", "message": "删除失败"}
    except HTTPException as e:
        return {"status": "error", "message": e.detail}
    except Exception as e:
        return {"status": "error", "message": str(e)}

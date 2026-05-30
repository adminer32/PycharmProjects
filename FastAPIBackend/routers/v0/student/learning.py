from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
import json
import database
import random

router = APIRouter(tags=["student-learning"])


class LearningStats(BaseModel):
    learning_hours: float
    homework_avg_score: float
    checkin_rate: float
    consecutive_checkin_days: int


class ActionScore(BaseModel):
    action_type: str
    current_score: float
    history_avg_score: float
    recent_scores: List[float]
    feedback: List[str]


class ShuttlecockSkill(BaseModel):
    skill_type: str
    personal_score: float
    class_avg_score: float


class ShuttlecockSkillsResponse(BaseModel):
    status: str
    data: dict


class WeeklyTrend(BaseModel):
    week: str
    learning_hours: float
    homework_score: float


class CheckinRecord(BaseModel):
    checkin_date: str
    checkin_type: str
    duration: int


@router.get("/stats/{student_id}", response_model=LearningStats)
def get_learning_stats(student_id: int):
    """获取学生学习统计数据"""
    # 获取学习时长
    result = database.get_student_learning(student_id)
    learning_hours = (
        result.get("info", {}).get("learning_hours", 0) if result.get("success") else 0
    )

    # 获取作业平均分
    homework_scores = database.get_homework_scores(student_id)
    if homework_scores.get("success") and homework_scores.get("scores"):
        scores = [s["score"] for s in homework_scores["scores"]]
        homework_avg_score = sum(scores) / len(scores) if scores else 0
    else:
        homework_avg_score = 0

    # 获取打卡统计
    checkin_result = database.get_checkin_stats(student_id)
    checkin_rate = (
        checkin_result.get("info", {}).get("checkin_rate", 0)
        if checkin_result.get("success")
        else 0
    )
    consecutive_days = (
        checkin_result.get("info", {}).get("consecutive_days", 0)
        if checkin_result.get("success")
        else 0
    )

    return LearningStats(
        learning_hours=learning_hours,
        homework_avg_score=round(homework_avg_score, 1),
        checkin_rate=checkin_rate,
        consecutive_checkin_days=consecutive_days,
    )


@router.get("/action-scores/{student_id}", response_model=List[ActionScore])
def get_action_scores(student_id: int):
    """获取动作能力评分"""
    result = database.get_action_scores(student_id)
    if not result.get("success"):
        return []

    action_scores = []

    def safe_float(val):
        try:
            return float(val) if val is not None else 0.0
        except (ValueError, TypeError):
            return 0.0

    for item in result.get("scores", []):
        recent_scores = item.get("recent_scores", [])
        if isinstance(recent_scores, str):
            try:
                recent_scores = json.loads(recent_scores)
            except:
                recent_scores = []

        feedback = item.get("feedback", [])
        if isinstance(feedback, str):
            try:
                feedback = json.loads(feedback)
            except:
                feedback = []

        action_scores.append(
            ActionScore(
                action_type=item["action_type"],
                current_score=safe_float(item["current_score"]),
                history_avg_score=safe_float(item["history_avg_score"]),
                recent_scores=[safe_float(s) for s in recent_scores],
                feedback=feedback,
            )
        )

    return action_scores


@router.get("/shuttlecock-skills/{student_id}")
def get_shuttlecock_skills(student_id: int):
    """获取毽球技能评估"""
    result = database.get_shuttlecock_skills(student_id)

    default_indicators = [
        {"name": "盘踢", "max": 100},
        {"name": "绷踢", "max": 100},
        {"name": "拐踢", "max": 100},
        {"name": "磕踢", "max": 100},
        {"name": "踏踢", "max": 100},
        {"name": "跳踢", "max": 100},
    ]

    if not result.get("success") or not result.get("skills"):
        return {
            "status": "success",
            "data": {"personal": [], "class_avg": [], "indicators": default_indicators},
        }

    skills = result.get("skills", [])
    personal = []
    class_avg = []

    for item in skills:
        personal.append(float(item.get("personal_score", 0)))
        class_avg.append(float(item.get("class_avg_score", 0)))

    return {
        "status": "success",
        "data": {
            "personal": personal,
            "class_avg": class_avg,
            "indicators": default_indicators,
        },
    }


@router.get("/weekly-trend/{student_id}", response_model=List[WeeklyTrend])
def get_weekly_trend(student_id: int):
    """获取周趋势数据"""
    result = database.get_weekly_trend(student_id)
    if not result.get("success"):
        return []

    def safe_float(val):
        try:
            return float(val) if val is not None else 0.0
        except (ValueError, TypeError):
            return 0.0

    return [
        WeeklyTrend(
            week=item["week"],
            learning_hours=safe_float(item["learning_hours"]),
            homework_score=safe_float(item["homework_score"]),
        )
        for item in result.get("trend", [])
    ]


@router.get("/checkin-records/{student_id}", response_model=List[CheckinRecord])
def get_checkin_records(student_id: int, month: Optional[int] = None):
    """获取打卡记录"""
    result = database.get_checkin_records(student_id, month)
    if not result.get("success"):
        return []

    records = []
    for item in result.get("records", []):
        records.append(
            CheckinRecord(
                checkin_date=str(item["checkin_date"]),
                checkin_type=item["checkin_type"],
                duration=item["duration"],
            )
        )

    return records


class PracticeRecordResponse(BaseModel):
    id: int
    student_id: int
    action_type: str
    video_url: str
    result_data: dict
    created_at: str


class SavePracticeRecordRequest(BaseModel):
    student_id: int
    action_type: str
    video_url: str
    result_data: dict


@router.post("/practice-record", response_model=dict)
def save_practice_record(request: SavePracticeRecordRequest):
    """保存练习记录"""
    result = database.save_practice_record(
        student_id=request.student_id,
        action_type=request.action_type,
        video_url=request.video_url,
        result_data=request.result_data,
    )
    if result.get("success"):
        return {"success": True, "record_id": result.get("record_id")}
    raise HTTPException(status_code=500, detail="保存失败")


@router.get(
    "/practice-records/{student_id}", response_model=List[PracticeRecordResponse]
)
def get_practice_records(student_id: int):
    """获取学生的所有练习记录"""
    result = database.get_practice_records(student_id)
    records = []
    for item in result.get("records", []):
        result_data = item.get("result_data", {})
        if isinstance(result_data, str):
            try:
                result_data = json.loads(result_data)
            except:
                result_data = {}
        records.append(
            PracticeRecordResponse(
                id=item["id"],
                student_id=item["student_id"],
                action_type=item["action_type"],
                video_url=item["video_url"],
                result_data=result_data,
                created_at=str(item["created_at"]),
            )
        )
    return records


@router.get("/practice-record/{record_id}", response_model=PracticeRecordResponse)
def get_practice_record(record_id: int):
    """获取指定练习记录"""
    result = database.get_practice_record_by_id(record_id)
    if not result.get("success"):
        raise HTTPException(status_code=404, detail="记录不存在")

    item = result.get("record", {})
    result_data = item.get("result_data", {})
    if isinstance(result_data, str):
        try:
            result_data = json.loads(result_data)
        except:
            result_data = {}
    return PracticeRecordResponse(
        id=item["id"],
        student_id=item["student_id"],
        action_type=item["action_type"],
        video_url=item["video_url"],
        result_data=result_data,
        created_at=str(item["created_at"]),
    )


@router.delete("/practice-record/{record_id}")
def delete_practice_record(record_id: int):
    """删除练习记录"""
    success = database.delete_practice_record(record_id)
    if success:
        return {"success": True}
    raise HTTPException(status_code=500, detail="删除失败")

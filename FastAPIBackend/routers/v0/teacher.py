from fastapi import APIRouter
from database.operation import query

router = APIRouter(prefix="/teacher", tags=["teacher"])


@router.get("/lessons")
async def get_teacher_lessons(class_id: int = None, teacher_id: int = None):
    """获取教师发布的课程列表"""
    sql = """
        SELECT 
            tl.id, tl.content, tl.video_url, tl.preview_image, tl.video_duration,
            tl.class_id, tl.teacher_id, tl.created_at,
            tp.name as teacher_name
        FROM teacher_lesson tl
        LEFT JOIN teacher_profile tp ON tl.teacher_id = tp.teacher_id
        WHERE 1=1
    """
    params = []

    if class_id is not None:
        sql += " AND tl.class_id = %s"
        params.append(class_id)

    if teacher_id is not None:
        sql += " AND tl.teacher_id = %s"
        params.append(teacher_id)

    sql += " ORDER BY tl.created_at DESC"

    rows = query(sql, tuple(params) if params else ())

    lessons = []
    for row in rows:
        lessons.append(
            {
                "id": str(row["id"]),
                "name": row["content"] or "未命名课程",
                "videoUrl": row["video_url"] or "",
                "coverImageUrl": row["preview_image"] or "",
                "duration": str(row["video_duration"]) + "秒"
                if row["video_duration"]
                else "0秒",
                "category": "教师课程",
                "description": row["content"] or "",
                "teacherId": row["teacher_id"],
                "teacherName": row["teacher_name"] or "未知教师",
                "classId": row["class_id"],
                "date": row["created_at"].strftime("%Y-%m-%d")
                if row["created_at"]
                else "",
            }
        )

    return {"status": "success", "data": lessons}


@router.get("/lessons/{lesson_id}")
async def get_teacher_lesson(lesson_id: int):
    """获取单个教师课程详情"""
    sql = """
        SELECT 
            tl.id, tl.content, tl.video_url, tl.preview_image, tl.video_duration,
            tl.class_id, tl.teacher_id, tl.created_at,
            tp.name as teacher_name
        FROM teacher_lesson tl
        LEFT JOIN teacher_profile tp ON tl.teacher_id = tp.teacher_id
        WHERE tl.id = %s
    """
    rows = query(sql, (lesson_id,))

    if not rows:
        return {"status": "error", "message": "课程不存在"}

    row = rows[0]
    return {
        "status": "success",
        "data": {
            "id": str(row["id"]),
            "name": row["content"] or "未命名课程",
            "videoUrl": row["video_url"] or "",
            "coverImageUrl": row["preview_image"] or "",
            "duration": str(row["video_duration"]) + "秒"
            if row["video_duration"]
            else "0秒",
            "category": "教师课程",
            "description": row["content"] or "",
            "teacherId": row["teacher_id"],
            "teacherName": row["teacher_name"] or "未知教师",
            "classId": row["class_id"],
            "date": row["created_at"].strftime("%Y-%m-%d") if row["created_at"] else "",
        },
    }

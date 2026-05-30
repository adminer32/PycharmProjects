from fastapi import APIRouter, Request
from .v0 import v0
from .v0 import video
import json

router = APIRouter(prefix="/api")

router.include_router(v0.router)
router.include_router(video.router)


@router.post("/analysis/save")
async def save_analysis_data(request: Request):
    """保存动作分析数据（存储原始JSON）"""
    from database.operation import change, insert_and_get_id

    try:
        body = await request.json()
        student_id = body.get("student_id")
        analysis_data = body.get("analysis_data", {})

        sql = """
            INSERT INTO motion_analysis_data 
            (student_id, analysis_data, created_at)
            VALUES (%s, %s, NOW())
        """
        new_id = insert_and_get_id(sql, (student_id, json.dumps(analysis_data)))

        if new_id:
            return {
                "status": "success",
                "message": "分析数据保存成功",
                "data": {"id": new_id},
            }
        return {"status": "error", "message": "保存失败"}
    except Exception as e:
        return {"status": "error", "message": str(e)}


def parse_motion_data_field(val):
    """解析动作数据字段"""
    if val is None:
        return []
    if isinstance(val, list):
        return val
    if isinstance(val, str):
        try:
            return json.loads(val)
        except:
            return []
    return []


@router.get("/analysis/query")
async def query_analysis_data(id: int):
    """查询动作分析数据"""
    from database.operation import query

    try:
        sql = """
            SELECT id, student_id, analysis_data, created_at
            FROM motion_analysis_data
            WHERE id = %s
        """
        rows = query(sql, (id,))

        if rows and len(rows) > 0:
            row = rows[0]
            return {
                "status": "success",
                "data": {
                    "id": row["id"],
                    "student_id": row["student_id"],
                    "analysis_data": json.loads(row["analysis_data"])
                    if row["analysis_data"]
                    else {},
                    "created_at": str(row["created_at"]) if row["created_at"] else "",
                },
            }
        return {"status": "error", "message": "数据不存在"}
    except Exception as e:
        return {"status": "error", "message": str(e)}


@router.get("/analysis/history")
async def query_analysis_history(student_id: int):
    """查询学生的分析历史记录"""
    from database.operation import query

    try:
        sql = """
            SELECT id, student_id, analysis_data, created_at
            FROM motion_analysis_data
            WHERE student_id = %s
            ORDER BY created_at DESC
            LIMIT 50
        """
        rows = query(sql, (student_id,))

        records = []
        for row in rows:
            analysis_data = {}
            if row["analysis_data"]:
                try:
                    analysis_data = (
                        json.loads(row["analysis_data"])
                        if isinstance(row["analysis_data"], str)
                        else row["analysis_data"]
                    )
                except:
                    analysis_data = {}

            records.append(
                {
                    "id": row["id"],
                    "student_id": row["student_id"],
                    "analysis_data": analysis_data,
                    "created_at": str(row["created_at"]) if row["created_at"] else "",
                }
            )

        return {"status": "success", "data": {"records": records}}
    except Exception as e:
        return {"status": "error", "message": str(e), "data": {"records": []}}


@router.get("/analysis/query")
async def query_analysis_data(task_id: str):
    """查询动作分析数据"""
    from database.operation import query

    try:
        sql = """
            SELECT id, task_id, self_left_x_data, self_left_y_data, 
                   self_right_x_data, self_right_y_data, created_at
            FROM motion_analysis_data
            WHERE task_id = %s
            ORDER BY created_at DESC
            LIMIT 1
        """
        rows = query(sql, (task_id,))

        if rows and len(rows) > 0:
            row = rows[0]
            return {
                "status": "success",
                "data": {
                    "id": row[0],
                    "task_id": row[1],
                    "self_left_x_data": json.loads(row[2]) if row[2] else [],
                    "self_left_y_data": json.loads(row[3]) if row[3] else [],
                    "self_right_x_data": json.loads(row[4]) if row[4] else [],
                    "self_right_y_data": json.loads(row[5]) if row[5] else [],
                    "created_at": str(row[6]) if row[6] else "",
                },
            }
        return {"status": "error", "message": "数据不存在"}
    except Exception as e:
        return {"status": "error", "message": str(e)}


VIDEO_BASE_PATH = "/home/mrliao/下载/GVHMR/InputVideo/视频"


def get_video_duration(filepath: str) -> str:
    """获取视频时长（秒转换为 MM:SS 格式）"""
    import os

    try:
        size = os.path.getsize(filepath)
        # 粗略估算：假设码率约 1MB/s
        seconds = size // (1024 * 1024)
        minutes = seconds // 60
        secs = seconds % 60
        return f"{minutes:02d}:{secs:02d}"
    except:
        return "00:00"


@router.post("/course/query")
async def query_courses(request: Request):
    """根据文件夹结构返回课程视频列表"""
    import os
    import re

    body = await request.json()
    param = body.get("param", {})
    parent_category_name = param.get("parent_category_name", "")

    # 定义父分类映射
    parent_mapping = {
        "基本课程": ["基本动作", "基本技术", "比赛规则", "战术运用"],
        "综合课程": ["单人专项动作", "系列课程"],
    }

    folders_to_scan = parent_mapping.get(parent_category_name, [])

    result = []

    if not os.path.exists(VIDEO_BASE_PATH):
        return {"status": "error", "message": f"视频目录不存在: {VIDEO_BASE_PATH}"}

    for folder_name in os.listdir(VIDEO_BASE_PATH):
        folder_path = os.path.join(VIDEO_BASE_PATH, folder_name)

        # 跳过文件，只处理文件夹
        if not os.path.isdir(folder_path):
            continue

        # 如果指定了父分类，只扫描对应的子文件夹
        if folders_to_scan and folder_name not in folders_to_scan:
            continue

        # 确定父分类
        for parent, children in parent_mapping.items():
            if folder_name in children:
                parent_cat = parent
                break
        else:
            parent_cat = folder_name

        # 扫描该文件夹下的视频文件
        courses = []
        for filename in os.listdir(folder_path):
            if filename.endswith((".mp4", ".avi", ".mov", ".mkv")):
                filepath = os.path.join(folder_path, filename)
                video_url = f"/videos/{folder_name}/{filename}"
                duration = get_video_duration(filepath)

                # 从文件名提取课程名称
                name = (
                    filename.replace(".mp4", "")
                    .replace(".avi", "")
                    .replace(".mov", "")
                    .replace(".mkv", "")
                )

                courses.append(
                    {
                        "id": len(courses) + 1,
                        "name": name,
                        "teacherName": "教练",
                        "coverImageUrl": "",
                        "enabled": True,
                        "createTime": "",
                        "content": "",
                        "description": "",
                        "videoUrl": video_url,
                        "videoDuration": duration,
                        "levelName": "初级",
                    }
                )

        if courses:
            result.append(
                {
                    "subCategoryId": folder_name,
                    "subCategoryName": folder_name,
                    "subCategoryCourse": courses,
                }
            )

    return {
        "status": "success",
        "data": {
            "list": result,
            "total": sum(len(r["subCategoryCourse"]) for r in result),
        },
    }


@router.get("/course/getNoteById")
async def get_course_note(id: int):
    """根据课程ID获取AI笔记"""
    from database.operation import query

    sql = """
        SELECT id, task_id, video_url, title, auto_chapters, 
               summarization, meeting_assistance, personal_note, task_status, create_time
        FROM ai_transcription_task 
        WHERE id = %s AND deleted = 0
    """

    try:
        rows = query(sql, (id,))
        if rows:
            row = rows[0]
            return {
                "status": "success",
                "data": {
                    "id": row["id"],
                    "taskId": row["task_id"],
                    "videoUrl": row["video_url"] or "",
                    "title": row["title"] or "",
                    "autoChapters": row["auto_chapters"] or "",
                    "summarization": row["summarization"] or "",
                    "meetingAssistance": row["meeting_assistance"] or "",
                    "personalNote": row["personal_note"] or "",
                    "taskStatus": row["task_status"] or "进行中",
                    "createTime": str(row["create_time"]) if row["create_time"] else "",
                },
            }
        return {"status": "error", "message": "笔记不存在"}
    except Exception as e:
        return {"status": "error", "message": str(e)}


@router.get("/video/progress")
async def get_video_progress(user_id: int):
    """获取用户所有视频的观看进度"""
    from database.operation import query

    sql = "SELECT video_path, progress, update_time FROM video_progress WHERE user_id = %s"
    try:
        rows = query(sql, (user_id,))
        result = {}
        for row in rows:
            result[row["video_path"]] = {
                "progress": row["progress"],
                "updateTime": str(row["update_time"]) if row["update_time"] else "",
            }
        return {"status": "success", "data": result}
    except Exception as e:
        return {"status": "error", "message": str(e)}


@router.post("/video/progress")
async def save_video_progress(request: Request):
    """保存视频观看进度"""
    from database.operation import change

    body = await request.json()
    user_id = body.get("user_id")
    video_path = body.get("video_path")
    progress = body.get("progress")

    if not user_id or not video_path or progress is None:
        return {"status": "error", "message": "参数不完整"}

    # 检查进度记录是否存在
    from database.operation import query

    check_sql = "SELECT id FROM video_progress WHERE user_id = %s AND video_path = %s"
    rows = query(check_sql, (user_id, video_path))

    if rows:
        sql = "UPDATE video_progress SET progress = %s, update_time = NOW() WHERE user_id = %s AND video_path = %s"
        success = change(sql, (progress, user_id, video_path))
    else:
        sql = "INSERT INTO video_progress (user_id, video_path, progress, create_time, update_time) VALUES (%s, %s, %s, NOW(), NOW())"
        success = change(sql, (user_id, video_path, progress))

    if success:
        return {"status": "success", "message": "进度保存成功"}
    return {"status": "error", "message": "进度保存失败"}


@router.api_route("/ai/{path:path}", methods=["GET", "POST", "PUT", "DELETE"])
async def ai_route_forward(path: str, request: Request):
    """将 /ai/* 路径适配到 /v0/ai/*"""
    from database.operation import query, change, insert_and_get_id
    from datetime import datetime

    body = await request.json() if request.method in ["POST", "PUT"] else {}

    if path == "queryPersonalNotes":
        try:
            param = body.get("param", {})
            sql = "SELECT id, task_id, create_time, video_url, title, auto_chapters, summarization, meeting_assistance, personal_note, task_status FROM ai_transcription_task WHERE 1=1"
            params = []

            if param.get("title"):
                sql += " AND title LIKE %s"
                params.append(f"%{param.get('title')}%")
            if param.get("id"):
                sql += " AND id = %s"
                params.append(param.get("id"))
            if param.get("task_status"):
                sql += " AND task_status = %s"
                params.append(param.get("task_status"))
            if param.get("task_id"):
                sql += " AND task_id = %s"
                params.append(param.get("task_id"))

            sql += " ORDER BY create_time DESC"
            rows = query(sql, tuple(params))
            notes = []
            for row in rows:
                notes.append(
                    {
                        "id": row["id"],
                        "taskId": row["task_id"],
                        "createTime": str(row["create_time"])
                        if row["create_time"]
                        else "",
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

    elif path == "addTranscriptionTask":
        try:
            title = body.get("title", "未命名任务")
            video_url = body.get("video_url", "")
            task_id = f"task_{datetime.now().strftime('%Y%m%d%H%M%S')}"

            sql = "INSERT INTO ai_transcription_task (task_id, title, video_url, task_status, create_time) VALUES (%s, %s, %s, %s, NOW())"
            new_id = insert_and_get_id(sql, (task_id, title, video_url, "进行中"))

            if new_id:
                return {
                    "status": "success",
                    "data": {
                        "id": new_id,
                        "taskId": task_id,
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

    elif path == "updateNote":
        try:
            note_id = body.get("id")
            title = body.get("title")
            personal_note = body.get("personal_note", "")

            if note_id and title:
                sql = "UPDATE ai_transcription_task SET title = %s, personal_note = %s WHERE id = %s"
                success = change(sql, (title, personal_note, note_id))
                if success:
                    return {"status": "success", "message": "更新成功"}
            return {"status": "error", "message": "更新失败"}
        except Exception as e:
            return {"status": "error", "message": str(e)}

    elif path == "del":
        try:
            ids = request.query_params.get("ids", "")
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

    return {"status": "error", "message": "Unknown endpoint"}


@router.get("/physique/query")
async def query_physique(student_id: int):
    """查询学生体质数据"""
    from database.operation import query

    try:
        sql = """
            SELECT * FROM student_physique WHERE student_id = %s LIMIT 1
        """
        rows = query(sql, (student_id,))
        if rows:
            return {"status": "success", "data": rows[0]}
        return {"status": "success", "data": None, "message": "体质数据不存在"}
    except Exception as e:
        return {"status": "error", "message": str(e)}


@router.post("/physique/create")
async def create_physique(request: Request):
    """创建学生体质数据"""
    from database.operation import insert_and_get_id

    try:
        body = await request.json()
        student_id = body.get("student_id")

        sql = """
            INSERT INTO student_physique (
                student_id, weight, bmi, fat_percentage, skeletal_muscle_mass,
                visceral_fat_level, limb_skeletal_muscle_index, estimated_waist_hip_ratio,
                body_type, body_shape, basal_metabolism_rate, moisture_rate,
                bone_salt_amount, protein_percentage, lean_body_mass, body_age,
                heart_rate, segment_moisture, segment_protein, segment_fat_mass,
                segment_bone_salt, segment_fat_total, segment_fat_right_upper,
                segment_fat_left_upper, segment_fat_trunk, segment_fat_right_lower,
                segment_fat_left_lower, segment_skeletal_muscle_total,
                segment_skeletal_right_upper, segment_skeletal_left_upper,
                segment_skeletal_trunk, segment_skeletal_right_lower,
                segment_skeletal_left_lower
            ) VALUES (
                %(student_id)s, %(weight)s, %(bmi)s, %(fat_percentage)s, %(skeletal_muscle_mass)s,
                %(visceral_fat_level)s, %(limb_skeletal_muscle_index)s, %(estimated_waist_hip_ratio)s,
                %(body_type)s, %(body_shape)s, %(basal_metabolism_rate)s, %(moisture_rate)s,
                %(bone_salt_amount)s, %(protein_percentage)s, %(lean_body_mass)s, %(body_age)s,
                %(heart_rate)s, %(segment_moisture)s, %(segment_protein)s, %(segment_fat_mass)s,
                %(segment_bone_salt)s, %(segment_fat_total)s, %(segment_fat_right_upper)s,
                %(segment_fat_left_upper)s, %(segment_fat_trunk)s, %(segment_fat_right_lower)s,
                %(segment_fat_left_lower)s, %(segment_skeletal_muscle_total)s,
                %(segment_skeletal_right_upper)s, %(segment_skeletal_left_upper)s,
                %(segment_skeletal_trunk)s, %(segment_skeletal_right_lower)s,
                %(segment_skeletal_left_lower)s
            )
        """
        new_id = insert_and_get_id(sql, body)
        if new_id:
            return {
                "status": "success",
                "message": "体质数据创建成功",
                "data": {"id": new_id},
            }
        return {"status": "error", "message": "创建失败"}
    except Exception as e:
        return {"status": "error", "message": str(e)}


@router.put("/physique/update")
async def update_physique(request: Request):
    """更新学生体质数据"""
    from database.operation import change

    try:
        body = await request.json()
        student_id = body.get("student_id")

        sql = """
            UPDATE student_physique SET
                weight = %(weight)s, bmi = %(bmi)s, fat_percentage = %(fat_percentage)s,
                skeletal_muscle_mass = %(skeletal_muscle_mass)s, visceral_fat_level = %(visceral_fat_level)s,
                limb_skeletal_muscle_index = %(limb_skeletal_muscle_index)s,
                estimated_waist_hip_ratio = %(estimated_waist_hip_ratio)s,
                body_type = %(body_type)s, body_shape = %(body_shape)s,
                basal_metabolism_rate = %(basal_metabolism_rate)s, moisture_rate = %(moisture_rate)s,
                bone_salt_amount = %(bone_salt_amount)s, protein_percentage = %(protein_percentage)s,
                lean_body_mass = %(lean_body_mass)s, body_age = %(body_age)s,
                heart_rate = %(heart_rate)s, segment_moisture = %(segment_moisture)s,
                segment_protein = %(segment_protein)s, segment_fat_mass = %(segment_fat_mass)s,
                segment_bone_salt = %(segment_bone_salt)s, segment_fat_total = %(segment_fat_total)s,
                segment_fat_right_upper = %(segment_fat_right_upper)s,
                segment_fat_left_upper = %(segment_fat_left_upper)s,
                segment_fat_trunk = %(segment_fat_trunk)s,
                segment_fat_right_lower = %(segment_fat_right_lower)s,
                segment_fat_left_lower = %(segment_fat_left_lower)s,
                segment_skeletal_muscle_total = %(segment_skeletal_muscle_total)s,
                segment_skeletal_right_upper = %(segment_skeletal_right_upper)s,
                segment_skeletal_left_upper = %(segment_skeletal_left_upper)s,
                segment_skeletal_trunk = %(segment_skeletal_trunk)s,
                segment_skeletal_right_lower = %(segment_skeletal_right_lower)s,
                segment_skeletal_left_lower = %(segment_skeletal_left_lower)s
            WHERE student_id = %(student_id)s
        """
        success = change(sql, body)
        if success:
            return {"status": "success", "message": "体质数据更新成功"}
        return {"status": "error", "message": "更新失败"}
    except Exception as e:
        return {"status": "error", "message": str(e)}

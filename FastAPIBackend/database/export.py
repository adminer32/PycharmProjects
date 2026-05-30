from . import operation
from passlib.hash import sha512_crypt
import random
from .config import table_config as config

user_table = config.users.name
roles = config.users.roles
student_table = config.students.name
student_table_columns = config.students.columns


def check_data_validity(username: str, password: str, role: str) -> bool:
    if not username or not password:
        return False

    role = role.upper()
    if role not in roles:
        return False

    return True


def generate_salt() -> str:
    chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_=+[]{}|;:'\",<.>/?！￥……（）——【】、；：‘’“”，。《》？あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをんがぎぐげござじずぜぞだぢづでどばびぶべぼぱぴぷぺぽぁぃぅぇぉゃゅょっー"
    salt = "".join(random.choice(chars) for _ in range(16))

    return salt


def check_userpassword(username: str, password: str, role: str = "student") -> bool:
    if not check_data_validity(username, password, role):
        return False

    role = role.upper()
    sql = "SELECT * FROM {table_name} WHERE username=%s AND role=%s AND active=TRUE".format(
        table_name=user_table
    )
    params = (username, role)
    rows = operation.query(sql, params)

    if len(rows) < 1:
        return False

    user = rows[0]
    password_hash = user["password_hash"]
    password_salt = user["password_salt"]

    if password_hash.startswith("$6$"):
        return sha512_crypt.verify(password, password_hash)
    else:
        import hashlib

        calc_hash = hashlib.sha512((password + password_salt).encode()).hexdigest()
        return password_hash == calc_hash


def insert_student(
    username: str, password: str, active: bool = False, profile: str = {}
) -> bool:
    if not check_data_validity(username, password, "STUDENT"):
        return False

    if len(profile) < 1:
        return False

    salt = generate_salt()
    password_hash = hashlib.sha512((password + salt).encode()).hexdigest()
    real_name = None
    gender = None
    grade = None
    age = None
    major = None
    class_id = None
    email = None
    avatar = None
    bio = profile.get("bio", "这个人不懒，什么都写了。")

    try:
        student_id = profile["student_id"]
        real_name = profile["real_name"]
        gender = profile["gender"]
        age = profile["age"]
        grade = profile["grade"]
        major = profile["major"]
        class_id = profile["class_id"]
        email = profile["email"]
        avatar = profile["avatar"]
    except:
        return False

    sql = "INSERT INTO {table_name} (username, password_hash, password_salt, role, active) VALUES (%s, %s, %s, 'STUDENT', %s)".format(
        table_name=user_table
    )
    params = (username, password_hash, salt, active)
    success = operation.change(sql, params)

    if not success:
        return False

    sql = "SELECT id FROM {table_name} WHERE username=%s".format(table_name=user_table)
    params = (username,)
    rows = operation.query(sql, params)

    if len(rows) < 1:
        return False

    user_id = rows[0]["id"]

    sql = "INSERT INTO {table_name} (id, student_id, real_name, gender, age, grade, major, class_id, email, avatar, bio) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)".format(
        table_name=student_table
    )
    params = (
        user_id,
        student_id,
        real_name,
        gender,
        age,
        grade,
        major,
        class_id,
        email,
        avatar,
        bio,
    )
    success = operation.change(sql, params)

    return success


def get_student_by_id(user_id: int) -> dict:
    sql = "SELECT * FROM {table_name} WHERE id=%s".format(table_name=student_table)

    params = (user_id,)
    rows = operation.query(sql, params)

    if len(rows) > 0:
        return {"success": True, "info": rows[0]}
    else:
        return {"success": False, "info": None}


def get_student_by_username(username: str) -> dict:
    sql = "SELECT * FROM {table_name} WHERE username=%s".format(table_name=user_table)

    params = (username,)
    rows = operation.query(sql, params)

    if len(rows) < 1:
        return {"success": False, "info": None}

    user_id = rows[0]["id"]
    sql = "SELECT * FROM {table_name} WHERE id=%s".format(table_name=student_table)
    params = (user_id,)
    rows = operation.query(sql, params)
    rows[0]["user_id"] = user_id

    if len(rows) > 0:
        return {"success": True, "info": rows[0]}
    else:
        return {"success": False, "info": None}


def get_user_by_id(user_id: int) -> dict:
    sql = "SELECT * FROM {table_name} WHERE id=%s".format(table_name=user_table)

    params = (user_id,)
    rows = operation.query(sql, params)

    if len(rows) > 0:
        return {"success": True, "info": rows[0]}
    else:
        return {"success": False, "info": None}


def get_users() -> dict:
    sql = "SELECT * FROM {table_name} WHERE role!='ADMIN'".format(table_name=user_table)

    rows = operation.query(sql, ())

    if len(rows) > 0:
        return {"success": True, "info": rows}
    else:
        return {"success": False, "info": None}


def activate_user(user_id: int) -> bool:
    sql = "UPDATE {table_name} SET active=TRUE WHERE id=%s".format(
        table_name=user_table
    )

    params = (user_id,)
    success = operation.change(sql, params)
    return success


def deactivate_user(user_id: int) -> bool:
    sql = "UPDATE {table_name} SET active=FALSE WHERE id=%s".format(
        table_name=user_table
    )

    params = (user_id,)
    success = operation.change(sql, params)
    return success


def update_user_password(user_id: int, new_password: str) -> bool:
    legal_characters = "0123456789abcdef"
    new_password = new_password.lower()
    if len(new_password) != 128 or any(c not in legal_characters for c in new_password):
        return False

    sql = "SELECT * FROM {table_name} WHERE id=%s".format(table_name=user_table)
    params = (user_id,)
    rows = operation.query(sql, params)

    if len(rows) < 1:
        return False

    salt = generate_salt()
    new_password_hash = hashlib.sha512((new_password + salt).encode()).hexdigest()

    sql = (
        "UPDATE {table_name} SET password_hash=%s password_salt=%s WHERE id=%s".format(
            table_name=user_table
        )
    )
    params = (new_password_hash, salt, user_id)
    success = operation.change(sql, params)
    return success


def update_student_profile(user_id: int, profile: dict) -> bool:
    if len(profile) < 1 or not isinstance(profile, dict):
        return False

    sql = "SELECT * FROM {table_name} WHERE id=%s AND role='STUDENT'".format(
        table_name=user_table
    )
    params = (user_id,)
    rows = operation.query(sql, params)

    if len(rows) < 1:
        return False

    for key in profile:
        if key not in student_table_columns:
            return False

    sql = "UPDATE {table_name} SET {columns} WHERE id=%s".format(
        table_name=student_table, columns=", ".join([key + "=%s" for key in profile])
    )
    params = tuple(profile.values()) + (user_id,)
    success = operation.change(sql, params)
    return success


def delete_user_by_id(user_id: int) -> bool:
    sql = "DELETE from {table_name} WHERE id=%s AND role!='ADMIN'".format(
        table_name=user_table
    )

    params = (user_id,)
    success = operation.change(sql, params)
    return success


def delete_user_by_username(username: str) -> bool:
    sql = "DELETE from {table_name} WHERE username=%s AND role!='ADMIN'".format(
        table_name=user_table
    )

    params = (username,)
    success = operation.change(sql, params)
    return success


def insert_jwt_blacklist(token_id: str, access_token: str, refresh_token: str) -> bool:
    sql = "INSERT INTO jwt_blacklist (token_id, access_token, refresh_token) VALUES (%s, %s, %s)"
    params = (token_id, access_token, refresh_token)
    success = operation.change(sql, params)
    return success


def delete_jwt_blacklist(token_id: str) -> bool:
    sql = "DELETE FROM jwt_blacklist WHERE token_id=%s"
    params = (token_id,)
    success = operation.change(sql, params)
    return success


def get_jwt_blacklist(token_id: str) -> dict:
    sql = "SELECT * FROM jwt_blacklist WHERE token_id=%s"
    params = (token_id,)
    rows = operation.query(sql, params)

    if len(rows) > 0:
        return {"success": True, "info": rows[0]}
    else:
        return {"success": False, "info": None}


def insert_jwt_ip(token_id: str, ip_address: str) -> bool:
    sql = "INSERT INTO jwt_ip (token_id, ip_address) VALUES (%s, %s)"
    params = (token_id, ip_address)
    success = operation.change(sql, params)
    return success


def delete_jwt_ip(token_id: str) -> bool:
    sql = "DELETE FROM jwt_ip WHERE token_id=%s"
    params = (token_id,)
    success = operation.change(sql, params)
    return success


def get_jwt_ip(token_id: str) -> dict:
    sql = "SELECT * FROM jwt_ip WHERE token_id=%s"
    params = (token_id,)
    rows = operation.query(sql, params)

    if len(rows) > 0:
        return {"success": True, "info": rows[0]}
    else:
        return {"success": False, "info": None}


# ============ 学情分析相关函数 ============


def get_student_learning(student_id: int) -> dict:
    sql = "SELECT * FROM student_learning WHERE student_id=%s"
    params = (student_id,)
    rows = operation.query(sql, params)
    if len(rows) > 0:
        total_hours = sum(float(row.get("learning_hours", 0) or 0) for row in rows)
        rows[0]["learning_hours"] = total_hours
        return {"success": True, "info": rows[0]}
    else:
        return {"success": False, "info": None}


def get_homework_scores(student_id: int) -> dict:
    sql = (
        "SELECT * FROM homework_scores WHERE student_id=%s ORDER BY homework_date DESC"
    )
    params = (student_id,)
    rows = operation.query(sql, params)
    if len(rows) > 0:
        return {"success": True, "scores": rows}
    else:
        return {"success": False, "scores": []}


def get_checkin_stats(student_id: int) -> dict:
    sql = "SELECT * FROM student_checkin WHERE student_id=%s ORDER BY checkin_date DESC"
    params = (student_id,)
    rows = operation.query(sql, params)

    if len(rows) > 0:
        import datetime

        now = datetime.datetime.now()
        current_month = now.month
        current_year = now.year
        days_in_month = (
            (
                datetime.date(current_year, current_month + 1, 1)
                - datetime.date(current_year, current_month, 1)
            ).days
            if current_month < 12
            else (
                datetime.date(current_year + 1, 1, 1)
                - datetime.date(current_year, current_month, 1)
            ).days
        )

        current_month_dates = set()
        for row in rows:
            checkin_date = row.get("checkin_date")
            if (
                checkin_date
                and checkin_date.year == current_year
                and checkin_date.month == current_month
            ):
                current_month_dates.add(checkin_date.day)

        checkin_rate = (
            (len(current_month_dates) / days_in_month) * 100 if days_in_month > 0 else 0
        )

        consecutive = 0
        dates = sorted([r["checkin_date"] for r in rows], reverse=True)
        if dates:
            consecutive = 1
            for i in range(1, len(dates)):
                diff = (dates[i - 1] - dates[i]).days
                if diff == 1:
                    consecutive += 1
                else:
                    break

        return {
            "success": True,
            "info": {"checkin_rate": checkin_rate, "consecutive_days": consecutive},
        }
    else:
        return {"success": False, "info": {"checkin_rate": 0, "consecutive_days": 0}}


def get_action_scores(student_id: int) -> dict:
    sql = "SELECT * FROM action_scores WHERE student_id=%s"
    params = (student_id,)
    rows = operation.query(sql, params)
    if len(rows) > 0:
        return {"success": True, "scores": rows}
    else:
        return {"success": False, "scores": []}


def get_shuttlecock_skills(student_id: int) -> dict:
    sql = "SELECT * FROM shuttlecock_skills WHERE student_id=%s"
    params = (student_id,)
    rows = operation.query(sql, params)
    if len(rows) > 0:
        return {"success": True, "skills": rows}
    else:
        return {"success": False, "skills": []}


def get_checkin_records(student_id: int, month: int = None) -> dict:
    if month:
        sql = "SELECT * FROM student_checkin WHERE student_id=%s AND MONTH(checkin_date)=%s ORDER BY checkin_date DESC"
        params = (student_id, month)
    else:
        sql = "SELECT * FROM student_checkin WHERE student_id=%s ORDER BY checkin_date DESC"
        params = (student_id,)
    rows = operation.query(sql, params)
    if len(rows) > 0:
        return {"success": True, "records": rows}
    else:
        return {"success": False, "records": []}


def get_weekly_trend(student_id: int) -> dict:
    # 获取近4周的统计数据
    sql = """SELECT 
                DATE_FORMAT(homework_date, '%%Y-%%u') as week,
                AVG(score) as homework_score,
                (SELECT SUM(duration)/3600 FROM student_checkin WHERE student_id=%s AND DATE_FORMAT(checkin_date, '%%Y-%%u') = DATE_FORMAT(homework_date, '%%Y-%%u')) as learning_hours
            FROM homework_scores 
            WHERE student_id=%s 
            GROUP BY DATE_FORMAT(homework_date, '%%Y-%%u')
            ORDER BY week ASC
            LIMIT 4"""
    params = (student_id, student_id)
    rows = operation.query(sql, params)
    if len(rows) > 0:
        return {"success": True, "trend": rows}
    else:
        weeks = ["第1周", "第2周", "第3周", "第4周"]
        trend = [
            {
                "week": w,
                "learning_hours": random.randint(5, 20),
                "homework_score": random.randint(70, 95),
            }
            for w in weeks
        ]
        return {"success": True, "trend": trend}


# ============ 练习记录相关函数 ============


def save_practice_record(
    student_id: int, action_type: str, video_url: str, result_data: dict
) -> dict:
    """保存练习记录"""
    import json

    sql = """INSERT INTO practice_records (student_id, action_type, video_url, result_data, created_at) 
             VALUES (%s, %s, %s, %s, NOW())"""
    params = (student_id, action_type, video_url, json.dumps(result_data))
    record_id = operation.insert_and_get_id(sql, params)
    if record_id:
        return {"success": True, "record_id": record_id}
    return {"success": False, "record_id": None}


def get_practice_records(student_id: int) -> dict:
    """获取学生的所有练习记录"""
    sql = """SELECT id, student_id, action_type, video_url, result_data, created_at 
             FROM practice_records WHERE student_id=%s ORDER BY created_at DESC"""
    params = (student_id,)
    rows = operation.query(sql, params)
    if len(rows) > 0:
        return {"success": True, "records": rows}
    return {"success": True, "records": []}


def get_practice_record_by_id(record_id: int) -> dict:
    """获取指定练习记录"""
    sql = """SELECT id, student_id, action_type, video_url, result_data, created_at 
             FROM practice_records WHERE id=%s"""
    params = (record_id,)
    rows = operation.query(sql, params)
    if len(rows) > 0:
        return {"success": True, "record": rows[0]}
    return {"success": False, "record": None}


def delete_practice_record(record_id: int) -> bool:
    """删除练习记录"""
    sql = "DELETE FROM practice_records WHERE id=%s"
    params = (record_id,)
    success = operation.change(sql, params)
    return success


# ============ 日程相关函数 ============


def get_schedules_by_month(student_id: int, year_month: str) -> dict:
    """获取学生指定月份的日程"""
    sql = """SELECT id, student_id, schedule_date, time, type, content, created_at 
             FROM schedules WHERE student_id=%s AND DATE_FORMAT(schedule_date, '%%Y-%%m')=%s 
             ORDER BY schedule_date, time"""
    params = (student_id, year_month)
    rows = operation.query(sql, params)

    schedule_map = {}
    for row in rows:
        date_str = str(row["schedule_date"])
        if date_str not in schedule_map:
            schedule_map[date_str] = []
        schedule_map[date_str].append(
            {
                "id": row["id"],
                "time": row["time"],
                "type": row["type"] or "warning",
                "content": row["content"] or "",
            }
        )
    return {"success": True, "data": schedule_map}


def save_schedule(
    student_id: int, schedule_date: str, time: str, schedule_type: str, content: str
) -> dict:
    """保存日程"""
    sql = """INSERT INTO schedules (student_id, schedule_date, time, type, content) 
             VALUES (%s, %s, %s, %s, %s)"""
    params = (student_id, schedule_date, time, schedule_type, content)
    schedule_id = operation.insert_and_get_id(sql, params)
    if schedule_id:
        return {"success": True, "id": schedule_id}
    return {"success": False, "id": None}


def update_schedule(
    schedule_id: int, time: str, schedule_type: str, content: str
) -> bool:
    """更新日程"""
    sql = """UPDATE schedules SET time=%s, type=%s, content=%s WHERE id=%s"""
    params = (time, schedule_type, content, schedule_id)
    success = operation.change(sql, params)
    return success


def delete_schedules(ids: str) -> bool:
    """删除日程（支持批量）"""
    sql = f"DELETE FROM schedules WHERE id IN ({ids})"
    success = operation.change(sql, ())
    return success

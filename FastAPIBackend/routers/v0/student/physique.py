from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse
from utils.jwt_manager import JWTManager
from database import operation

router = APIRouter()
jwt_manager = JWTManager()


def get_physique_by_student_id(student_id: int) -> dict:
    """根据学生ID获取体质数据"""
    sql = "SELECT * FROM student_physique WHERE student_id = %s"
    rows = operation.query(sql, (student_id,))
    if rows:
        return {"success": True, "info": rows[0]}
    return {"success": False, "message": "体质数据不存在"}


def insert_physique(physique_data: dict) -> bool:
    """插入体质数据"""
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
    params = {
        "student_id": physique_data.get("student_id"),
        "weight": physique_data.get("weight"),
        "bmi": physique_data.get("bmi"),
        "fat_percentage": physique_data.get("fat_percentage"),
        "skeletal_muscle_mass": physique_data.get("skeletal_muscle_mass"),
        "visceral_fat_level": physique_data.get("visceral_fat_level"),
        "limb_skeletal_muscle_index": physique_data.get("limb_skeletal_muscle_index"),
        "estimated_waist_hip_ratio": physique_data.get("estimated_waist_hip_ratio"),
        "body_type": physique_data.get("body_type"),
        "body_shape": physique_data.get("body_shape"),
        "basal_metabolism_rate": physique_data.get("basal_metabolism_rate"),
        "moisture_rate": physique_data.get("moisture_rate"),
        "bone_salt_amount": physique_data.get("bone_salt_amount"),
        "protein_percentage": physique_data.get("protein_percentage"),
        "lean_body_mass": physique_data.get("lean_body_mass"),
        "body_age": physique_data.get("body_age"),
        "heart_rate": physique_data.get("heart_rate"),
        "segment_moisture": physique_data.get("segment_moisture"),
        "segment_protein": physique_data.get("segment_protein"),
        "segment_fat_mass": physique_data.get("segment_fat_mass"),
        "segment_bone_salt": physique_data.get("segment_bone_salt"),
        "segment_fat_total": physique_data.get("segment_fat_total"),
        "segment_fat_right_upper": physique_data.get("segment_fat_right_upper"),
        "segment_fat_left_upper": physique_data.get("segment_fat_left_upper"),
        "segment_fat_trunk": physique_data.get("segment_fat_trunk"),
        "segment_fat_right_lower": physique_data.get("segment_fat_right_lower"),
        "segment_fat_left_lower": physique_data.get("segment_fat_left_lower"),
        "segment_skeletal_muscle_total": physique_data.get(
            "segment_skeletal_muscle_total"
        ),
        "segment_skeletal_right_upper": physique_data.get(
            "segment_skeletal_right_upper"
        ),
        "segment_skeletal_left_upper": physique_data.get("segment_skeletal_left_upper"),
        "segment_skeletal_trunk": physique_data.get("segment_skeletal_trunk"),
        "segment_skeletal_right_lower": physique_data.get(
            "segment_skeletal_right_lower"
        ),
        "segment_skeletal_left_lower": physique_data.get("segment_skeletal_left_lower"),
    }
    return operation.change(sql, params)


def update_physique(student_id: int, physique_data: dict) -> bool:
    """更新体质数据"""
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
    params = {
        "student_id": student_id,
        "weight": physique_data.get("weight"),
        "bmi": physique_data.get("bmi"),
        "fat_percentage": physique_data.get("fat_percentage"),
        "skeletal_muscle_mass": physique_data.get("skeletal_muscle_mass"),
        "visceral_fat_level": physique_data.get("visceral_fat_level"),
        "limb_skeletal_muscle_index": physique_data.get("limb_skeletal_muscle_index"),
        "estimated_waist_hip_ratio": physique_data.get("estimated_waist_hip_ratio"),
        "body_type": physique_data.get("body_type"),
        "body_shape": physique_data.get("body_shape"),
        "basal_metabolism_rate": physique_data.get("basal_metabolism_rate"),
        "moisture_rate": physique_data.get("moisture_rate"),
        "bone_salt_amount": physique_data.get("bone_salt_amount"),
        "protein_percentage": physique_data.get("protein_percentage"),
        "lean_body_mass": physique_data.get("lean_body_mass"),
        "body_age": physique_data.get("body_age"),
        "heart_rate": physique_data.get("heart_rate"),
        "segment_moisture": physique_data.get("segment_moisture"),
        "segment_protein": physique_data.get("segment_protein"),
        "segment_fat_mass": physique_data.get("segment_fat_mass"),
        "segment_bone_salt": physique_data.get("segment_bone_salt"),
        "segment_fat_total": physique_data.get("segment_fat_total"),
        "segment_fat_right_upper": physique_data.get("segment_fat_right_upper"),
        "segment_fat_left_upper": physique_data.get("segment_fat_left_upper"),
        "segment_fat_trunk": physique_data.get("segment_fat_trunk"),
        "segment_fat_right_lower": physique_data.get("segment_fat_right_lower"),
        "segment_fat_left_lower": physique_data.get("segment_fat_left_lower"),
        "segment_skeletal_muscle_total": physique_data.get(
            "segment_skeletal_muscle_total"
        ),
        "segment_skeletal_right_upper": physique_data.get(
            "segment_skeletal_right_upper"
        ),
        "segment_skeletal_left_upper": physique_data.get("segment_skeletal_left_upper"),
        "segment_skeletal_trunk": physique_data.get("segment_skeletal_trunk"),
        "segment_skeletal_right_lower": physique_data.get(
            "segment_skeletal_right_lower"
        ),
        "segment_skeletal_left_lower": physique_data.get("segment_skeletal_left_lower"),
    }
    return operation.change(sql, params)


@router.get("/")
async def get_physique(request: Request):
    """获取当前学生的体质数据"""
    ip_address = request.headers.get("X-Real-IP")
    access_token = request.headers.get("access_token")

    if not access_token:
        return JSONResponse(
            status_code=401, content={"success": False, "message": "访问凭证缺失"}
        )

    user_id = jwt_manager.verify_access_token(access_token, ip_address, "student")
    if not user_id:
        return JSONResponse(
            status_code=401,
            content={"success": False, "message": "访问凭证无效或已过期"},
        )

    result = get_physique_by_student_id(user_id)
    if result["success"]:
        return JSONResponse(
            status_code=200, content={"success": True, "data": result["info"]}
        )
    return JSONResponse(status_code=404, content=result)


@router.post("/")
async def create_physique(request: Request):
    """创建体质数据"""
    ip_address = request.headers.get("X-Real-IP")
    access_token = request.headers.get("access_token")

    if not access_token:
        return JSONResponse(
            status_code=401, content={"success": False, "message": "访问凭证缺失"}
        )

    user_id = jwt_manager.verify_access_token(access_token, ip_address, "student")
    if not user_id:
        return JSONResponse(
            status_code=401,
            content={"success": False, "message": "访问凭证无效或已过期"},
        )

    physique_data = await request.json()
    physique_data["student_id"] = user_id

    existing = get_physique_by_student_id(user_id)
    if existing["success"]:
        return JSONResponse(
            status_code=400,
            content={"success": False, "message": "体质数据已存在，请使用更新接口"},
        )

    success = insert_physique(physique_data)
    if success:
        return JSONResponse(
            status_code=200, content={"success": True, "message": "体质数据创建成功"}
        )
    return JSONResponse(
        status_code=500, content={"success": False, "message": "创建失败"}
    )


@router.put("/")
async def update_physique_data(request: Request):
    """更新体质数据"""
    ip_address = request.headers.get("X-Real-IP")
    access_token = request.headers.get("access_token")

    if not access_token:
        return JSONResponse(
            status_code=401, content={"success": False, "message": "访问凭证缺失"}
        )

    user_id = jwt_manager.verify_access_token(access_token, ip_address, "student")
    if not user_id:
        return JSONResponse(
            status_code=401,
            content={"success": False, "message": "访问凭证无效或已过期"},
        )

    physique_data = await request.json()

    existing = get_physique_by_student_id(user_id)
    if not existing["success"]:
        return JSONResponse(
            status_code=404, content={"success": False, "message": "体质数据不存在"}
        )

    success = update_physique(user_id, physique_data)
    if success:
        return JSONResponse(
            status_code=200, content={"success": True, "message": "体质数据更新成功"}
        )
    return JSONResponse(
        status_code=500, content={"success": False, "message": "更新失败"}
    )

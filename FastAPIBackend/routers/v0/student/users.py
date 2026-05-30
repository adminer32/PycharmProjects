from fastapi import APIRouter
from fastapi.requests import Request
from fastapi.responses import JSONResponse
from utils.jwt_manager import JWTManager
from utils import save_file
import database
import vtoken

router = APIRouter()
jwt_manager = JWTManager()

@router.get("/")
async def get_profile(request: Request):
    ip_address = request.headers.get("X-Real-IP")
    access_token = request.headers.get("access_token")

    if not access_token:
        body = {
            "success": False,
            "message": "访问凭证缺失"
        }
        return JSONResponse(status_code=401, content=body)
    
    user_id = jwt_manager.verify_access_token(access_token, ip_address, "student")
    access_token_valid = user_id is not None
    if not access_token_valid:
        body = {
            "success": False,
            "message": "访问凭证无效或已过期"
        }
        return JSONResponse(status_code=401, content=body)
    
    result = database.get_student_by_id(user_id)
    success = result.get("success", False)

    if not success:
        body = {
            "success": False,
            "message": "获取用户信息失败或用户不存在"
        }
        return JSONResponse(status_code=404, content=body)
    
    info = result["info"]
    body = {
        "success": True,
        "data": info
    }
    return JSONResponse(status_code=200, content=body)

@router.post("/")
async def create_profile(request: Request):
    verify_token = request.headers.get("Authorization")

    if not verify_token:
        body = {
            "success": False,
            "message": "临时注册凭证缺失"
        }
        return JSONResponse(status_code=401, content=body)
    
    result = vtoken.decode(verify_token)
    success = result.get("success", False)

    if not success:
        body = {
            "success": False,
            "message": "临时注册凭证无效或已过期"
        }
        return JSONResponse(status_code=401, content=body)
    
    user_info = result["data"]
    user_profile = await request.json()

    if not user_profile:
        body = {
            "success": False,
            "message": "用户信息缺失"
        }
        return JSONResponse(status_code=400, content=body)
    
    username = user_info.get("username")
    password = user_info.get("password")
    email = user_info.get("email")
    gender = user_profile.get("gender")
    age = user_profile.get("age")
    class_id = user_profile.get("class_id")
    bio = user_profile.get("bio")
    avatar = user_profile.get("avatar")

    is_age_valid = age is not None and isinstance(age, int) and age >= 18 and age <= 25
    is_gender_valid = gender is not None and isinstance(gender, str) and gender in ["男", "女"]
    is_class_id_valid = class_id is not None and isinstance(class_id, int) and class_id > 0 and class_id <= 60
    is_bio_valid = bio is not None and isinstance(bio, str) and len(bio) <= 150
    if not username or not password or not is_age_valid or not is_gender_valid or not is_bio_valid or not is_class_id_valid:
        body = {
            "success": False,
            "message": "用户信息不合法"
        }
        return JSONResponse(status_code=400, content=body)
    
    if not avatar is None or avatar != "":
        avatar = save_file.save_avatar(avatar, "student")
        user_profile["avatar"] = avatar

    user_profile["email"] = email
    
    success = database.insert_student(username, password, True, user_profile)

    if not success:
        body = {
            "success": False,
            "message": "用户档案建立失败"
        }
        return JSONResponse(status_code=500, content=body)
    
    body = {
        "success": True,
        "message": "注册成功"
    }
    return JSONResponse(status_code=200, content=body)

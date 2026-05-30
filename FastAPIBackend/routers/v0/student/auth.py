from fastapi import APIRouter
from fastapi.requests import Request
from fastapi.responses import JSONResponse
from utils.jwt_manager import JWTManager
import database
from cloudflare_challenge import captcha_manager
import vtoken
import vemail

router = APIRouter()
jwt_manager = JWTManager()

@router.get("/token")
async def check_token(request: Request):
    ip_address = request.headers.get("X-Real-IP")
    access_token = request.headers.get("access_token")
    refresh_token = request.headers.get("refresh_token")
    access_token_valid = jwt_manager.verify_access_token(access_token, ip_address, "student") != None if access_token else False
    refresh_token_valid = jwt_manager.verify_refresh_token(refresh_token, ip_address) != None if refresh_token else False
    body = {
        "success": True,
        "access_token_valid": access_token_valid,
        "refresh_token_valid": refresh_token_valid
    }

    return JSONResponse(status_code=200, content=body)

@router.post("/token")
async def login(request: Request):
    ip_address = request.headers.get("X-Real-IP")
    data = await request.json()
    username = data.get("username")
    password = data.get("password")
    captcha = data.get("captcha")

    if not username or not password or not captcha:
        body = {
            "success": False,
            "message": "参数缺失或参数格式不合法"
        }
        return JSONResponse(status_code=400, content=body)

    if not captcha_manager.is_valid_captcha(captcha):
        captcha_manager.disable_captcha(captcha)

        body = {
            "success": False,
            "message": "人机认证未通过"
        }
        return JSONResponse(status_code=418, content=body)
    
    captcha_manager.disable_captcha(captcha)

    success = database.check_userpassword(username, password)
    if not success:
        body = {
            "success": False,
            "message": "用户名或密码错误"
        }
        return JSONResponse(status_code=401, content=body)
    
    result = database.get_student_by_username(username)
    success = result.get("success", False)
    if not success:
        body = {
            "success": False,
            "message": "用户不存在，或未完成注册"
        }
        return JSONResponse(status_code=404, content=body)
    
    info = result.get("info")
    user_id = int(info.get("user_id"))

    token = jwt_manager.create_token(user_id, ip_address)
    access_token = token.get("access_token")
    refresh_token = token.get("refresh_token")
    body = {
        "success": True,
        "access_token": access_token,
        "refresh_token": refresh_token,
        "redirect": "/student/dash"
    }
    return JSONResponse(status_code=200, content=body)

@router.post("/token/refresh")
async def refresh_token(request: Request):
    ip_address = request.headers.get("X-Real-IP")
    data = await request.json()
    access_token = data.get("access_token")
    refresh_token = data.get("refresh_token")
    captcha = data.get("captcha")

    if not access_token or not  refresh_token:
        if not captcha is None:
            captcha_manager.disable_captcha(captcha)
        
        body = {
            "success": False,
            "message": "参数缺失或参数格式不合法"
        }
        return JSONResponse(status_code=400, content=body)
    
    if not captcha_manager.is_valid_captcha(captcha):
        captcha_manager.disable_captcha(captcha)

        body = {
            "success": False,
            "message": "人机认证未通过"
        }
        return JSONResponse(status_code=418, content=body)
    
    captcha_manager.disable_captcha(captcha)
    
    result = jwt_manager.refresh_access_token(access_token, refresh_token, ip_address)
    success = result.get("success", False)

    if not success:
        body = {
            "success": False,
            "message": "认证失败，请重新登录"
        }
        return JSONResponse(status_code=401, content=body)

    token = result.get("token")
    access_token = token.get("access_token")
    refresh_token = token.get("refresh_token")

    body = {
        "success": True,
        "access_token": access_token,
        "refresh_token": refresh_token
    }
    return JSONResponse(status_code=200, content=body)

@router.get("/vtoken")
async def check_vtoken(request: Request):
    token = request.headers.get("Authorization")
    if not token:
        body = {
            "success": False,
            "message": "参数缺失或参数格式不合法"
        }
        return JSONResponse(status_code=400, content=body)
    
    result = vtoken.decode(token)
    success = result.get("success", False)
    if not success:
        body = {
            "success": False,
            "message": "验证失败"
        }
        return JSONResponse(status_code=401, content=body)

    body = {
        "success": True,
        "message": "验证成功"
    }
    return JSONResponse(status_code=200, content=body)

@router.post("/vtoken")
async def create_vtoken(request: Request):
    data = await request.json()
    username = data.get("username")
    password = data.get("password")
    email = data.get("email")
    captcha = data.get("captcha")

    if not username or not password or not captcha:
        if not captcha is None:
            captcha_manager.disable_captcha(captcha)

        body = {
            "success": False,
            "message": "参数缺失或参数格式不合法"
        }
        return JSONResponse(status_code=400, content=body)
    
    legal_username_chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    if not all(c in legal_username_chars for c in username):
        captcha_manager.disable_captcha(captcha)

        body = {
            "success": False,
            "message": "用户名不合法"
        }
        return JSONResponse(status_code=400, content=body)

    legal_email_suffix = ["qq.com", "163.com", "126.com", "139.com"]
    if len(username) < 6 or len(username) > 12 or len(email) > 128 or not "@" in email or not "." in email or not email.split("@")[-1] in legal_email_suffix:
        if not captcha is None:
            captcha_manager.disable_captcha(captcha)

        body = {
            "success": False,
            "message": "用户名长度不合法或不支持的邮箱"
        }
        return JSONResponse(status_code=400, content=body)

    if not captcha_manager.is_valid_captcha(captcha):
        captcha_manager.disable_captcha(captcha)

        body = {
            "success": False,
            "message": "人机认证未通过"
        }
        return JSONResponse(status_code=418, content=body)
    
    captcha_manager.disable_captcha(captcha)

    payload = {
        "username": username,
        "password": password,
        "email": email
    }

    token = vtoken.encode(payload)
    vemail.send(email, token)

    body = {
        "success": True,
        "message": "验证邮件已发送至您的注册邮箱，请注意查收"
    }
    return JSONResponse(status_code=200, content=body)
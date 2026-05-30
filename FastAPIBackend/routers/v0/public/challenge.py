from fastapi import APIRouter
from fastapi.requests import Request
from fastapi.responses import JSONResponse
from cloudflare_challenge.verify import CloudflareChallenge
from cloudflare_challenge import captcha_manager

router = APIRouter()
cf_challenge = CloudflareChallenge()

@router.get("/captcha")
async def login():
    captcha = captcha_manager.generate_captcha()
    body = {
        "success": True,
        "captcha": captcha
    }
    return JSONResponse(status_code=200, content=body)

@router.post("/token")
async def challenge(request: Request):
    data = await request.json()
    captcha = data.get("captcha")
    token = data.get("token")

    if not captcha or not token:
        if captcha:
            captcha_manager.disable_captcha(captcha)

            body = {
                "success": False,
                "message": "验证码错误"
            }
            return JSONResponse(status_code=401, content=body)

        body = {
            "success": False,
            "message": "参数错误"
        }
        return JSONResponse(status_code=400, content=body)
    
    success = cf_challenge.verify_challenge(token)
    captcha_manager.enable_captcha(captcha)
    
    if success:
        body = {
            "success": True,
            "message": "验证成功"
        }
        return JSONResponse(status_code=200, content=body)
    else:
        body = {
            "success": False,
            "message": "验证失败"
        }
        return JSONResponse(status_code=418, content=body)
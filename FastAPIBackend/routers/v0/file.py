from fastapi import APIRouter, UploadFile, File, HTTPException
import os
import uuid
from datetime import datetime

router = APIRouter(tags=["file"])

UPLOAD_DIR = "data/uploads"

if not os.path.exists(UPLOAD_DIR):
    os.makedirs(UPLOAD_DIR)


@router.post("/upload")
async def upload_file(file: UploadFile = File(...)):
    """文件上传接口"""
    try:
        suffix = os.path.splitext(file.filename)[1] if file.filename else ""
        if not suffix:
            suffix = ".bin"

        filename = (
            f"{datetime.now().strftime('%Y%m%d%H%M%S')}_{uuid.uuid4().hex[:8]}{suffix}"
        )
        filepath = os.path.join(UPLOAD_DIR, filename)

        with open(filepath, "wb") as f:
            content = await file.read()
            f.write(content)

        return {
            "status": "success",
            "message": "上传成功",
            "data": {"object": f"/uploads/{filename}"},
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"上传失败: {str(e)}")

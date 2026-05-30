from fastapi import APIRouter
from .student import auth, users, learning, schedule, physique
from .public import challenge
from . import ai
from . import file
from . import transcribe
from . import teacher

router = APIRouter(prefix="/v0", tags=["v0"])

router.include_router(auth.router, prefix="/student/auth", tags=["student", "auth"])
router.include_router(users.router, prefix="/student/users", tags=["student", "users"])
router.include_router(
    learning.router, prefix="/student/learning", tags=["student", "learning"]
)
router.include_router(
    schedule.router, prefix="/student/schedule", tags=["student", "schedule"]
)

router.include_router(
    physique.router, prefix="/student/physique", tags=["student", "physique"]
)

router.include_router(
    challenge.router, prefix="/public/challenge", tags=["public", "challenge"]
)

router.include_router(ai.router, prefix="/ai", tags=["ai"])

router.include_router(file.router, prefix="/file/operation", tags=["file"])

router.include_router(transcribe.router, prefix="/transcribe", tags=["transcribe"])

router.include_router(teacher.router, tags=["teacher"])

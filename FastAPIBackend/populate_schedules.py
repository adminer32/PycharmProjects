"""
填充 schedules 表测试数据
"""

import sys

sys.path.insert(0, ".")

from database.export import save_schedule
from datetime import datetime, timedelta
import random

# 假设 student_id = 1 的测试用户
STUDENT_ID = 1

# 毽球训练动作
TRAINING_ACTIONS = [
    "盘踢",
    "磕踢",
    "外摆踢",
    "里合踢",
    "绷踢",
    "拐踢",
    "跳踢",
    "踏踢",
    "接头练习",
    "控球练习",
    "耐力训练",
    "速度训练",
]

# AI课程
AI_COURSES = [
    "AI毽球大师课-入门",
    "AI毽球大师课-进阶",
    "AI毽球大师课-高级",
    "盘踢技巧详解",
    "磕踢技巧详解",
    "外摆踢技巧详解",
    "里合踢技巧详解",
    "绷踢技巧详解",
    "拐踢技巧详解",
    "战术分析基础",
    "比赛策略入门",
    "体能训练指导",
]

# 时间段
TIME_SLOTS = [
    "08:00",
    "09:00",
    "10:00",
    "11:00",
    "14:00",
    "15:00",
    "16:00",
    "17:00",
    "18:00",
    "19:00",
    "20:00",
]

# 日程类型
TYPES = ["warning", "success", "error", "info"]

# 从今天开始生成未来30天内的数据
start_date = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)

inserted = 0
for i in range(100):
    # 随机日期（未来30天内）
    days_offset = random.randint(0, 30)
    schedule_date = (start_date + timedelta(days=days_offset)).strftime("%Y-%m-%d")

    # 随机时间
    time = random.choice(TIME_SLOTS)

    # 随机类型
    schedule_type = random.choice(TYPES)

    # 随机内容（训练动作或AI课程）
    if random.random() > 0.5:
        content = random.choice(TRAINING_ACTIONS)
    else:
        content = random.choice(AI_COURSES)

    result = save_schedule(STUDENT_ID, schedule_date, time, schedule_type, content)
    if result.get("success"):
        inserted += 1

print(f"成功插入 {inserted} 条日程数据")

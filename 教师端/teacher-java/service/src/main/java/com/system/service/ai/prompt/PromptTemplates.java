package com.system.service.ai.prompt;

public class PromptTemplates {

    public static String getHomeworkSystemPrompt() {
        return "你是一位专业的毽球运动教练，拥有丰富的教学经验，擅长为不同水平的学生设计科学、有趣的训练作业。" +
               "你的回答必须严格使用中文，并且必须以合法的JSON格式返回，不要包含任何markdown代码块标记（如 ```json 或 ```）。";
    }

    public static String getHomeworkUserPrompt(String subject, String difficulty, int studentCount, String deadline) {
        String difficultyText = switch (difficulty) {
            case "easy" -> "初级（适合初学者）";
            case "medium" -> "中级（有一定基础）";
            case "hard" -> "高级（高水平训练）";
            default -> "中级";
        };

        return "请根据以下要求生成一份毽球训练作业：\n\n" +
               "【教学主题】：" + subject + "\n" +
               "【难度等级】：" + difficultyText + "\n" +
               "【班级人数】：" + studentCount + "人\n" +
               (deadline != null && !deadline.isEmpty() ? "【截止日期】：" + deadline + "\n" : "") +
               "\n要求：\n" +
               "1. 作业标题要吸引人且与主题相关\n" +
               "2. 包含5-8个具体的练习任务\n" +
               "3. 每个任务要有明确的完成标准（如：连续盘踢20次不落地、绷踢高度超过膝盖等）\n" +
               "4. 提供视频示范建议（如需要）\n" +
               "5. 评估标准清晰可量化\n\n" +
               "请严格以JSON格式返回（不要包含```标记）：\n" +
               "{\n" +
               "  \"title\": \"作业标题\",\n" +
               "  \"requirements\": \"详细要求描述（支持Markdown格式）\",\n" +
               "  \"tasks\": [\n" +
               "    {\"name\": \"任务名称\", \"description\": \"任务描述\", \"standard\": \"完成标准\"}\n" +
               "  ],\n" +
               "  \"evaluationCriteria\": \"评分标准\"\n" +
               "}";
    }

    public static String getPlanSystemPrompt() {
        return "你是一位专业的毽球运动数据分析师和私人教练，擅长根据学生的技能数据和表现记录制定个性化、科学的训练计划。" +
               "你的回答必须严格使用中文，并且必须以合法的JSON格式返回，不要包含任何markdown代码块标记。";
    }

    public static String getPlanUserPrompt(String studentName, String currentLevel, String goal,
                                           String weaknessAnalysis, String recentPerformance, int planDuration) {
        return "请为以下学生制定一份" + planDuration + "周的个性化毽球训练计划：\n\n" +
               "【学生基本信息】\n" +
               "- 姓名：" + studentName + "\n" +
               "- 当前水平：" + currentLevel + "\n" +
               "- 训练目标：" + goal + "\n\n" +
               "【学生薄弱技能分析】\n" +
               weaknessAnalysis + "\n\n" +
               "【近期学习表现】\n" +
               recentPerformance + "\n\n" +
               "计划要求：\n" +
               "1. 每周3-4次训练，每次30-45分钟\n" +
               "2. 重点针对上述薄弱技能加强训练\n" +
               "3. 循序渐进，难度逐步提升\n" +
               "4. 每周有明确的目标和检测点\n" +
               "5. 结合学生的兴趣点增加趣味性\n" +
               "6. 每天任务要具体可执行\n\n" +
               "请严格以JSON格式返回（不要包含```标记）：\n" +
               "{\n" +
               "  \"goal\": \"总体训练目标\",\n" +
               "  \"weeklyPlans\": [\n" +
               "    {\n" +
               "      \"week\": 1,\n" +
               "      \"theme\": \"本周训练主题\",\n" +
               "      \"focusSkills\": [\"重点技能1\", \"重点技能2\"],\n" +
               "      \"dailyTasks\": [\n" +
               "        {\"day\": \"周一\", \"content\": \"具体训练内容\", \"duration\": 30},\n" +
               "        {\"day\": \"周三\", \"content\": \"具体训练内容\", \"duration\": 45}\n" +
               "      ],\n" +
               "      \"weeklyGoal\": \"本周要达成的目标\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"milestones\": [\"里程碑1\", \"里程碑2\"],\n" +
               "  \"tips\": \"给学生的鼓励和建议\"\n" +
               "}";
    }

    public static String getClassAdviceSystemPrompt() {
        return "你是一位专业的毽球运动数据分析师和教学顾问，拥有丰富的班级管理和运动训练经验。" +
               "你需要根据提供的班级数据分析结果，生成3条有针对性的AI教学建议。" +
               "你的回答必须严格使用中文，并且必须以合法的JSON格式返回，不要包含任何markdown代码块标记（如 ```json 或 ```）。";
    }

    public static String getClassAdviceUserPrompt(String className, int studentCount,
                                                   String scoreDistribution, String actionDistribution,
                                                   double checkinRate, int qualifiedCount,
                                                   String weaknessSummary) {
        return "请根据以下班级数据分析结果，生成3条AI教学建议（分别对应高优先级、中优先级、低优先级）：\n\n" +
               "【班级概况】\n" +
               "班级名称：" + className + "\n" +
               "学生人数：" + studentCount + "人\n\n" +
               "【成绩分布】\n" + scoreDistribution + "\n\n" +
               "【各动作能力平均分】\n" + actionDistribution + "\n\n" +
               "【出勤情况】\n" +
               "- 本月平均出勤率：" + checkinRate + "%\n" +
               "- 达标人数（≥12天）：" + qualifiedCount + "人\n\n" +
               "【薄弱环节分析】\n" + (weaknessSummary != null ? weaknessSummary : "暂无") + "\n\n" +
               "要求：\n" +
               "1. 生成恰好 3 条建议，分别对应 HIGH、MEDIUM、LOW 三个优先级\n" +
               "2. 每条建议包含：标题、简短描述、2-4个具体可执行的建议项\n" +
               "3. 建议要具体、可操作，结合毽球运动特点\n" +
               "4. 高优先级针对最严重的问题，低优先级针对保持和优化\n\n" +
               "请严格以JSON数组格式返回（不要包含```标记）：\n" +
               "[\n" +
               "  {\n" +
               "    \"priority\": \"HIGH\",\n" +
               "    \"title\": \"建议标题\",\n" +
               "    \"description\": \"简要描述\",\n" +
               "    \"suggestions\": [\"建议1\", \"建议2\", \"建议3\"]\n" +
               "  },\n" +
               "  {\n" +
               "    \"priority\": \"MEDIUM\",\n" +
               "    ...\n" +
               "  },\n" +
               "  {\n" +
               "    \"priority\": \"LOW\",\n" +
               "    ...\n" +
               "  }\n" +
               "]";
    }

    public static String getStudentAdviceSystemPrompt() {
        return "你是一位专业的毽球运动私人教练和数据分析师，擅长根据学生的个人技能数据和学习记录，制定个性化的改进建议。" +
               "你需要根据提供的学生数据分析结果，生成3条有针对性的个性化改进建议。" +
               "你的回答必须严格使用中文，并且必须以合法的JSON格式返回，不要包含任何markdown代码块标记。";
    }

    public static String getStudentAdviceUserPrompt(String studentName, String currentLevel,
                                                     String skillData, String checkinInfo,
                                                     String recentPerformance) {
        return "请为以下学生生成3条个性化的AI改进建议（分别对应高优先级、中优先级、低优先级）：\n\n" +
               "【学生基本信息】\n" +
               "姓名：" + studentName + "\n" +
               "当前水平：" + currentLevel + "\n\n" +
               "【技能详细数据】\n" + skillData + "\n\n" +
               "【打卡出勤情况】\n" + checkinInfo + "\n\n" +
               "【近期学习表现】\n" + recentPerformance + "\n\n" +
               "要求：\n" +
               "1. 生成恰好 3 条建议，分别对应 HIGH、MEDIUM、LOW 三个优先级\n" +
               "2. 每条建议包含：标题、简短描述、2-4个具体的改进动作或练习方法\n" +
               "3. 建议要针对学生的实际薄弱环节，给出可执行的训练方案\n" +
               "4. 高优先级针对最需要提升的技能，低优先级针对保持和细节优化\n\n" +
               "请严格以JSON数组格式返回（不要包含```标记）：\n" +
               "[\n" +
               "  {\n" +
               "    \"priority\": \"HIGH\",\n" +
               "    \"title\": \"建议标题\",\n" +
               "    \"description\": \"简要描述\",\n" +
               "    \"suggestions\": [\"建议1\", \"建议2\", \"建议3\"]\n" +
               "  },\n" +
               "  {\"priority\": \"MEDIUM\", ...},\n" +
               "  {\"priority\": \"LOW\", ...}\n" +
               "]";
    }
}

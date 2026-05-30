package com.saaes.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saaes.common.core.utils.CommonUtil;
import com.saaes.common.core.web.MyException;
import com.saaes.common.core.web.PageQuery;
import com.saaes.system.client.dto.ScheduleEventDTO;
import com.saaes.system.client.entity.AIAnalysisInfo;
import com.saaes.system.client.entity.AIStandardData;
import com.saaes.system.client.entity.AnalysisResult;
import com.saaes.system.client.entity.ScheduleEvent;
import com.saaes.system.handler.AIAnalysisHandler;
import com.saaes.system.mapper.AIAnalysisInfoMapper;
import com.saaes.system.mapper.AIStandardDataMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AIAnalysisInfoService {

    @Resource
    private AIContentInfoService aiContentInfoService;

    @Resource
    private AIAnalysisInfoMapper aiAnalysisInfoMapper;

    @Resource
    private AIStandardDataMapper aiStandardDataMapper;

    @Resource
    private AIAnalysisHandler aiAnalysisHandler;

    @Resource
    private AiService aiService;

    @Transactional(readOnly = true)
    public Page<AIAnalysisInfo> query(PageQuery<Map<String, Object>> pageQuery) {
        Map<String, Object> param = pageQuery.getParam();
        Long loginId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<AIAnalysisInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AIAnalysisInfo::getDeleted, false);

        if (CommonUtil.isNotEmpty(loginId)) {
            queryWrapper.eq(AIAnalysisInfo::getCreateBy, loginId);
        }
        if (CommonUtil.isNotEmpty(param.get("id"))) {
            queryWrapper.eq(AIAnalysisInfo::getId, param.get("id"));
        }
        if (CommonUtil.isNotEmpty(param.get("task_id"))) {
            queryWrapper.like(AIAnalysisInfo::getTaskId, param.get("task_id"));
        }

        Page<AIAnalysisInfo> page = new Page<>(pageQuery.getCurrentPage(), pageQuery.getPageSize());
        return aiAnalysisInfoMapper.selectPage(page, queryWrapper);
    }

    @Transactional(readOnly = true)
    public AIAnalysisInfo queryByTaskId(String taskId) {
        QueryWrapper<AIAnalysisInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        return aiAnalysisInfoMapper.selectOne(queryWrapper);
    }

    @Transactional
    public Boolean save(Map<String, Object> data) throws JsonProcessingException {
        if (CommonUtil.isEmpty(data)) {throw new MyException("数据不能为空");}
        if ((CommonUtil.isEmpty(data.get("task_id")))) {throw new MyException("任务ID不能为空");}

        AIAnalysisInfo aiAnalysisInfo = new AIAnalysisInfo();
        ObjectMapper objectMapper = new ObjectMapper();
        UpdateWrapper<AIAnalysisInfo> updateWrapper = new UpdateWrapper<>();

        if (CommonUtil.isNotEmpty(data.get("self_left_x_data"))) {
            updateWrapper.set("self_left_x_data", data.get("self_left_x_data").toString());
        }

        if (CommonUtil.isNotEmpty(data.get("self_left_y_data"))) {
            updateWrapper.set("self_left_y_data", data.get("self_left_y_data").toString());
        }

        if (CommonUtil.isNotEmpty(data.get("self_right_x_data"))) {
            updateWrapper.set("self_right_x_data", data.get("self_right_x_data").toString());
        }

        if (CommonUtil.isNotEmpty(data.get("self_right_y_data"))) {
            updateWrapper.set("self_right_y_data", data.get("self_right_y_data").toString());
        }

//        // 处理self_left_x_data，确保转换为UTF-8编码
//        if (CommonUtil.isNotEmpty(data.get("self_left_x_data"))) {
//            String selfLeftXDataStr = new String(data.get("self_left_x_data").toString().getBytes(), StandardCharsets.UTF_8);
//            List<Integer> listResult = objectMapper.readValue(selfLeftXDataStr, objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class));
//            aiAnalysisInfo.setSelfLeftXData(listResult);
//        }
//
//        // 处理self_left_y_data，确保转换为UTF-8编码
//        if (CommonUtil.isNotEmpty(data.get("self_left_y_data"))) {
//            String selfLeftYDataStr = new String(data.get("self_left_y_data").toString().getBytes(), StandardCharsets.UTF_8);
//            List<Integer> listResult = objectMapper.readValue(selfLeftYDataStr, objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class));
//            aiAnalysisInfo.setSelfLeftYData(listResult);
//        }
//
//        // 处理self_right_x_data，确保转换为UTF-8编码
//        if (CommonUtil.isNotEmpty(data.get("self_right_x_data"))) {
//            String selfRightXDataStr = new String(data.get("self_right_x_data").toString().getBytes(), StandardCharsets.UTF_8);
//            List<Integer> listResult = objectMapper.readValue(selfRightXDataStr, objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class));
//            aiAnalysisInfo.setSelfRightXData(listResult);
//        }
//
//        // 处理self_right_y_data，确保转换为UTF-8编码
//        if (CommonUtil.isNotEmpty(data.get("self_right_y_data"))) {
//            String selfRightYDataStr = new String(data.get("self_right_y_data").toString().getBytes(), StandardCharsets.UTF_8);
//            List<Integer> listResult = objectMapper.readValue(selfRightYDataStr, objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class));
//            aiAnalysisInfo.setSelfRightYData(listResult);
//        }


        if (CommonUtil.isNotEmpty(data.get("overall_score"))) {
            aiAnalysisInfo.setOverallScore(Double.valueOf(data.get("overall_score").toString()));
        }

        if (CommonUtil.isNotEmpty(data.get("stability_score"))) {
            aiAnalysisInfo.setStabilityScore(Double.valueOf(data.get("stability_score").toString()));
        }

        if (CommonUtil.isNotEmpty(data.get("fluency_score"))) {
            aiAnalysisInfo.setFluencyScore(Double.valueOf(data.get("fluency_score").toString()));
        }

        if (CommonUtil.isNotEmpty(data.get("proficiency_score"))) {
            aiAnalysisInfo.setProficiencyScore(Double.valueOf(data.get("proficiency_score").toString()));
        }

        if (CommonUtil.isNotEmpty(data.get("stability_improvement"))) {
            aiAnalysisInfo.setStabilityImprovement(data.get("stability_improvement").toString());
        }

        if (CommonUtil.isNotEmpty(data.get("proficiency_enhancement"))) {
            aiAnalysisInfo.setProficiencyEnhancement(data.get("proficiency_enhancement").toString());
        }

        if (CommonUtil.isNotEmpty(data.get("fluency_promotion"))) {
            aiAnalysisInfo.setFluencyPromotion(data.get("fluency_promotion").toString());
        }

        if (CommonUtil.isNotEmpty(data.get("general_advice"))) {
            aiAnalysisInfo.setGeneralAdvice(data.get("general_advice").toString());
        }

        if (CommonUtil.isNotEmpty(data.get("task_status"))) {
            aiAnalysisInfo.setTaskStatus(data.get("task_status").toString());
            updateWrapper.set("update_time", LocalDateTime.now());
        } else {
            aiAnalysisInfo.setTaskStatus("进行中");
//            updateWrapper.set("update_time", LocalDateTime.now());
        }

        updateWrapper.eq("deleted", false);
        updateWrapper.eq("task_id", data.get("task_id").toString());
        if (CommonUtil.isNotEmpty(data.get("id"))) {updateWrapper.set("id", data.get("id").toString());}
        boolean updateResult = aiAnalysisInfoMapper.update(aiAnalysisInfo, updateWrapper) > 0;
        if (!updateResult) {
            throw new MyException("数据更新失败");
        }

        if (CommonUtil.isNotEmpty(data.get("self_left_x_data")) || CommonUtil.isNotEmpty(data.get("self_left_y_data"))
                || CommonUtil.isNotEmpty(data.get("self_right_x_data")) || CommonUtil.isNotEmpty(data.get("self_right_y_data"))) {

            String taskId = (String) data.get("task_id");

            // 注册事务完成后的回调
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 在事务提交后执行异步任务
                    aiAnalysisHandler.submit(AIAnalysisInfoService.this, taskId);
                }
            });
        }

        return true;
    }

    @Transactional
    public void addAIAnalysisTask(String taskId) throws NoApiKeyException, InputRequiredException, JsonProcessingException {
        if (taskId == null || CommonUtil.isEmpty(taskId)) {
            throw new MyException("任务ID不能为空");
        }

        // 查询分析任务信息
        QueryWrapper<AIAnalysisInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        AIAnalysisInfo aiAnalysisInfo = aiAnalysisInfoMapper.selectOne(queryWrapper);
        if (aiAnalysisInfo == null) {
            throw new MyException("该任务ID所属数据不存在或已删除");
        }

        // 帧率与视频时长计算
        int frameRate = 30;
        int arrayLength = aiAnalysisInfo.getSelfLeftXData().isEmpty()
                ? aiAnalysisInfo.getSelfRightXData().size()
                : aiAnalysisInfo.getSelfLeftXData().size();
        double videoDurationSeconds = (double) arrayLength / frameRate;
        String videoDurationFormatted = String.format("%.2f秒", videoDurationSeconds);

        // 查询标准动作数据
        QueryWrapper<AIStandardData> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.eq("name", aiAnalysisInfo.getMotionName());
        AIStandardData aiStandardData = aiStandardDataMapper.selectOne(queryWrapper2);
        if (aiStandardData == null) {
            throw new MyException("动作标准数据不存在");
        }

        // 构建关键点映射
        Map<String, String> motionKeyPointMap = new HashMap<>();
        motionKeyPointMap.put("左脚单踢-脚内侧踢球", "选择点15和14。");
        motionKeyPointMap.put("左脚单踢-脚外侧踢球", "选择点15和11。");
        motionKeyPointMap.put("左脚单踢-脚背踢球", "选择点15和13。");
        motionKeyPointMap.put("左脚单踢-腿部触球", "选择点13和11。");

        motionKeyPointMap.put("右脚单踢-脚内侧踢球", "选择点16和13。");
        motionKeyPointMap.put("右脚单踢-脚外侧踢球", "选择点16和12。");
        motionKeyPointMap.put("右脚单踢-脚背踢球", "选择点16和14。");
        motionKeyPointMap.put("右脚单踢-腿部触球", "选择点14和12。");

        motionKeyPointMap.put("双脚交替-脚内侧踢球", "选择点(13,14)和(11,12)。");
        motionKeyPointMap.put("双脚交替-脚背踢球", "选择点(16,14)和(15,13)。");
        motionKeyPointMap.put("双脚交替-腿部触球", "选择点(14,12)和(13,11)。");

        // 构造分析内容
        String motionName = aiAnalysisInfo.getMotionName();
        StringBuilder basicText = new StringBuilder();
        basicText.append("你是毽球姿态分析专家，以下数据是我对毽球盘踢腿部姿态分析，通过yolo11x-pose模型识别视频内人体17个点位，")
                .append("并取按视频每一帧检测的在X轴和Y轴上的相对坐标值变化，数据的X-Y大小是以选定的标注点对应方向上的坐标减去另一个点的坐标得出的相对值。")
                .append("双点动作仅记录X1/Y1数据，四点动作包含完整X1/Y1/X2/Y2数组。以标准动作坐标数组为基准参照（100分），")
                .append("分析后续动作并以稳定性、熟练度、流畅性和综合评分为维度进行评估。误差±3分不扣分。\n\n");

        basicText.append("评分维度说明：\n");
        basicText.append("1. 稳定性（stability_score）：判断动作是否连贯，坐标波动小为佳。\n");
        basicText.append("2. 熟练度（proficiency_score）：是否接近标准样本，动作精准度。\n");
        basicText.append("3. 流畅性（fluency_score）：动作是否顺滑自然，无卡顿感。\n");
        basicText.append("4. 综合评分（overall_score）：综合以上三项评估，满分100。\n");
        basicText.append("每项评分保留两位小数，允许±3分误差不计入扣分范围。\n");

        basicText.append("【分析说明】\n")
                .append("- X1/Y1 代表左脚关键点坐标数组，X2/Y2 为右脚。\n")
                .append("- 每秒30帧，单脚动作数组长度为 ").append(arrayLength).append("。\n")
                .append("，视频时长约 ").append(videoDurationFormatted).append("秒，(计算原理：单脚数组长度÷每秒30帧)。\n")
                .append("通过生物力学与图像分析原则，进行评分与建议输出.\n")
                .append("X1/Y1/X2/Y2指的是：X1Y1是左脚，X2Y2是右脚，避免在结论中直接出现X1/Y1/X2/Y2/X轴/Y轴. \n")
                .append("内容使用专业术语，建议部分应具体到时间点（如第几秒），用毽球术语描述，且训练建议的内容要科学，丰富，侧重实际动作改善方案。\n\n");

        basicText.append("关键点说明：动作名称为").append(motionName).append("，")
                .append(motionKeyPointMap.getOrDefault(motionName, "未配置关键点。")).append("\n\n");


        // 添加标准动作数据
        basicText.append("标准动作坐标数组如下（作为满分参照）：\n");
        if (aiStandardData.getStandardLeftXData() != null) {
            basicText.append("X1=").append(aiStandardData.getStandardLeftXData()).append("\n");
        }
        if (aiStandardData.getStandardLeftYData() != null) {
            basicText.append("Y1=").append(aiStandardData.getStandardLeftYData()).append("\n");
        }
        if (aiStandardData.getStandardRightXData() != null) {
            basicText.append("X2=").append(aiStandardData.getStandardRightXData()).append("\n");
        }
        if (aiStandardData.getStandardRightYData() != null) {
            basicText.append("Y2=").append(aiStandardData.getStandardRightYData()).append("\n");
        }

        // 添加用户动作数据
        basicText.append("\n待分析动作数据如下：\n");
        log.info("运动分析数据：" + aiAnalysisInfo.toString());
        if (aiAnalysisInfo.getSelfLeftXData() != null) {
            basicText.append("X1=").append(aiAnalysisInfo.getSelfLeftXData()).append("\n");
        }
        if (aiAnalysisInfo.getSelfLeftYData() != null) {
            basicText.append("Y1=").append(aiAnalysisInfo.getSelfLeftYData()).append("\n");
        }
        if (aiAnalysisInfo.getSelfRightXData() != null) {
            basicText.append("X2=").append(aiAnalysisInfo.getSelfRightXData()).append("\n");
        }
        if (aiAnalysisInfo.getSelfRightYData() != null) {
            basicText.append("Y2=").append(aiAnalysisInfo.getSelfRightYData()).append("\n");
        }

        // 追加 JSON 输出要求
        basicText.append("\n请仅返回以下 JSON 格式的分析结果，不包含任何解释、注释或 markdown 标记：\n")
                .append("{\n")
                .append("  \"stability_score\": ,\n")
                .append("  \"proficiency_score\": ,\n")
                .append("  \"fluency_score\": ,\n")
                .append("  \"overall_score\": ,\n")
                .append("  \"stability_improvement\": ,\n")
                .append("  \"proficiency_enhancement\": ,\n")
                .append("  \"fluency_promotion\": ,\n")
                .append("  \"general_advice\": \n")
                .append("}");

        // 调用 AI 服务
        Map<String, Object> params = new HashMap<>();
        params.put("prompt", "你是一个毽球评分专家，接下来我将给你提供毽球盘踢腿部姿态标准数据和需要分析的数据，请帮我合理分析和评分，使用我提供的json模板将数据填充进去后返回给我，只需要返回json数据，分数和评价尽可能客观合理，不添加markdown语法或注释。");
        params.put("message", basicText.toString());

        String result = aiService.queryQwen(params);
//        System.out.println(result);

        String resultRegex = "\\{[^\\}]*\\}";
        Pattern pattern = Pattern.compile(resultRegex);
        Matcher matcher = pattern.matcher(result);
        while (matcher.find()) {
            result = matcher.group();
        }

        log.info("分析结果：" + result);

        if (!result.trim().startsWith("{") && result.trim().endsWith("}")) {throw new MyException("AI分析返回的内容不包含所需数据");}

        ObjectMapper objectMapper = new ObjectMapper();
        AnalysisResult analysisResult = objectMapper.readValue(result, AnalysisResult.class);

        Map<String, Object> updateParams = new HashMap<>();

        if (analysisResult != null && analysisResult.getFluency_score() != null && analysisResult.getOverall_score() != null && analysisResult.getFluency_promotion() != null && analysisResult.getStability_score() != null) {
            updateParams.put("stability_score", analysisResult.getStability_score());
            updateParams.put("proficiency_score", analysisResult.getProficiency_score());
            updateParams.put("fluency_score", analysisResult.getFluency_score());
            updateParams.put("overall_score", analysisResult.getOverall_score());
            updateParams.put("stability_improvement", analysisResult.getStability_improvement());
            updateParams.put("proficiency_enhancement", analysisResult.getProficiency_enhancement());
            updateParams.put("fluency_promotion", analysisResult.getFluency_promotion());
            updateParams.put("general_advice", analysisResult.getGeneral_advice());
            updateParams.put("task_status", "已完成");
            updateParams.put("task_id", taskId);

            boolean flag = save(updateParams);
            if (!flag) {
                updateParams.clear();
                updateParams.put("task_status", "失败");
                updateParams.put("task_id", taskId);
                save(updateParams);
            }
        }
    }

//    public List<ScheduleEventDTO> generateTrainingPlanWithAIAnalysisInfo(String historyId, String dayNum) throws NoApiKeyException, InputRequiredException {
//        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
//
//        List<AIAnalysisInfo> analysisInfoList = aiContentInfoService.getListAnalysisByHistoryId(Integer.parseInt(historyId));
//
//        if (analysisInfoList.isEmpty()) {throw new MyException("当前历史ID无关联的运动数据");}
//
//        StringBuilder message = new StringBuilder();
//        message.append("你是毽球训练专家，请根据以下评分和建议为运动员制定连续")
//                .append(dayNum)
//                .append("天的训练计划（当前日期：")
//                .append(currentDate)
//                .append("），每天安排早训、午训、晚训。\n\n");
//
//        for (int i = 0; i == 0; i++) {
//            AIAnalysisInfo info = analysisInfoList.get(i);
//            message.append("建议：")
//                    .append("稳定性改进：").append(info.getStabilityImprovement()).append("；")
//                    .append("熟练度提升：").append(info.getProficiencyEnhancement()).append("；")
//                    .append("流畅性优化：").append(info.getFluencyPromotion()).append("；")
//                    .append("综合建议：").append(info.getGeneralAdvice()).append("。\n\n");
//        }
//
//        message.append("请仅输出 JSON 数组，每天 3 条记录，连续3天，共9条记录：\n")
//                .append("字段包括：scheduleDate（yyyy-MM-dd），time（HH:mm），type（早训/午训/晚训），content（训练内容）。\n")
//                .append("时间区间：早训08:00，午训14:00，晚训19:00")
//                .append("不要重复输出评分内容，不包含任何解释、注释或 markdown 标记。");
//
//        String result = aiService.queryQwenPlus(Map.of(
//                "prompt", "根据评分和建议生成毽球训练计划，训练内容要针对当前评分和建议数据，且符合实际。请仅输出 JSON 数组，不要包含```json等markdown格式",
//                "message", message.toString()
//        ));
//
//        System.out.println(result);
//
//        return JSON.parseArray(result, ScheduleEventDTO.class);
//    }

    public List<ScheduleEventDTO> generateTrainingPlanWithAIAnalysisInfo(String historyId, String dayNum)
            throws NoApiKeyException, InputRequiredException {

        int historyIdInt = Integer.parseInt(historyId);
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        List<AIAnalysisInfo> analysisInfoList = aiContentInfoService.getListAnalysisByHistoryId(historyIdInt);

        if (analysisInfoList.isEmpty()) {
            throw new MyException("当前历史ID无关联的运动数据");
        }

        // 仅提取第1组建议信息
        AIAnalysisInfo info = analysisInfoList.get(0);

        StringBuilder message = new StringBuilder();
        message.append("你是毽球训练专家，请根据以下建议为运动员制定连续 ")
                .append(dayNum)
                .append(" 天的训练计划，每天安排早训、午训、晚训，共 ")
                .append(Integer.parseInt(dayNum) * 3)
                .append(" 条训练记录。\n\n");

        message.append("建议：")
                .append("稳定性：").append(info.getStabilityImprovement()).append("；")
                .append("熟练度：").append(info.getProficiencyEnhancement()).append("；")
                .append("流畅性：").append(info.getFluencyPromotion()).append("；")
                .append("综合：").append(info.getGeneralAdvice()).append("。\n\n");

        message.append("请仅输出 JSON 数组，\n")
                .append("每条记录格式：scheduleDate（yyyy-MM-dd），time（HH:mm），type（早训/午训/晚训），content（训练内容）。\n")
                .append("时间固定为：08:00（早训），14:00（午训），19:00（晚训）。从当前日期 ")
                .append(currentDate)
                .append(" 开始，连续安排 ")
                .append(dayNum)
                .append(" 天。");

        String result = aiService.queryQwenTurbo(Map.of(
                "prompt", "根据评分和建议生成毽球训练计划，训练内容要针对当前评分和建议数据，且符合实际。请仅输出 JSON 数组，不要包含```json等markdown格式",
                "message", message.toString()
        ));

        System.out.println(result);
        // 预处理去除 ```json 开头和 ``` 结尾
        result = result.trim();
        if (result.startsWith("```json")) {
            result = result.substring(7).trim(); // 去掉 ```json（长度为7）
        }
        if (result.endsWith("```")) {
            result = result.substring(0, result.length() - 3).trim(); // 去掉末尾的 ```
        }
        return JSON.parseArray(result, ScheduleEventDTO.class);
    }


}

package com.saaes.system.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.saaes.common.core.Constant;
import com.saaes.common.core.utils.HttpUtil;
import com.saaes.common.core.web.MyException;
import com.saaes.system.client.dto.ChatMessageDTO;
import com.saaes.system.client.entity.*;
import com.saaes.system.mapper.AIAnalysisInfoMapper;
import com.saaes.system.mapper.AICardInfoMapper;
import com.saaes.system.mapper.AIContentInfoMapper;
import com.saaes.system.mapper.AIStandardDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AIContentInfoService {

    @Autowired
    private AIContentInfoMapper aiContentInfoMapper;

    @Autowired
    private AIAnalysisInfoMapper aiAnalysisInfoMapper;

    @Autowired
    private AICardInfoMapper aiCardInfoMapper;

    @Autowired
    private AIStandardDataMapper aiStandardDataMapper;

    private static final String postUrl = Constant.VIDEO_POST_URL;

    private static final String videoUrlPrefix = Constant.BASE_URL;

    // 根据消息 ID 获取内容并关联卡片信息
    public List<AIContentInfo> getAllContentWithCardInfo() {
        return aiContentInfoMapper.getContentWithCardInfo();
    }

    public List<AIContentInfo> getContentWithCardInfoByHistoryId(Integer historyId) {
        return aiContentInfoMapper.getContentWithCardInfoByHistoryId(historyId);
    }

    public List<Integer> selectCardIdsByHistoryId(Integer historyId) {
        return aiContentInfoMapper.selectCardIdsByHistoryId(historyId);
    }

    public List<ChatMessageDTO> getTextMessagesByHistory(String historyId) {
        return aiContentInfoMapper.selectTextByHistoryId(historyId);
    }

    public Integer insertContentInfo(AIContentInfo contentInfo) {
        return aiContentInfoMapper.insert(contentInfo);
    }

    public List<AIAnalysisInfo> getListAnalysisByHistoryId(Integer historyId) {
        return aiContentInfoMapper.listAnalysisByHistoryId(historyId);
    }


    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> insertContentWithCardWithAnalysisInfo(SysFile sysFile, AIContentInfo aiContentInfo, String motionName) {
        UUID uuid = UUID.randomUUID();

        String taskId = uuid.toString();

        // 创建分析任务
        AIAnalysisInfo analysisInfo = new AIAnalysisInfo();
        analysisInfo.setVideoUrl(sysFile.getObject());
        analysisInfo.setMotionName(motionName);
        analysisInfo.setTaskId(taskId);
        analysisInfo.setTaskStatus("进行中");

        int analysisCount = aiAnalysisInfoMapper.insert(analysisInfo);


        // 检查是否插入成功
        if (analysisCount <= 0 || analysisInfo.getId() <= 0) {
            throw new MyException("分析任务创建失败");
        }


        // 更新 AICardInfo
        AICardInfo aiCardInfo = aiContentInfo.getCard();
        aiCardInfo.setAnalysisId(analysisInfo.getId());
        aiCardInfo.setVideoUrl(sysFile.getObject());

        System.out.println(aiCardInfo.getVideoUrl());

        // 更新 aiCardInfo
        int cardInsertCount = aiCardInfoMapper.insert(aiCardInfo);

        aiContentInfo.setCardId(aiCardInfo.getId());

        // 更新 AIContentInfo
        int contentInsertCount = aiContentInfoMapper.insert(aiContentInfo);

        if (cardInsertCount <= 0 || contentInsertCount <= 0) {
            throw new MyException("新增失败");
        }

        JSONObject postJson = new JSONObject();
        postJson.put("video_url", videoUrlPrefix + sysFile.getObject());
        postJson.put("task_id", taskId);
        postJson.put("motion_name", motionName);
        postJson.put("history_id", aiContentInfo.getHistoryId());
        postJson.put("content_id", aiContentInfo.getId());

//        System.setProperty("jsse.enableSNIExtension", "false");

        try {
            String response = HttpUtil.sendHttpRequestWithJson(postUrl, postJson.toString());
            JSONObject responseJson = JSONObject.parseObject(response);
            if (responseJson == null) {
                throw new MyException("Python后端返回空响应");
            }

            String message = responseJson.getString("message");
            if (!"success".equals(message)) {
                String errorMsg = responseJson.getString("error");
                throw new MyException("Python后端返回失败：" + (errorMsg != null ? errorMsg : "未知错误"));
            }

        } catch (Exception e) {
            throw new MyException("向python后端传递数据失败：" + e.getMessage());
        }

        return Map.of("analysis_id", analysisInfo.getId(), "content_id",aiContentInfo.getId(), "task_id", taskId);
    }

    public List<Map<String, Object>> getActionCategory() {
        QueryWrapper<AIStandardData> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "name");
        return aiStandardDataMapper.selectMaps(queryWrapper);
    }


}

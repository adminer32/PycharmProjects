package com.saaes.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saaes.common.core.web.MyException;
import com.saaes.common.core.web.RestResponse;
import com.saaes.system.client.entity.*;
import com.saaes.system.handler.AIWebSocketHandlerImpl;
import com.saaes.system.manager.WebSocketSessionManager;
import com.saaes.system.service.AICardInfoService;
import com.saaes.system.service.AIContentInfoService;
import com.saaes.system.service.AIVideosHistoryService;
import com.saaes.system.service.SysFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Tag(name = "聊天记录")
@RestController
@RequestMapping("/api/content")
public class AIContentInfoController {

    @Autowired
    private AIContentInfoService aiContentInfoService;

    @Autowired
    private AIVideosHistoryService aiVideosHistoryService;

    @Autowired
    private SysFileService sysFileService;

    @Autowired
    private WebSocketSessionManager webSocketSessionManager;

    @Autowired
    private AIWebSocketHandlerImpl aiWebSocketHandlerImpl;

    // 获取所有聊天记录并关联卡片信息
    @Operation(description = "获取所有聊天记录并关联卡片信息")
    @GetMapping("/all")
    public RestResponse<List<AIContentInfo>> getAllContent() {
        List<AIContentInfo> contentInfos = aiContentInfoService.getAllContentWithCardInfo();
        return RestResponse.success(contentInfos);

    }

    /**
     * 一条历史记录对多个分析结果
     * @param file
     * @param aiContentInfoJson
     * @param motionName
     * @return
     * @throws IOException
     */
    @Operation(description = "新增聊天信息和分析任务")
    @PostMapping("/insert")
    public RestResponse<?> insertContentInfo(@RequestParam(value = "file", required = false) MultipartFile file ,
                                             @RequestParam("aiContentInfo") String aiContentInfoJson, @RequestParam(value = "motionName", required = false) String motionName) throws IOException {

        // 将 JSON 字符串转换为 AIContentInfo 对象
        AIContentInfo aiContentInfo = new ObjectMapper().readValue(aiContentInfoJson, AIContentInfo.class);

        // 检查聊天信息是否为空
        if (aiContentInfo == null) {throw new MyException("聊天信息不能为空");}

        // 检查聊天类型是否有效
        if (aiContentInfo.getType() == null) {throw new MyException("聊天类型不能为空");}

        if (aiContentInfo.getType().equals("card") && motionName == null) {throw new MyException("运动类型不能为空");}

        if (aiContentInfo.getHistoryId() == null) {
            throw new MyException("历史记录ID不能为空");
        }

        AIVideosHistory aiVideosHistory = aiVideosHistoryService.queryById(aiContentInfo.getHistoryId());
        if (aiVideosHistory == null) {
            throw new MyException("该历史记录ID：" + aiContentInfo.getHistoryId() + "，所属数据不存在或已被删除");
        }

        String type = aiContentInfo.getType();

        // 处理类型为文本的情况
        if ("text".equals(type)) {
            int result = aiContentInfoService.insertContentInfo(aiContentInfo);
            if (result > 0) {
                String loginToken = StpUtil.getTokenInfo().getTokenValue();
                WebSocketSession session = webSocketSessionManager.getSession(loginToken);
                if(session != null) {
                    TextMessage textMessage = new TextMessage(aiContentInfoJson);
                    aiWebSocketHandlerImpl.handleTextMessage(session, textMessage);
                }
                return RestResponse.success("文本聊天添加成功", Map.of("content_id", aiContentInfo.getId()));
            }
            return RestResponse.error("文本聊天添加失败");
        }

        // 处理类型为卡片的情况
        if ("card".equals(type)) {
            // 如果文件为空或未上传
            if (file == null || file.isEmpty()) {
                throw new MyException("卡片信息必须包含上传的视频文件");
            }

            if(motionName == null) {
                throw new MyException("动作类型不能为空");
            }
            // 文件处理逻辑
            try {
                SysFile sysFile = sysFileService.uploadFile(file);
                return RestResponse.success("信息已上传", aiContentInfoService.insertContentWithCardWithAnalysisInfo(sysFile, aiContentInfo, motionName));
            } catch (Exception e) {
                return RestResponse.error("文件上传失败: " + e.getMessage());
            }
        }

        // 如果聊天类型不匹配
        throw new MyException("不支持的聊天类型");
    }

    @Operation(description = "获取所有动作分类")
    @GetMapping("/queryCategory")
    public RestResponse<List<Map<String, Object>>> getActionCategory() {
        return RestResponse.success("查询成功", aiContentInfoService.getActionCategory());
    }


    @Operation(description = "获取与历史记录id相关的聊天记录和卡片信息")
    @GetMapping("/queryByHistoryId")
    public RestResponse<List<AIContentInfo>> getContentByHistoryId(Integer historyId) {
        List<AIContentInfo> contentInfos = aiContentInfoService.getContentWithCardInfoByHistoryId(historyId);
        return RestResponse.success("查询成功", contentInfos);
    }

}



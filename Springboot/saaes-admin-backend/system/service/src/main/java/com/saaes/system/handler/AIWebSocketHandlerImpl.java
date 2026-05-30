package com.saaes.system.handler;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.saaes.common.core.Constant;
import com.saaes.system.client.dto.ChatMessageDTO;
import com.saaes.system.client.entity.AIAnalysisInfo;
import com.saaes.system.client.entity.AIContentInfo;
import com.saaes.system.manager.WebSocketSessionManager;
import com.saaes.system.service.AIContentInfoService;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class AIWebSocketHandlerImpl extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(AIWebSocketHandlerImpl.class);

    private static final String API_KEY = Constant.DASH_SCOPE_API_KEY;
    private static final String MODEL = Constant.DASH_SCOPE_MODEL;

    private final AIContentInfoService aiContentInfoService;
    private final WebSocketSessionManager webSocketSessionManager;  // 注入 WebSocketSessionManager

    public AIWebSocketHandlerImpl(AIContentInfoService aiContentInfoService, WebSocketSessionManager webSocketSessionManager) {
        this.aiContentInfoService = aiContentInfoService;
        this.webSocketSessionManager = webSocketSessionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 解析 Token，获取用户 ID
        String token = session.getUri().getQuery();
        if (token == null || !token.startsWith("token=")) {
            try {
                session.close();
            } catch (IOException ignored) {}
            return;
        }

        String userToken = token.substring(6); // 提取 token= 后的部分
        Object loginId = StpUtil.getLoginIdByToken(userToken);

        // 使用 WebSocketSessionManager 存储会话
        webSocketSessionManager.addSession(userToken, session);
        logger.info("用户Token： {} 连接 WebSocket", userToken);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        // 解析 Token，获取用户 ID
        String token = session.getUri().getQuery();
        if (token == null || !token.startsWith("token=")) {
            try {
                session.close();
            } catch (IOException ignored) {}
            return;
        }

        String userToken = token.substring(6); // 提取 token= 后的部分
        String payload = message.getPayload();
        JSONObject json = JSON.parseObject(payload);
        Integer historyId = json.getInteger("historyId");
        String userMessage = json.getString("message") == null ? json.getString("text") : json.getString("message");

        logger.info("用户 {} 发送消息: {}", userToken, userMessage);

        try {
            Generation gen = new Generation();
            Message userMsg = Message.builder().role(Role.USER.getValue()).content(userMessage).build();

            // 进行 AI 处理并流式返回
            streamCallWithMessage(session, gen, userMsg, historyId);
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            logger.error("API 调用异常: {}", e.getMessage());
            session.sendMessage(new TextMessage("发生错误: " + e.getMessage()));
        }
    }

    private void streamCallWithMessage(WebSocketSession session, Generation gen, Message userMsg, Integer historyId)
            throws NoApiKeyException, ApiException, InputRequiredException {

        List<Message> chatHistory = buildChatHistory(historyId, userMsg.getContent());

        GenerationParam param = GenerationParam.builder()
                .apiKey(API_KEY)
                .model(MODEL)
                .messages(chatHistory)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .incrementalOutput(true)
                .build();

        logger.info("开始流式调用 AI...");

        Flowable<GenerationResult> result = gen.streamCall(param);
        StringBuilder aiResponse = new StringBuilder();

        try {
            result.blockingForEach(response -> {
                String content = response.getOutput().getChoices().get(0).getMessage().getContent();
                aiResponse.append(content);
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(content));
                    } catch (IOException e) {
                        logger.error("WebSocket 发送消息失败", e);
                        throw new RuntimeException(e);
                    }
                } else {
                    logger.warn("WebSocket session 已关闭，停止发送");
                    throw new RuntimeException("WebSocket 已关闭");
                }
            });
        } catch (Exception e) {
            logger.warn("AI 流式输出中断: {}", e.getMessage());
        } finally {
            // 即使中途断开，也尽可能保存已生成的部分
            if (historyId != null && aiResponse.length() > 0) {
                AIContentInfo aiContentInfo = new AIContentInfo();
                aiContentInfo.setHistoryId(historyId);
                aiContentInfo.setSender("ai");
                aiContentInfo.setType("text");
                aiContentInfo.setText(aiResponse.toString());
                aiContentInfoService.insertContentInfo(aiContentInfo);
                logger.info("中断后保存AI内容成功");
            }
        }

        logger.info("AI 生成完成: {}", aiResponse);
    }

    public void sendMessageToUser(String loginId, String message) {
        WebSocketSession session = webSocketSessionManager.getSession(loginId);  // 通过 WebSocketSessionManager 获取会话
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                logger.error("WebSocket 发送消息失败", e);
            }
        } else {
            logger.error("WebSocket 会话不存在或已关闭，无法发送消息");
        }
    }

    private List<Message> buildChatHistory(Integer historyId, String userMessage) {

        List<Message> messageList = new ArrayList<>();

        if (historyId == null) {
            messageList.add(Message.builder().role(Role.USER.getValue()).content(userMessage).build());
            return messageList;
        }

        List<AIAnalysisInfo> analysisInfos = aiContentInfoService.getListAnalysisByHistoryId(historyId);

        List<ChatMessageDTO> historyList = aiContentInfoService.getTextMessagesByHistory(String.valueOf(historyId));

        // 添加 AI 分析结果（转为 message 格式，作为 AI 的历史消息）
        for (AIAnalysisInfo analysisInfo : analysisInfos) {
            String content = JSON.toJSONString(analysisInfo);
            messageList.add(Message.builder()
                    .role(Role.ASSISTANT.getValue())
                    .content(content)
                    .build());
        }

        // 将历史内容按顺序封装为 Message（注意 sender 是 user/ai）
        for (ChatMessageDTO item : historyList) {
            if ("user".equals(item.getRole())) {
                messageList.add(Message.builder().role(Role.USER.getValue()).content(item.getContent()).build());
            } else if ("ai".equals(item.getRole())) {
                messageList.add(Message.builder().role(Role.ASSISTANT.getValue()).content(item.getContent()).build());
            }
        }

        messageList.add(Message.builder().role(Role.USER.getValue()).content("在后续聊天中不要出现原始运动数据(比如self_left_x_data,self_left_y_data,x1,y1等)，X1/Y1/X2/Y2指的是：X1Y1是左脚，X2Y2是右脚，避免在回答中直接出现X1/Y1/X2/Y2/X轴/Y轴，如需显示原始数据请以视频第几秒代替，回答问题时不要生成代码，仅回复文本内容。").build());

        // 当前用户输入也加入上下文
        messageList.add(Message.builder().role(Role.USER.getValue()).content(userMessage).build());

        logger.info("聊天记录：" + JSON.toJSONString(messageList));

        return messageList;
    }
}

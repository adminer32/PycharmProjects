package com.saaes.system.listener;

import com.saaes.system.client.event.PracticeRemindEvent;
import com.saaes.system.handler.AIWebSocketHandlerImpl;
import jakarta.annotation.Resource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 练习提醒事件监听器 (在 Service 模块中，负责通过 WebSocket 发送通知)
 */
@Component
public class PracticeRemindListener {

    @Resource
    private AIWebSocketHandlerImpl webSocketHandler;

    @EventListener
    public void handlePracticeRemindEvent(PracticeRemindEvent event) {
        String message = "{\"type\": \"PRACTICE_REMINDER\", \"title\": \"" + event.getTaskTitle()
                + "\", \"content\": \"老师提醒您及时完成练习任务\"}";
        webSocketHandler.sendMessageToUser(event.getUserId(), message);
    }
}

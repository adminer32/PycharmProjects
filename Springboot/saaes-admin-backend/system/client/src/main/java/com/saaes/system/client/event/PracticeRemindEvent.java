package com.saaes.system.client.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 练习提醒事件 (用于解耦 Admin 模块与 WebSocket 推送)
 */
@Getter
public class PracticeRemindEvent extends ApplicationEvent {

    private final String userId;
    private final String taskTitle;

    public PracticeRemindEvent(Object source, String userId, String taskTitle) {
        super(source);
        this.userId = userId;
        this.taskTitle = taskTitle;
    }
}

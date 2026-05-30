package com.system.service.home.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class HomeVO {
    
    @Data
    public static class Overview {
        private Integer totalStudents;
        private Map<String, Object> todayHomework;
        private Map<String, Object> todayCheckin;
        private Integer pendingHomework;
    }
    
    @Data
    public static class PendingItem {
        private Long id;
        private String itemType;
        private String icon;
        private String title;
        private String description;
        private Integer countTotal;
        private Integer countUrgent;
        private String actionText;
        private String actionRoute;
    }
    
    @Data
    public static class Notice {
        private Long id;
        private String title;
        private String content;
        private String notificationType;
        private Boolean isImportant;
        private Integer readCount;
        private Integer totalStudents;
        private String time;
    }
    
    @Data
    public static class NoticeInput {
        private Long id;
        private Long classId;
        private Long teacherId;
        private String title;
        private String content;
        private Boolean isImportant;
    }
}

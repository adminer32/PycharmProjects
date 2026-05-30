package com.system.service.ai.vo;

import lombok.Data;

@Data
public class AiTeachingAdvice {
    private Long id;
    private String targetType;
    private Long targetId;
    private String priorityLevel;
    private String title;
    private String description;
    private Object suggestions;
    private java.time.LocalDate weekStartDate;
    private java.time.LocalDateTime createdAt;
}

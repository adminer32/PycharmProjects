package com.system.service.homework.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(title = "学生提交视图")
@Data
public class SubmissionVO implements Serializable {

    @Schema(title = "提交ID")
    private Long id;

    @Schema(title = "学生ID")
    private Long studentId;

    @Schema(title = "学生姓名")
    private String studentName;

    @Schema(title = "学生头像")
    private String studentAvatar;

    @Schema(title = "学生账号")
    private String studentAccount;

    @Schema(title = "视频URL")
    private String videoUrl;

    @Schema(title = "提交时间")
    private LocalDateTime submitTime;

    @Schema(title = "AI评分")
    private BigDecimal aiScore;

    @Schema(title = "教师评分")
    private BigDecimal teacherScore;

    @Schema(title = "教师评语")
    private String feedback;

    @Schema(title = "AI 建议")
    private String aiSuggestion;

    @Schema(title = "状态：0-待批改，1-已批改，-1-未提交")
    private Integer status;

    @Schema(title = "是否已提交")
    private Boolean submitted;

    @Data
    public static class StudentInfo implements Serializable {
        private Long studentId;
        private String studentName;
        private String studentAvatar;
        private String studentAccount;
    }
}

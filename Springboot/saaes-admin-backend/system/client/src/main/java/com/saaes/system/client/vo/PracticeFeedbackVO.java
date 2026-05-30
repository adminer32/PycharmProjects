package com.saaes.system.client.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 练习评价列表VO
 */
@Schema(title = "练习评价列表")
@Data
public class PracticeFeedbackVO {

    @Schema(title = "提交ID")
    private Integer submissionId;

    @Schema(title = "任务ID")
    private Integer taskId;

    @Schema(title = "任务标题")
    private String taskTitle;

    @Schema(title = "学生ID")
    private Integer studentId;

    @Schema(title = "学生姓名")
    private String studentName;

    @Schema(title = "班级ID")
    private Integer classId;

    @Schema(title = "班级名称")
    private String className;

    @Schema(title = "视频URL")
    private String videoUrl;

    @Schema(title = "文件ID")
    @JsonIgnore
    private Integer fileId;

    @Schema(title = "文件安全码")
    private String fileCode;

    @Schema(title = "提交时间")
    private LocalDateTime submitTime;

    @Schema(title = "评价状态 (0: 未评价, 1: 已评价)")
    private Integer feedbackStatus;

    @Schema(title = "评价ID")
    private Integer feedbackId;

    @Schema(title = "评价内容")
    private String comment;

    @Schema(title = "评价时间")
    private LocalDateTime feedbackTime;
}

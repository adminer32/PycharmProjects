package com.saaes.system.client.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(title = "练习任务视图对象")
public class PracticeTaskVO {

    @Schema(title = "任务ID")
    private Integer id;

    @Schema(title = "任务标题")
    private String title;

    @Schema(title = "任务要求/描述")
    private String content;

    @Schema(title = "发布老师名称")
    private String teacherName;

    @Schema(title = "班级名称")
    private String className;

    @Schema(title = "截止时间")
    private LocalDateTime deadline;

    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @Schema(title = "提交状态 (0: 未完成, 1: 已完成)")
    private Integer submissionStatus;

    @Schema(title = "批阅状态 (0: 未批阅, 1: 已批阅)")
    private Integer feedbackStatus;

    @Schema(title = "视频URL")
    private String videoUrl;

    @Schema(title = "评语")
    private String comment;

    @Schema(title = "文件安全码")
    @JsonIgnore
    private String fileCode;
}

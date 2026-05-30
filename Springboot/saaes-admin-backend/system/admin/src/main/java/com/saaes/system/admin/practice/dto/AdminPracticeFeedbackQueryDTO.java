package com.saaes.system.admin.practice.dto;

import com.saaes.common.core.admin.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Schema(title = "练习评价查询")
@EqualsAndHashCode(callSuper = true)
public class AdminPracticeFeedbackQueryDTO extends BasePageQuery {

    @Schema(title = "任务ID")
    private Integer taskId;

    @Schema(title = "学生姓名")
    private String studentName;

    @Schema(title = "班级ID")
    private Integer classId;

    @Schema(title = "评价状态 (0: 未评价, 1: 已评价)")
    private Integer feedbackStatus;
}

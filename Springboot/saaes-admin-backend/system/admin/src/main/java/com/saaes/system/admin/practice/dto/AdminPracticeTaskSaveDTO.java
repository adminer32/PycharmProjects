package com.saaes.system.admin.practice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.saaes.common.core.validator.CreateGroup;
import com.saaes.common.core.validator.UpdateGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(title = "练习任务保存对象")
public class AdminPracticeTaskSaveDTO {

    @Schema(title = "任务ID (更新时必传)")
    @NotNull(message = "任务ID不能为空", groups = UpdateGroup.class)
    private Integer id;

    @Schema(title = "任务标题")
    @NotBlank(message = "任务标题不能为空", groups = CreateGroup.class)
    private String title;

    @Schema(title = "任务要求/描述")
    private String content;

    @Schema(title = "分配班级ID")
    @NotNull(message = "必须指定班级", groups = CreateGroup.class)
    private Integer classId;

    @Schema(title = "截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS][XXX]", timezone = "UTC")
    private LocalDateTime deadline;

    @Schema(title = "发布老师ID (默认为当前登录用户)")
    private Integer teacherId;
}

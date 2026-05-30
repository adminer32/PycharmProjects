package com.system.service.homework.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(title = "作业详情视图")
@Data
@EqualsAndHashCode(callSuper = true)
public class HomeworkDetailVO extends HomeworkListVO {

    @Schema(title = "示范视频URL")
    private String demoVideoUrl;

    @Schema(title = "班级ID")
    private Long classId;

    @Schema(title = "班级名称")
    private String className;
}

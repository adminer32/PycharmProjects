package com.saaes.system.admin.classes.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(title = "班级信息VO")
public class AdminClassVO {

    @Schema(title = "班级ID")
    private Integer id;

    @Schema(title = "班级名称")
    private String name;

    @Schema(title = "主教老师ID")
    private Integer teacherId;

    @Schema(title = "主教老师姓名")
    private String teacherName;

    @Schema(title = "班级等级ID")
    private Integer levelId;

    @Schema(title = "班级等级名称")
    private String levelName;

    @Schema(title = "训练目标/简介")
    private String description;

    @Schema(title = "是否启用")
    private Boolean enabled;

    @Schema(title = "班级人数")
    private Integer studentCount;

    @Schema(title = "创建时间")
    private LocalDateTime createTime;
}

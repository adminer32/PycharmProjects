package com.saaes.system.admin.classes.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(title = "班级学生VO")
public class AdminStudentVO {

    @Schema(title = "学生用户ID")
    private Integer id;

    @Schema(title = "账号")
    private String code;

    @Schema(title = "姓名")
    private String name;

    @Schema(title = "手机号")
    private String telephone;

    @Schema(title = "入班时间")
    private LocalDateTime joinTime;
}

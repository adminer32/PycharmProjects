package com.saaes.system.admin.classes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title = "班级学生创建对象")
public class AdminStudentSaveDTO {

    @Schema(title = "班级ID (必填)")
    private Integer classId;

    @Schema(title = "学生姓名 (必填)")
    private String name;

    @Schema(title = "账号/Code (必填)")
    private String code;

    @Schema(title = "密码 (选填，不填则默认为123456)")
    private String password;

    @Schema(title = "手机号")
    private String telephone;
}

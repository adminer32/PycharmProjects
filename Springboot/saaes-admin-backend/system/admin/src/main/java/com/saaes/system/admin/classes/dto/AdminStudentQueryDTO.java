package com.saaes.system.admin.classes.dto;

import com.saaes.common.core.admin.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@Schema(title = "班级学生查询参数")
@EqualsAndHashCode(callSuper = true)
public class AdminStudentQueryDTO extends BasePageQuery {

    @Schema(title = "班级ID (必填)")
    private Integer classId;

    @Schema(title = "学生姓名")
    private String name;

    @Schema(title = "账号/Code")
    private String code;
}

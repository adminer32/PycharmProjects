package com.saaes.system.admin.classes.dto;

import com.saaes.common.core.admin.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Schema(title = "班级查询参数")
@EqualsAndHashCode(callSuper = true)
public class AdminClassQueryDTO extends BasePageQuery {

    @Schema(title = "班级名称")
    private String name = "";

    @Schema(title = "老师ID (超管可查特定老师，老师仅查自己)")
    private Integer teacherId;

    @Schema(title = "班级等级ID")
    private Integer levelId;

    @Schema(title = "启用状态")
    private Boolean enabled = true;
}

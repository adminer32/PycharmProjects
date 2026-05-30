package com.saaes.system.admin.practice.dto;

import com.saaes.common.core.admin.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Schema(title = "练习任务查询参数")
@EqualsAndHashCode(callSuper = true)
public class AdminPracticeTaskQueryDTO extends BasePageQuery {

    @Schema(title = "任务标题")
    private String title;

    @Schema(title = "班级ID")
    private Integer classId;

}

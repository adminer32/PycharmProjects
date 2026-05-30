package com.saaes.system.client.dto;

import com.saaes.common.core.admin.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Schema(title = "练习任务查询参数")
@EqualsAndHashCode(callSuper = true)
public class PracticeTaskQueryDTO extends BasePageQuery {

    @Schema(title = "任务标题")
    private String title;

}

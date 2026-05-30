package com.saaes.system.admin.classes.dto;

import com.saaes.common.core.validator.UpdateGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "班级保存对象")
public class AdminClassSaveDTO {

    @Schema(title = "班级ID (更新时必传)")
    @NotNull(message = "班级ID不能为空", groups = UpdateGroup.class)
    private Integer id;

    @Schema(title = "班级名称")
    @NotBlank(message = "班级名称不能为空")
    private String name;

    @Schema(title = "老师ID (超管可指定，老师默认为自己)")
    private Integer teacherId;

    @Schema(title = "班级等级ID")
    private Integer levelId;

    @Schema(title = "训练目标/简介")
    private String description;

    @Schema(title = "是否启用")
    private Boolean enabled;
}

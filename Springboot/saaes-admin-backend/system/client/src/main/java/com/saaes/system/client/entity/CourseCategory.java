package com.saaes.system.client.entity;

import com.saaes.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CourseCategory extends BaseEntity<Integer> {

    @Schema(title = "分类id")
    private Integer id;

    @Schema(title = "父分类id")
    private Integer parentId;

    @Schema(title = "父类名称")
    private String parentName;

    @Schema(title = "分类名称")
    private String name;

    @Schema(title = "状态")
    private Integer enable;

}

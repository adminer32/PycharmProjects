package com.saaes.system.client.entity;

import com.saaes.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(title = "课程章节")
@Table
@Data
@EqualsAndHashCode(callSuper = true)
public class ChapterInfo extends BaseEntity<Integer> {

    @Schema(title = "章节名称")
    private String name;

    @Schema(title = "关联的课程ID")
    private String course_id;

    @Schema(title = "章节内容")
    private String content;

    @Schema(title = "状态")
    private Integer enable;

}

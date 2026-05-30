package com.saaes.system.client.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


import io.swagger.v3.oas.annotations.media.Schema;


@Schema(title = "班级")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_class")
public class SysClass extends com.saaes.common.core.entity.BaseEntity<Integer> {


    private String name;

    @TableField("teacher_id")
    private Integer teacherId;

    @TableField("level_id")
    private Integer levelId;

    private String description;

    private Boolean enabled;

}

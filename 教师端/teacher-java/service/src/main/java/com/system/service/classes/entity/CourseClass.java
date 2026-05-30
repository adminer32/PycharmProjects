package com.system.service.classes.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@TableName("`class`")
public class CourseClass {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("teacher_id")
    private Long teacherId;

    private Integer grade;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

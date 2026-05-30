package com.saaes.system.client.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;


import com.saaes.common.core.entity.BaseEntity;

@Schema(title = "班级学生表")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("class_students")
public class ClassStudents extends BaseEntity<Integer> {


    @TableField("class_id")
    private Integer classId;

    @TableField("user_id")
    private Integer userId;

}

package com.system.service.profile.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


@Data
@TableName("teacher_profile")
public class TeacherProfile implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teacherId;

    private String name;

    private String avatar;

    private String email;

    private String phone;

    private Integer gender;

    private String specialty;

    private String bio;

    private Integer active;

    private String department;

    private Integer teachingYears;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

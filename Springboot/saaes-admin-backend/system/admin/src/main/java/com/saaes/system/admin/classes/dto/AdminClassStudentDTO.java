package com.saaes.system.admin.classes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(title = "班级成员管理对象")
public class AdminClassStudentDTO {

    @Schema(title = "班级ID")
    private Integer classId;

    @Schema(title = "学生ID列表")
    private List<Integer> studentIds;
}

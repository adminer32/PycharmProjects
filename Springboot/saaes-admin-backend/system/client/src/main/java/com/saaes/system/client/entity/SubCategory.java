package com.saaes.system.client.entity;

import lombok.Data;

import java.util.List;

// 子类
@Data
public class SubCategory {
    private Integer subCategoryId;
    private String subCategoryName;
    private List<CourseInfo> subCategoryCourse; // 课程列表

}

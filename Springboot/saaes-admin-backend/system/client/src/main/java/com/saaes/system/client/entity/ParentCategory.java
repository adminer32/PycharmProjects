package com.saaes.system.client.entity;

import lombok.Data;

import java.util.List;

// 父类
@Data
public class ParentCategory {
    private Integer parentId;
    private String parentName;
    private List<SubCategory> subCategoryList; // 子类列表

}

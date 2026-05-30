package com.system.service.classes.controller;

import com.system.service.classes.service.ClassService;
import com.system.service.classes.vo.ClassListVO;
import com.system.web.RestResponse;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "班级管理接口")
@RestController
@RequestMapping("/api/class")
@SaCheckRole("TEACHER")
public class ClassController {
    
    @Resource
    private ClassService classService;

    @Operation(summary = "获取教师所属班级列表", description = "根据当前登录教师ID查询其管理的所有班级")
    @GetMapping("/list")
    public RestResponse<List<ClassListVO>> getTeacherClasses() {
        Long teacherId = StpUtil.getLoginIdAsLong();
        List<ClassListVO> classes = classService.getTeacherClasses(teacherId);
        return RestResponse.success(classes);
    }
}

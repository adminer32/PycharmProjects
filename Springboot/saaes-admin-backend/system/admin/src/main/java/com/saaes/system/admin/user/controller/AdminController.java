package com.saaes.system.admin.user.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.saaes.common.core.web.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台专用接口")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Operation(description = "后台仪表盘数据")
    @SaCheckRole(value = { "teacher", "super_admin" }, mode = SaMode.OR) // 对老师或超级管理员进行验证
    @GetMapping("/dashboard")
    public RestResponse<String> dashboard() {
        return RestResponse.success("欢迎进入后台仪表盘！");
    }

}
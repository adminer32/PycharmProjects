package com.system.service.profile.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.system.service.profile.entity.TeacherProfile;
import com.system.service.profile.service.TeacherProfileService;
import com.system.web.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


@Tag(name = "教师档案管理")
@RestController
@RequestMapping("/api/profile")
@SaCheckRole("TEACHER")
public class TeacherProfileController {

    @Resource
    private TeacherProfileService teacherProfileService;

    @Operation(description = "获取当前登录教师档案")
    @GetMapping("/me")
    public RestResponse<TeacherProfile> getMyProfile() {
        Long userId = StpUtil.getLoginIdAsLong();
        TeacherProfile profile = teacherProfileService.getByUserId(userId);
        return RestResponse.success(profile);
    }

    @Operation(description = "更新当前教师档案")
    @PutMapping("/me")
    public RestResponse<String> updateProfile(@RequestBody TeacherProfile profile) {
        Long userId = StpUtil.getLoginIdAsLong();
        teacherProfileService.saveOrUpdateProfile(userId, profile);
        return RestResponse.success("保存成功");
    }
}

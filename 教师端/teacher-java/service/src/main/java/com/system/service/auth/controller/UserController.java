package com.system.service.auth.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import com.system.service.auth.dto.LoginRequestDTO;
import com.system.service.auth.vo.UserInfoVO;
import com.system.service.auth.service.UserService;
import com.system.web.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户认证接口")
@RestController
@RequestMapping("/api/system/user")
@SaCheckRole("TEACHER")
public class UserController {

    @Resource
    private UserService userService;

    @SaIgnore
    @Operation(description = "用户登录 (带 Turnstile)")
    @PostMapping("/login")
    public RestResponse<String> login(
            @RequestBody LoginRequestDTO loginDTO) {
        String token = userService.login(loginDTO);
        return RestResponse.success("登录成功", token);
    }

    @Operation(description = "获取个人基本信息")
    @GetMapping("/me")
    public RestResponse<UserInfoVO> getInfo() {
        return RestResponse.success(userService.getUserInfo());
    }

    @Operation(description = "用户注销")
    @PostMapping("/logout")
    public RestResponse<?> logout() {
        userService.logout();
        return RestResponse.success("注销成功");
    }
}

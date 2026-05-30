package com.saaes.system.admin.auth.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.saaes.common.core.web.RestResponse;
import com.saaes.system.admin.auth.service.AdminInternalAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import com.saaes.system.admin.auth.vo.AdminTokenCheckVO;

@SaIgnore
@Tag(name = "Auth-token验证")
@RestController
@RequestMapping("/api/auth")
public class AdminInternalAuthController {

    @Resource
    private AdminInternalAuthService adminInternalAuthService;

    @Operation(description = "校验Token并返回用户信息")
    @GetMapping("/token")
    public RestResponse<?> checkToken(
            @RequestParam String token,
            @RequestParam(required = false) String secret,
            HttpServletRequest request) {
        try {
            String finalSecret = request.getHeader("Internal-Secret");
            if (finalSecret == null || finalSecret.isEmpty()) {
                finalSecret = secret;
            }

            AdminTokenCheckVO result = adminInternalAuthService.checkToken(token, finalSecret);
            return RestResponse.success(result);
        } catch (Exception e) {
            return RestResponse.error(e.getMessage());
        }
    }
}

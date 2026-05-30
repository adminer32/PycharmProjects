package com.saaes.system.admin.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.saaes.system.admin.auth.service.AdminInternalAuthService;
import com.saaes.system.admin.user.service.AdminUserService;
import com.saaes.system.admin.user.vo.AdminUserVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

import com.saaes.system.admin.auth.vo.AdminTokenCheckVO;

@Service
public class AdminInternalAuthServiceImpl implements AdminInternalAuthService {

    @Resource
    private AdminUserService adminUserService;

    @org.springframework.beans.factory.annotation.Value("${sys.auth.internalSecret:saaes-secret-key}")
    private String internalSecret;

    @Override
    public AdminTokenCheckVO checkToken(String token, String secret) {
        // 0. 安全校验：校验内部密钥
        if (secret == null || !internalSecret.equals(secret)) {
            throw new RuntimeException("非法调用：内部密钥错误");
        }

        // 1. 获取 Token 对应的 LoginId
        Object loginId = StpUtil.getLoginIdByToken(token);

        if (loginId == null) {
            throw new RuntimeException("Token 无效或已过期");
        }

        // 2. 获取用户详情
        Integer userId;
        try {
            userId = Integer.valueOf(loginId.toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Token 数据异常");
        }

        AdminUserVO userVO = adminUserService.getUserDetail(userId);
        if (userVO == null) {
            throw new RuntimeException("用户不存在");
        }

        // 3. 获取角色和权限列表
        List<String> roleList = StpUtil.getRoleList(loginId);
        List<String> permissionList = StpUtil.getPermissionList(loginId);

        // 4. 使用 Builder 模式构造 VO
        return AdminTokenCheckVO.builder()
                .valid(true)
                .user(userVO)
                .roles(roleList)
                .permissions(permissionList)
                .build();
    }
}

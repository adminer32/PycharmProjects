package com.system.configuration;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * SaToken 自定义权限加载实现类
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 目前没有细分权限，返回空列表
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 从 Session 中动态获取 Role
        List<String> list = new ArrayList<>();
        String role = (String) StpUtil.getSessionByLoginId(loginId).get("Role");
        if (StrUtil.isNotBlank(role)) {
            list.add(role);
        }
        return list;
    }
}

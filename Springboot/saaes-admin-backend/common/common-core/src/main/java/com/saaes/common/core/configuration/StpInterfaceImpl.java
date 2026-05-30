package com.saaes.common.core.configuration;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.saaes.common.core.dto.SysLoginUserInfoDTO;
import com.saaes.common.core.dto.SysMenuDTO;
import com.saaes.common.core.dto.SysOrgRoleDTO;
import com.saaes.common.core.service.CommonService;
import com.saaes.common.core.utils.LoginUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义权限加载接口实现类
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private CommonService commonService;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        SysLoginUserInfoDTO userInfo = session.getModel(LoginUtil.SYS_USER_KEY, SysLoginUserInfoDTO.class);
        if (userInfo == null || userInfo.getRoles() == null)
            return null;

        // 获取当前激活的角色ID
        Integer roleId = userInfo.getRoles().stream()
                .filter(r -> Boolean.TRUE.equals(r.getActive()))
                .map(SysOrgRoleDTO::getSysRoleId)
                .findFirst()
                .orElse(null);

        // 如果没有激活角色，尝试使用列表第一个（兜底策略），或者直接返回空
        if (roleId == null && !userInfo.getRoles().isEmpty()) {
            roleId = userInfo.getRoles().get(0).getSysRoleId();
        }

        if (roleId == null)
            return java.util.Collections.emptyList();

        List<SysMenuDTO> rolePermissions = commonService.getRolePermissions(roleId, false);
        return rolePermissions.stream().map(SysMenuDTO::getName).toList();
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        SysLoginUserInfoDTO loginUserInfoDTO = session.getModel(LoginUtil.SYS_USER_KEY, SysLoginUserInfoDTO.class);
        if (loginUserInfoDTO == null)
            return null;
        List<SysOrgRoleDTO> roles = loginUserInfoDTO.getRoles();
        return roles.stream().map(SysOrgRoleDTO::getRoleName).toList();
    }
}

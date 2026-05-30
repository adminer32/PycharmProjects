package com.system.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.system.dto.OnlineUserDTO;
import com.system.dto.SysLoginUserInfoDTO;


/**
 * 获取当前登录用户
 */
public class LoginUtil {
    public final static String SYS_USER_KEY = "sysUserInfo";
    public final static String WX_USER_KEY = "wxUserInfo";

    /**
     * 获取当前系统登录信息
     */
    public static SysLoginUserInfoDTO getSysUserInfo() {
        return StpUtil.getSession().getModel(SYS_USER_KEY, SysLoginUserInfoDTO.class);
    }

    /**
     * 获取当前登录用户详情
     */
    public static OnlineUserDTO getOnlineUserInfo() {
        return StpUtil.getTokenSession().getModel(SYS_USER_KEY, OnlineUserDTO.class);
    }


    /**
     * 获取当前系统登录用户ID
     */
    public static Integer getUserId() {
        SysLoginUserInfoDTO userInfo = getSysUserInfo();
        if (userInfo != null && userInfo.getUser() != null) {
            return userInfo.getUser().getId();
        }
        return StpUtil.getLoginIdAsInt();
    }
}

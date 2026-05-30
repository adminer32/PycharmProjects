package com.saaes.system.admin.auth.service;

import com.saaes.system.admin.auth.vo.AdminTokenCheckVO;


/**
 * 内部 Auth 验证服务接口
 */
public interface AdminInternalAuthService {

    /**
     * 校验 Token 并获取完整的用户上下文
     */
    AdminTokenCheckVO checkToken(String token, String secret);
}

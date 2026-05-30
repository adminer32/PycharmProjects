package com.saaes.system.admin.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.saaes.common.core.web.PageResult;
import com.saaes.system.admin.user.dto.AdminUserQueryDTO;
import com.saaes.system.admin.user.dto.AdminUserSaveDTO;
import com.saaes.system.admin.user.vo.AdminUserVO;
import com.saaes.system.client.entity.SysRole;
import com.saaes.system.client.entity.SysUser;

import java.util.List;


public interface AdminUserService extends IService<SysUser> {

    /**
     * 分页查询用户
     */
    PageResult<AdminUserVO> selectUserPage(AdminUserQueryDTO queryDTO);

    /**
     * 保存用户 (新增或更新)
     */
    SysUser createOrUpdate(AdminUserSaveDTO saveDTO);

    /**
     * 获取用户详情 (包含角色信息)
     */
    AdminUserVO getUserDetail(Integer id);

    /**
     * 踢人下线
     */
    void kick(Integer id);

    /**
     * 获取所有可用角色列表
     */
    List<SysRole> getRoleList();

    /**
     * 查询已删除用户 (回收站)
     */
    PageResult<AdminUserVO> selectRecyclePage(AdminUserQueryDTO queryDTO);

    /**
     * 恢复已删除用户
     */
    void restore(List<Integer> ids);

    void deleteUsers(List<Integer> ids);
}

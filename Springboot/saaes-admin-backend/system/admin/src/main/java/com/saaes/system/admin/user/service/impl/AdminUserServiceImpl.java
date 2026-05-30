package com.saaes.system.admin.user.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saaes.common.core.web.MyException;
import com.saaes.common.core.web.PageResult;
import com.saaes.system.admin.classes.mapper.AdminClassStudentMapper;
import com.saaes.system.admin.user.dto.AdminUserQueryDTO;
import com.saaes.system.admin.user.dto.AdminUserSaveDTO;
import com.saaes.system.admin.user.mapper.AdminSysUserJobMapper;
import com.saaes.system.admin.user.mapper.AdminRoleMapper;
import com.saaes.system.admin.user.mapper.AdminUserMapper;
import com.saaes.system.admin.user.service.AdminUserService;
import com.saaes.system.admin.user.vo.AdminUserVO;
import com.saaes.system.client.entity.ClassStudents;
import com.saaes.system.client.entity.SysRole;
import com.saaes.system.client.entity.SysUser;
import com.saaes.system.client.entity.SysUserJob;

import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class AdminUserServiceImpl extends ServiceImpl<AdminUserMapper, SysUser> implements AdminUserService {

    @Resource
    private AdminSysUserJobMapper adminSysUserJobMapper;

    @Resource
    private AdminRoleMapper adminRoleMapper;

    @Resource
    private AdminClassStudentMapper adminClassStudentMapper;

    @Override
    public PageResult<AdminUserVO> selectUserPage(AdminUserQueryDTO queryDTO) {
        Page<AdminUserVO> pageObj = queryDTO.toPage();

        QueryWrapper<SysUser> wrapper = this.commonPage(queryDTO);

        wrapper.orderByDesc("u.create_time");

        return new PageResult<>(this.baseMapper.selectUserPage(pageObj, wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser createOrUpdate(AdminUserSaveDTO saveDTO) {
        if (saveDTO.getId() == null) {
            return this.createUser(saveDTO);
        } else {
            return this.updateUser(saveDTO);
        }
    }

    @Override
    public AdminUserVO getUserDetail(Integer id) {
        Assert.notNull(id, "用户ID不能为空");
        SysUser sysUser = this.getById(id);
        if (sysUser == null) {
            throw new MyException("用户不存在或已被删除");
        }
        AdminUserVO vo = BeanUtil.copyProperties(sysUser, AdminUserVO.class);

        // 查询关联的角色信息
        SysUserJob job = adminSysUserJobMapper.selectOne(new LambdaQueryWrapper<SysUserJob>()
                .eq(SysUserJob::getUserId, id)
                .eq(SysUserJob::getType, 1)
                .last("LIMIT 1"));

        if (job != null && job.getSysRoleId() != null) {
            vo.setRoleId(job.getSysRoleId());
            // 补充查询角色名称
            SysRole role = adminRoleMapper.selectById(job.getSysRoleId());
            if (role != null) {
                vo.setRoleName(role.getName());
            }
        }
        return vo;
    }

    @Override
    public void kick(Integer id) {
        StpUtil.kickout(id);
    }

    @Override
    public List<SysRole> getRoleList() {
        return adminRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getEnabled, true)
                .eq(SysRole::getDeleted, false) // 过滤已删除的角色
                .orderByAsc(SysRole::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUsers(List<Integer> userIds) {
        Assert.notEmpty(userIds, "待删除用户ID列表不能为空");

        // 校验用户是否存在且未被删除
        long existCount = this.count(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, userIds));
        if (existCount != userIds.size()) {
            // 找出无效id
            List<Integer> validIds = this.list(new LambdaQueryWrapper<SysUser>()
                            .select(SysUser::getId)
                            .in(SysUser::getId, userIds))
                    .stream().map(SysUser::getId).collect(Collectors.toList());
            Collection<Integer> invalidIds = CollUtil.subtract(userIds, validIds);
            throw new MyException("用户不存在或已被删除：" + invalidIds);
        }

        // 级联删除：用户-角色关联（仅删除学生角色）
        adminSysUserJobMapper.delete(new LambdaQueryWrapper<SysUserJob>()
                .in(SysUserJob::getUserId, userIds)
                .eq(SysUserJob::getType, 1)
                .eq(SysUserJob::getDeleted, false));  // 只删除未删除的关联

        // 级联删除：班级学生关联
        adminClassStudentMapper.delete(new LambdaQueryWrapper<ClassStudents>()
                .in(ClassStudents::getUserId, userIds)
                .eq(ClassStudents::getDeleted, false));

        // 逻辑删除用户主表
        this.removeByIds(userIds);
    }

    @Override
    public PageResult<AdminUserVO> selectRecyclePage(AdminUserQueryDTO queryDTO) {
        Page<AdminUserVO> pageObj = queryDTO.toPage();

        QueryWrapper<SysUser> wrapper = this.commonPage(queryDTO);

        wrapper.orderByDesc("u.update_time");

        return new PageResult<>(this.baseMapper.selectRecyclePage(pageObj, wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(List<Integer> ids) {
        Assert.notEmpty(ids, "待恢复的用户ids列表不能为空");

        //恢复用户本身
        this.baseMapper.restoreByIds(ids);

        //恢复用户的角色关联
        adminSysUserJobMapper.update(null, Wrappers.<SysUserJob>lambdaUpdate()
                .set(SysUserJob::getDeleted, false)
                .in(SysUserJob::getUserId, ids)
                .eq(SysUserJob::getType, 1));

        //恢复班级关联
        adminClassStudentMapper.restoreByUserIds(ids);
    }

    /**
     * 统一查询用户参数
     */
    private QueryWrapper<SysUser> commonPage(AdminUserQueryDTO queryDTO) {
        QueryWrapper<SysUser> wrapper = Wrappers.query();
        wrapper.like(StrUtil.isNotBlank(queryDTO.getName()), "u.name", queryDTO.getName())
                .like(StrUtil.isNotBlank(queryDTO.getCode()), "u.code", queryDTO.getCode())
                .like(StrUtil.isNotBlank(queryDTO.getEmail()), "u.email", queryDTO.getEmail())
                .like(StrUtil.isNotBlank(queryDTO.getTelephone()), "u.telephone", queryDTO.getTelephone())
                .eq(queryDTO.getEnabled() != null, "u.enabled", queryDTO.getEnabled());
        return wrapper;
    }

    /**
     * 新增用户
     */
    private SysUser createUser(AdminUserSaveDTO dto) {
        if (dto.getRoleId() == null) {
            throw new MyException("必须为新用户分配一个角色！");
        }
        //校验唯一
        this.checkCodeUnique(dto.getCode(), null);

        SysUser user = BeanUtil.copyProperties(dto, SysUser.class);

        String rawPassword = StrUtil.isBlank(dto.getPassword()) ? "123456" : dto.getPassword();
        user.setPassword(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));

        user.setPlatform(this.determinePlatform(dto));
        user.setEnabled(ObjectUtil.defaultIfNull(dto.getEnabled(), true));
        user.setStatus(ObjectUtil.defaultIfNull(dto.getStatus(), 1));
        user.setDeleted(false);

        this.save(user);

        this.bindUserRole(user.getId(), dto.getRoleId());

        return user;
    }

    /**
     * 修改用户
     */
    private SysUser updateUser(AdminUserSaveDTO dto) {
        SysUser user = this.getById(dto.getId());
        if (user == null) {
            throw new MyException("用户不存在，无法修改！");
        }

        //校验唯一
        if (StrUtil.isNotBlank(dto.getCode())) {
            this.checkCodeUnique(dto.getCode(), dto.getId());
        }

        BeanUtil.copyProperties(dto, user, CopyOptions.create().ignoreNullValue().setIgnoreProperties("password"));

        if (StrUtil.isNotBlank(dto.getPassword())) {
            user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        }

        if (StrUtil.isNotBlank(dto.getPlatform())) {
            user.setPlatform(this.normalizePlatform(dto.getPlatform()));
        }

        this.updateById(user);

        if (dto.getRoleId() != null) {
            this.bindUserRole(user.getId(), dto.getRoleId());
        }

        return user;
    }

    /**
     * 绑定用户角色
     */
    private void bindUserRole(Integer userId, Integer roleId) {
        //删除旧关系
        adminSysUserJobMapper.delete(new LambdaQueryWrapper<SysUserJob>()
                .eq(SysUserJob::getUserId, userId)
                .eq(SysUserJob::getType, 1));

        // 建立新关系
        SysUserJob job = new SysUserJob();
        job.setUserId(userId);
        job.setSysRoleId(roleId);
        job.setType(1);
        job.setSysOrgId(1);
        job.setEnabled(true);
        job.setDeleted(false);
        adminSysUserJobMapper.insert(job);
    }

    /**
     * 校验账号唯一性
     */
    private void checkCodeUnique(String code, Integer userId) {
        long count = this.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getCode, code)
                .ne(userId != null, SysUser::getId, userId));
        if (count > 0) {
            throw new MyException("账号 [" + code + "] 已存在");
        }
    }

    /**
     * 平台标识智能返回
     */
    private String determinePlatform(AdminUserSaveDTO dto) {
        if (StrUtil.isNotBlank(dto.getPlatform())) {
            return normalizePlatform(dto.getPlatform());
        }
        if (Integer.valueOf(1).equals(dto.getRoleId()) || Integer.valueOf(2).equals(dto.getRoleId())) {
            return "admin";
        }
        return "Web";
    }

    /**
     * 平台标识大小写规范化
     */
    private String normalizePlatform(String platform) {
        if ("web".equalsIgnoreCase(platform)) return "Web";
        if ("app".equalsIgnoreCase(platform)) return "App";
        return platform;
    }
}

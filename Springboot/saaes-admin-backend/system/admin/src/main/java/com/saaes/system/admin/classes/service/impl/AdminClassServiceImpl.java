package com.saaes.system.admin.classes.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.saaes.system.admin.classes.mapper.AdminClassStudentMapper;
import com.saaes.system.admin.user.mapper.AdminSysUserJobMapper;
import com.saaes.system.admin.classes.mapper.AdminLevelInfoMapper;
import com.saaes.system.client.entity.ClassStudents;
import com.saaes.system.client.entity.SysUserJob;
import com.saaes.system.client.entity.LevelInfo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saaes.common.core.web.PageResult;
import com.saaes.common.core.utils.LoginUtil;
import com.saaes.common.core.web.MyException;
import com.saaes.system.admin.classes.dto.AdminClassQueryDTO;
import com.saaes.system.admin.classes.dto.AdminClassSaveDTO;
import com.saaes.system.admin.classes.mapper.AdminClassMapper;
import com.saaes.system.admin.classes.service.AdminClassService;
import com.saaes.system.admin.classes.vo.AdminClassVO;
import com.saaes.system.client.entity.SysClass;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminClassServiceImpl extends ServiceImpl<AdminClassMapper, SysClass> implements AdminClassService {

    @Resource
    private AdminClassMapper adminClassMapper;

    @Resource
    private AdminClassStudentMapper adminClassStudentMapper;

    @Resource
    private AdminSysUserJobMapper adminSysUserJobMapper;

    @Resource
    private AdminLevelInfoMapper adminLevelInfoMapper;

    @Override
    public PageResult<AdminClassVO> selectClassPage(AdminClassQueryDTO queryDTO) {
        Page<AdminClassVO> pageObj = queryDTO.toPage();
        QueryWrapper<SysClass> queryWrapper = this.commonPage(queryDTO);
        queryWrapper.orderByDesc("c.create_time");

        return new PageResult<>(this.baseMapper.selectClassPage(pageObj, queryWrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrUpdate(AdminClassSaveDTO saveDTO) {
        // 前置校验
        validateCommonRules(saveDTO);

        if (saveDTO.getId() == null) {
            createClass(saveDTO);
        } else {
            updateClass(saveDTO);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClass(List<Integer> ids) {
        Assert.notEmpty(ids, "班级ID列表不能为空");

        Integer currentUserId = LoginUtil.getUserId();

        // 判断当前用户是否为超管
        Long adminCount = adminSysUserJobMapper.selectCount(new LambdaQueryWrapper<SysUserJob>()
                .eq(SysUserJob::getUserId, currentUserId)
                .eq(SysUserJob::getSysRoleId, 1)
                .eq(SysUserJob::getType, 1));
        boolean isAdmin = adminCount > 0;

        // 查询当前用户有权删除的班级（超管查所有，老师只查自己创建的）
        LambdaQueryWrapper<SysClass> queryWrapper = new LambdaQueryWrapper<SysClass>()
                .in(SysClass::getId, ids);
        if (!isAdmin) {
            queryWrapper.eq(SysClass::getTeacherId, currentUserId);
        }

        List<SysClass> authorizedClasses = this.list(queryWrapper.select(SysClass::getId));
        List<Integer> authorizedIds = authorizedClasses.stream()
                .map(SysClass::getId).collect(Collectors.toList());

        if (authorizedIds.size() != ids.size()) {
            Collection<Integer> invalidIds = CollUtil.subtract(ids, authorizedIds);
            String roleMsg = isAdmin ? "不存在或已被删除" : "非本人创建、不存在或已被删除";
            throw new MyException("部分班级" + roleMsg + "，无效ID：" + invalidIds);
        }

        // 逻辑删除班级与学生的关联关系
        adminClassStudentMapper.delete(new LambdaQueryWrapper<ClassStudents>()
                .in(ClassStudents::getClassId, ids));

        // 逻辑删除班级本身
        this.removeByIds(ids);
    }

    public AdminClassVO getClassDetail(Integer id) {
        // 检查基础存在性与权限
        AdminClassVO vo = adminClassMapper.selectClassDetail(id);

        if (vo == null) {
            //不存在
            return null;
        }

        // 权限校验
        if (StpUtil.hasRole("teacher") && !StpUtil.hasRole("super_admin")) {
            if (vo.getTeacherId() != null && !vo.getTeacherId().equals(LoginUtil.getUserId())) {
                throw new MyException("无权查看此班级详情");
            }
        }
        return vo;
    }

    @Override
    public List<Map<String, Object>> getOptionList() {
        LambdaQueryWrapper<SysClass> queryWrapper = new LambdaQueryWrapper<>();

        // 老师只能看自己的选项
        if (StpUtil.hasRole("teacher") && !StpUtil.hasRole("super_admin")) {
            queryWrapper.eq(SysClass::getTeacherId, LoginUtil.getUserId());
        }

        List<SysClass> list = this.list(queryWrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysClass cls : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cls.getId());
            map.put("name", cls.getName());
            result.add(map);
        }
        return result;
    }

    @Override
    public PageResult<AdminClassVO> selectRecyclePage(AdminClassQueryDTO queryDTO) {
        Page<AdminClassVO> pageObj = queryDTO.toPage();
        QueryWrapper<SysClass> queryWrapper = this.commonPage(queryDTO);
        queryWrapper.orderByDesc("c.update_time");

        return new PageResult<>(this.baseMapper.selectDeletedPage(pageObj, queryWrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreClass(List<Integer> ids) {
        Assert.notEmpty(ids, "待恢复的班级ids列表不能为空");

        //恢复班级
        adminClassMapper.restoreByIds(ids);

        //恢复关联的学生
        adminClassStudentMapper.restoreByClassIds(ids);
    }

    private QueryWrapper<SysClass> commonPage(AdminClassQueryDTO queryDTO) {
        //权限判断
        if (StpUtil.hasRole("teacher") && !StpUtil.hasRole("super_admin")) {
            queryDTO.setTeacherId(LoginUtil.getUserId());
        }
        QueryWrapper<SysClass> wrapper = Wrappers.query();
        wrapper.like(StringUtils.hasText(queryDTO.getName()), "c.name", queryDTO.getName())
                .eq(queryDTO.getTeacherId() != null, "c.teacher_id", queryDTO.getTeacherId())
                .eq(queryDTO.getLevelId() != null, "c.level_id", queryDTO.getLevelId())
                .eq(queryDTO.getEnabled() != null, "c.enabled", queryDTO.getEnabled());

        return wrapper;
    }
    /**
     * 新增班级
     */
    private void createClass(AdminClassSaveDTO saveDTO) {
        // 校验名称是否重复
        checkClassNameUnique(saveDTO.getName(), null);

        SysClass newClass = buildNewClass(saveDTO);

        // 老师只能创建自己的班级
        handleTeacherPermission(newClass);

        save(newClass);
    }

    /**
     * 更新班级
     */
    private void updateClass(AdminClassSaveDTO saveDTO) {
        // 检查班级是否存在
        SysClass existingClass = getById(saveDTO.getId());
        if (existingClass == null) {
            throw new MyException("班级不存在");
        }

        // 普通老师只能修改自己的班级
        checkUpdatePermission(existingClass, saveDTO);

        // 校验名称是否重复
        if (StrUtil.isNotBlank(saveDTO.getName())) {
            checkClassNameUnique(saveDTO.getName(), saveDTO.getId());
        }
        // 更新班级信息
        updateClassInfo(existingClass, saveDTO);
    }

    /**
     * 公共校验逻辑
     */
    private void validateCommonRules(AdminClassSaveDTO saveDTO) {
        // 校验班级等级
        if (saveDTO.getLevelId() != null) {
            LevelInfo level = adminLevelInfoMapper.selectById(saveDTO.getLevelId());
            if (level == null) {
                throw new MyException("班级等级不存在");
            }
        }

        // 校验老师合法性
        if (saveDTO.getTeacherId() != null) {
            validateTeacher(saveDTO.getTeacherId());
        }
    }

    /**
     * 校验老师合法性
     */
    private void validateTeacher(Integer teacherId) {
        SysUserJob userJob = adminSysUserJobMapper.selectOne(
                Wrappers.<SysUserJob>lambdaQuery()
                        .eq(SysUserJob::getUserId, teacherId)
                        .eq(SysUserJob::getType, 1)
                        .last("LIMIT 1")
        );

        if (userJob == null) {
            throw new MyException("指定的老师不存在或未分配角色");
        }

        // 检查角色是否为老师或超管
        if (userJob.getSysRoleId() != 1 && userJob.getSysRoleId() != 2) {
            throw new MyException("该用户不是老师角色，无法担任班主任");
        }
    }

    /**
     * 校验班级名称唯一性
     */
    private void checkClassNameUnique(String className, Integer excludeId) {
        if (StrUtil.isBlank(className)) {
            return;
        }

        LambdaQueryWrapper<SysClass> wrapper = Wrappers.<SysClass>lambdaQuery()
                .eq(SysClass::getName, className);

        if (excludeId != null) {
            wrapper.ne(SysClass::getId, excludeId);
        }

        boolean exists = baseMapper.exists(wrapper);
        if (exists) {
            throw new MyException("班级名称已存在，请使用其他名称");
        }
    }

    /**
     * 构建新班级对象
     */
    private SysClass buildNewClass(AdminClassSaveDTO saveDTO) {
        SysClass sysClass = BeanUtil.copyProperties(saveDTO, SysClass.class);

        // 设置创建信息
        sysClass.setCreateTime(LocalDateTime.now());
        sysClass.setCreateBy(LoginUtil.getUserId());

        // 设置默认值
        if (sysClass.getEnabled() == null) {
            sysClass.setEnabled(true);
        }

        return sysClass;
    }

    /**
     * 处理老师权限
     */
    private void handleTeacherPermission(SysClass sysClass) {
        // 自动设为当前登录老师
        boolean isTeacher = StpUtil.hasRole("teacher");
        boolean isSuperAdmin = StpUtil.hasRole("super_admin");

        if (isTeacher && !isSuperAdmin) {
            sysClass.setTeacherId(LoginUtil.getUserId());
        }
    }

    /**
     * 检查更新权限
     */
    private void checkUpdatePermission(SysClass existingClass, AdminClassSaveDTO saveDTO) {
        boolean isTeacher = StpUtil.hasRole("teacher");
        boolean isSuperAdmin = StpUtil.hasRole("super_admin");

        // 老师只能修改自己的班级
        if (isTeacher && !isSuperAdmin) {
            if (!existingClass.getTeacherId().equals(LoginUtil.getUserId())) {
                throw new MyException("您只能修改自己管理的班级");
            }
            // 保持原老师不变
            saveDTO.setTeacherId(existingClass.getTeacherId());
        }
    }

    /**
     * 更新班级信息
     */
    private void updateClassInfo(SysClass existingClass, AdminClassSaveDTO saveDTO) {
        // 复制属性，排除创建相关字段
        BeanUtil.copyProperties(saveDTO, existingClass, "createTime", "createBy");

        // 设置更新信息
        existingClass.setUpdateTime(LocalDateTime.now());
        existingClass.setUpdateBy(LoginUtil.getUserId());

        // 更新到数据库
        updateById(existingClass);
    }
}

package com.saaes.system.admin.classes.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saaes.common.core.web.PageResult;
import com.saaes.common.core.utils.LoginUtil;
import com.saaes.common.core.web.MyException;
import com.saaes.system.admin.classes.dto.AdminClassStudentDTO;
import com.saaes.system.admin.classes.dto.AdminStudentQueryDTO;
import com.saaes.system.admin.classes.mapper.AdminClassMapper;
import com.saaes.system.admin.classes.mapper.AdminClassStudentMapper;
import com.saaes.system.admin.classes.service.AdminClassStudentService;
import com.saaes.system.admin.classes.vo.AdminStudentVO;
import com.saaes.system.admin.user.mapper.AdminSysUserJobMapper;
import com.saaes.system.client.entity.ClassStudents;
import com.saaes.system.client.entity.SysClass;
import com.saaes.system.admin.classes.dto.AdminStudentSaveDTO;
import com.saaes.system.admin.user.dto.AdminUserSaveDTO;
import com.saaes.system.admin.user.mapper.AdminUserMapper;
import com.saaes.system.admin.user.service.AdminUserService;
import com.saaes.system.client.entity.SysUser;
import com.saaes.system.client.entity.SysUserJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminClassStudentServiceImpl extends ServiceImpl<AdminClassStudentMapper, ClassStudents>
        implements AdminClassStudentService {

    @Resource
    private AdminClassStudentMapper adminClassStudentMapper;

    @Resource
    private AdminClassMapper adminClassMapper;

    @Resource
    private AdminUserService adminUserService;

    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private AdminSysUserJobMapper adminSysUserJobMapper;

    @Override
    public PageResult<AdminStudentVO> selectStudentPage(AdminStudentQueryDTO queryDTO) {
        Assert.notNull(queryDTO.getClassId(), "班级ID不能为空");

        checkClassPermission(queryDTO.getClassId());

        QueryWrapper<ClassStudents> wrapper = Wrappers.query();

        wrapper.eq("cs.class_id", queryDTO.getClassId());

        wrapper.like(StrUtil.isNotBlank(queryDTO.getName()), "u.name", queryDTO.getName())
                .like(StrUtil.isNotBlank(queryDTO.getCode()), "u.code", queryDTO.getCode());

        return new PageResult<>(this.baseMapper.selectClassStudentPage(queryDTO.toPage(), wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStudents(AdminClassStudentDTO studentDTO) {
        Assert.notNull(studentDTO, "添加学生的参数对象不能为空");
        Integer classId = studentDTO.getClassId();
        List<Integer> targetUserIds = studentDTO.getStudentIds();

        Assert.notNull(classId, "班级ID不能为空");
        Assert.notEmpty(targetUserIds, "学生ID列表不能为空");

        checkClassPermission(classId);

        // 用户身份校验
        ensureAllAreStudents(targetUserIds);

        List<ClassStudents> existingRecords = adminClassStudentMapper.selectListWithDeleted(classId, targetUserIds);

        // 分类处理
        Set<Integer> existingUserIds = new HashSet<>();
        List<Integer> userIdsToRestore = new ArrayList<>();

        for (ClassStudents record : existingRecords) {
            Integer userId = record.getUserId();
            existingUserIds.add(userId);
            if (Boolean.TRUE.equals(record.getDeleted())) {
                userIdsToRestore.add(userId);
            }
        }

        // 恢复已删除的记录
        if (CollUtil.isNotEmpty(userIdsToRestore)) {
            adminClassStudentMapper.restoreStudents(classId, userIdsToRestore, LocalDateTime.now());
        }

        // 新增记录
        List<ClassStudents> toInsert = new ArrayList<>();
        for (Integer userId : targetUserIds) {
            if (!existingUserIds.contains(userId)) {
                ClassStudents newRecord = new ClassStudents();
                newRecord.setClassId(classId);
                newRecord.setUserId(userId);
                newRecord.setCreateTime(LocalDateTime.now());
                newRecord.setDeleted(false);
                toInsert.add(newRecord);
            }
        }

        if (CollUtil.isNotEmpty(toInsert)) {
            this.saveBatch(toInsert);
        }
    }

    @Override
    public void removeStudents(AdminClassStudentDTO studentDTO) {
        Assert.notNull(studentDTO, "请求参数不能为空");
        Integer classId = studentDTO.getClassId();
        List<Integer> studentIds = studentDTO.getStudentIds();

        if (classId == null) {
            throw new MyException("班级ID不能为空");
        }
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }

        checkClassPermission(classId);

        LambdaQueryWrapper<ClassStudents> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassStudents::getClassId, classId)
                .in(ClassStudents::getUserId, studentIds);

        this.remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createStudent(AdminStudentSaveDTO saveDTO) {
        if (saveDTO == null) {
            throw new MyException("请求参数不能为空");
        }
        if (saveDTO.getClassId() == null)
            throw new MyException("必须指定班级ID");
        checkClassPermission(saveDTO.getClassId());

        // 创建用户账号
        AdminUserSaveDTO userSaveDTO = new AdminUserSaveDTO();
        BeanUtils.copyProperties(saveDTO, userSaveDTO);
        userSaveDTO.setRoleId(3); // 强制为学生
        userSaveDTO.setPlatform("Web"); // 统一平台标识

        SysUser user = adminUserService.createOrUpdate(userSaveDTO);

        //获取新建用户的ID
        if (user == null || user.getId() == null)
            throw new MyException("学生创建失败，未找到用户信息");

        //加入班级
        AdminClassStudentDTO classStudentDTO = new AdminClassStudentDTO();
        classStudentDTO.setClassId(saveDTO.getClassId());
        classStudentDTO.setStudentIds(Arrays.asList(user.getId()));
        this.addStudents(classStudentDTO);
    }

    private void checkClassPermission(Integer classId) {
        if (StpUtil.hasRole("teacher") && !StpUtil.hasRole("super_admin")) {
            SysClass cls = adminClassMapper.selectById(classId);
            if (cls == null)
                throw new MyException("班级不存在");
            if (!cls.getTeacherId().equals(LoginUtil.getUserId())) {
                throw new MyException("无权操作此班级");
            }
        }
    }

    /**
     * 校验用户id是否全部都是学生
     */
    private void ensureAllAreStudents(List<Integer> userIds) {
        // 查询这些用户的学生角色记录
        List<SysUserJob> studentRoleRecords = adminSysUserJobMapper.selectList(
                new LambdaQueryWrapper<SysUserJob>()
                        .in(SysUserJob::getUserId, userIds)
                        .eq(SysUserJob::getType, 1)
                        .eq(SysUserJob::getSysRoleId, 3)
        );
        // 去重
        Set<Integer> userIdWithStudentRole = studentRoleRecords.stream()
                .map(SysUserJob::getUserId)
                .collect(Collectors.toSet());

        // 找出不是学生的用户
        List<Integer> invalidUserIds = userIds.stream()
                .filter(uid -> !userIdWithStudentRole.contains(uid))
                .collect(Collectors.toList());

        if (!invalidUserIds.isEmpty()) {
            throw new MyException("添加失败，以下用户不是学生账号或账号不存在: " + invalidUserIds);
        }
    }
}

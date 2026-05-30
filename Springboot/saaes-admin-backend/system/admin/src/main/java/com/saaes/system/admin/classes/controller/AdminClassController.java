package com.saaes.system.admin.classes.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.saaes.common.core.web.PageResult;
import com.saaes.common.core.web.RestResponse;
import com.saaes.system.admin.classes.dto.*;
import com.saaes.system.admin.classes.mapper.AdminLevelInfoMapper;
import com.saaes.system.admin.classes.service.AdminClassService;
import com.saaes.system.admin.classes.service.AdminClassStudentService;
import com.saaes.system.admin.classes.vo.AdminClassVO;
import com.saaes.system.admin.classes.vo.AdminStudentVO;
import com.saaes.system.client.entity.LevelInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import com.saaes.common.core.validator.CreateGroup;
import com.saaes.common.core.validator.UpdateGroup;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "管理后台-班级学生管理")
@RestController
@RequestMapping("/api/admin/classes")
@SaCheckRole(value = { "super_admin", "teacher" }, mode = SaMode.OR)
public class AdminClassController {

    @Resource
    private AdminClassService adminClassService;

    @Resource
    private AdminClassStudentService adminClassStudentService;

    @Resource
    private AdminLevelInfoMapper adminLevelInfoMapper;

    // --- 班级管理 ---

    @Operation(description = "分页获取班级信息")
    @GetMapping
    public RestResponse<PageResult<AdminClassVO>> listClasses(AdminClassQueryDTO queryDTO) {
        return RestResponse.success(adminClassService.selectClassPage(queryDTO));
    }

    @Operation(description = "新增班级")
    @PostMapping
    public RestResponse<?> create(@Validated(CreateGroup.class) @RequestBody AdminClassSaveDTO saveDTO) {
        saveDTO.setId(null);
        adminClassService.createOrUpdate(saveDTO);
        return RestResponse.success();
    }

    @Operation(description = "修改班级")
    @PutMapping("/{id}")
    public RestResponse<?> update(@PathVariable Integer id, @Validated(UpdateGroup.class) @RequestBody AdminClassSaveDTO saveDTO) {
        saveDTO.setId(id);
        adminClassService.createOrUpdate(saveDTO);
        return RestResponse.success();
    }

    @Operation(description = "获取班级详情")
    @GetMapping("/{id}")
    public RestResponse<AdminClassVO> getById(@PathVariable Integer id) {
        return RestResponse.success(adminClassService.getClassDetail(id));
    }

    @Operation(description = "删除班级")
    @DeleteMapping
    public RestResponse<?> delete(@RequestParam List<Integer> ids) {
        Assert.notEmpty(ids, "待删除班级ids列表不能为空");
        List<Integer> distinctIds = ids.stream().distinct().collect(Collectors.toList());
        adminClassService.deleteClass(distinctIds);
        return RestResponse.success();
    }

    // --- 班级回收站 (仅超管) ---

    @Operation(description = "分页获取被删除的班级")
    @GetMapping("/recycle")
    @SaCheckRole("super_admin")
    public RestResponse<PageResult<AdminClassVO>> listRecycle(AdminClassQueryDTO queryDTO) {
        return RestResponse.success(adminClassService.selectRecyclePage(queryDTO));
    }

    @Operation(description = "恢复班级")
    @PutMapping("/restore")
    @SaCheckRole("super_admin")
    public RestResponse<?> restore(@RequestParam List<Integer> ids) {
        Assert.notEmpty(ids, "待恢复的班级ids列表不能为空");
        List<Integer> distinctIds = ids.stream().distinct().collect(Collectors.toList());
        adminClassService.restoreClass(distinctIds);
        return RestResponse.success();
    }

    @Operation(description = "获取班级下拉列表")
    @GetMapping("/options")
    public RestResponse<List<Map<String, Object>>> getOptionList() {
        return RestResponse.success(adminClassService.getOptionList());
    }

    @Operation(description = "获取班级等级下拉列表")
    @GetMapping("/levels")
    public RestResponse<List<LevelInfo>> getLevels() {
        // 简单查询，不走Service
        return RestResponse.success(adminLevelInfoMapper.selectList(null));
    }

    // --- 学生管理 (基于班级) ---

    @Operation(description = "查询班级内学生")
    @GetMapping("/{classId}/students")
    public RestResponse<PageResult<AdminStudentVO>> listClassStudents(@PathVariable Integer classId, AdminStudentQueryDTO queryDTO) {
        queryDTO.setClassId(classId);
        return RestResponse.success(adminClassStudentService.selectStudentPage(queryDTO));
    }

    @Operation(description = "已有学生加入班级")
    @PostMapping("/{classId}/students")
    public RestResponse<?> addStudents(@PathVariable Integer classId, @RequestBody AdminClassStudentDTO dto) {
        dto.setClassId(classId);
        adminClassStudentService.addStudents(dto);
        return RestResponse.success();
    }

    @Operation(description = "班级移除学生")
    @DeleteMapping("/{classId}/students")
    public RestResponse<?> removeStudents(@PathVariable Integer classId, @RequestBody AdminClassStudentDTO dto) {
        dto.setClassId(classId);
        adminClassStudentService.removeStudents(dto);
        return RestResponse.success();
    }

    @Operation(description = "创建学生并加入班级")
    @PostMapping("/{classId}/newStudents")
    public RestResponse<?> createStudent(@PathVariable Integer classId, @RequestBody AdminStudentSaveDTO dto) {
        dto.setClassId(classId);
        adminClassStudentService.createStudent(dto);
        return RestResponse.success();
    }
}

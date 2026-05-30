package com.saaes.system.admin.user.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.saaes.common.core.web.PageResult;
import com.saaes.common.core.web.RestResponse;
import com.saaes.system.admin.user.dto.AdminUserQueryDTO;
import com.saaes.system.admin.user.dto.AdminUserSaveDTO;
import com.saaes.system.admin.user.service.AdminUserService;
import com.saaes.system.admin.user.vo.AdminUserVO;
import com.saaes.system.client.entity.SysRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import com.saaes.common.core.validator.CreateGroup;
import com.saaes.common.core.validator.UpdateGroup;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "管理后台-用户管理")
@RestController
@RequestMapping("/api/admin/users")
@SaCheckRole(value = { "super_admin" }, mode = SaMode.OR) // 只有超管能进
public class AdminUserController {

    @Resource
    private AdminUserService adminUserService;

    @Operation(description = "分页获取用户列表")
    @GetMapping
    public RestResponse<PageResult<AdminUserVO>> list(AdminUserQueryDTO queryDTO){
        return RestResponse.success(adminUserService.selectUserPage(queryDTO));
    }

    @Operation(description = "新增用户")
    @PostMapping
    public RestResponse<?> create(@Validated(CreateGroup.class) @RequestBody AdminUserSaveDTO saveDTO) {
        saveDTO.setId(null);
        adminUserService.createOrUpdate(saveDTO);
        return RestResponse.success();
    }

    @Operation(description = "修改用户")
    @PutMapping("/{id}")
    public RestResponse<?> update(@PathVariable Integer id,@Validated(UpdateGroup.class) @RequestBody AdminUserSaveDTO saveDTO) {
        saveDTO.setId(id);
        adminUserService.createOrUpdate(saveDTO);
        return RestResponse.success();
    }

    @Operation(description = "获取用户详情")
    @GetMapping("/{id}")
    public RestResponse<AdminUserVO> getById(@PathVariable Integer id) {
        return RestResponse.success(adminUserService.getUserDetail(id));
    }

    @Operation(description = "踢人下线")
    @PostMapping("/kick/{id}")
    public RestResponse<?> kick(@PathVariable Integer id) {
        adminUserService.kick(id);
        return RestResponse.success();
    }

    @Operation(description = "删除用户")
    @DeleteMapping
    public RestResponse<?> delete(@RequestParam List<Integer> ids) {
        Assert.notEmpty(ids, "待删除用户ids列表不能为空");
        List<Integer> distinctIds = ids.stream().distinct().collect(Collectors.toList());
        adminUserService.deleteUsers(distinctIds);
        return RestResponse.success();
    }

    // --- 用户回收站 ---

    @Operation(description = "分页获取被删除的用户")
    @GetMapping("/recycle")
    public RestResponse<PageResult<AdminUserVO>> listRecycle(AdminUserQueryDTO queryDTO) {
        return RestResponse.success(adminUserService.selectRecyclePage(queryDTO));
    }

    @Operation(description = "恢复用户")
    @PutMapping("/restore")
    public RestResponse<?> restore(@RequestParam List<Integer> ids) {
        Assert.notEmpty(ids, "待恢复的用户ids列表不能为空");
        List<Integer> distinctIds = ids.stream().distinct().collect(Collectors.toList());
        adminUserService.restore(distinctIds);
        return RestResponse.success();
    }
    @Operation(description = "获取角色列表")
    @GetMapping("/roles")
    public RestResponse<List<SysRole>> getRoleList() {
        return RestResponse.success(adminUserService.getRoleList());
    }
}

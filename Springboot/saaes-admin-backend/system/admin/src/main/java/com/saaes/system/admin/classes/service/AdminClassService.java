package com.saaes.system.admin.classes.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.saaes.common.core.web.PageResult;
import com.saaes.system.admin.classes.dto.AdminClassQueryDTO;
import com.saaes.system.admin.classes.dto.AdminClassSaveDTO;
import com.saaes.system.admin.classes.vo.AdminClassVO;
import com.saaes.system.client.entity.SysClass;

import java.util.List;
import java.util.Map;

public interface AdminClassService extends IService<SysClass> {

    /**
     * 分页查询班级
     */
    PageResult<AdminClassVO> selectClassPage(AdminClassQueryDTO queryDTO);

    /**
     * 保存/修改班级
     */
    void createOrUpdate(AdminClassSaveDTO saveDTO);

    /**
     * 删除班级
     */
    void deleteClass(List<Integer> ids);

    /**
     * 获取班级详情
     */
    AdminClassVO getClassDetail(Integer id);

    /**
     * 获取班级下拉列表
     */
    List<Map<String, Object>> getOptionList();

    /**
     * 查询已删除的班级 (回收站)
     */
    PageResult<AdminClassVO> selectRecyclePage(AdminClassQueryDTO queryDTO);

    /**
     * 恢复已删除的班级
     */
    void restoreClass(List<Integer> ids);
}

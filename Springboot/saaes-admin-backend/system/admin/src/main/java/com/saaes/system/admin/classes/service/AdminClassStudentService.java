package com.saaes.system.admin.classes.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.saaes.common.core.web.PageResult;
import com.saaes.system.admin.classes.dto.AdminClassStudentDTO;
import com.saaes.system.admin.classes.dto.AdminStudentQueryDTO;
import com.saaes.system.admin.classes.dto.AdminStudentSaveDTO;
import com.saaes.system.admin.classes.vo.AdminStudentVO;
import com.saaes.system.client.entity.ClassStudents;

public interface AdminClassStudentService extends IService<ClassStudents> {

    /**
     * 查询班级内的学生
     */
    PageResult<AdminStudentVO> selectStudentPage(AdminStudentQueryDTO queryDTO);

    /**
     * 将已有学生添加到班级
     */
    void addStudents(AdminClassStudentDTO studentDTO);

    /**
     * 从班级移除学生
     */
    void removeStudents(AdminClassStudentDTO studentDTO);

    /**
     * 创建学生账号并添加到班级
     */
    void createStudent(AdminStudentSaveDTO saveDTO);
}

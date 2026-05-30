package com.saaes.system.admin.classes.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.saaes.system.admin.classes.vo.AdminStudentVO;
import com.saaes.system.client.entity.ClassStudents;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminClassStudentMapper extends BaseMapper<ClassStudents> {

    /**
     * 班级学生分页查询
     */
    IPage<AdminStudentVO> selectClassStudentPage(IPage<AdminStudentVO> page, @Param(Constants.WRAPPER) Wrapper<ClassStudents> wrapper);

    /**
     * 恢复指定班级的学生关联
     */
    int restoreByClassIds(@Param("classIds") List<Integer> classIds);

    /**
     * 批量恢复班级学生关联
     */
    int restoreStudents(@Param("classId") Integer classId, @Param("userIds") List<Integer> userIds, @Param("createTime") LocalDateTime createTime);

    /**
     * 分页查询指定班级的学生关联（包含逻辑删除的数据）
     */
    List<ClassStudents> selectListWithDeleted(@Param("classId") Integer classId, @Param("userIds") List<Integer> userIds);

    /**
     * 根据用户id批量逻辑删除所有班级关联（用户删除时级联）
     */
    int softDeleteByUserIds(@Param("userIds") List<Integer> userIds);

    /**
     * 恢复班级关联（用户恢复时级联）
     */
    int restoreByUserIds(@Param("userIds") List<Integer> userIds);

    /**
     * 物理删除已过期的班级学生关联数据
     */
    int deleteHardExpired(@Param("threshold") LocalDateTime threshold);
}

package com.system.service.classes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.system.service.classes.entity.CourseClass;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 班级 Mapper
 */
@Mapper
public interface ClassMapper extends BaseMapper<CourseClass> {
    
    int countClassStudents(@Param("classId") Long classId);
}

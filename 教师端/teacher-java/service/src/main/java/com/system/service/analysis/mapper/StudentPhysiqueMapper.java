package com.system.service.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.system.service.analysis.entity.StudentPhysique;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StudentPhysiqueMapper extends BaseMapper<StudentPhysique> {

    @Select("SELECT * FROM student_physique WHERE student_id = #{studentId} LIMIT 1")
    StudentPhysique findByStudentId(Integer studentId);
}

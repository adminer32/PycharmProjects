package com.system.service.homework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.system.service.homework.entity.Homework;
import com.system.service.homework.vo.SubmissionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface HomeworkMapper extends BaseMapper<Homework> {

    int countClassStudents(@Param("classId") Long classId);

    int countAllStudentsByTeacher(@Param("teacherId") Long teacherId);

    int countSubmissionsByHomeworkId(@Param("homeworkId") Long homeworkId);

    int countPendingGrades(@Param("homeworkId") Long homeworkId);

    BigDecimal calculateAverageScore(@Param("homeworkId") Long homeworkId);

    SubmissionVO.StudentInfo getStudentInfo(@Param("studentId") Long studentId);

    List<SubmissionVO.StudentInfo> getClassStudents(@Param("classId") Long classId);

    List<SubmissionVO.StudentInfo> getAllTeacherStudents(@Param("teacherId") Long teacherId);
}

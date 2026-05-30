package com.system.service.classes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.system.service.classes.entity.CourseClass;
import com.system.service.classes.mapper.ClassMapper;
import com.system.service.classes.service.ClassService;
import com.system.service.classes.vo.ClassListVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class ClassServiceImpl implements ClassService {
    
    @Resource
    private ClassMapper classMapper;
    
    @Override
    public List<ClassListVO> getTeacherClasses(Long teacherId) {
        LambdaQueryWrapper<CourseClass> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseClass::getTeacherId, teacherId);
        
        List<CourseClass> classes = classMapper.selectList(wrapper);
        
        return classes.stream().map(cls -> {
            ClassListVO vo = new ClassListVO();
            vo.setId(cls.getId());
            vo.setName(cls.getName());
            vo.setGrade(cls.getGrade());
            vo.setStudentCount(classMapper.countClassStudents(cls.getId()));
            return vo;
        }).collect(Collectors.toList());
    }
}

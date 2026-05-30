package com.system.service.classes.service;

import com.system.service.classes.vo.ClassListVO;

import java.util.List;

public interface ClassService {
    List<ClassListVO> getTeacherClasses(Long teacherId);
}

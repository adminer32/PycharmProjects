package com.system.service.analysis.service;

import com.system.service.analysis.entity.MotionAnalysisData;
import com.system.service.analysis.entity.StudentPhysique;
import com.system.service.analysis.vo.AnalysisVO;
import java.util.List;
import java.util.Map;

public interface AnalysisService {

    AnalysisVO.ClassDashboard getClassDashboard(Long classId, String month);

    AnalysisVO.StudentDashboard getStudentDashboard(Long studentId, String month);

    List<Map<String, Object>> getStudentsByClass(Long classId);

    MotionAnalysisData getMotionAnalysisData(Integer studentId);

    StudentPhysique getStudentPhysique(Integer studentId);
}

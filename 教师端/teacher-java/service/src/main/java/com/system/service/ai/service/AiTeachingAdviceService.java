package com.system.service.ai.service;

import com.system.service.ai.vo.AiTeachingAdvice;
import java.util.List;

public interface AiTeachingAdviceService {

    List<AiTeachingAdvice> getClassAdvice(Long classId);

    List<AiTeachingAdvice> getStudentAdvice(Long studentId);

    List<AiTeachingAdvice> regenerateClassAdvice(Long classId);

    List<AiTeachingAdvice> regenerateStudentAdvice(Long studentId);
}

package com.system.service.homework.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.system.service.homework.dto.CreateHomeworkDTO;
import com.system.service.homework.dto.GradeSubmissionDTO;
import com.system.service.homework.entity.Homework;
import com.system.service.homework.vo.HomeworkDetailVO;
import com.system.service.homework.vo.HomeworkListVO;
import com.system.service.homework.vo.SubmissionVO;

import java.util.List;

public interface HomeworkService extends IService<Homework> {

    List<HomeworkListVO> getHomeworkList(Long teacherId, Long classId);

    HomeworkDetailVO getHomeworkDetail(Long homeworkId);

    Long createHomework(CreateHomeworkDTO dto, Long teacherId);

    void updateHomework(Long homeworkId, CreateHomeworkDTO dto);

    void deleteHomework(Long homeworkId);

    List<SubmissionVO> getSubmissions(Long homeworkId);

    SubmissionVO getSubmissionDetail(Long submissionId);

    void gradeSubmission(Long submissionId, GradeSubmissionDTO dto);
}

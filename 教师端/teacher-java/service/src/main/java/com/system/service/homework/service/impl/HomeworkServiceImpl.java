package com.system.service.homework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.system.service.homework.dto.CreateHomeworkDTO;
import com.system.service.homework.dto.GradeSubmissionDTO;
import com.system.service.homework.entity.Homework;
import com.system.service.homework.entity.HomeworkFeedback;
import com.system.service.homework.entity.HomeworkSubmission;
import com.system.service.homework.mapper.HomeworkFeedbackMapper;
import com.system.service.homework.mapper.HomeworkMapper;
import com.system.service.homework.mapper.HomeworkSubmissionMapper;
import com.system.service.homework.service.HomeworkService;
import com.system.service.homework.vo.HomeworkDetailVO;
import com.system.service.homework.vo.HomeworkListVO;
import com.system.service.homework.vo.SubmissionVO;
import com.system.web.MyException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HomeworkServiceImpl extends ServiceImpl<HomeworkMapper, Homework> implements HomeworkService {

    @Resource
    private HomeworkSubmissionMapper homeworkSubmissionMapper;

    @Resource
    private HomeworkFeedbackMapper homeworkFeedbackMapper;

    @Override
    public List<HomeworkListVO> getHomeworkList(Long teacherId, Long classId) {
        List<HomeworkListVO> result = new ArrayList<>();
        
        LambdaQueryWrapper<Homework> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Homework::getTeacherId, teacherId);
        
        if (classId != null) {
            wrapper.eq(Homework::getClassId, classId);
        }
        
        wrapper.orderByDesc(Homework::getCreatedAt);
        
        List<Homework> homeworkList = this.list(wrapper);
        
        for (Homework homework : homeworkList) {
            HomeworkListVO vo = new HomeworkListVO();
            BeanUtils.copyProperties(homework, vo);
            
            Long homeworkId = homework.getId();
            Long hwClassId = homework.getClassId();
            
            int totalCount = (hwClassId == null || hwClassId == 0) 
                    ? baseMapper.countAllStudentsByTeacher(teacherId) 
                    : baseMapper.countClassStudents(hwClassId);
            
            int submissionCount = baseMapper.countSubmissionsByHomeworkId(homeworkId);
            int pendingGradeCount = countPendingGrades(homeworkId);
            BigDecimal avgScore = calculateAverageScore(homeworkId);
            
            vo.setTotalCount(totalCount);
            vo.setSubmissionCount(submissionCount);
            vo.setPendingGradeCount(pendingGradeCount);
            vo.setAverageScore(avgScore);
            
            result.add(vo);
        }
        
        return result;
    }

    @Override
    public HomeworkDetailVO getHomeworkDetail(Long homeworkId) {
        Homework homework = this.getById(homeworkId);
        if (homework == null) {
            throw new MyException("作业不存在");
        }
        
        HomeworkDetailVO vo = new HomeworkDetailVO();
        BeanUtils.copyProperties(homework, vo);
        
        Long hwClassId = homework.getClassId();
        int totalCount = (hwClassId == null || hwClassId == 0) 
                ? baseMapper.countAllStudentsByTeacher(homework.getTeacherId()) 
                : baseMapper.countClassStudents(hwClassId);
                
        int submissionCount = baseMapper.countSubmissionsByHomeworkId(homeworkId);
        int pendingGradeCount = countPendingGrades(homeworkId);
        BigDecimal avgScore = calculateAverageScore(homeworkId);
        
        vo.setTotalCount(totalCount);
        vo.setSubmissionCount(submissionCount);
        vo.setPendingGradeCount(pendingGradeCount);
        vo.setAverageScore(avgScore);
        
        return vo;
    }

    @Override
    @Transactional
    public Long createHomework(CreateHomeworkDTO dto, Long teacherId) {
        Homework homework = new Homework();
        BeanUtils.copyProperties(dto, homework);
        homework.setTeacherId(teacherId);
        homework.setStatus(1);
        if (homework.getRequirements() == null || homework.getRequirements().isEmpty()) {
            homework.setRequirements("无特殊要求");
        }
        this.save(homework);
        return homework.getId();
    }

    @Override
    @Transactional
    public void updateHomework(Long homeworkId, CreateHomeworkDTO dto) {
        Homework homework = this.getById(homeworkId);
        if (homework == null) {
            throw new MyException("作业不存在");
        }
        Long originalClassId = homework.getClassId();
        BeanUtils.copyProperties(dto, homework);
        if (dto.getClassId() == null || dto.getClassId() == 0) {
            homework.setClassId(originalClassId);
        }
        this.updateById(homework);
    }

    @Override
    @Transactional
    public void deleteHomework(Long homeworkId) {
        Homework homework = this.getById(homeworkId);
        if (homework == null) {
            throw new MyException("作业不存在");
        }
        
        LambdaQueryWrapper<HomeworkFeedback> feedbackWrapper = new LambdaQueryWrapper<>();
        feedbackWrapper.eq(HomeworkFeedback::getHomeworkId, homeworkId);
        homeworkFeedbackMapper.delete(feedbackWrapper);
        
        LambdaQueryWrapper<HomeworkSubmission> submissionWrapper = new LambdaQueryWrapper<>();
        submissionWrapper.eq(HomeworkSubmission::getHomeworkId, homeworkId);
        homeworkSubmissionMapper.delete(submissionWrapper);
        
        this.removeById(homeworkId);
    }

    @Override
    public List<SubmissionVO> getSubmissions(Long homeworkId) {
        Homework homework = this.getById(homeworkId);
        if (homework == null) {
            throw new MyException("作业不存在");
        }
        
        Long hwClassId = homework.getClassId();
        List<SubmissionVO.StudentInfo> allStudents;
        if (hwClassId == null || hwClassId == 0) {
            allStudents = baseMapper.getAllTeacherStudents(homework.getTeacherId());
        } else {
            allStudents = baseMapper.getClassStudents(hwClassId);
        }
        
        LambdaQueryWrapper<HomeworkSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HomeworkSubmission::getHomeworkId, homeworkId);
        List<HomeworkSubmission> submissions = homeworkSubmissionMapper.selectList(wrapper);
        
        Map<Long, HomeworkSubmission> submissionMap = submissions.stream()
                .collect(Collectors.toMap(
                    HomeworkSubmission::getStudentId, 
                    s -> s, 
                    (existing, replacement) -> existing.getId() > replacement.getId() ? existing : replacement
                ));
        
        List<SubmissionVO> result = new ArrayList<>();
        
        // 第一步：处理所有应交作业的学生
        for (SubmissionVO.StudentInfo student : allStudents) {
            SubmissionVO vo = new SubmissionVO();
            vo.setStudentId(student.getStudentId());
            vo.setStudentName(student.getStudentName());
            vo.setStudentAvatar(student.getStudentAvatar());
            
            HomeworkSubmission submission = submissionMap.get(student.getStudentId());
            if (submission != null) {
                vo.setSubmitted(true);
                vo.setId(submission.getId());
                vo.setVideoUrl(submission.getVideoUrl());
                vo.setSubmitTime(submission.getCreatedAt());
                
                fillFeedbackInfo(vo, submission.getId());
                submissionMap.remove(student.getStudentId()); // 标记已处理
            } else {
                vo.setSubmitted(false);
                vo.setStatus(-1);
            }
            result.add(vo);
        }
        
        // 第二步：处理提交了作业但不在当前班级名单中的学生（防止数据丢失）
        for (HomeworkSubmission submission : submissionMap.values()) {
            SubmissionVO vo = new SubmissionVO();
            vo.setStudentId(submission.getStudentId());
            vo.setSubmitted(true);
            vo.setId(submission.getId());
            vo.setVideoUrl(submission.getVideoUrl());
            vo.setSubmitTime(submission.getCreatedAt());
            
            SubmissionVO.StudentInfo studentInfo = baseMapper.getStudentInfo(submission.getStudentId());
            if (studentInfo != null) {
                vo.setStudentName(studentInfo.getStudentName());
                vo.setStudentAvatar(studentInfo.getStudentAvatar());
                vo.setStudentAccount(studentInfo.getStudentAccount());
            } else {
                vo.setStudentName("学生(ID:" + submission.getStudentId() + ")");
            }
            
            fillFeedbackInfo(vo, submission.getId());
            result.add(vo);
        }
        
        return result;
    }

    private void fillFeedbackInfo(SubmissionVO vo, Long submissionId) {
        LambdaQueryWrapper<HomeworkFeedback> feedbackWrapper = new LambdaQueryWrapper<>();
        feedbackWrapper.eq(HomeworkFeedback::getSubmissionId, submissionId);
        HomeworkFeedback feedback = homeworkFeedbackMapper.selectOne(feedbackWrapper);
        
        if (feedback != null) {
            vo.setAiScore(feedback.getAiScore());
            vo.setTeacherScore(feedback.getTeacherScore());
            vo.setFeedback(feedback.getTeacherComment());
            vo.setAiSuggestion(feedback.getAiSuggestion());
            vo.setStatus(feedback.getStatus());
        } else {
            vo.setStatus(0);
        }
    }

    @Override
    public SubmissionVO getSubmissionDetail(Long submissionId) {
        HomeworkSubmission submission = homeworkSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new MyException("提交记录不存在");
        }
        return buildSubmissionVO(submission);
    }

    @Override
    @Transactional
    public void gradeSubmission(Long submissionId, GradeSubmissionDTO dto) {
        LambdaQueryWrapper<HomeworkFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HomeworkFeedback::getSubmissionId, submissionId);
        HomeworkFeedback feedback = homeworkFeedbackMapper.selectOne(wrapper);

        if (feedback == null) {
            HomeworkSubmission submission = homeworkSubmissionMapper.selectById(submissionId);
            if (submission == null) {
                throw new MyException("提交记录不存在");
            }
            feedback = HomeworkFeedback.builder()
                    .submissionId(submissionId)
                    .homeworkId(submission.getHomeworkId())
                    .teacherScore(dto.getTeacherScore())
                    .teacherComment(dto.getTeacherComment())
                    .aiSuggestion(dto.getAiSuggestion())
                    .status(1)
                    .build();
            homeworkFeedbackMapper.insert(feedback);
        } else {
            feedback.setTeacherScore(dto.getTeacherScore());
            feedback.setTeacherComment(dto.getTeacherComment());
            feedback.setAiSuggestion(dto.getAiSuggestion());
            feedback.setStatus(1);
            homeworkFeedbackMapper.updateById(feedback);
        }
    }



    private int countPendingGrades(Long homeworkId) {
        return baseMapper.countPendingGrades(homeworkId);
    }

    private BigDecimal calculateAverageScore(Long homeworkId) {
        BigDecimal avgScore = baseMapper.calculateAverageScore(homeworkId);
        if (avgScore != null) {
            avgScore = avgScore.setScale(2, RoundingMode.HALF_UP);
        }
        return avgScore;
    }

    private SubmissionVO buildSubmissionVO(HomeworkSubmission submission) {
        SubmissionVO vo = new SubmissionVO();
        vo.setId(submission.getId());
        vo.setStudentId(submission.getStudentId());
        vo.setVideoUrl(submission.getVideoUrl());
        vo.setSubmitTime(submission.getCreatedAt());
        
        SubmissionVO.StudentInfo studentInfo = baseMapper.getStudentInfo(submission.getStudentId());
        if (studentInfo != null) {
            vo.setStudentName(studentInfo.getStudentName());
            vo.setStudentAvatar(studentInfo.getStudentAvatar());
        }
        
        LambdaQueryWrapper<HomeworkFeedback> feedbackWrapper = new LambdaQueryWrapper<>();
        feedbackWrapper.eq(HomeworkFeedback::getSubmissionId, submission.getId());
        HomeworkFeedback feedback = homeworkFeedbackMapper.selectOne(feedbackWrapper);
        
        if (feedback != null) {
            vo.setAiScore(feedback.getAiScore());
            vo.setTeacherScore(feedback.getTeacherScore());
            vo.setFeedback(feedback.getTeacherComment());
            vo.setAiSuggestion(feedback.getAiSuggestion());
            vo.setStatus(feedback.getStatus());
        } else {
            vo.setStatus(0);
        }
        
        return vo;
    }
}

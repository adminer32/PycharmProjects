package com.saaes.system.admin.practice.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saaes.common.core.utils.LoginUtil;
import com.saaes.common.core.web.MyException;
import com.saaes.common.core.web.PageResult;
import com.saaes.system.admin.classes.mapper.AdminClassMapper;
import com.saaes.system.admin.classes.vo.AdminStudentVO;
import com.saaes.system.admin.practice.dto.AdminPracticeFeedbackQueryDTO;
import com.saaes.system.admin.practice.service.AdminPracticeService;
import com.saaes.system.admin.practice.mapper.PracticeTaskMapper;
import com.saaes.system.admin.practice.mapper.PracticeSubmissionMapper;
import com.saaes.system.admin.practice.mapper.PracticeFeedbackMapper;
import com.saaes.system.admin.practice.mapper.PracticeReminderMapper;
import com.saaes.system.admin.classes.mapper.AdminClassStudentMapper;
import com.saaes.system.client.dto.PracticeFeedbackSaveDTO;
import com.saaes.system.client.entity.*;
import com.saaes.system.client.vo.PracticeFeedbackVO;
import com.saaes.system.client.vo.PracticeTaskVO;
import com.saaes.system.client.event.PracticeRemindEvent;
import com.saaes.system.admin.practice.dto.AdminPracticeTaskSaveDTO;
import com.saaes.system.admin.practice.dto.AdminPracticeTaskQueryDTO;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminPracticeServiceImpl extends ServiceImpl<PracticeTaskMapper, PracticeTask>
        implements AdminPracticeService {

    @Resource
    private PracticeTaskMapper practiceTaskMapper;

    @Resource
    private PracticeSubmissionMapper practiceSubmissionMapper;

    @Resource
    private PracticeFeedbackMapper practiceFeedbackMapper;

    @Resource
    private PracticeReminderMapper practiceReminderMapper;

    @Resource
    private AdminClassStudentMapper adminClassStudentMapper;

    @Resource
    private AdminClassMapper adminClassMapper;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Override
    public PageResult<PracticeTaskVO> selectTaskPage(AdminPracticeTaskQueryDTO queryDTO) {
        Page<PracticeTaskVO> page = queryDTO.toPage();
        QueryWrapper<PracticeTask> queryWrapper = new QueryWrapper<>();

        // 老师只能看自己的
        if (!StpUtil.hasRole("super_admin")) {
            queryWrapper.eq("pt.teacher_id", LoginUtil.getUserId());
        }

        queryWrapper.like(StrUtil.isNotBlank(queryDTO.getTitle()), "pt.title", queryDTO.getTitle())
                    .eq(queryDTO.getClassId() != null, "pt.class_id", queryDTO.getClassId());
        queryWrapper.orderByDesc("pt.create_time");

        return new PageResult<>(practiceTaskMapper.selectTaskPage(page, queryWrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrUpdateTask(AdminPracticeTaskSaveDTO saveDTO) {
        // 校验班级存在和权限
        validateClassPermission(saveDTO.getClassId());

        if (saveDTO.getId() == null) {
            createPracticeTask(saveDTO);
        } else {
            updatePracticeTask(saveDTO);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrUpdateFeedback(PracticeFeedbackSaveDTO feedbackDTO) {
        // 验证基础数据
        PracticeSubmission submission = validateSubmission(feedbackDTO.getSubmissionId());
        validateTaskOwnership(submission.getTaskId());

        // 查找现有评价
        PracticeFeedback existingFeedback = findExistingFeedback(feedbackDTO.getSubmissionId());

        if (existingFeedback == null) {
            createNewFeedback(feedbackDTO, submission);
        } else {
            updateExistingFeedback(feedbackDTO, existingFeedback);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remindUnfinishedStudents(Integer taskId) {
        PracticeTask task = this.getById(taskId);
        if (task == null || task.getDeleted()) {
            throw new MyException("任务不存在");
        }
        if (!task.getTeacherId().equals(LoginUtil.getUserId())) {
            throw new MyException("无权操作该任务");
        }
        if (task.getDeadline() != null && task.getDeadline().isBefore(LocalDateTime.now())) {
            throw new MyException("任务已截止，无法发送提醒");
        }

        // 获取班级内所有有效学生id
        List<Integer> allStudentIds = getActiveStudentIdsByClassId(task.getClassId());
        if (allStudentIds.isEmpty()) {
            throw new MyException("该班级暂无有效学生");
        }

        // 查询已提交的学生id
        Set<Integer> finishedStudentIds = practiceSubmissionMapper.selectList(
                        new LambdaQueryWrapper<PracticeSubmission>()
                                .eq(PracticeSubmission::getTaskId, taskId)
                                .eq(PracticeSubmission::getDeleted, false))
                .stream()
                .map(PracticeSubmission::getStudentId)
                .collect(Collectors.toSet());

        // 过滤出未完成的学生
        List<Integer> unfinishedStudentIds = allStudentIds.stream()
                .filter(sid -> !finishedStudentIds.contains(sid))
                .collect(Collectors.toList());

        if (unfinishedStudentIds.isEmpty()) {
            throw new MyException("所有学生都已完成该任务");
        }

        // 发送提醒
        for (Integer studentId : unfinishedStudentIds) {
            PracticeReminder reminder = new PracticeReminder();
            reminder.setTaskId(taskId);
            reminder.setStudentId(studentId);
            reminder.setContent("老师提醒您及时完成练习任务: " + task.getTitle());
            reminder.setIsRead(0);
            reminder.setCreateTime(LocalDateTime.now());
            practiceReminderMapper.insert(reminder);

            eventPublisher.publishEvent(new PracticeRemindEvent(this, String.valueOf(studentId), task.getTitle()));
        }
    }

    /**
     * 获取指定班级下所有有效学生id
     */
    private List<Integer> getActiveStudentIdsByClassId(Integer classId) {
        List<ClassStudents> activeMembers = adminClassStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudents>()
                        .eq(ClassStudents::getClassId, classId)
                        .eq(ClassStudents::getDeleted, false));

        return activeMembers.stream()
                .map(ClassStudents::getUserId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTasks(List<Integer> ids) {
        Assert.notEmpty(ids, "任务ID列表不能为空");

        // 权限校验：只能删除自己创建且未逻辑删除的任务
        long myTaskCount = this.count(new LambdaQueryWrapper<PracticeTask>()
                .in(PracticeTask::getId, ids)
                .eq(PracticeTask::getTeacherId, LoginUtil.getUserId()));

        if (myTaskCount != ids.size()) {
            List<Integer> authorizedIds = this.list(new LambdaQueryWrapper<PracticeTask>()
                            .select(PracticeTask::getId)
                            .in(PracticeTask::getId, ids)
                            .eq(PracticeTask::getTeacherId, LoginUtil.getUserId()))
                    .stream().map(PracticeTask::getId).collect(Collectors.toList());
            Collection<Integer> unauthorizedIds = CollUtil.subtract(ids, authorizedIds);
            throw new MyException("选中的任务中包含非本人创建或已删除的任务，无权删除。任务id：" + unauthorizedIds);
        }

        // 级联删除提交记录及评价记录（逻辑删除）
        deleteSubmissionsAndFeedbacks(ids);

        // 物理删除提醒记录
        practiceReminderMapper.deleteHardByTaskIds(ids);

        // 逻辑删除任务本身
        this.removeByIds(ids);
    }

    @Override
    public PageResult<PracticeFeedbackVO> selectFeedbacksPage(AdminPracticeFeedbackQueryDTO queryDTO) {
        Page<PracticeFeedbackVO> pageObj = queryDTO.toPage();

        QueryWrapper<Object> wrapper = Wrappers.query();

        wrapper.eq("pt.teacher_id", LoginUtil.getUserId());

        wrapper.eq(queryDTO.getTaskId() != null, "pt.id", queryDTO.getTaskId())
                .eq(queryDTO.getClassId() != null, "pt.class_id", queryDTO.getClassId())
                .like(StringUtils.hasText(queryDTO.getStudentName()), "su.name", queryDTO.getStudentName());

        if (queryDTO.getFeedbackStatus() != null) {
            if (queryDTO.getFeedbackStatus() == 0) {
                wrapper.isNull("pf.id");
            } else if (queryDTO.getFeedbackStatus() == 1) {
                wrapper.isNotNull("pf.id");
            }
        }

        wrapper.orderByDesc("ps.create_time");

        IPage<PracticeFeedbackVO> resultPage = this.practiceSubmissionMapper.selectFeedbackPage(pageObj, wrapper);

        // 使用fileCode生成安全的视频URL
        if (resultPage.getRecords() != null) {
            for (PracticeFeedbackVO vo : resultPage.getRecords()) {
                if (StrUtil.isNotBlank(vo.getFileCode())) {
                    vo.setVideoUrl("/api/file/operation/preview/" + vo.getFileCode());
                }
            }
        }

        return new PageResult<>(this.practiceSubmissionMapper.selectFeedbackPage(pageObj, wrapper));
    }

    @Override
    public PracticeFeedbackVO getSubmissionDetail(Integer submissionId) {
        if (submissionId == null) {
            throw new MyException("提交id不能为空");
        }

        Integer teacherId = LoginUtil.getUserId();

        // 查询详情，同时验证权限（只能查看自己任务的提交）
        PracticeFeedbackVO detail = practiceSubmissionMapper
                .selectSubmissionDetail(submissionId, teacherId);

        if (detail == null) {
            throw new MyException("提交记录不存在或无权访问");
        }

        // 使用fileCode生成安全的视频URL
        if (StrUtil.isNotBlank(detail.getFileCode())) {
            detail.setVideoUrl("/api/file/operation/preview/" + detail.getFileCode());
        }

        return detail;
    }

    /**
     * 创建练习任务
     */
    private void createPracticeTask(AdminPracticeTaskSaveDTO saveDTO) {

        // 构建新任务对象
        PracticeTask task = buildNewTask(saveDTO);

        // 设置创建信息
        task.setTeacherId(LoginUtil.getUserId());
        task.setCreateTime(LocalDateTime.now());
        task.setCreateBy(LoginUtil.getUserId());
        task.setDeleted(false);

        save(task);

    }

    /**
     * 更新练习任务
     */
    private void updatePracticeTask(AdminPracticeTaskSaveDTO saveDTO) {
        //查找现有练习任务
        PracticeTask existingTask = getById(saveDTO.getId());
        if (existingTask == null || existingTask.getDeleted()) {
            throw new MyException("练习任务不存在");
        }
        //只能修改自己的练习任务
        checkTaskOwnership(existingTask);

        updateTaskInfo(existingTask, saveDTO);

    }

    /**
     * 校验班级存在和权限
     */
    private void validateClassPermission(Integer classId) {
        if (classId == null) {
            return;
        }
        SysClass clazz = adminClassMapper.selectById(classId);
        if (clazz == null || clazz.getDeleted()) {
            throw new MyException("班级不存在");
        }
        //老师只能给自己的班级发布任务
        if (clazz.getTeacherId() != null && !clazz.getTeacherId().equals(LoginUtil.getUserId())) {
            throw new MyException("您无权给该班级发布任务");
        }
    }

    /**
     * 构建新任务对象
     */
    private PracticeTask buildNewTask(AdminPracticeTaskSaveDTO saveDTO) {
        PracticeTask task = new PracticeTask();
        BeanUtil.copyProperties(saveDTO, task);
        return task;
    }

    /**
     * 检查任务所有权
     */
    private void checkTaskOwnership(PracticeTask task) {
        if (!task.getTeacherId().equals(LoginUtil.getUserId())) {
            throw new MyException("您只能修改自己发布的任务");
        }
    }

    /**
     * 更新任务信息
     */
    private void updateTaskInfo(PracticeTask task, AdminPracticeTaskSaveDTO saveDTO) {
        // 复制属性，排除创建相关的字段和老师ID
        BeanUtil.copyProperties(saveDTO, task, "createTime", "createBy", "teacherId");
        // 设置更新信息
        task.setUpdateTime(LocalDateTime.now());
        task.setUpdateBy(LoginUtil.getUserId());
        // 更新到数据库
        updateById(task);
    }

    /**
     * 验证提交记录是否存在
     */
    private PracticeSubmission validateSubmission(Integer submissionId) {
        PracticeSubmission submission = practiceSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new MyException("提交记录不存在");
        }
        return submission;
    }

    /**
     * 验证任务所有权
     */
    private PracticeTask validateTaskOwnership(Integer taskId) {
        PracticeTask task = practiceTaskMapper.selectById(taskId);
        if (task == null) {
            throw new MyException("相关练习任务不存在");
        }

        // 只能评价自己任务的提交
        if (!task.getTeacherId().equals(LoginUtil.getUserId())) {
            throw new MyException("您只能评价自己任务的提交");
        }

        return task;
    }

    /**
     * 查找现有评价
     */
    private PracticeFeedback findExistingFeedback(Integer submissionId) {
        return practiceFeedbackMapper.selectOne(
                Wrappers.<PracticeFeedback>lambdaQuery()
                        .eq(PracticeFeedback::getSubmissionId, submissionId)
                        .eq(PracticeFeedback::getDeleted, false)
        );
    }

    /**
     * 创建新评价
     */
    private void createNewFeedback(PracticeFeedbackSaveDTO feedbackDTO, PracticeSubmission submission) {
        PracticeFeedback feedback = new PracticeFeedback();
        feedback.setSubmissionId(feedbackDTO.getSubmissionId());
        feedback.setTeacherId(LoginUtil.getUserId());
        feedback.setComment(feedbackDTO.getComment());
        feedback.setCreateTime(LocalDateTime.now());
        feedback.setDeleted(false);

        practiceFeedbackMapper.insert(feedback);

        // 更新提交状态为已批阅
        submission.setStatus(1);
        practiceSubmissionMapper.updateById(submission);
    }

    /**
     * 更新现有评价
     */
    private void updateExistingFeedback(PracticeFeedbackSaveDTO feedbackDTO, PracticeFeedback existingFeedback) {
        // 只能修改自己的评价
        if (!existingFeedback.getTeacherId().equals(LoginUtil.getUserId())) {
            throw new MyException("您只能修改自己的评价");
        }

        existingFeedback.setComment(feedbackDTO.getComment());
        existingFeedback.setUpdateTime(LocalDateTime.now());

        practiceFeedbackMapper.updateById(existingFeedback);
    }

    /**
     * 级联删除任务的提交记录及对应的评价记录（逻辑删除）
     */
    private void deleteSubmissionsAndFeedbacks(List<Integer> taskIds) {
        List<PracticeSubmission> submissions = practiceSubmissionMapper.selectList(
                new LambdaQueryWrapper<PracticeSubmission>()
                        .in(PracticeSubmission::getTaskId, taskIds));

        if (submissions.isEmpty()) {
            return;
        }

        List<Integer> submissionIds = submissions.stream()
                .map(PracticeSubmission::getId)
                .collect(Collectors.toList());

        // 逻辑删除评价记录
        practiceFeedbackMapper.delete(new LambdaQueryWrapper<PracticeFeedback>()
                .in(PracticeFeedback::getSubmissionId, submissionIds));

        // 逻辑删除提交记录
        practiceSubmissionMapper.delete(new LambdaQueryWrapper<PracticeSubmission>()
                .in(PracticeSubmission::getTaskId, taskIds));
    }
}

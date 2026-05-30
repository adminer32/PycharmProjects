package com.saaes.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saaes.common.core.utils.LoginUtil;
import com.saaes.common.core.web.MyException;
import com.saaes.common.core.web.PageResult;
import com.saaes.system.admin.classes.mapper.AdminClassStudentMapper;
import com.saaes.system.client.dto.PracticeSubmissionDTO;
import com.saaes.system.client.dto.PracticeTaskQueryDTO;
import com.saaes.system.client.entity.*;
import com.saaes.system.client.vo.PracticeTaskVO;
import com.saaes.system.admin.practice.mapper.PracticeReminderMapper;
import com.saaes.system.admin.practice.mapper.PracticeSubmissionMapper;
import com.saaes.system.admin.practice.mapper.PracticeTaskMapper;
import com.saaes.system.mapper.SysFileMapper;
import com.saaes.system.service.PracticeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PracticeServiceImpl extends ServiceImpl<PracticeTaskMapper, PracticeTask> implements PracticeService {

    @Resource
    private PracticeTaskMapper practiceTaskMapper;

    @Resource
    private PracticeSubmissionMapper practiceSubmissionMapper;

    @Resource
    private PracticeReminderMapper practiceReminderMapper;

    @Resource
    private SysFileMapper sysFileMapper;

    @Resource
    private AdminClassStudentMapper adminClassStudentMapper;

    @Override
    public PageResult<PracticeTaskVO> queryStudentTasks(PracticeTaskQueryDTO queryDTO) {
        Integer studentId = LoginUtil.getUserId();
        Page<PracticeTaskVO> pageObj = queryDTO.toPage();

        QueryWrapper<PracticeTask> queryWrapper = new QueryWrapper<>();

        queryWrapper.like(StrUtil.isNotBlank(queryDTO.getTitle()), "pt.title", queryDTO.getTitle());

        queryWrapper.orderByDesc("pt.create_time");

        IPage<PracticeTaskVO> resultPage = baseMapper.selectStudentTaskPage(pageObj, studentId, queryWrapper);

        // 处理视频链接
        if (resultPage.getRecords() != null) {
            resultPage.getRecords().forEach(vo -> {
                if (StrUtil.isNotBlank(vo.getFileCode())) {
                    vo.setVideoUrl("/api/file/operation/preview/" + vo.getFileCode());
                }
            });
        }

        return new PageResult<>(resultPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrUpdatePractice(PracticeSubmissionDTO submissionDTO) {
        // 基础验证
        Integer studentId = LoginUtil.getUserId();
        PracticeTask task = validateTask(submissionDTO.getTaskId());
        // 验证文件信息
        SysFile file = validateFileInfo(submissionDTO.getFileCode());
        // 检查截止时间
        checkDeadline(task);
        // 查找现有提交
        PracticeSubmission existingSubmission = findExistingSubmission(studentId, submissionDTO.getTaskId());

        if (existingSubmission == null) {
            createNewSubmission(submissionDTO, studentId, task, file);
        } else {
            updateExistingSubmission(submissionDTO, existingSubmission, file);
        }
    }

    @Override
    public PracticeTaskVO getTaskDetail(Integer taskId) {
        Integer studentId = LoginUtil.getUserId();
        PracticeTaskVO vo = practiceTaskMapper.selectTaskDetail(taskId, studentId);
        if (vo == null) {
            throw new MyException("该练习不存在");
        }
        // 处理视频链接
        if (StrUtil.isNotBlank(vo.getFileCode())) {
            vo.setVideoUrl("/api/file/operation/preview/" + vo.getFileCode());
        }

        return vo;
    }

    @Override
    public List<PracticeReminder> getUnreadReminders() {
        Integer studentId = LoginUtil.getUserId();
        return practiceReminderMapper.selectList(new LambdaQueryWrapper<PracticeReminder>()
                .eq(PracticeReminder::getStudentId, studentId)
                .eq(PracticeReminder::getIsRead, 0)
                .orderByDesc(PracticeReminder::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markReminderRead(Integer reminderId) {
        Integer studentId = LoginUtil.getUserId();
        PracticeReminder reminder = practiceReminderMapper.selectById(reminderId);

        if (reminder == null) {
            throw new MyException("提醒不存在");
        }

        // 验证权限：只能标记自己的提醒
        if (!reminder.getStudentId().equals(studentId)) {
            throw new MyException("无权操作该提醒");
        }

        reminder.setIsRead(1);
        practiceReminderMapper.updateById(reminder);
    }

    /**
     * 清理学生某个任务相关的所有提醒（提交后自动调用）
     */
    private void clearTaskReminders(Integer studentId, Integer taskId) {
        practiceReminderMapper.update(null, new LambdaUpdateWrapper<PracticeReminder>()
                .eq(PracticeReminder::getStudentId, studentId)
                .eq(PracticeReminder::getTaskId, taskId)
                .eq(PracticeReminder::getIsRead, 0)
                .set(PracticeReminder::getIsRead, 1));
    }

    /**
     * 验证练习任务
     */
    private PracticeTask validateTask(Integer taskId) {
        PracticeTask task = getById(taskId);
        Integer currentUserId = LoginUtil.getUserId();
        if (task == null) {
            throw new MyException("练习任务不存在");
        }

        if (task.getClassId() != null) {
            Long count = adminClassStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudents>()
                    .eq(ClassStudents::getClassId, task.getClassId())
                    .eq(ClassStudents::getUserId, currentUserId)
                    .eq(ClassStudents::getDeleted, false));
            if (count == 0) {
                throw new MyException("无权操作该练习任务");
            }
        }

        return task;
    }

    /**
     * 验证文件信息
     */
    private SysFile validateFileInfo(String fileCode) {
        if (StrUtil.isBlank(fileCode)) {
            throw new MyException("请上传练习视频文件");
        }

        // 根据fileCode查询文件信息
        SysFile file = sysFileMapper.selectValidFileByFileCode(fileCode);
        if (file == null) {
            throw new MyException("上传的文件不存在或已失效");
        }

        // 验证文件类型为视频
        if (file.getContentType() == null || !file.getContentType().startsWith("video/")) {
            throw new MyException("只能上传视频文件");
        }

        return file;
    }

    /**
     * 检查截止时间
     */
    private void checkDeadline(PracticeTask task) {
        if (task.getDeadline() != null && task.getDeadline().isBefore(LocalDateTime.now())) {
            throw new MyException("已超过提交截止时间，无法提交");
        }
    }

    /**
     * 查找现有提交记录
     */
    private PracticeSubmission findExistingSubmission(Integer studentId, Integer taskId) {
        return practiceSubmissionMapper.selectOne(
                Wrappers.<PracticeSubmission>lambdaQuery()
                        .eq(PracticeSubmission::getTaskId, taskId)
                        .eq(PracticeSubmission::getStudentId, studentId)
                        .eq(PracticeSubmission::getDeleted, false)
        );
    }

    /**
     * 创建新提交
     */
    private void createNewSubmission(PracticeSubmissionDTO submissionDTO, Integer studentId, PracticeTask task, SysFile file) {
        // 创建提交记录
        PracticeSubmission submission = new PracticeSubmission();
        submission.setTaskId(submissionDTO.getTaskId());
        submission.setStudentId(studentId);
        submission.setVideoUrl("/api/file/operation/preview/" + file.getFileCode());
        submission.setFileId(file.getId());
        submission.setFileCode(file.getFileCode());
        submission.setStatus(0);
        submission.setCreateTime(LocalDateTime.now());
        submission.setUpdateTime(LocalDateTime.now());
        submission.setDeleted(false);

        practiceSubmissionMapper.insert(submission);

        // 清理该任务相关的提醒
        clearTaskReminders(studentId, submissionDTO.getTaskId());
    }

    /**
     * 更新已有提交
     */
    private void updateExistingSubmission(PracticeSubmissionDTO submissionDTO, PracticeSubmission existing, SysFile file) {
        // 检查是否已批阅
        if (existing.getStatus() != null && existing.getStatus() == 1) {
            throw new MyException("老师已批阅，无法修改提交内容");
        }

        // 更新提交信息
        existing.setVideoUrl("/api/file/operation/preview/" + file.getFileCode());
        existing.setFileId(file.getId());
        existing.setFileCode(file.getFileCode());
        existing.setUpdateTime(LocalDateTime.now());
        existing.setStatus(0); // 重置为未批阅状态

        practiceSubmissionMapper.updateById(existing);
    }
}

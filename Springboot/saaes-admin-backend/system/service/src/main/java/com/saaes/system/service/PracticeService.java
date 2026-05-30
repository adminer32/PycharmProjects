package com.saaes.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.saaes.common.core.web.PageResult;
import com.saaes.system.client.dto.PracticeSubmissionDTO;
import com.saaes.system.client.dto.PracticeTaskQueryDTO;
import com.saaes.system.client.entity.PracticeReminder;
import com.saaes.system.client.entity.PracticeTask;
import com.saaes.system.client.vo.PracticeTaskVO;

import java.util.List;

public interface PracticeService extends IService<PracticeTask> {

    /**
     * 查询学生练习任务列表
     */
    PageResult<PracticeTaskVO> queryStudentTasks(PracticeTaskQueryDTO queryDTO);

    /**
     * 学生首次提交练习视频
     */
    void createOrUpdatePractice(PracticeSubmissionDTO submissionDTO);

    /**
     * 获取单个任务详情 (包含提交和评价)
     */
    PracticeTaskVO getTaskDetail(Integer taskId);

    /**
     * 获取当前学生的未读提醒列表
     */
    List<PracticeReminder> getUnreadReminders();

    /**
     * 标记提醒为已读
     */
    void markReminderRead(Integer reminderId);
}

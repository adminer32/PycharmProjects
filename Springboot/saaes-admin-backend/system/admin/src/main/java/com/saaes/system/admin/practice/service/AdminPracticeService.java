package com.saaes.system.admin.practice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.saaes.common.core.web.PageResult;
import com.saaes.system.admin.practice.dto.AdminPracticeFeedbackQueryDTO;
import com.saaes.system.client.dto.PracticeFeedbackSaveDTO;
import com.saaes.system.client.entity.PracticeTask;
import com.saaes.system.client.vo.PracticeFeedbackVO;
import com.saaes.system.client.vo.PracticeTaskVO;
import com.saaes.system.admin.practice.dto.AdminPracticeTaskSaveDTO;
import com.saaes.system.admin.practice.dto.AdminPracticeTaskQueryDTO;

import java.util.List;

public interface AdminPracticeService extends IService<PracticeTask> {

    /**
     * 分页查询练习任务 (老师端)
     */
    PageResult<PracticeTaskVO> selectTaskPage(AdminPracticeTaskQueryDTO queryDTO);

    /**
     * 保存/修改任务
     */
    void createOrUpdateTask(AdminPracticeTaskSaveDTO saveDTO);

    /**
     * 首次评价/修改学生提交
     */
    void createOrUpdateFeedback(PracticeFeedbackSaveDTO feedbackDTO);

    /**
     * 一键提醒未完成学生
     */
    void remindUnfinishedStudents(Integer taskId);

    /**
     * 删除任务 (逻辑删除)
     */
    void deleteTasks(List<Integer> ids);

    /**
     * 分页查询评价列表（老师端）
     */
    PageResult<PracticeFeedbackVO> selectFeedbacksPage(AdminPracticeFeedbackQueryDTO queryDTO);

    /**
     * 查询学生某次练习的详细信息（供老师查看和评价）
     */
    PracticeFeedbackVO getSubmissionDetail(Integer submissionId);
}

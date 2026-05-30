package com.saaes.system.admin.practice.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saaes.system.admin.practice.dto.AdminPracticeFeedbackQueryDTO;
import com.saaes.system.client.entity.PracticeFeedback;
import com.saaes.system.client.entity.PracticeSubmission;
import com.saaes.system.client.vo.PracticeFeedbackVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface PracticeSubmissionMapper extends BaseMapper<PracticeSubmission> {

        /**
         * 分页查询老师的评价列表
         */
        IPage<PracticeFeedbackVO> selectFeedbackPage(@Param("page") IPage<PracticeFeedbackVO> page, @Param(Constants.WRAPPER) Wrapper<?> queryWrapper);

        /**
         * 查询学生某次练习的详细信息（供老师查看和评价）
         */
        PracticeFeedbackVO selectSubmissionDetail(@Param("submissionId") Integer submissionId, @Param("teacherId") Integer teacherId);

        /**
         * 物理删除已过期练习提交
         */
        int deleteHardExpired(@Param("threshold") LocalDateTime threshold);
}

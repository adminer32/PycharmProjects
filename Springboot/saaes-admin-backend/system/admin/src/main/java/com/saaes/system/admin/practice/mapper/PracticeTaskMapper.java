package com.saaes.system.admin.practice.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saaes.system.client.entity.PracticeTask;
import com.saaes.system.client.vo.PracticeTaskVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PracticeTaskMapper extends BaseMapper<PracticeTask> {

        /**
         * 分页查询练习任务 (老师端使用)
         */
        IPage<PracticeTaskVO> selectTaskPage(Page<PracticeTaskVO> page, @Param(Constants.WRAPPER) Wrapper<PracticeTask> queryWrapper);

        /**
         * 分页查询学生收到的练习任务 (学生端使用)
         */
        IPage<PracticeTaskVO> selectStudentTaskPage(Page<PracticeTaskVO> page, @Param("studentId") Integer studentId, @Param(Constants.WRAPPER) Wrapper<PracticeTask> queryWrapper);

        /**
         * 获取任务详情
         */
        PracticeTaskVO selectTaskDetail(@Param("taskId") Integer taskId, @Param("studentId") Integer studentId);

        /**
         * 物理删除已过期练习任务
         */
        int deleteHardExpired(@Param("threshold") LocalDateTime threshold);

        /**
         * 查询所有已删除的任务id
         */
        List<Integer> selectDeletedTaskIds();
}

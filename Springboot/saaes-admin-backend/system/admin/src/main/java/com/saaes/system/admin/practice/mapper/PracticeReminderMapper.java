package com.saaes.system.admin.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saaes.system.client.entity.PracticeReminder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PracticeReminderMapper extends BaseMapper<PracticeReminder> {
    /**
     * 物理删除已读过期的提醒
     */
    int deleteHardExpired(@Param("threshold") LocalDateTime threshold);

    /**
     * 物理删除未读过期的提醒
     */
    int deleteHardUnreadExpired(@Param("threshold") LocalDateTime threshold);

    /**
     * 物理删除指定任务的提醒
     */
    int deleteHardByTaskIds(@Param("taskIds") List<Integer> taskIds);
}

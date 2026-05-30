package com.saaes.system.admin.task;

import com.saaes.system.admin.classes.mapper.AdminClassMapper;
import com.saaes.system.admin.classes.mapper.AdminClassStudentMapper;
import com.saaes.system.admin.practice.mapper.PracticeFeedbackMapper;
import com.saaes.system.admin.practice.mapper.PracticeReminderMapper;
import com.saaes.system.admin.practice.mapper.PracticeSubmissionMapper;
import com.saaes.system.admin.practice.mapper.PracticeTaskMapper;
import com.saaes.system.admin.user.mapper.AdminSysUserJobMapper;
import com.saaes.system.admin.user.mapper.AdminUserMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

/**
 * 数据清理定时任务
 */
@Slf4j
@Component
public class DataCleanupTask {

    @Resource
    private AdminClassMapper adminClassMapper;

    @Resource
    private AdminClassStudentMapper adminClassStudentMapper;

    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private AdminSysUserJobMapper adminSysUserJobMapper;

    @Resource
    private PracticeTaskMapper practiceTaskMapper;

    @Resource
    private PracticeSubmissionMapper practiceSubmissionMapper;

    @Resource
    private PracticeFeedbackMapper practiceFeedbackMapper;

    @Resource
    private PracticeReminderMapper practiceReminderMapper;

    @Value("${task.cleanup.retention-days:30}")
    private int retentionDays;

    @Value("${task.cleanup.reminder-read-retention-days:7}")
    private int readReminderRetentionDays;

    /**
     * 定时任务入口
     */
    @Scheduled(cron = "${task.cleanup.cron}")
    @Transactional(rollbackFor = Exception.class)
    public void cleanupOldData() {
        StopWatch stopWatch = new StopWatch("数据清理任务");
        stopWatch.start();

        log.info(">>>>>> [定时任务] 开始执行：回收站数据清理 (保留天数: {})", retentionDays);

        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
            int totalDeleted = 0;

            // 清理基础模块数据
            totalDeleted += executeClean("过期班级", () -> adminClassMapper.deleteHardExpired(threshold));
            totalDeleted += executeClean("过期用户", () -> adminUserMapper.deleteHardExpired(threshold));
            totalDeleted += executeClean("过期班级学生关联", () -> adminClassStudentMapper.deleteHardExpired(threshold));
            totalDeleted += executeClean("过期用户角色关联", () -> adminSysUserJobMapper.deleteHardExpired(threshold));

            // 清理练习模块
            totalDeleted += cleanupPracticeModule(threshold);

            // 清理提醒消息
            totalDeleted += cleanupReminders();

            stopWatch.stop();
            log.info("<<<<<< [定时任务] 执行完成：共清理 {} 条数据，总耗时 {}ms", totalDeleted, stopWatch.getTotalTimeMillis());

        } catch (Exception e) {
            //
            log.error("!!!!!! [定时任务] 异常：数据清理失败", e);
            throw e;
        }
    }

    /**
     * 处理练习模块清理逻辑
     */
    private int cleanupPracticeModule(LocalDateTime threshold) {
        int count = 0;

        // 级联清理
        List<Integer> deletedTaskIds = practiceTaskMapper.selectDeletedTaskIds();
        if (deletedTaskIds != null && !deletedTaskIds.isEmpty()) {
            //
            int cascadeCount = practiceReminderMapper.deleteHardByTaskIds(deletedTaskIds);
            if (cascadeCount > 0) {
                log.info("清理了 {} 条已删除任务关联的练习提醒", cascadeCount);
                count += cascadeCount;
            }
        }

        // 清理主表
        count += executeClean("过期练习任务", () -> practiceTaskMapper.deleteHardExpired(threshold));
        count += executeClean("过期练习提交", () -> practiceSubmissionMapper.deleteHardExpired(threshold));
        count += executeClean("过期练习评价", () -> practiceFeedbackMapper.deleteHardExpired(threshold));

        return count;
    }

    /**
     * 处理提醒消息清理逻辑
     */
    private int cleanupReminders() {
        int count = 0;
        LocalDateTime readThreshold = LocalDateTime.now().minusDays(readReminderRetentionDays);
        LocalDateTime unreadThreshold = LocalDateTime.now().minusDays(retentionDays);

        count += executeClean("已读过期提醒", () -> practiceReminderMapper.deleteHardExpired(readThreshold));
        count += executeClean("未读过期提醒", () -> practiceReminderMapper.deleteHardUnreadExpired(unreadThreshold));

        return count;
    }

    /**
     * 通用清理执行器
     */
    private int executeClean(String businessName, Supplier<Integer> cleaner) {
        int rows = cleaner.get();
        if (rows > 0) {
            // 使用统一的缩进格式
            log.info("  [-] 清理了 {} 条 {}", rows, businessName);
        }
        return rows;
    }
}
package com.saaes.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.saaes.common.core.web.MyException;
import com.saaes.system.client.entity.ScheduleEvent;
import com.saaes.system.mapper.ScheduleEventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleEventService {

    @Autowired
    private ScheduleEventMapper scheduleEventMapper; // 假设你有对应的 Mapper

    public Map<String, List<Map<String, Object>>> getGroupedEventsByDate(String date, long loginId) {
        LocalDate firstDay;
        LocalDate lastDay;

        try {
            if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                // 格式 yyyy-MM-dd，按“某一天”处理
                LocalDate localDate = LocalDate.parse(date);
                firstDay = localDate;
                lastDay = localDate;
            } else if (date.matches("\\d{4}-\\d{2}")) {
                // 格式 yyyy-MM，按整月处理
                YearMonth yearMonth = YearMonth.parse(date, DateTimeFormatter.ofPattern("yyyy-MM"));
                firstDay = yearMonth.atDay(1);
                lastDay = yearMonth.atEndOfMonth();
            } else {
                throw new MyException("非法日期格式，应为 yyyy-MM 或 yyyy-MM-dd");
            }
        } catch (DateTimeParseException e) {
            throw new MyException("日期解析失败：" + e.getMessage());
        }

        List<ScheduleEvent> userEvents = scheduleEventMapper.selectList(new QueryWrapper<ScheduleEvent>()
                .between("schedule_date", firstDay, lastDay)
                .eq("deleted", false)
                .eq("create_by", loginId));

        return userEvents.stream()
                .sorted(Comparator.comparing(ScheduleEvent::getScheduleDate)
                        .thenComparing(ScheduleEvent::getTime))  // 按日期和时间排序
                .collect(Collectors.groupingBy(
                        e -> e.getScheduleDate().toString(),
                        LinkedHashMap::new,
                        Collectors.mapping(e -> {
                            Map<String, Object> item = new HashMap<>();
                            item.put("id", e.getId());
                            item.put("time", e.getTime() != null ? e.getTime().toString() : null);
                            item.put("type", e.getType());
                            item.put("content", e.getContent());
                            return item;
                        }, Collectors.toList())
                ));
    }

    @Transactional
    public ScheduleEvent saveOrUpdateEvent(ScheduleEvent event) {
        if (event == null) {
            throw new MyException("事件数据不能为空");
        }

        if (event.getScheduleDate() == null || event.getType() == null) {
            throw new MyException("事件数据不完整：缺少日期或类型");
        }

        // 进一步检查时间是否有效
        if (event.getTime() != null && event.getTime().isBefore(LocalTime.MIN)) {
            throw new MyException("事件时间无效");
        }

        // 更新事件
        if (event.getId() != null) {
            boolean updated = scheduleEventMapper.updateById(event) > 0;
            if (!updated) {
                throw new MyException("事件更新失败，可能是由于数据冲突或其他原因");
            }
            return scheduleEventMapper.selectById(event.getId());

        } else {
            // 新增事件
            boolean saved = scheduleEventMapper.insert(event) > 0;
            if (!saved) {
                throw new MyException("事件保存失败，可能是由于数据库问题或其他原因");
            }
            return event;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(List<ScheduleEvent> events) {
        if (events == null || events.isEmpty()) {
            return false;
        }

        for (ScheduleEvent event : events) {
            scheduleEventMapper.insert(event);
        }

        return true;
    }

    @Transactional
    public void deleteBatchEvents(List<Long> ids) {
        long loginId = StpUtil.getLoginIdAsLong();

        // 使用 LambdaUpdateWrapper 执行批量更新操作
        boolean result = scheduleEventMapper.update(
                new LambdaUpdateWrapper<ScheduleEvent>()
                        .in(ScheduleEvent::getId, ids) // 通过 id 列表进行批量更新
                        .eq(ScheduleEvent::getCreateBy, loginId) // 仅删除当前用户创建的数据
                        .set(ScheduleEvent::getDeleted, true) // 将 deleted 字段设置为 true
        ) > 0;

        if (!result) {
            throw new MyException("批量删除失败，可能是没有找到匹配的数据");
        }
    }
}

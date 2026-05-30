package com.system.service.home.service.impl;

import com.system.service.home.mapper.HomeMapper;
import com.system.service.home.service.HomeService;
import com.system.service.home.vo.HomeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HomeServiceImpl implements HomeService {

    @Autowired
    private HomeMapper homeMapper;

    @Override
    public HomeVO.Overview getHomeOverview(String classId) {
        Map<String, Object> raw = homeMapper.getClassOverview(classId);
        if (raw == null || raw.isEmpty()) {
            HomeVO.Overview empty = new HomeVO.Overview();
            empty.setTotalStudents(0);
            Map<String, Object> hwEmpty = new HashMap<>();
            hwEmpty.put("submitted", 0);
            hwEmpty.put("total", 0);
            hwEmpty.put("rate", "0%");
            empty.setTodayHomework(hwEmpty);
            Map<String, Object> ckEmpty = new HashMap<>();
            ckEmpty.put("submitted", 0);
            ckEmpty.put("total", 0);
            ckEmpty.put("rate", "0%");
            empty.setTodayCheckin(ckEmpty);
            empty.setPendingHomework(0);
            return empty;
        }

        Number totalStu = (Number) raw.getOrDefault("totalStudents", 0);
        Number hwSub = (Number) raw.getOrDefault("homeworkSubmitted", 0);
        Number hwTotal = (Number) raw.getOrDefault("homeworkTotal", 1);
        Number ckSub = (Number) raw.getOrDefault("checkinSubmitted", 0);
        Number ckTotal = (Number) raw.getOrDefault("checkinTotal", 1);

        HomeVO.Overview overview = new HomeVO.Overview();
        overview.setTotalStudents(totalStu.intValue());
        
        Map<String, Object> todayHw = new HashMap<>();
        todayHw.put("submitted", hwSub.intValue());
        todayHw.put("total", hwTotal.intValue());
        todayHw.put("rate", calcRate(hwSub, hwTotal));
        overview.setTodayHomework(todayHw);
        
        Map<String, Object> todayCk = new HashMap<>();
        todayCk.put("submitted", ckSub.intValue());
        todayCk.put("total", ckTotal.intValue());
        todayCk.put("rate", calcRate(ckSub, ckTotal));
        overview.setTodayCheckin(todayCk);
        
        Integer pendingHw = homeMapper.getPendingHomeworkCount(classId);
        overview.setPendingHomework(pendingHw != null ? pendingHw : 0);
        
        return overview;
    }

    @Override
    public List<HomeVO.PendingItem> getPendingItems(Long teacherId, String classId) {
        List<HomeVO.PendingItem> allItems = new ArrayList<>();
        String safeClassId = (classId != null && !classId.isEmpty()) ? classId : "0";

        // 1. 待批改作业（写死样式，有数据才显示）
        Integer pendingHw = homeMapper.getPendingHomeworkCount(safeClassId);
        if (pendingHw == null) pendingHw = 0;
        if (pendingHw > 0) {
            HomeVO.PendingItem hwItem = new HomeVO.PendingItem();
            hwItem.setId(1L);
            hwItem.setItemType("HOMEWORK_REVIEW");
            hwItem.setIcon("📝");
            hwItem.setTitle("待批改作业");
            String hwDesc = pendingHw + "份学生提交待批改";
            if (pendingHw > 3) {
                int urgent = Math.min(pendingHw - 3, pendingHw);
                hwDesc += "，" + urgent + "份需要教师人工复核";
            }
            hwItem.setDescription(hwDesc);
            hwItem.setActionText("去批改");
            hwItem.setActionRoute("/homework");
            allItems.add(hwItem);
        }

        // 2. AI训练计划审批（写死样式，有数据才显示）
        Integer pendingPlans = homeMapper.getPendingTrainingPlanCount(safeClassId);
        if (pendingPlans == null) pendingPlans = 0;
        if (pendingPlans > 0) {
            HomeVO.PendingItem planItem = new HomeVO.PendingItem();
            planItem.setId(2L);
            planItem.setItemType("TRAINING_PLAN_APPROVE");
            planItem.setIcon("🤖");
            planItem.setTitle("AI训练计划待审批");
            planItem.setDescription(pendingPlans + "名学生生成了ai训练计划，点击前往审批");
            planItem.setActionText("去审批");
            planItem.setActionRoute("/learning");
            allItems.add(planItem);
        }

        // 3. 班级学情报告导出（始终显示，写死的入口）
        HomeVO.PendingItem reportItem = new HomeVO.PendingItem();
        reportItem.setId(3L);
        reportItem.setItemType("REPORT_EXPORT");
        reportItem.setIcon("📊");
        reportItem.setTitle("班级学情报告待导出");
        reportItem.setDescription("本周学情报告已更新，可导出分享给家长");
        reportItem.setActionText("去导出");
        reportItem.setActionRoute("/analysis");
        allItems.add(reportItem);

        return allItems;
    }

    @Override
    public Map<String, Object> getNotices(String classId, int page, int size) {
        int offset = (page - 1) * size;
        List<HomeVO.Notice> list = homeMapper.getNotices(classId, offset, size);
        Integer total = homeMapper.getNoticeCount(classId);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list != null ? list : Collections.emptyList());
        result.put("total", total != null ? total : 0);
        return result;
    }

    @Override
    public HomeVO.Notice saveNotice(HomeVO.NoticeInput input) {
        String type = Boolean.TRUE.equals(input.getIsImportant()) ? "urgent" : "normal";
        homeMapper.insertNotice(input.getClassId(), input.getTeacherId(), 
                input.getTitle(), input.getContent(), type);
        Long newId = homeMapper.getLastInsertId();
        return homeMapper.getNoticeById(newId);
    }

    @Override
    public HomeVO.Notice updateNotice(HomeVO.NoticeInput input) {
        String type = Boolean.TRUE.equals(input.getIsImportant()) ? "urgent" : "normal";
        homeMapper.updateNotice(input.getId(), input.getTitle(), input.getContent(), type);
        return homeMapper.getNoticeById(input.getId());
    }

    @Override
    public boolean deleteNotice(Long id) {
        return homeMapper.deleteNotice(id) > 0;
    }

    private String calcRate(Number submitted, Number total) {
        double s = submitted.doubleValue();
        double t = Math.max(total.doubleValue(), 1);
        double rate = (s / t) * 100;
        return BigDecimal.valueOf(rate).setScale(0, RoundingMode.HALF_UP).intValue() + "%";
    }
}

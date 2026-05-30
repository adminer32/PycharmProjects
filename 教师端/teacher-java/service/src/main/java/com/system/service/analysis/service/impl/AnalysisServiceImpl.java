package com.system.service.analysis.service.impl;

import com.system.service.analysis.mapper.AnalysisMapper;
import com.system.service.analysis.mapper.MotionAnalysisMapper;
import com.system.service.analysis.mapper.StudentPhysiqueMapper;
import com.system.service.analysis.entity.MotionAnalysisData;
import com.system.service.analysis.entity.StudentPhysique;
import com.system.service.analysis.service.AnalysisService;
import com.system.service.analysis.vo.AnalysisVO;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    @Resource
    private AnalysisMapper analysisMapper;

    @Resource
    private MotionAnalysisMapper motionAnalysisMapper;

    @Resource
    private StudentPhysiqueMapper studentPhysiqueMapper;

    @Override
    public AnalysisVO.ClassDashboard getClassDashboard(Long classId, String month) {
        AnalysisVO.ClassDashboard dashboard = new AnalysisVO.ClassDashboard();


        LocalDate queryDate = (month != null && !month.isEmpty())
                ? LocalDate.parse(month + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : LocalDate.now();
        String currentMonthStr = queryDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String startOfMonth = queryDate.with(TemporalAdjusters.firstDayOfMonth()).toString();
        String endOfMonth = queryDate.with(TemporalAdjusters.lastDayOfMonth()).toString();


        AnalysisVO.ClassMetrics metrics = new AnalysisVO.ClassMetrics();
        BigDecimal avgStudyDuration = analysisMapper.getClassAvgStudyDuration(classId);
        metrics.setAvgStudyDuration(avgStudyDuration != null ? avgStudyDuration.setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        BigDecimal homeworkAvg = analysisMapper.getClassHomeworkAvg(classId);
        metrics.setHomeworkAvg(homeworkAvg != null ? homeworkAvg.setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        BigDecimal avgSkillPoints = analysisMapper.getClassAvgSkillPoints(classId);
        metrics.setAvgSkillPoints(avgSkillPoints != null ? avgSkillPoints.setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        Integer totalStudents = analysisMapper.getTotalClassStudents(classId);
        if (totalStudents == null || totalStudents <= 0) {
            totalStudents = 1;
        }

        LocalDate now = LocalDate.now();
        boolean isCurrentMonth = queryDate.getYear() == now.getYear() && queryDate.getMonthValue() == now.getMonthValue();
        int totalDaysInMonth = isCurrentMonth ? now.getDayOfMonth() : queryDate.lengthOfMonth();

        Integer totalCheckinDays = analysisMapper.getClassTotalCheckinDays(classId, currentMonthStr);
        if (totalCheckinDays == null) totalCheckinDays = 0;

        double rawCheckinRate = 0.0;
        int possibleTotal = totalStudents * totalDaysInMonth;
        if (possibleTotal > 0) {
            rawCheckinRate = (totalCheckinDays * 100.0) / possibleTotal;
            rawCheckinRate = Math.min(rawCheckinRate, 88.0);
        }
        BigDecimal checkinRate = BigDecimal.valueOf(rawCheckinRate).setScale(0, RoundingMode.HALF_UP);

        metrics.setClassCheckinRate(checkinRate);
        dashboard.setMetrics(metrics);


        dashboard.setDurationTrend(analysisMapper.getClassDurationTrend(classId));
        if (dashboard.getDurationTrend() != null) {
            dashboard.getDurationTrend().forEach(t -> t.setValue(t.getValue() != null ? t.getValue().setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO));
        }
        dashboard.setScoreTrend(analysisMapper.getClassScoreTrend(classId));
        if (dashboard.getScoreTrend() != null) {
            dashboard.getScoreTrend().forEach(t -> t.setValue(t.getValue() != null ? t.getValue().setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO));
        }
        dashboard.setRadarData(analysisMapper.getClassRadarData(classId));
        if (dashboard.getRadarData() != null) {
            dashboard.getRadarData().forEach(r -> {
                if (r.getClassAvgScore() != null) r.setClassAvgScore(r.getClassAvgScore().setScale(0, RoundingMode.HALF_UP));
            });
        }


        // 获取每日打卡次数（每天不同学生的人数）
        List<Map<String, Object>> dailyCounts = analysisMapper.getClassCheckinCountsPerDay(classId, currentMonthStr);
        Map<Integer, Integer> dayCountsMap = new HashMap<>();
        for (Map<String, Object> map : dailyCounts) {
            int day = ((Number) map.get("day")).intValue();
            int count = ((Number) map.getOrDefault("count", 0)).intValue();
            dayCountsMap.put(day, count);
        }

        int totalStudentsCount = totalStudents != null ? totalStudents : 1;

        Integer qualifiedCount = analysisMapper.getQualifiedCheckinStudents(classId, startOfMonth, endOfMonth, 12);
        if (qualifiedCount == null) qualifiedCount = 0;

        AnalysisVO.CheckinSummary checkinData = new AnalysisVO.CheckinSummary();
        checkinData.setMonthRate(checkinRate);
        checkinData.setWeekRate(checkinRate);
        checkinData.setDayCounts(dayCountsMap);
        checkinData.setQualifiedCount(qualifiedCount);

        Map<String, Object> topStudy = analysisMapper.getTopStudyStudent(classId, currentMonthStr);
        if (topStudy != null && !topStudy.isEmpty()) {
            checkinData.setTopStudyStudent((String) topStudy.get("name"));
            checkinData.setTopStudyHours(new BigDecimal(topStudy.get("totalHours").toString()));
        }

        Map<String, Object> topCheckin = analysisMapper.getTopCheckinStudent(classId, currentMonthStr);
        if (topCheckin != null && !topCheckin.isEmpty()) {
            checkinData.setTopCheckinStudent((String) topCheckin.get("name"));
            checkinData.setTopCheckinDays(((Number) topCheckin.get("days")).intValue());
        }

        Map<String, Object> topContinuous = analysisMapper.getTopContinuousCheckinStudent(classId, currentMonthStr);
        if (topContinuous != null && !topContinuous.isEmpty()) {
            checkinData.setTopContinuousStudent((String) topContinuous.get("name"));
            checkinData.setTopContinuousDays(((Number) topContinuous.get("continuousDays")).intValue());
        }

        dashboard.setCheckinData(checkinData);


        List<Map<String, Object>> scoreDist = analysisMapper.getClassHomeworkScoreDistribution(classId);
        Map<String, Integer> distMap = new HashMap<>();
        if (!scoreDist.isEmpty()) {
            Map<String, Object> row = scoreDist.get(0);
            distMap.put("excellent", ((Number) row.getOrDefault("excellent", 0)).intValue());
            distMap.put("good", ((Number) row.getOrDefault("good", 0)).intValue());
            distMap.put("pass", ((Number) row.getOrDefault("pass", 0)).intValue());
            distMap.put("fail", ((Number) row.getOrDefault("fail", 0)).intValue());
        }
        dashboard.setScoreDistribution(distMap);

        List<AnalysisVO.ActionAbility> actionDist = dashboard.getRadarData() != null
                ? dashboard.getRadarData().stream().map(sk -> {
            AnalysisVO.ActionAbility ability = new AnalysisVO.ActionAbility();
            ability.setName(sk.getSkillType());
            ability.setAvg(sk.getClassAvgScore());
            return ability;
        }).collect(java.util.stream.Collectors.toList())
                : Collections.emptyList();
        dashboard.setActionDistribution(actionDist);
        if (dashboard.getActionDistribution() != null) {
            dashboard.getActionDistribution().forEach(a -> {
                if (a.getAvg() != null) a.setAvg(a.getAvg().setScale(0, RoundingMode.HALF_UP));
            });
        }

        return dashboard;
    }

    @Override
    public AnalysisVO.StudentDashboard getStudentDashboard(Long studentId, String month) {
        AnalysisVO.StudentDashboard dashboard = new AnalysisVO.StudentDashboard();

        LocalDate queryDate = (month != null && !month.isEmpty())
                ? LocalDate.parse(month + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : LocalDate.now();
        String currentMonthStr = queryDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String startOfMonth = queryDate.with(TemporalAdjusters.firstDayOfMonth()).toString();
        String endOfMonth = queryDate.with(TemporalAdjusters.lastDayOfMonth()).toString();


        AnalysisVO.StudentMetrics metrics = new AnalysisVO.StudentMetrics();
        BigDecimal studyDuration = analysisMapper.getStudentStudyDuration(studentId);
        metrics.setStudyDuration(studyDuration != null ? studyDuration.setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        BigDecimal homeworkAvg = analysisMapper.getStudentHomeworkAvg(studentId);
        metrics.setHomeworkAvg(homeworkAvg != null ? homeworkAvg.setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        metrics.setSkillPoints(analysisMapper.getStudentHitSkillPoints(studentId));

        // 获取学生当月打卡天数
        List<Integer> studentCheckinDays = analysisMapper.getStudentCheckindays(studentId, currentMonthStr);
        int checkinDays = studentCheckinDays != null ? studentCheckinDays.size() : 0;

        // 获取当月总天数
        int daysInMonth = queryDate.getMonthValue() == LocalDate.now().getMonthValue() && queryDate.getYear() == LocalDate.now().getYear()
                ? LocalDate.now().getDayOfMonth()
                : queryDate.lengthOfMonth();

        // 计算出勤率，确保不超过100%
        double rate = 0.0;
        if (daysInMonth > 0) {
            // 学生的打卡天数不能超过当月总天数
            checkinDays = Math.min(checkinDays, daysInMonth);
            rate = (checkinDays * 100.0) / daysInMonth;
            // 最终确保不超过100%
            rate = Math.min(rate, 100.0);
        }
        metrics.setCheckinRate(BigDecimal.valueOf(rate).setScale(0, RoundingMode.HALF_UP));

        // 获取学生当月打卡总次数
        Integer totalCheckinCount = analysisMapper.getStudentCheckinCountInPeriod(studentId, startOfMonth, endOfMonth);
        if (totalCheckinCount == null) totalCheckinCount = 0;
        metrics.setMonthTotalCheckin(totalCheckinCount);


        // 设置个人打卡数据
        if (studentCheckinDays == null) studentCheckinDays = Collections.emptyList();
        dashboard.setPersonalCheckinData(studentCheckinDays);

        // 计算连续打卡天数
        int continuous = 0;
        if (!studentCheckinDays.isEmpty()) {
            int daysEvaluated = queryDate.getMonthValue() == LocalDate.now().getMonthValue() && queryDate.getYear() == LocalDate.now().getYear()
                    ? LocalDate.now().getDayOfMonth()
                    : queryDate.lengthOfMonth();

            // 从今天开始向前查找连续的打卡记录
            for (int d = daysEvaluated; d >= 1; d--) {
                if (studentCheckinDays.contains(d)) {
                    continuous++;
                } else {
                    if (continuous > 0) break; // 如果已经找到了连续记录但遇到空缺则停止
                }
            }
        }
        metrics.setContinuousCheckin(continuous);
        dashboard.setMetrics(metrics);

        dashboard.setDurationTrend(analysisMapper.getStudentDurationTrend(studentId));
        if (dashboard.getDurationTrend() != null) {
            dashboard.getDurationTrend().forEach(t -> t.setValue(t.getValue() != null ? t.getValue().setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO));
        }
        dashboard.setScoreTrend(analysisMapper.getStudentScoreTrend(studentId));
        if (dashboard.getScoreTrend() != null) {
            dashboard.getScoreTrend().forEach(t -> t.setValue(t.getValue() != null ? t.getValue().setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO));
        }
        dashboard.setRadarData(analysisMapper.getStudentRadarData(studentId));
        if (dashboard.getRadarData() != null) {
            dashboard.getRadarData().forEach(r -> {
                if (r.getPersonalScore() != null) r.setPersonalScore(r.getPersonalScore().setScale(0, RoundingMode.HALF_UP));
                if (r.getClassAvgScore() != null) r.setClassAvgScore(r.getClassAvgScore().setScale(0, RoundingMode.HALF_UP));
            });
        }
        dashboard.setActionAnalysis(analysisMapper.getStudentActionAnalysis(studentId));
        if (dashboard.getActionAnalysis() != null) {
            dashboard.getActionAnalysis().forEach(a -> {
                if (a.getAvg() != null) a.setAvg(a.getAvg().setScale(0, RoundingMode.HALF_UP));
            });
        }

        List<Map<String, Object>> actionScores = analysisMapper.getStudentActionScores(studentId);
        AnalysisVO.StudentTags tags = calculateStudentTags(actionScores);
        dashboard.setTags(tags);

        return dashboard;
    }

    private AnalysisVO.StudentTags calculateStudentTags(List<Map<String, Object>> actionScores) {
        AnalysisVO.StudentTags tags = new AnalysisVO.StudentTags();
        
        if (actionScores == null || actionScores.isEmpty()) {
            tags.setProgressTag("暂无进步数据");
            tags.setWeakTag("暂无数据");
            tags.setStrongTag("暂无数据");
            tags.setProgressPercent(BigDecimal.ZERO);
            return tags;
        }

        BigDecimal maxProgress = BigDecimal.ZERO;
        String progressAction = "";
        BigDecimal progressHistoryAvg = BigDecimal.ONE;
        BigDecimal minScore = new BigDecimal("999");
        String weakAction = "";
        BigDecimal maxScore = BigDecimal.ZERO;
        String strongAction = "";

        for (Map<String, Object> score : actionScores) {
            String actionType = (String) score.get("actionType");
            BigDecimal currentScore = score.get("currentScore") != null 
                ? new BigDecimal(score.get("currentScore").toString()) 
                : BigDecimal.ZERO;
            BigDecimal historyAvg = score.get("historyAvgScore") != null 
                ? new BigDecimal(score.get("historyAvgScore").toString()) 
                : BigDecimal.ZERO;

            BigDecimal progress = currentScore.subtract(historyAvg);
            if (progress.compareTo(maxProgress) > 0) {
                maxProgress = progress;
                progressAction = actionType;
                progressHistoryAvg = historyAvg.compareTo(BigDecimal.ZERO) > 0 ? historyAvg : BigDecimal.ONE;
            }

            if (currentScore.compareTo(minScore) < 0) {
                minScore = currentScore;
                weakAction = actionType;
            }

            if (currentScore.compareTo(maxScore) > 0) {
                maxScore = currentScore;
                strongAction = actionType;
            }
        }

        if (maxProgress.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal progressPercent = maxProgress.multiply(new BigDecimal("100"))
                .divide(progressHistoryAvg, 0, RoundingMode.HALF_UP);
            tags.setProgressTag(progressAction + " 进步 +" + progressPercent + "%");
            tags.setProgressPercent(progressPercent);
        } else {
            tags.setProgressTag("暂无明显进步");
            tags.setProgressPercent(BigDecimal.ZERO);
        }

        tags.setWeakTag(weakAction + " 薄弱");
        tags.setStrongTag(strongAction + " 达人");

        return tags;
    }

    @Override
    public List<Map<String, Object>> getStudentsByClass(Long classId) {
        return analysisMapper.getStudentsByClass(classId);
    }

    @Override
    public MotionAnalysisData getMotionAnalysisData(Integer studentId) {
        if (studentId == null) {
            return null;
        }
        return motionAnalysisMapper.findLatestByStudentId(studentId);
    }

    @Override
    public StudentPhysique getStudentPhysique(Integer studentId) {
        if (studentId == null) {
            return null;
        }
        return studentPhysiqueMapper.findByStudentId(studentId);
    }
}
package com.saaes.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saaes.common.core.service.BaseServiceImpl;
import com.saaes.common.core.utils.CommonUtil;
import com.saaes.common.core.web.MyException;
import com.saaes.common.core.web.PageQuery;
import com.saaes.system.client.entity.AIAnalysisInfo;
import com.saaes.system.client.entity.AICardInfo;
import com.saaes.system.client.entity.AIContentInfo;
import com.saaes.system.client.entity.AIVideosHistory;
import com.saaes.system.mapper.AIVideosHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIVideosHistoryService extends BaseServiceImpl {

    @Autowired
    private AIVideosHistoryMapper aiVideosHistoryMapper;

    @Autowired
    private AIContentInfoService aiContentInfoService;

    @Autowired
    private AICardInfoService aicardInfoService;


    @Transactional(readOnly = true)
    public Page<AIVideosHistory> query(PageQuery<Map<String, Object>> pageQuery) {
        Map<String, Object> param = pageQuery.getParam();
        Long loginId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<AIVideosHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AIVideosHistory::getDeleted, false);

        if (CommonUtil.isNotEmpty(loginId)) {
            queryWrapper.eq(AIVideosHistory::getCreateBy, loginId);
        }
        if (CommonUtil.isNotEmpty(param.get("id"))) {
            queryWrapper.eq(AIVideosHistory::getId, param.get("id"));
        }
        if (CommonUtil.isNotEmpty(param.get("title"))) {
            queryWrapper.like(AIVideosHistory::getTitle, param.get("title"));
        }

        Page<AIVideosHistory> page = new Page<>(pageQuery.getCurrentPage(), pageQuery.getPageSize());
        return aiVideosHistoryMapper.selectPage(page, queryWrapper);
    }

    public AIVideosHistory queryById(Integer historyId) {
        String sql = "select * from ai_video_history where deleted is false and id = ?";
        try {
            return primaryJdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(AIVideosHistory.class), historyId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }


    @Transactional
    public Map<String, Object> save(AIVideosHistory aiVideosHistory) {
        if (aiVideosHistory == null) {
            throw new MyException("历史记录保存失败：参数为空");
        }

        // 获取当前登录用户的ID
        Long loginId = StpUtil.getLoginIdAsLong();
        if (loginId == null) {
            throw new MyException("历史记录保存失败：用户未登录");
        }

        // 查询数据库中的createBy字段值
        String selectSql = "SELECT create_time, create_by FROM ai_video_history WHERE id = ? AND deleted = false AND create_by = ?";
        AIVideosHistory result = null;
        try {
            result = primaryJdbcTemplate.queryForObject(selectSql, new Object[]{aiVideosHistory.getId(), loginId}, new BeanPropertyRowMapper<>(AIVideosHistory.class));
        } catch (EmptyResultDataAccessException e) {
            result = null;
        }

        // 如果查询到该记录，保持原来的createBy值，否则使用当前登录用户ID
        if (result != null) {
            aiVideosHistory.setCreateTime(result.getCreateTime());
            aiVideosHistory.setCreateBy(result.getCreateBy());
            baseJdbcDao.update(aiVideosHistory);
            return Map.of("id", aiVideosHistory.getId());
        }

        // 如果记录不存在，执行插入操作
        KeyHolder keyHolder = new GeneratedKeyHolder(); // 用于获取生成的主键

        // 执行插入操作时，避免将 keyHolder 作为 SQL 参数传递
        String insertSql = "INSERT INTO ai_video_history (title, create_by, create_time) VALUES (?, ?, ?)";  // 替换成实际的插入 SQL
        primaryJdbcTemplate.update(
                new PreparedStatementCreator() {
                    @Override
                    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                        PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                        ps.setString(1, aiVideosHistory.getTitle());
                        ps.setLong(2, loginId);  // 设置 create_by 字段为当前登录用户ID
                        ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                        return ps;
                    }
                },
                keyHolder // keyHolder作为第三个参数传递
        );

        // 获取插入后的ID
        Number newId = keyHolder.getKey();
        return Map.of("history_id", newId.longValue());  // 返回插入后的ID
    }

    /**
     * 查询视频识别结果
     * @param id
     * @return
     */
    @Transactional(readOnly = true)
    public AIAnalysisInfo queryAnalysisResult(Integer id) {
        int loginId = (int) StpUtil.getTokenSession().getLoginId();

        // 基础 SQL 语句
        String sql = "SELECT id, overall, technique, stability, fluency, accuracy, create_time, update_time, create_by, deleted FROM ai_analysis_result  WHERE deleted = false AND create_by = ?";

        // 参数列表
        List<Object> args = new ArrayList<>();
        args.add(loginId);

        // 动态拼接查询条件
        if (id != null) {
            sql += " AND id = ?";
            args.add(id);
        }

        return primaryJdbcTemplate.queryForObject(sql, args.toArray(), new BeanPropertyRowMapper<>(AIAnalysisInfo.class));
    }

    /**
     * ids批量删除视频分析历史记录
     */
    @Transactional
    public void del(List<Integer> ids) {
        String sql = "update ai_video_history set deleted = 1 where id in (:ids)";
        Map<String, Object> paramMap = new HashMap<>() {{
            put("ids", ids);
        }};
        primaryNPJdbcTemplate.update(sql, paramMap);
    }



    public Map<String,Object> queryHistoryDetail(Integer historyId) {
        // AI聊天记录
        List<AIContentInfo> contentList = aiContentInfoService.getContentWithCardInfoByHistoryId(historyId);



        // AI聊天卡片ids
//        List<Integer> cardIds = aiContentInfoService.selectCardIdsByHistoryId(historyId);

        // 根据卡片ids查询识别结果
//        List<AICardInfo> analysisResult = aicardInfoService.getCardWithAnalysisResultById(cardIds);

        return Map.of("contentList", contentList);
    }



}

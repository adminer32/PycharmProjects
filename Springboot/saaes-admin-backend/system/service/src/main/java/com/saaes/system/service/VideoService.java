package com.saaes.system.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSONObject;
import com.saaes.common.core.Constant;
import com.saaes.common.core.service.BaseServiceImpl;
import com.saaes.common.core.web.MyException;
import com.saaes.system.client.entity.AIVideosHistory;
import com.saaes.system.client.entity.SysFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.Map;
import java.util.UUID;

@Service
public class VideoService extends BaseServiceImpl {

    @Value("${saaes-config.video-post-url}")
    private static String postUrl;

//    private static final String postUrl = Constant.VIDEO_POST_URL;

    private static final String videoUrlPrefix = Constant.BASE_URL;

    @Autowired
    private AIVideosHistoryService aiVideosHistoryService;

    @Transactional
    public Map<String, Object> save(SysFile sysFile, Integer historyId) {

        if (sysFile == null || sysFile.getObject() == null) {
            throw new MyException("已上传的视频文件对象为空");
        }

        UUID uuid = UUID.randomUUID();
        Long loginId = StpUtil.getLoginIdAsLong();

        KeyHolder keyHolder = new GeneratedKeyHolder(); // 用于获取生成的主键

        String insertSql = "INSERT INTO ai_analysis_result (video_id, task_id, create_by, create_time) VALUES (?, ?, ?, ?)";  // 替换成实际的插入 SQL
        primaryJdbcTemplate.update(
                new PreparedStatementCreator() {
                    @Override
                    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                        PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                        ps.setString(1, String.valueOf(sysFile.getId()));
                        ps.setString(2, uuid.toString());
                        ps.setLong(3, loginId);  // 设置 create_by 字段为当前登录用户ID
                        ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                        return ps;
                    }
                },
                keyHolder // keyHolder作为第三个参数传递
        );

        // 获取插入后的ID
        Number newId = keyHolder.getKey();

        if (historyId != null) {
             AIVideosHistory aiVideosHistory = aiVideosHistoryService.queryById(historyId);
//             aiVideosHistory.setAnalysisResultId((int) newId.intValue());
             baseJdbcDao.update(aiVideosHistory);
        }

        JSONObject postJson = new JSONObject();
        postJson.put("video_url", videoUrlPrefix + sysFile.getObject());
        postJson.put("task_id", uuid.toString());

        String response = HttpRequest.post(postUrl)
                .contentType(ContentType.JSON.getValue()) // 设置 Content-Type
                .body(postJson.toString()) // 设置请求体
                .execute()
                .body(); // 获取返回内容
        System.out.println(response);

        return Map.of("taskId", uuid.toString(), "AnalysisResultId", newId.intValue());

    }

}

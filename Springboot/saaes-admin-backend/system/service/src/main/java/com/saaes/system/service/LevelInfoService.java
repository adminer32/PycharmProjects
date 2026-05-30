package com.saaes.system.service;

import com.saaes.common.core.service.BaseServiceImpl;
import com.saaes.system.client.entity.LevelInfo;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class LevelInfoService extends BaseServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(LevelInfoService.class);

    /**
     * 根据名字查询等级信息
     * @param name 等级名称
     * @return 查询到的 LevelInfo 实体，如果没有结果，返回 null
     */
    public LevelInfo queryByName(String name) {
        String sql = "SELECT level_id, level_name FROM level_info WHERE level_name = ?";

        // 使用 query 方法避免抛出异常
        List<LevelInfo> levelInfos = primaryJdbcTemplate.query(
                sql,
                new BeanPropertyRowMapper<>(LevelInfo.class),
                name
        );

        if (levelInfos.isEmpty()) {
            return null;  // 返回 null 或者其他默认值
        }

        return levelInfos.get(0);  // 返回第一个匹配的结果
    }
}

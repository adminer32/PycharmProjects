package com.saaes.common.core.service;

import com.saaes.common.core.dao.BaseJdbcDao;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 基础service实现类
 */
@Repository(value = "baseService")
public class BaseServiceImpl implements BaseService {
    @Resource
    protected JdbcTemplate primaryJdbcTemplate;
    @Resource
    protected NamedParameterJdbcTemplate primaryNPJdbcTemplate;
    @Resource
    protected BaseJdbcDao baseJdbcDao;
    @Resource
    protected RedisTemplate<String, Object> redisTemplate;

    public JdbcTemplate getJdbcTemplate() {
        return primaryJdbcTemplate;
    }

}

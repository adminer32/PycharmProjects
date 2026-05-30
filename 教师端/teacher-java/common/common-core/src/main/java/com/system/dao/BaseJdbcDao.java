package com.system.dao;

import com.system.web.MyException;
import com.system.web.PageQuery;
import com.system.web.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Repository
public interface BaseJdbcDao {
    <K> K findById(Class<K> clazz, Serializable id);

    <K> K findById(Class<K> clazz, JdbcTemplate jdbcTemplate, Serializable id);

    <K> K findBySql(Class<K> classname, String sql, Object... args) throws MyException;

    <K> K findBySql(Class<K> classname, String sql, JdbcTemplate jdbcTemplate, Object... args) throws MyException;

    <K> List<K> findList(Class<K> clazz, String sql, Object... args);

    <K> List<K> findList(Class<K> clazz, String sql, JdbcTemplate jdbcTemplate, Object... args);

    PageResult<Map> query(PageQuery<?> pageQuery);

    <K> PageResult<K> query(Class<K> clazz, PageQuery<?> pageQuery);

    <K> PageResult<K> query(Class<K> clazz, PageQuery<?> pageQuery, JdbcTemplate jdbcTemplate);

    <E> void insert(JdbcTemplate jdbcTemplate, E[] entities);

    <E> void insert(E[] entities);

    <E> Integer insert(E entity);

    <E> void insert(JdbcTemplate jdbcTemplate, E entity);

    <E> void update(E entity);

    <E> void update(JdbcTemplate jdbcTemplate, E entity);

    <E> void deleteById(Class<E> clazz, Serializable id);

    <E> void deleteById(Class<E> clazz, JdbcTemplate jdbcTemplate, Serializable id);
}

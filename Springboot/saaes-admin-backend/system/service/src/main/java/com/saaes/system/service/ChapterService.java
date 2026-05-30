package com.saaes.system.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.saaes.common.core.service.BaseServiceImpl;
import com.saaes.common.core.utils.CommonUtil;
import com.saaes.common.core.web.PageQuery;
import com.saaes.common.core.web.PageResult;
import com.saaes.system.client.entity.ChapterInfo;
import com.saaes.system.client.entity.CourseInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class ChapterService extends BaseServiceImpl {


    /**
     * 章节查询
     *
     */
    public PageResult<CourseInfo> query(PageQuery<Map<String, Object>> pageQuery) {
        Map<String, Object> params = pageQuery.getParam();
        String sql = "SELECT ci.id, ci.name, ci.category_id, ci.teacher_name, ci.enabled, ci.deleted, " +
                "cc.name AS category_name " +
                "FROM course_info ci " +
                "LEFT JOIN course_category cc ON ci.category_id = cc.id " +
                "WHERE ci.deleted IS FALSE ";

        if (CommonUtil.isNotEmpty(params.get("name"))) {
            sql += " AND ci.name LIKE '%' || ? || '%'";
            pageQuery.addArg(params.get("name"));
        }

        if (CommonUtil.isNotEmpty(params.get("category_name"))) {
            sql += " AND cc.name = ?";
            pageQuery.addArg(params.get("category_name"));
        }

        if (CommonUtil.isNotEmpty(params.get("teacher_name"))) {
            sql += " AND ci.teacher_name LIKE '%' || ? || '%'";
            pageQuery.addArg(params.get("teacher_name"));
        }

        if (CommonUtil.isNotEmpty(params.get("enabled"))) {
            sql += " AND ci.enabled = ?";
            pageQuery.addArg(params.get("enabled"));
        }

        pageQuery.setBaseSql(sql);
        return baseJdbcDao.query(CourseInfo.class, pageQuery);
    }


    /**
     * 章节保存
     */
    @Transactional
    public ChapterInfo save(ChapterInfo chapter_info) {
        if(chapter_info == null || chapter_info.getName().isEmpty() || chapter_info.getContent().isEmpty())return null;
        SaSession session = StpUtil.getSession();
        if(chapter_info.getId() == null) {
            chapter_info.setCreateBy((Integer) session.getLoginId());
            baseJdbcDao.insert(chapter_info);
        } else {
            CourseInfo findResult = baseJdbcDao.findById(CourseInfo.class, chapter_info.getId());
            if(findResult != null && Objects.equals(findResult.getId(), chapter_info.getId())) {
                chapter_info.setUpdateBy((Integer) session.getLoginId());
                baseJdbcDao.update(chapter_info);
                return chapter_info;
            }
            return null;
        }
        return chapter_info;
    }

    /**
     * 章节删除
     * @param ids
     * @return
     */
    @Transactional
    public Map del(List<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Map.of("success", 0, "error",0);
        }
        log.info("批量删除章节--");
        String sql = "update chapter_info set deleted = 1 where id in (:ids)";
        Map<String, Object> paramMap = Collections.singletonMap("ids", ids);
        int result = primaryNPJdbcTemplate.update(sql, paramMap);
        return Map.of("success", result, "error", ids.size() - result);
    }

}

package com.saaes.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.saaes.common.core.Constant;
import com.saaes.common.core.service.BaseServiceImpl;
import com.saaes.common.core.utils.CommonUtil;
import com.saaes.common.core.web.MyException;
import com.saaes.common.core.web.PageQuery;
import com.saaes.common.core.web.PageResult;
import com.saaes.system.client.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CourseService extends BaseServiceImpl {

    @Autowired
    private SysFileService sysFileService;

    @Autowired
    private LevelInfoService levelInfoService;

    @Autowired
    private AiService aiService;

    /**
     * 课程分类查询
     * @param pageQuery
     * @return
     */
    public PageResult<CourseCategory> getCourseCateGoryList(PageQuery<Map<String, Object>> pageQuery) {
        Map<String, Object> params = pageQuery.getParam();
        String sql = """
                select a.*, b.name parent_name from course_category a
                left join course_category b on a.parent_id = b.id where a.deleted is false
                """;
        if (CommonUtil.isNotEmpty(params.get("name"))) {
            sql += " and name like '%' ? '%'";
            pageQuery.addArg(params.get("name"));
        }

        if (CommonUtil.isNotEmpty(params.get("id"))) {
            sql += " and id = ?";
            pageQuery.addArg(params.get("id"));
        }

        if (CommonUtil.isNotEmpty(params.get("enabled"))) {
            sql += " and enabled = ?";
            pageQuery.addArg(params.get("enabled"));
        }
        pageQuery.setBaseSql(sql);
        return baseJdbcDao.query(CourseCategory.class, pageQuery);
    }

    /**
     * 课程查询，返回父类 -> 子类 -> 课程的结构
     */

    public PageResult<SubCategory> query(PageQuery<Map<String, Object>> pageQuery) {
        Map<String, Object> params = pageQuery.getParam();

        // 获取分页参数
        int pageSize = pageQuery.getPageSize();
        int pageNum = pageQuery.getCurrentPage();
        int offset = (pageNum - 1) * pageSize;

        // SQL 查询
        String sql = "SELECT cc.id AS categoryId, " +
                "cc.name AS categoryName, " +
                "ci.id AS courseId, " +
                "ci.name AS courseName, " +
                "ci.teacher_name AS teacherName, " +
                "ci.cover_image_url AS coverImageUrl, " +
                "ci.enabled, " +
                "ci.create_time AS createTime, " +
                "ci.update_time AS updateTime, " +
                "ci.content, " +
                "ci.description, " +
                "fs.object AS videoUrl, " +
                "fs.video_duration AS videoDuration, " +
                "lv.level_name AS levelName, " +
                "ci.ai_note_id AS aiNoteId, " +
                "COALESCE(pcc.name, cc.name) AS parentCategoryName, " +
                "COALESCE(pcc.id, cc.id) AS parentCategoryId " +
                "FROM course_info ci " +
                "LEFT JOIN course_category cc ON ci.category_id = cc.id " +
                "LEFT JOIN course_category pcc ON cc.parent_id = pcc.id " +
                "LEFT JOIN sys_file fs ON ci.video_url_id = fs.id " +
                "LEFT JOIN level_info lv ON ci.level_id = lv.level_id " +
                "WHERE ci.deleted IS FALSE ";

        // 添加筛选条件
        if (CommonUtil.isNotEmpty(params.get("parent_category_name"))) {
            sql += " AND COALESCE(pcc.name, cc.name) = ?";
            pageQuery.addArg(params.get("parent_category_name"));

            // 如果是个人课程，则添加 create_by 条件
            if ("个人课程".equals(params.get("parent_category_name")) && CommonUtil.isNotEmpty(StpUtil.getLoginIdAsLong())) {
                sql += " AND ci.create_by = ?";
                pageQuery.addArg(StpUtil.getLoginIdAsLong());
            }
        }

        if (CommonUtil.isNotEmpty(params.get("category_name"))) {
            sql += " AND cc.name = ?";
            pageQuery.addArg(params.get("category_name"));
        }
        if (CommonUtil.isNotEmpty(params.get("teacher_name"))) {
            sql += " AND ci.teacher_name = ?";
            pageQuery.addArg(params.get("teacher_name"));
        }

        // 分页 SQL
        String pagedSql = sql + " LIMIT " + pageSize + " OFFSET " + offset;

        // 存储子类
        Map<Integer, SubCategory> groupedResult = new HashMap<>();

        // 执行分页查询，使用 RowMapper 昳射到实体类
        List<SubCategory> rawData = primaryJdbcTemplate.query(pagedSql, pageQuery.getArgs().toArray(), new RowMapper<SubCategory>() {
            @Override
            public SubCategory mapRow(ResultSet rs, int rowNum) throws SQLException {
                String subCategoryName = rs.getString("categoryName");
                SubCategory subCategory = groupedResult.get(rs.getInt("categoryId"));

                // 查找是否已有相同名称的子类
                if (subCategory == null) {
                    subCategory = new SubCategory();
                    subCategory.setSubCategoryId(rs.getInt("categoryId"));
                    subCategory.setSubCategoryName(subCategoryName);
                    subCategory.setSubCategoryCourse(new ArrayList<>());
                    groupedResult.put(rs.getInt("categoryId"), subCategory);
                }

                // 创建课程并添加到子类的课程列表中
                CourseInfo course = new CourseInfo();
                course.setId(rs.getInt("courseId"));
                course.setName(rs.getString("courseName"));
                course.setTeacherName(rs.getString("teacherName"));
                course.setCoverImageUrl(rs.getString("coverImageUrl"));
                course.setEnabled(rs.getBoolean("enabled"));
                course.setCreateTime((LocalDateTime) rs.getObject("createTime"));
                course.setUpdateTime((LocalDateTime) rs.getObject("updateTime"));
                course.setContent(rs.getString("content"));
                course.setDescription(rs.getString("description"));
                course.setVideoUrl(rs.getString("videoUrl"));
                course.setVideoDuration(rs.getString("videoDuration"));
                course.setLevelName(rs.getString("levelName"));
                course.setAiNoteId(rs.getInt("aiNoteId"));

                // 将课程添加到对应子类
                subCategory.getSubCategoryCourse().add(course);

                return subCategory;
            }
        });

        // 计算总数
        String countSql = "SELECT COUNT(*) FROM course_info ci " +
                "LEFT JOIN course_category cc ON ci.category_id = cc.id " +
                "LEFT JOIN course_category pcc ON cc.parent_id = pcc.id " +
                "WHERE ci.deleted IS FALSE ";

        if (CommonUtil.isNotEmpty(params.get("parent_category_name"))) {
            countSql += " AND COALESCE(pcc.name, cc.name) = ?";

            // 修改：个人课程添加 create_by 条件
            if ("个人课程".equals(params.get("parent_category_name")) && CommonUtil.isNotEmpty(StpUtil.getLoginIdAsLong())) {
                countSql += " AND ci.create_by = ?";
            }
        }

        if (CommonUtil.isNotEmpty(params.get("category_name"))) {
            countSql += " AND cc.name = ?";
        }
        if (CommonUtil.isNotEmpty(params.get("teacher_name"))) {
            countSql += " AND ci.teacher_name = ?";
        }

        // 查询总数
        int total = primaryJdbcTemplate.queryForObject(countSql, pageQuery.getArgs().toArray(), Integer.class);

        // 返回分页结果，只返回子类
        return new PageResult<>(new ArrayList<>(groupedResult.values()), total);
    }


    /**
     * 随机返回5条课程信息
     * @return
     */
    public List<Map<String, Object>> queryRandomCoursesWithCategory() {
        String sql = "SELECT ci.id, ci.create_time, ci.update_time, ci.create_by, ci.update_by, ci.deleted, " +
                "ci.name, ci.category_id, ci.content, ci.description, ci.video_url_id, fs.object AS video_url, " +
                "fs.video_duration, lv.level_name, lv.level_id, ci.ai_note_id, ci.teacher_name, ci.cover_image_url, " +
                "ci.enabled, cc.name AS category_name " +
                "FROM course_info ci " +
                "LEFT JOIN sys_file fs ON ci.video_url_id = fs.id " +
                "LEFT JOIN level_info lv ON ci.level_id = lv.level_id " +
                "LEFT JOIN course_category cc ON ci.category_id = cc.id " +
                "WHERE ci.deleted IS FALSE";

        // 查询所有结果
        List<Map<String, Object>> allCourses = primaryJdbcTemplate.query(sql, new ColumnMapRowMapper());

        // 打乱并取前5个
        Collections.shuffle(allCourses);
        List<Map<String, Object>> result = allCourses.stream().limit(5).collect(Collectors.toList());

        // 格式化字段
        for (Map<String, Object> map : result) {
            map.put("videoUrl", Constant.BASE_URL + map.get("video_url"));
            map.put("videoDuration", map.get("video_duration"));
            map.put("coverImageUrl", map.get("cover_image_url"));
            map.put("levelName", map.get("level_name"));
            map.put("categoryName", map.get("category_name"));

            // 移除数据库原始字段
            map.keySet().removeIf(key -> key.contains("_") && !List.of(
                    "videoUrl", "videoDuration", "coverImageUrl", "levelName", "levelId", "categoryName"
            ).contains(key));
        }

        return result;
    }






    /**
     * ai笔记查询
     * @param id
     * @return
     */
    public AiNoteInfo queryNote(Integer id) {
        if(id == null) {
            throw new MyException("笔记id不能为空");
        }
        String sql = "SELECT * FROM ai_note_info WHERE id = ? AND deleted IS FALSE";

        try {
            // 使用 BeanPropertyRowMapper 将查询结果映射到 AiNote 实体
            AiNoteInfo aiNote = primaryJdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(AiNoteInfo.class), id);
            return aiNote;
        } catch (EmptyResultDataAccessException e) {
            throw new MyException("id所属课程笔记不存在");
        }

    }


    /**
     * 课程保存
     */
    @Transactional
    public CourseInfo save(CourseInfo courseInfo, SysFile sysFile, String url) {

        LevelInfo levelInfo = null;

        if (courseInfo.getLevelName() != null || !courseInfo.getLevelName().isEmpty()) {
            levelInfo = levelInfoService.queryByName(courseInfo.getLevelName());
        }

        if (courseInfo.getCategoryId() == null) {throw new MyException("新增或更新课程时，分类ID不能为空");}

        //新增
        if (courseInfo.getId() == null) {
            if (sysFile == null) {throw new MyException("新增课程时，视频不能为空");}
            courseInfo.setVideoUrlId(sysFile.getId());
            if (levelInfo != null) {courseInfo.setLevelId(levelInfo.getLevelId());}

            Long loginId = StpUtil.getLoginIdAsLong();

            KeyHolder keyHolder = new GeneratedKeyHolder(); // 用于获取生成的主键

            String insertSql = "INSERT INTO course_info (name, category_id, content, description, video_url_id, teacher_name, cover_image_url, level_id, create_by, create_time, enabled, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            primaryJdbcTemplate.update(
                    new PreparedStatementCreator() {
                        @Override
                        public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                            PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                            ps.setString(1, courseInfo.getName());
                            ps.setLong(2, courseInfo.getCategoryId());
                            ps.setString(3, courseInfo.getContent());
                            ps.setString(4, courseInfo.getDescription());
                            ps.setLong(5, courseInfo.getVideoUrlId());
                            ps.setString(6, courseInfo.getTeacherName() == null ? "" : courseInfo.getTeacherName());
                            ps.setString(7, courseInfo.getCoverImageUrl() == null ? "" : courseInfo.getCoverImageUrl());
                            ps.setLong(8, courseInfo.getLevelId() == null ? 0L : courseInfo.getLevelId());
                            ps.setLong(9, loginId);
                            ps.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
                            ps.setInt(11, 1);
                            ps.setInt(12, 0);
                            return ps;
                        }
                    },
                    keyHolder // keyHolder作为第三个参数传递
            );

            // 获取插入后的ID
            Number newId = keyHolder.getKey();
            assert newId != null;
            aiService.add(Map.of("url", url, "video_id", sysFile.getId(), "course_id", newId));
            String idSql = "SELECT * FROM course_info WHERE id = ? AND deleted IS FALSE";
            return baseJdbcDao.findBySql(CourseInfo.class, idSql, newId);
        }

        CourseInfo findResult = baseJdbcDao.findById(CourseInfo.class, courseInfo.getId());

        // 更新，有更新文件
        if (findResult != null && sysFile != null && sysFile.getId() != null && sysFile.getObject() != null) {
            courseInfo.setVideoUrlId(sysFile.getId());
            if (levelInfo != null) {courseInfo.setLevelId(levelInfo.getLevelId());}
            aiService.add(Map.of("url", sysFile.getObject(), "video_id", sysFile.getId(), "course_id", courseInfo.getId()));
            return courseInfo;
        }
        // 更新普通字段
        if(findResult != null && Objects.equals(findResult.getId(), courseInfo.getId())) {
            baseJdbcDao.update(courseInfo);
        }
        return courseInfo;
    }

    @Transactional
    public Map del(List<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Map.of("success", 0, "error",0);
        }
        log.info("批量删除课程--");
        String sql = "update course_info set deleted = 1 where id in (:ids)";
        Map<String, Object> paramMap = Collections.singletonMap("ids", ids);
        int result = primaryNPJdbcTemplate.update(sql, paramMap);
        return Map.of("success", result, "error", ids.size() - result);
    }


}

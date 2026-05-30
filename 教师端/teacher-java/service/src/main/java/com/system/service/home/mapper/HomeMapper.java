package com.system.service.home.mapper;

import com.system.service.home.vo.HomeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface HomeMapper {
    Map<String, Object> getClassOverview(@Param("classId") String classId);
    
    Integer getPendingHomeworkCount(@Param("classId") String classId);
    
    Integer getPendingTrainingPlanCount(@Param("classId") String classId);
    
    List<HomeVO.Notice> getNotices(@Param("classId") String classId, @Param("offset") int offset, @Param("limit") int limit);
    
    Integer getNoticeCount(@Param("classId") String classId);
    
    void insertNotice(@Param("classId") Long classId, @Param("teacherId") Long teacherId,
            @Param("title") String title, @Param("content") String content, @Param("type") String type);
    
    int updateNotice(@Param("id") Long id, @Param("title") String title, 
            @Param("content") String content, @Param("type") String type);
    
    int deleteNotice(@Param("id") Long id);
    
    Long getLastInsertId();
    
    HomeVO.Notice getNoticeById(@Param("id") Long id);
}

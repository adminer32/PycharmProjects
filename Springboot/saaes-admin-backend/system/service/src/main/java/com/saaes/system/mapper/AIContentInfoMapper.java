package com.saaes.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saaes.system.client.dto.ChatMessageDTO;
import com.saaes.system.client.entity.AIAnalysisInfo;
import com.saaes.system.client.entity.AIContentInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AIContentInfoMapper extends BaseMapper<AIContentInfo> {

    List<AIContentInfo> getContentWithCardInfo();

    List<AIContentInfo> getContentWithCardInfoByHistoryId(Integer historyId);

    @Select("SELECT card_id FROM ai_content_info WHERE history_id = #{historyId} AND type = 'card'")
    List<Integer> selectCardIdsByHistoryId(@Param("historyId") Integer historyId);

    @Select("SELECT sender AS role, text AS content " +
            "FROM ai_content_info " +
            "WHERE history_id = #{historyId} " +
            "AND type = 'text' " +
            "ORDER BY send_time ASC")
    List<ChatMessageDTO> selectTextByHistoryId(@Param("historyId") String historyId);

    /**
     * 根据 historyId 查询 AI 分析信息（仅 type = 'card' 的内容）
     * @param historyId 历史会话ID
     * @return 分析信息列表
     */
    List<AIAnalysisInfo> listAnalysisByHistoryId(@Param("historyId") Integer historyId);

    // 更新 AIContentInfo
    int update(AIContentInfo aiContentInfo);
}


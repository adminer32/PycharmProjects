package com.saaes.system.service;

import com.saaes.system.client.entity.AIAnalysisInfo;
import com.saaes.system.client.entity.AICardInfo;
import com.saaes.system.mapper.AIAnalysisInfoMapper;
import com.saaes.system.mapper.AICardInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AICardInfoService {

    @Autowired
    private AICardInfoMapper aiCardInfoMapper;

    @Autowired
    private AIAnalysisInfoMapper aiAnalysisInfoMapper;

    // 根据 cardId列表 查询卡片和识别结果信息
    @Transactional
    public List<AICardInfo> getCardWithAnalysisResultById(List<Integer> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<AICardInfo> cardInfos = aiCardInfoMapper.selectByIds(cardIds);
        if (cardInfos.isEmpty()) {
            return cardInfos;
        }

        List<Integer> analysisResultIds = cardInfos.stream()
                .map(AICardInfo::getAnalysisId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!analysisResultIds.isEmpty()) {
            Map<Integer, AIAnalysisInfo> analysisInfoMap = aiAnalysisInfoMapper.selectBatchIds(analysisResultIds)
                    .stream()
                    .collect(Collectors.toMap(AIAnalysisInfo::getId, Function.identity()));
            for (AICardInfo cardInfo : cardInfos) {
                cardInfo.setAnalysisInfo(analysisInfoMap.get(cardInfo.getAnalysisId()));
            }
        }
        return cardInfos;
    }

}



package com.system.service.learning.service.impl;

import com.system.service.learning.entity.ActionVideo;
import com.system.service.learning.mapper.ActionVideoMapper;
import com.system.service.learning.service.ActionVideoService;
import com.system.service.learning.vo.ActionVideoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActionVideoServiceImpl implements ActionVideoService {

    @Autowired
    private ActionVideoMapper actionVideoMapper;

    @Override
    public List<ActionVideoVO.VideoInfo> getVideoList(String classId, String category) {
        List<ActionVideoVO.VideoInfo> list = actionVideoMapper.getVideoList(classId, category);
        if (list == null) return new ArrayList<>();
        for (ActionVideoVO.VideoInfo info : list) {
            if (info.getRecommendedToStr() != null && !info.getRecommendedToStr().isEmpty()) {
                String[] ids = info.getRecommendedToStr().split(",");
                List<Long> studentIds = new ArrayList<>();
                for (String id : ids) {
                    studentIds.add(Long.parseLong(id.trim()));
                }
                info.setRecommendedTo(studentIds);
            } else {
                info.setRecommendedTo(new ArrayList<>());
            }
        }
        return list;
    }

    @Override
    public ActionVideoVO.VideoInfo addVideo(ActionVideoVO.AddVideoInput input) {
        ActionVideo video = ActionVideo.builder()
                .title(input.getTitle())
                .url(input.getUrl())
                .duration(input.getDuration() != null ? input.getDuration() : "00:00")
                .category(input.getCategory())
                .description(input.getDescription())
                .teacherId(input.getTeacherId())
                .classId(input.getClassId())
                .build();
        actionVideoMapper.insertActionVideo(video);

        ActionVideoVO.VideoInfo result = new ActionVideoVO.VideoInfo();
        result.setId(video.getId());
        result.setTitle(video.getTitle());
        result.setUrl(video.getUrl());
        result.setDuration(video.getDuration());
        result.setCategory(video.getCategory());
        result.setDescription(video.getDescription());
        result.setRecommendedTo(new ArrayList<>());
        return result;
    }

    @Override
    public boolean recommendVideo(ActionVideoVO.RecommendInput input) {
        if (input.getStudentIds() == null || input.getStudentIds().isEmpty()) {
            return false;
        }
        for (Long studentId : input.getStudentIds()) {
            Map<String, Object> params = new HashMap<>();
            params.put("videoId", input.getVideoId());
            params.put("studentId", studentId);
            params.put("teacherId", input.getTeacherId());
            params.put("comment", input.getComment());
            actionVideoMapper.insertRecommendation(params);
        }
        return true;
    }
}

package com.system.service.learning.service;

import com.system.service.learning.vo.ActionVideoVO;
import java.util.List;

public interface ActionVideoService {

    List<ActionVideoVO.VideoInfo> getVideoList(String classId, String category);

    ActionVideoVO.VideoInfo addVideo(ActionVideoVO.AddVideoInput input);

    boolean recommendVideo(ActionVideoVO.RecommendInput input);
}

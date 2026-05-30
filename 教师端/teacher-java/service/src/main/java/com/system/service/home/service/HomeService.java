package com.system.service.home.service;

import com.system.service.home.vo.HomeVO;
import java.util.List;
import java.util.Map;

public interface HomeService {
    HomeVO.Overview getHomeOverview(String classId);

    List<HomeVO.PendingItem> getPendingItems(Long teacherId, String classId);

    Map<String, Object> getNotices(String classId, int page, int size);

    HomeVO.Notice saveNotice(HomeVO.NoticeInput input);

    HomeVO.Notice updateNotice(HomeVO.NoticeInput input);

    boolean deleteNotice(Long id);
}

package com.system.service.learning.vo;

import lombok.Data;
import java.util.List;

@Data
public class ActionVideoVO {

    @Data
    public static class VideoInfo {
        private Long id;
        private String title;
        private String url;
        private String duration;
        private String category;
        private String description;
        private String recommendedToStr;
        private List<Long> recommendedTo;
    }

    @Data
    public static class AddVideoInput {
        private String title;
        private String duration;
        private String url;
        private String category;
        private String description;
        private Long teacherId;
        private Long classId;
    }

    @Data
    public static class RecommendInput {
        private Long videoId;
        private List<Long> studentIds;
        private Long teacherId;
        private String comment;
    }
}

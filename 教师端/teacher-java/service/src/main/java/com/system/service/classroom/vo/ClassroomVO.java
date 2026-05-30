package com.system.service.classroom.vo;

import lombok.Data;

@Data
public class ClassroomVO {

    @Data
    public static class LessonItem {
        private Long id;
        private String title;
        private String thumbnail;
        private String videoUrl;
        private String duration;
        private String recordedAt;
        private String className;
    }

    @Data
    public static class KnowledgePointVO {
        private Long id;
        private String title;
        private String description;
        private Integer orderNum;
        private Boolean hasVideo;
        private String videoUrl;
        private Long lessonId;
    }

    @Data
    public static class NoteVO {
        private Long id;
        private String content;
        private String createdAt;
        private Long lessonId;
    }

    @Data
    public static class AddKnowledgeInput {
        private Long lessonId;
        private String title;
        private String description;
        private String demoVideoUrl;
    }

    @Data
    public static class AddNoteInput {
        private Long lessonId;
        private String content;
    }

    @Data
    public static class NoteItem {
        private Long id;
        private String content;
    }

    @Data
    public static class SaveNotesInput {
        private Long lessonId;
        private java.util.List<NoteItem> notes;
    }

    @Data
    public static class AnalyzeInput {
        private Long videoId;
        private Long teacherId;
    }

    @Data
    public static class AnalyzeResult {
        private Integer totalStudents;
        private Integer activeStudents;
        private Integer attendanceRate;
        private Integer participationRate;
        private String analyzedAt;
    }
}

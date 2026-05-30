package com.saaes.system.client.dto;

import lombok.Data;

@Data
public class AiDataStatsDTO {
    private CardStats cardStats;
    private NoteStats noteStats;

    // 嵌套结构
    @Data
    public static class CardStats {
        private int totalCount;
        private int todayCount;
        private int last30DaysCount;
    }

    @Data
    public static class NoteStats {
        private int totalCount;
        private int todayCount;
        private int last30DaysCount;
    }
}

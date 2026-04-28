package com.livestock.health.model.vo;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 羊只行为详情 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SheepBehaviorDetailVO {

    private String animalId;

    private String name;

    private String breed;

    private Integer age;

    private Double weight;

    private Double healthScore;

    private Integer ruminationTime;

    private Integer feedingCount;

    private Integer activityLevel;

    private Integer restingTime;

    private Long ruminationEfficiency;

    private Long feedingQuality;

    private String status;

    private LocalDateTime lastUpdate;

    private LocalDateTime lastDietAdjustAt;

    private String summary;

    private List<String> nutritionAdvice;

    private List<PatternPoint> hourlyPattern;

    private List<HistoryRecord> history;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryRecord {
        private String date;
        private Integer ruminationTime;
        private Integer feedingCount;
        private Integer ruminationEfficiency;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatternPoint {
        private String label;
        private Integer rumination;
        private Integer feeding;
        private Integer activity;
        private Integer resting;
    }
}

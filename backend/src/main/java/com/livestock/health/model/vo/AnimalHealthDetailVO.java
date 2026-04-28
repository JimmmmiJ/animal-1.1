package com.livestock.health.model.vo;

import lombok.*;

import java.util.List;

/**
 * 牲畜健康详情 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalHealthDetailVO {

    private String animalId;

    private String name;

    private String breed;

    private String deviceId;

    private Integer age;

    private Double weight;

    private Double healthScore;

    private String healthStatus;

    private String riskLevel;

    private String behaviorStatus;

    private String estrusStatus;

    private Double currentTemperature;

    private Integer currentHeartRate;

    private Integer currentActivity;

    private Integer dailyRuminationTime;

    private Integer dailyFeedingCount;

    private String focusReason;

    private List<TrendPoint> temperatureTrend;

    private List<TrendPoint> activityTrend;

    private List<String> interventionSuggestions;

    private List<HealthRecord> recentRecords;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthRecord {
        private String time;
        private String type;
        private String value;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String label;
        private Double value;
    }
}

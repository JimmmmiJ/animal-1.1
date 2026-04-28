package com.livestock.health.model.vo;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风险牲畜详情 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAnimalVO {

    private String animalId;

    private String name;

    private String breed;

    private Integer age;

    private Double weight;

    private String riskLevel;

    private Double riskScore;

    private String riskType;

    private String mainSymptoms;

    private String riskTrend;

    private String recommendation;

    private Double currentTemperature;

    private Integer currentHeartRate;

    private Double healthScore;

    private LocalDateTime lastCheckTime;

    private LocalDateTime lastUpdateAt;

    private List<String> symptomTags;

    private List<String> riskBasis;

    private List<TrendPoint> temperatureTrend;

    private List<TrendPoint> heartRateTrend;

    private List<TrendPoint> activityTrend;

    private List<DetectionRecord> recentDetections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String label;
        private Double value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectionRecord {
        private LocalDateTime time;
        private String item;
        private String value;
        private String status;
        private String note;
    }
}

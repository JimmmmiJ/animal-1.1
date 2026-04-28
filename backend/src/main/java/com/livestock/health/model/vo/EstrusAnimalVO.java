package com.livestock.health.model.vo;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 牲畜发情详情 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstrusAnimalVO {

    private String animalId;

    private String name;

    private String breed;

    private Integer age;

    private Double weight;

    private String estrusStatus;

    private Double estrusProbability;

    private String estrusStage;

    private Integer activityIndex;

    private Integer dailyRuminationTime;

    private Integer dailyFeedingCount;

    private LocalDateTime lastEstrusAt;

    private LocalDateTime nextEstrusPredictedAt;

    private Double currentTemperature;

    private Integer currentHeartRate;

    private LocalDateTime lastUpdateAt;

    private String suggestion;

    private String breedingRecommendation;

    private Map<String, Object> breedingPlan;

    private List<DetectionRecord> recentDetections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectionRecord {
        private LocalDateTime time;
        private String item;
        private String value;
        private String status;
    }
}

package com.livestock.health.model.vo;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警事件 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEventVO {

    private String alertId;

    private String animalId;

    private String animalName;

    private String deviceId;

    private String alertType;

    private String alertTypeText;

    private String severity;

    private String severityText;

    private String title;

    private String message;

    private String triggerValue;

    private String thresholdValue;

    private Double currentTemperature;

    private Integer currentHeartRate;

    private Integer currentActivity;

    private String animalRiskLevel;

    private String deviceStatus;

    private String handlerSuggestion;

    private String status;

    private String statusText;

    private LocalDateTime createdAt;

    private LocalDateTime acknowledgedAt;

    private String acknowledgedBy;

    private LocalDateTime resolvedAt;

    private String resolvedBy;

    private String resolutionNote;

    private List<MetricCard> metricCards;

    private List<ProcessRecord> processRecords;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricCard {
        private String label;
        private String value;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessRecord {
        private String stage;
        private String operator;
        private LocalDateTime time;
        private String note;
    }
}

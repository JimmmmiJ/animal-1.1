package com.livestock.health.model.vo;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 告警事件 VO（列表用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEventListVO {

    private String alertId;

    private String animalId;

    private String animalName;

    private String alertType;

    private String severity;

    private String title;

    private String message;

    private String triggerValue;

    private String thresholdValue;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime acknowledgedAt;

    private String acknowledgedBy;

    private LocalDateTime resolvedAt;

    private String resolvedBy;

    private String resolutionNote;
}
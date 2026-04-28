package com.livestock.health.model.vo;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 行为事件 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorEventVO {

    private String eventId;

    private String animalId;

    private String animalName;

    private String eventType;

    private LocalDateTime eventTime;

    private Integer duration;

    private String description;

    private Integer ruminationTime;

    private Integer feedingCount;

    private Integer restingTime;

    private String severity;
}

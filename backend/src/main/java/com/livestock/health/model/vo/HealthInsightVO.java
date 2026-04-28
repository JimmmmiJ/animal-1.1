package com.livestock.health.model.vo;

import lombok.*;

import java.util.List;

/**
 * 健康洞察 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthInsightVO {

    private String title;

    private String type;

    private String severity;

    private String description;

    private List<String> affectedAnimals;

    private String recommendation;

    private Double confidence;
}

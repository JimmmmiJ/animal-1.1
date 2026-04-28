package com.livestock.health.model.vo;

import lombok.*;

/**
 * 疾病风险概览 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseRiskSummaryVO {

    private FarmBaselineVO baseline;

    private Long highRiskCount;

    private Long mediumRiskCount;

    private Long lowRiskCount;

    private Long healthyCount;

    private Double healthyPercent;

    private Long alertCount;

    private Double modelAccuracy;

    private Long totalAnimals;
}

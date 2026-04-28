package com.livestock.health.model.vo;

import lombok.*;

/**
 * 行为分析概览 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorSummaryVO {

    private FarmBaselineVO baseline;

    private Long avgRuminationTime;

    private String ruminationStandardRange;

    private Long avgFeedingCount;

    private String feedingStandardRange;

    private Integer abnormalAnimalCount;

    private Integer totalAnimalCount;

    private Double abnormalPercent;

    private Double normalPercent;

    private Double digestiveHealthScore;
}

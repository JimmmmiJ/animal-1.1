package com.livestock.health.model.vo;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 发情期检测概览 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstrusSummaryVO {

    private FarmBaselineVO baseline;

    private Long inEstrusCount;

    private Double inEstrusPercent;

    private Long approachingEstrusCount;

    private Double approachingEstrusPercent;

    private Long pregnantCount;

    private Double pregnantPercent;

    private Long normalCount;

    private Double normalPercent;

    private Long totalCount;
}

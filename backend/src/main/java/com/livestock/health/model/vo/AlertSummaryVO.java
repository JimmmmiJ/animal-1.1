package com.livestock.health.model.vo;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 告警概览统计 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSummaryVO {

    private FarmBaselineVO baseline;

    private Long pendingCount;

    private Long acknowledgedCount;

    private Long urgentCount;

    private Long resolvedCount;

    private Long totalCount;

    private Double pushSuccessRate;

    private Long todayNewCount;

    private Long todayResolvedCount;
}

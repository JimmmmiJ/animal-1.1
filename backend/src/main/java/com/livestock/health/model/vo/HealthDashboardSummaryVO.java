package com.livestock.health.model.vo;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * 健康看板概览 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthDashboardSummaryVO {

    private FarmBaselineVO baseline;

    private Integer totalAnimals;

    private Integer healthyCount;

    private Double healthyPercent;

    private Integer attentionCount;

    private Integer alertCount;

    private Long highRiskCount;

    private Long mediumRiskCount;

    private Long inEstrusCount;

    private Double avgHealthScore;

    private Double avgTemperature;

    private Integer avgActivity;

    private Long avgRuminationTime;

    private Double ruminationPassRate;

    private List<HealthTrendPoint> weeklyTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthTrendPoint {
        private String date;
        private Double score;
    }
}

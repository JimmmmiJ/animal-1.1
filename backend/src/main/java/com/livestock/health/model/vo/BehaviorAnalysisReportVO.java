package com.livestock.health.model.vo;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * 行为分析报告 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorAnalysisReportVO {

    private Map<String, Integer> behaviorPattern;

    private List<HealthTrendPoint> healthTrend;

    private String analysisSummary;

    private Long abnormalAnimalCount;

    private Double avgRuminationTime;

    private Double avgFeedingCount;

    private List<String> recommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthTrendPoint {
        private String date;
        private Double score;
    }
}

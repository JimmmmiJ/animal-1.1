package com.livestock.health.model.vo;

import lombok.*;

import java.util.List;

/**
 * 发情概率趋势 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstrusProbabilityTrendVO {

    private String animalId;

    private List<TrendPoint> trend;

    private Double currentProbability;

    private String prediction;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String date;
        private Double probability;
        private String status;
    }
}

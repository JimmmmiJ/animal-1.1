package com.livestock.health.model.vo;

import lombok.*;

import java.util.List;

/**
 * 体温趋势数据 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemperatureTrendVO {

    private String animalId;

    private List<TrendPoint> trend;

    private Double avgTemperature;

    private Double maxTemperature;

    private Double minTemperature;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String time;
        private Double value;
    }
}

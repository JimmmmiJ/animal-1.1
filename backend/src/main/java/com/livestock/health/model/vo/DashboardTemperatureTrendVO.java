package com.livestock.health.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTemperatureTrendVO {

    private Double warningThreshold;

    private Double dangerThreshold;

    private List<TrendPoint> points;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {

        private String timeBucket;

        private Double value;

        private Boolean hasData;
    }
}

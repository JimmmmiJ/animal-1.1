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
public class DashboardActivityHeatmapVO {

    private List<String> timeBuckets;

    private List<MetricMeta> metrics;

    private List<HeatmapCell> cells;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricMeta {

        private String key;

        private String label;

        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapCell {

        private String timeBucket;

        private String metricKey;

        private String metricLabel;

        private Integer intensity;

        private Double rawValue;

        private String unit;

        private Boolean hasData;
    }
}

package com.livestock.health.model.vo;

import lombok.*;

import java.util.List;

/**
 * 疾病类型分析 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseTypeAnalysisVO {

    private List<DiseaseType> diseaseTypes;

    private List<TrendPoint> weeklyTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiseaseType {
        private String name;
        private Integer count;
        private Double percent;
        private String severity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String date;
        private Integer highRisk;
        private Integer mediumRisk;
        private Integer lowRisk;
    }
}

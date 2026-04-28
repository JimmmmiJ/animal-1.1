package com.livestock.health.model.vo;

import lombok.*;

import java.util.List;

/**
 * 健康热力图 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthHeatmapVO {

    private List<HeatmapCell> cells;

    private Integer gridSize;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapCell {
        private Integer x;
        private Integer y;
        private String animalId;
        private Double healthScore;
        private String status;
        private String color;
    }
}

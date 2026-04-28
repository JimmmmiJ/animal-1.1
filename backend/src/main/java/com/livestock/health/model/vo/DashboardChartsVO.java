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
public class DashboardChartsVO {

    private FarmBaselineVO baseline;

    private List<DashboardStatusSegmentVO> healthStatusDistribution;

    private DashboardTemperatureTrendVO temperatureTrend;

    private DashboardActivityHeatmapVO activityHeatmap;
}

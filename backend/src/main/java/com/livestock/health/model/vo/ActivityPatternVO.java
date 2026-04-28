package com.livestock.health.model.vo;

import lombok.*;

import java.util.List;

/**
 * 活动模式分析 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityPatternVO {

    private List<PatternPoint> hourlyPattern;

    private List<PatternPoint> dailyPattern;

    private Integer peakActivityHour;

    private Integer avgActivityLevel;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatternPoint {
        private String label;
        private Integer value;
        private String category;
    }
}

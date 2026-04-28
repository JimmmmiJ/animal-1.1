package com.livestock.health.model.vo;

import lombok.*;

/**
 * 设备统计概览 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceSummaryVO {

    private FarmBaselineVO baseline;

    private Long totalDevices;

    private Long onlineDevices;

    private Long offlineDevices;

    private Long faultDevices;

    private Double avgBatteryLevel;

    private Long devicesWithLowBattery;

    private Long devicesWithWeakSignal;

    private Long abnormalDeviceCount;

    private Double avgHealthScore;

    private Long healthyAnimals;

    private String dataUpdateFrequency;

    private Double todayDataCompletionRate;
}

package com.livestock.health.model.vo;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 设备状态 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatusVO {

    private String deviceId;

    private String deviceModel;

    private String deviceSn;

    private Long farmId;

    private String status;

    private Integer batteryLevel;

    private Integer signalStrength;

    private Double currentTemperature;

    private Integer currentHeartRate;

    private Integer currentActivity;

    private LocalDateTime lastOnlineAt;

    private LocalDateTime lastDataUpdateAt;

    private String animalId;

    private String animalName;

    private String breed;

    private String protocol;

    private String firmwareVersion;

    private Integer uploadIntervalMinutes;

    private Double temperatureMinThreshold;

    private Double temperatureMaxThreshold;

    private Integer heartRateMinThreshold;

    private Integer heartRateMaxThreshold;

    private Integer lowBatteryThreshold;

    private String installationLocation;

    private String note;
}

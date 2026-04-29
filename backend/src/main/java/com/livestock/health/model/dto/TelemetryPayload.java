package com.livestock.health.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.Map;

@Data
public class TelemetryPayload {

    @JsonAlias({"device_id"})
    private String deviceId;

    @JsonAlias({"device_sn", "sn"})
    private String deviceSn;

    @JsonAlias({"animal_id"})
    private String animalId;

    @JsonAlias({"farm_id"})
    private Long farmId;

    @JsonAlias({"time", "dataTime", "data_time", "ts"})
    private String timestamp;

    private Double temperature;

    @JsonAlias({"heart_rate"})
    private Integer heartRate;

    @JsonAlias({"activity_level", "activity"})
    private Integer activityLevel;

    @JsonAlias({"rumination_time"})
    private Integer ruminationTime;

    @JsonAlias({"feeding_count"})
    private Integer feedingCount;

    @JsonAlias({"resting_time"})
    private Integer restingTime;

    @JsonAlias({"battery_level"})
    private Integer batteryLevel;

    @JsonAlias({"signal_strength", "rssi"})
    private Integer signalStrength;

    private Map<String, Object> extra;
}

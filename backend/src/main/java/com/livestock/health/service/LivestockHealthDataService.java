package com.livestock.health.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livestock.health.model.entity.AnimalEntity;
import com.livestock.health.model.entity.DeviceEntity;
import com.livestock.health.model.entity.HealthDataEntity;
import com.livestock.health.model.vo.DeviceStatusVO;
import com.livestock.health.model.vo.DeviceSummaryVO;
import com.livestock.health.model.vo.FarmBaselineVO;
import com.livestock.health.model.vo.TemperatureTrendVO;
import com.livestock.health.repository.AnimalRepository;
import com.livestock.health.repository.DeviceRepository;
import com.livestock.health.repository.HealthDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivestockHealthDataService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final double DEFAULT_TEMP_MIN = 38.0;
    private static final double DEFAULT_TEMP_MAX = 41.0;
    private static final int DEFAULT_HEART_RATE_MIN = 60;
    private static final int DEFAULT_HEART_RATE_MAX = 120;
    private static final int DEFAULT_LOW_BATTERY_THRESHOLD = 20;
    private static final int DEFAULT_UPLOAD_INTERVAL_MINUTES = 60;

    private final DeviceRepository deviceRepository;
    private final AnimalRepository animalRepository;
    private final HealthDataRepository healthDataRepository;
    private final FarmSnapshotService farmSnapshotService;
    private final UserPermissionService userPermissionService;
    private final ObjectMapper objectMapper;

    public DeviceSummaryVO getDeviceSummary(Long farmId) {
        FarmBaselineVO baseline = farmSnapshotService.getBaseline(farmId);
        List<DeviceEntity> devices = deviceRepository.findByFarmId(farmId);
        List<AnimalEntity> animals = animalRepository.findByFarmId(farmId);

        Double avgBatteryLevel = deviceRepository.avgBatteryLevelByFarmId(farmId);
        long lowBatteryCount = devices.stream().filter(farmSnapshotService::isLowBattery).count();
        long weakSignalCount = devices.stream().filter(farmSnapshotService::isWeakSignal).count();
        long abnormalDeviceCount = devices.stream()
            .filter(device -> "offline".equals(device.getStatus())
                || "fault".equals(device.getStatus())
                || farmSnapshotService.isLowBattery(device)
                || farmSnapshotService.isWeakSignal(device))
            .count();
        long healthyAnimals = animals.stream().filter(farmSnapshotService::isHealthyAnimal).count();

        return DeviceSummaryVO.builder()
            .baseline(baseline)
            .totalDevices(baseline.getTotalDevices())
            .onlineDevices(baseline.getOnlineDevices())
            .offlineDevices(baseline.getOfflineDevices())
            .faultDevices(baseline.getFaultDevices())
            .avgBatteryLevel(avgBatteryLevel == null ? 0.0 : round1(avgBatteryLevel))
            .devicesWithLowBattery(lowBatteryCount)
            .devicesWithWeakSignal(weakSignalCount)
            .abnormalDeviceCount(abnormalDeviceCount)
            .avgHealthScore(round1(farmSnapshotService.averageHealthScore(animals)))
            .healthyAnimals(healthyAnimals)
            .dataUpdateFrequency(getUploadIntervalMinutes() + " 分钟")
            .todayDataCompletionRate(calculateTodayDataCompletionRate(farmId, baseline.getTotalDevices()))
            .build();
    }

    public List<DeviceStatusVO> getDeviceStatusList(Long farmId, String statusFilter) {
        List<DeviceEntity> devices = statusFilter == null || statusFilter.isBlank()
            ? deviceRepository.findByFarmId(farmId)
            : deviceRepository.findByFarmIdAndStatus(farmId, statusFilter);

        Map<Long, AnimalEntity> animalMap = animalRepository.findByFarmId(farmId).stream()
            .collect(Collectors.toMap(AnimalEntity::getId, animal -> animal));

        return devices.stream()
            .map(device -> convertToDeviceStatusVO(device, animalMap.get(device.getAnimalId()), getDeviceConfig(device)))
            .collect(Collectors.toList());
    }

    public DeviceStatusVO getDeviceDetail(String deviceId) {
        DeviceEntity device = resolveDevice(deviceId);
        AnimalEntity animal = device.getAnimalId() == null ? null : animalRepository.findById(device.getAnimalId()).orElse(null);
        return convertToDeviceStatusVO(device, animal, getDeviceConfig(device));
    }

    public TemperatureTrendVO getTemperatureTrend(String animalId, String timeRange) {
        AnimalEntity animal = resolveAnimal(animalId);
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = calculateStartTime(timeRange);

        List<HealthDataEntity> records = healthDataRepository
            .findByAnimalIdAndDataTimeBetweenOrderByDataTimeDesc(animal.getId(), startTime, endTime);

        if (records.isEmpty()) {
            return TemperatureTrendVO.builder()
                .animalId(animal.getAnimalId())
                .trend(new ArrayList<>())
                .avgTemperature(0.0)
                .maxTemperature(0.0)
                .minTemperature(0.0)
                .build();
        }

        Map<String, List<Double>> grouped = new LinkedHashMap<>();
        records.stream()
            .sorted((a, b) -> a.getDataTime().compareTo(b.getDataTime()))
            .forEach(record -> {
                if (record.getTemperature() != null) {
                    grouped.computeIfAbsent(record.getDataTime().format(TIME_FORMATTER), key -> new ArrayList<>())
                        .add(record.getTemperature());
                }
            });

        List<TemperatureTrendVO.TrendPoint> trend = grouped.entrySet().stream()
            .map(entry -> TemperatureTrendVO.TrendPoint.builder()
                .time(entry.getKey())
                .value(round1(entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0)))
                .build())
            .collect(Collectors.toList());

        List<Double> temperatures = records.stream()
            .map(HealthDataEntity::getTemperature)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return TemperatureTrendVO.builder()
            .animalId(animal.getAnimalId())
            .trend(trend)
            .avgTemperature(round1(temperatures.stream().mapToDouble(Double::doubleValue).average().orElse(0.0)))
            .maxTemperature(round1(temperatures.stream().mapToDouble(Double::doubleValue).max().orElse(0.0)))
            .minTemperature(round1(temperatures.stream().mapToDouble(Double::doubleValue).min().orElse(0.0)))
            .build();
    }

    public Map<String, Object> getActivityStatistics(String animalId, String timeRange) {
        AnimalEntity animal = resolveAnimal(animalId);
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = calculateStartTime(timeRange);

        List<HealthDataEntity> records = healthDataRepository
            .findByAnimalIdAndDataTimeBetweenOrderByDataTimeDesc(animal.getId(), startTime, endTime);

        Map<String, Object> result = new LinkedHashMap<>();
        if (records.isEmpty()) {
            result.put("avgActivity", 0);
            result.put("maxActivity", 0);
            result.put("distribution", createEmptyActivityDistribution());
            result.put("hourlyTrend", new ArrayList<>());
            return result;
        }

        int avgActivity = (int) Math.round(records.stream()
            .map(HealthDataEntity::getActivityLevel)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0));

        int maxActivity = records.stream()
            .map(HealthDataEntity::getActivityLevel)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .max()
            .orElse(0);

        Map<String, Integer> distribution = createEmptyActivityDistribution();
        Map<String, List<Integer>> hourlyTrend = new LinkedHashMap<>();

        records.stream()
            .sorted((a, b) -> a.getDataTime().compareTo(b.getDataTime()))
            .forEach(record -> {
                Integer value = record.getActivityLevel();
                if (value == null) {
                    return;
                }
                hourlyTrend.computeIfAbsent(record.getDataTime().format(TIME_FORMATTER), key -> new ArrayList<>()).add(value);
                if (value < 20) {
                    distribution.merge("静息", 1, Integer::sum);
                } else if (value < 50) {
                    distribution.merge("低活动", 1, Integer::sum);
                } else if (value < 80) {
                    distribution.merge("中活动", 1, Integer::sum);
                } else {
                    distribution.merge("高活动", 1, Integer::sum);
                }
            });

        List<Map<String, Object>> trend = hourlyTrend.entrySet().stream()
            .map(entry -> {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("time", entry.getKey());
                point.put("value", Math.round(entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0)));
                return point;
            })
            .collect(Collectors.toList());

        result.put("animalId", animal.getAnimalId());
        result.put("avgActivity", avgActivity);
        result.put("maxActivity", maxActivity);
        result.put("distribution", distribution);
        result.put("hourlyTrend", trend);
        return result;
    }

    public void updateDeviceConfig(String deviceId, Map<String, Object> config) {
        DeviceEntity device = resolveDevice(deviceId);
        Map<String, Object> merged = new LinkedHashMap<>(getDeviceConfig(device));
        merged.putAll(config);
        try {
            userPermissionService.upsertSystemConfig(
                getDeviceConfigKey(device.getDeviceId()),
                objectMapper.writeValueAsString(merged),
                "json",
                "Device level configuration for " + device.getDeviceId(),
                device.getFarmId()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("保存设备配置失败: " + deviceId, e);
        }
        log.info("Updated device config: deviceId={}, config={}", deviceId, merged);
    }

    public List<Map<String, Object>> getDataCollectionLogs(String deviceId, int limit) {
        DeviceEntity device = resolveDevice(deviceId);
        List<HealthDataEntity> latestData = healthDataRepository.findByAnimalIdOrderByDataTimeDesc(device.getAnimalId()).stream()
            .limit(limit)
            .collect(Collectors.toList());

        return latestData.stream()
            .map(record -> {
                Map<String, Object> log = new LinkedHashMap<>();
                log.put("time", record.getDataTime().format(LOG_TIME_FORMATTER));
                log.put("temperature", record.getTemperature());
                log.put("heartRate", record.getHeartRate());
                log.put("activityLevel", record.getActivityLevel());
                log.put("ruminationTime", record.getRuminationTime());
                log.put("feedingCount", record.getFeedingCount());
                log.put("status", determineLogStatus(record));
                return log;
            })
            .collect(Collectors.toList());
    }

    private DeviceStatusVO convertToDeviceStatusVO(DeviceEntity device, AnimalEntity animal, Map<String, Object> config) {
        return DeviceStatusVO.builder()
            .deviceId(device.getDeviceId())
            .deviceModel(device.getDeviceModel())
            .deviceSn(device.getDeviceSn())
            .farmId(device.getFarmId())
            .status(device.getStatus())
            .batteryLevel(device.getBatteryLevel())
            .signalStrength(device.getSignalStrength())
            .currentTemperature(device.getCurrentTemperature())
            .currentHeartRate(device.getCurrentHeartRate())
            .currentActivity(device.getCurrentActivity())
            .lastOnlineAt(device.getLastOnlineAt())
            .lastDataUpdateAt(device.getLastDataUpdateAt())
            .animalId(animal != null ? animal.getAnimalId() : null)
            .animalName(animal != null ? animal.getName() : null)
            .breed(animal != null ? animal.getBreed() : null)
            .protocol(asString(config.get("protocol"), "MQTT"))
            .firmwareVersion(asString(config.get("firmwareVersion"), buildFirmwareVersion(device)))
            .uploadIntervalMinutes(asInteger(config.get("uploadIntervalMinutes"), DEFAULT_UPLOAD_INTERVAL_MINUTES))
            .temperatureMinThreshold(asDouble(config.get("temperatureMinThreshold"), DEFAULT_TEMP_MIN))
            .temperatureMaxThreshold(asDouble(config.get("temperatureMaxThreshold"), DEFAULT_TEMP_MAX))
            .heartRateMinThreshold(asInteger(config.get("heartRateMinThreshold"), DEFAULT_HEART_RATE_MIN))
            .heartRateMaxThreshold(asInteger(config.get("heartRateMaxThreshold"), DEFAULT_HEART_RATE_MAX))
            .lowBatteryThreshold(asInteger(config.get("lowBatteryThreshold"), DEFAULT_LOW_BATTERY_THRESHOLD))
            .installationLocation(asString(config.get("installationLocation"), "示范羊舍 A 区"))
            .note(asString(config.get("note"), "设备运行正常，按手册样式展示设备详情与配置。"))
            .build();
    }

    private Map<String, Integer> createEmptyActivityDistribution() {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("静息", 0);
        distribution.put("低活动", 0);
        distribution.put("中活动", 0);
        distribution.put("高活动", 0);
        return distribution;
    }

    private String determineLogStatus(HealthDataEntity record) {
        if (farmSnapshotService.isTemperatureAlert(record) || farmSnapshotService.isHeartRateAlert(record)) {
            return "预警";
        }
        Integer activityLevel = record.getActivityLevel();
        return activityLevel != null && activityLevel < 15 ? "待关注" : "正常";
    }

    private AnimalEntity resolveAnimal(String animalId) {
        return animalRepository.findByAnimalId(animalId)
            .orElseThrow(() -> new RuntimeException("牲畜不存在: " + animalId));
    }

    private DeviceEntity resolveDevice(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId)
            .orElseThrow(() -> new RuntimeException("设备不存在: " + deviceId));
    }

    private Map<String, Object> getDeviceConfig(DeviceEntity device) {
        Map<String, Object> defaults = createDefaultDeviceConfig(device);
        String storedJson = userPermissionService.getSystemConfigValue(getDeviceConfigKey(device.getDeviceId()));
        if (storedJson == null || storedJson.isBlank()) {
            return defaults;
        }

        try {
            Map<String, Object> stored = objectMapper.readValue(storedJson, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> merged = new LinkedHashMap<>(defaults);
            stored.forEach((key, value) -> merged.put(String.valueOf(key), value));
            return merged;
        } catch (JsonProcessingException e) {
            log.warn("Invalid stored device config: deviceId={}, value={}", device.getDeviceId(), storedJson, e);
            return defaults;
        }
    }

    private Map<String, Object> createDefaultDeviceConfig(DeviceEntity device) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("protocol", getConfigString("device_protocol", "MQTT"));
        defaults.put("firmwareVersion", buildFirmwareVersion(device));
        defaults.put("uploadIntervalMinutes", getUploadIntervalMinutes());
        defaults.put("temperatureMinThreshold", getConfigDouble("temperature_min", DEFAULT_TEMP_MIN));
        defaults.put("temperatureMaxThreshold", getConfigDouble("temperature_max", DEFAULT_TEMP_MAX));
        defaults.put("heartRateMinThreshold", getConfigInteger("heart_rate_min", DEFAULT_HEART_RATE_MIN));
        defaults.put("heartRateMaxThreshold", getConfigInteger("heart_rate_max", DEFAULT_HEART_RATE_MAX));
        defaults.put("lowBatteryThreshold", getConfigInteger("device_low_battery", DEFAULT_LOW_BATTERY_THRESHOLD));
        defaults.put("installationLocation", "示范羊舍 A 区");
        defaults.put("note", "默认使用实时采集配置，支持设备级参数覆盖。");
        return defaults;
    }

    private String buildFirmwareVersion(DeviceEntity device) {
        String model = device.getDeviceModel() == null ? "COLLAR" : device.getDeviceModel().replaceAll("[^A-Za-z0-9]", "");
        return model + "-1.0.0";
    }

    private String getDeviceConfigKey(String deviceId) {
        return "device_config:" + deviceId;
    }

    private int getUploadIntervalMinutes() {
        return getConfigInteger("data_upload_interval", DEFAULT_UPLOAD_INTERVAL_MINUTES);
    }

    private String getConfigString(String key, String defaultValue) {
        String value = userPermissionService.getSystemConfigValue(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private Integer getConfigInteger(String key, Integer defaultValue) {
        return asInteger(userPermissionService.getSystemConfigValue(key), defaultValue);
    }

    private Double getConfigDouble(String key, Double defaultValue) {
        return asDouble(userPermissionService.getSystemConfigValue(key), defaultValue);
    }

    private LocalDateTime calculateStartTime(String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        return switch (timeRange) {
            case "1h" -> now.minusHours(1);
            case "6h" -> now.minusHours(6);
            case "12h" -> now.minusHours(12);
            case "7d" -> now.minusDays(7);
            default -> now.minusDays(1);
        };
    }

    private Double calculateTodayDataCompletionRate(Long farmId, Long totalDevices) {
        if (totalDevices == null || totalDevices == 0) {
            return 0.0;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        long expectedHours = ChronoUnit.HOURS.between(startOfDay, now) + 1;
        long expectedRecords = Math.max(1L, totalDevices * expectedHours);
        long actualRecords = healthDataRepository
            .findByFarmIdAndDataTimeBetweenOrderByDataTimeDesc(farmId, startOfDay, now)
            .size();

        return round1(Math.min(100.0, actualRecords * 100.0 / expectedRecords));
    }

    private String asString(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private Integer asInteger(Object value, Integer defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Double asDouble(Object value, Double defaultValue) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}

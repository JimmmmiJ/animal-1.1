package com.livestock.health.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livestock.health.model.entity.AnimalEntity;
import com.livestock.health.model.entity.DeviceEntity;
import com.livestock.health.model.entity.HealthDataEntity;
import com.livestock.health.model.vo.FarmBaselineVO;
import com.livestock.health.repository.AnimalRepository;
import com.livestock.health.repository.DeviceRepository;
import com.livestock.health.repository.HealthDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FarmSnapshotService {

    public static final double NORMAL_TEMPERATURE_MIN = 38.0;
    public static final double NORMAL_TEMPERATURE_MAX = 40.5;
    public static final int NORMAL_HEART_RATE_MIN = 60;
    public static final int NORMAL_HEART_RATE_MAX = 110;
    public static final int LOW_BATTERY_THRESHOLD = 20;
    public static final int SIGNAL_MIN_THRESHOLD = 40;

    private final AnimalRepository animalRepository;
    private final DeviceRepository deviceRepository;
    private final HealthDataRepository healthDataRepository;
    private final UserPermissionService userPermissionService;
    private final ObjectMapper objectMapper;

    public FarmBaselineVO getBaseline(Long farmId) {
        List<AnimalEntity> animals = animalRepository.findByFarmId(farmId);
        List<DeviceEntity> devices = deviceRepository.findByFarmId(farmId);

        long femaleAnimalCount = animals.stream()
            .filter(animal -> Integer.valueOf(2).equals(animal.getGender()))
            .count();
        long riskAnimalCount = animals.stream()
            .filter(this::isRiskAnimal)
            .count();
        long abnormalBehaviorAnimalCount = animals.stream()
            .filter(this::isBehaviorAbnormal)
            .count();

        long onlineDevices = devices.stream()
            .filter(device -> "online".equals(device.getStatus()))
            .count();
        long offlineDevices = devices.stream()
            .filter(device -> "offline".equals(device.getStatus()))
            .count();
        long faultDevices = devices.stream()
            .filter(device -> "fault".equals(device.getStatus()))
            .count();

        return FarmBaselineVO.builder()
            .farmId(farmId)
            .snapshotAt(LocalDateTime.now())
            .totalAnimals((long) animals.size())
            .totalDevices((long) devices.size())
            .femaleAnimalCount(femaleAnimalCount)
            .onlineDevices(onlineDevices)
            .offlineDevices(offlineDevices)
            .faultDevices(faultDevices)
            .abnormalBehaviorAnimalCount(abnormalBehaviorAnimalCount)
            .riskAnimalCount(riskAnimalCount)
            .build();
    }

    public List<AnimalEntity> getAnimals(Long farmId) {
        return animalRepository.findByFarmId(farmId);
    }

    public List<DeviceEntity> getDevices(Long farmId) {
        return deviceRepository.findByFarmId(farmId);
    }

    public Map<Long, HealthDataEntity> getLatestHealthDataByAnimalId(Long farmId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusDays(7);
        List<HealthDataEntity> records = healthDataRepository
            .findByFarmIdAndDataTimeBetweenOrderByDataTimeDesc(farmId, startTime, now);

        Map<Long, HealthDataEntity> latestMap = new LinkedHashMap<>();
        for (HealthDataEntity record : records) {
            latestMap.putIfAbsent(record.getAnimalId(), record);
        }
        return latestMap;
    }

    public boolean isRiskAnimal(AnimalEntity animal) {
        return animal != null
            && animal.getRiskLevel() != null
            && !"normal".equals(animal.getRiskLevel());
    }

    public boolean isBehaviorAbnormal(AnimalEntity animal) {
        return animal != null
            && ("warning".equals(animal.getBehaviorStatus()) || "abnormal".equals(animal.getBehaviorStatus()));
    }

    public boolean isHealthyAnimal(AnimalEntity animal) {
        return animal != null
            && "normal".equals(animal.getRiskLevel())
            && "normal".equals(animal.getBehaviorStatus());
    }

    public boolean isLowBattery(DeviceEntity device) {
        return device != null
            && device.getBatteryLevel() != null
            && device.getBatteryLevel() < getLowBatteryThreshold(device);
    }

    public boolean isWeakSignal(DeviceEntity device) {
        return device != null
            && device.getSignalStrength() != null
            && calculateSignalQuality(device) < getSignalMinThreshold();
    }

    public boolean isTemperatureAlert(HealthDataEntity record) {
        return isTemperatureAlert(record, null);
    }

    public boolean isTemperatureAlert(HealthDataEntity record, DeviceEntity device) {
        return record != null
            && record.getTemperature() != null
            && (record.getTemperature() < getTemperatureMinThreshold(device)
                || record.getTemperature() > getTemperatureMaxThreshold(device));
    }

    public boolean isHeartRateAlert(HealthDataEntity record) {
        return isHeartRateAlert(record, null);
    }

    public boolean isHeartRateAlert(HealthDataEntity record, DeviceEntity device) {
        return record != null
            && record.getHeartRate() != null
            && (record.getHeartRate() < getHeartRateMinThreshold(device)
                || record.getHeartRate() > getHeartRateMaxThreshold(device));
    }

    public double averageHealthScore(List<AnimalEntity> animals) {
        return animals.stream()
            .map(AnimalEntity::getHealthScore)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }

    public double getTemperatureMinThreshold() {
        return getDoubleConfig("temperature_min", NORMAL_TEMPERATURE_MIN);
    }

    public double getTemperatureMinThreshold(DeviceEntity device) {
        return getDeviceDoubleOverride(device, "temperatureMinThreshold", getTemperatureMinThreshold());
    }

    public double getTemperatureMaxThreshold() {
        return getDoubleConfig("temperature_max", NORMAL_TEMPERATURE_MAX);
    }

    public double getTemperatureMaxThreshold(DeviceEntity device) {
        return getDeviceDoubleOverride(device, "temperatureMaxThreshold", getTemperatureMaxThreshold());
    }

    public int getHeartRateMinThreshold() {
        return getIntegerConfig("heart_rate_min", NORMAL_HEART_RATE_MIN);
    }

    public int getHeartRateMinThreshold(DeviceEntity device) {
        return getDeviceIntegerOverride(device, "heartRateMinThreshold", getHeartRateMinThreshold());
    }

    public int getHeartRateMaxThreshold() {
        return getIntegerConfig("heart_rate_max", NORMAL_HEART_RATE_MAX);
    }

    public int getHeartRateMaxThreshold(DeviceEntity device) {
        return getDeviceIntegerOverride(device, "heartRateMaxThreshold", getHeartRateMaxThreshold());
    }

    public int getLowBatteryThreshold() {
        return getIntegerConfig("device_low_battery", LOW_BATTERY_THRESHOLD);
    }

    public int getLowBatteryThreshold(DeviceEntity device) {
        return getDeviceIntegerOverride(device, "lowBatteryThreshold", getLowBatteryThreshold());
    }

    public int getSignalMinThreshold() {
        return getIntegerConfig("device_signal_min", SIGNAL_MIN_THRESHOLD);
    }

    public int calculateSignalQuality(DeviceEntity device) {
        if (device == null || device.getSignalStrength() == null) {
            return 0;
        }
        int raw = device.getSignalStrength();
        if (raw >= 0 && raw <= 100) {
            return raw;
        }
        int quality = (raw + 100) * 2;
        return Math.max(0, Math.min(100, quality));
    }

    public String temperatureThresholdRange(DeviceEntity device) {
        return String.format("%.1f-%.1f", getTemperatureMinThreshold(device), getTemperatureMaxThreshold(device));
    }

    public String heartRateThresholdRange(DeviceEntity device) {
        return getHeartRateMinThreshold(device) + "-" + getHeartRateMaxThreshold(device);
    }

    private Map<String, Object> getDeviceConfig(DeviceEntity device) {
        if (device == null || device.getDeviceId() == null) {
            return Map.of();
        }

        String storedJson = userPermissionService.getSystemConfigValue("device_config:" + device.getDeviceId());
        if (storedJson == null || storedJson.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(storedJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private double getDeviceDoubleOverride(DeviceEntity device, String key, double defaultValue) {
        Object value = getDeviceConfig(device).get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int getDeviceIntegerOverride(DeviceEntity device, String key, int defaultValue) {
        Object value = getDeviceConfig(device).get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double getDoubleConfig(String key, double defaultValue) {
        String value = userPermissionService.getSystemConfigValue(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int getIntegerConfig(String key, int defaultValue) {
        String value = userPermissionService.getSystemConfigValue(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

package com.livestock.health.service;

import com.livestock.health.model.dto.TelemetryPayload;
import com.livestock.health.model.entity.AnimalEntity;
import com.livestock.health.model.entity.DeviceEntity;
import com.livestock.health.model.entity.HealthDataEntity;
import com.livestock.health.repository.AnimalRepository;
import com.livestock.health.repository.DeviceRepository;
import com.livestock.health.repository.HealthDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryIngestionService {

    private final DeviceRepository deviceRepository;
    private final AnimalRepository animalRepository;
    private final HealthDataRepository healthDataRepository;

    @Transactional
    public Map<String, Object> ingest(TelemetryPayload payload, String source) {
        if (payload == null) {
            throw new IllegalArgumentException("遥测数据不能为空");
        }

        DeviceEntity device = resolveDevice(payload);
        AnimalEntity animal = resolveAnimal(payload, device);
        LocalDateTime dataTime = parseTelemetryTime(payload.getTimestamp());
        Long farmId = resolveFarmId(payload, device, animal);

        updateDeviceSnapshot(device, animal, payload, dataTime, farmId);

        HealthDataEntity record = healthDataRepository.save(HealthDataEntity.builder()
            .deviceId(device.getId())
            .animalId(animal.getId())
            .farmId(farmId)
            .temperature(payload.getTemperature())
            .heartRate(payload.getHeartRate())
            .activityLevel(clampPercent(payload.getActivityLevel()))
            .ruminationTime(nonNegative(payload.getRuminationTime()))
            .feedingCount(nonNegative(payload.getFeedingCount()))
            .restingTime(nonNegative(payload.getRestingTime()))
            .dataTime(dataTime)
            .build());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("source", source == null ? "UNKNOWN" : source);
        result.put("recordId", record.getId());
        result.put("deviceId", device.getDeviceId());
        result.put("deviceSn", device.getDeviceSn());
        result.put("animalId", animal.getAnimalId());
        result.put("farmId", farmId);
        result.put("dataTime", dataTime);
        return result;
    }

    private DeviceEntity resolveDevice(TelemetryPayload payload) {
        if (hasText(payload.getDeviceId())) {
            return deviceRepository.findByDeviceId(payload.getDeviceId().trim())
                .orElseThrow(() -> new IllegalArgumentException("设备不存在: " + payload.getDeviceId()));
        }
        if (hasText(payload.getDeviceSn())) {
            return deviceRepository.findByDeviceSn(payload.getDeviceSn().trim())
                .orElseThrow(() -> new IllegalArgumentException("设备序列号不存在: " + payload.getDeviceSn()));
        }
        throw new IllegalArgumentException("缺少 deviceId 或 deviceSn");
    }

    private AnimalEntity resolveAnimal(TelemetryPayload payload, DeviceEntity device) {
        if (hasText(payload.getAnimalId())) {
            return animalRepository.findByAnimalId(payload.getAnimalId().trim())
                .orElseThrow(() -> new IllegalArgumentException("牲畜不存在: " + payload.getAnimalId()));
        }
        if (device.getAnimalId() != null) {
            return animalRepository.findById(device.getAnimalId())
                .orElseThrow(() -> new IllegalArgumentException("设备未绑定有效牲畜: " + device.getDeviceId()));
        }
        throw new IllegalArgumentException("设备未绑定牲畜，且上报数据缺少 animalId");
    }

    private Long resolveFarmId(TelemetryPayload payload, DeviceEntity device, AnimalEntity animal) {
        if (payload.getFarmId() != null) {
            return payload.getFarmId();
        }
        if (device.getFarmId() != null) {
            return device.getFarmId();
        }
        return animal.getFarmId();
    }

    private void updateDeviceSnapshot(DeviceEntity device, AnimalEntity animal, TelemetryPayload payload, LocalDateTime dataTime, Long farmId) {
        device.setFarmId(farmId);
        device.setAnimalId(animal.getId());
        device.setStatus("online");
        device.setLastOnlineAt(dataTime);
        device.setLastDataUpdateAt(dataTime);

        if (payload.getTemperature() != null) {
            device.setCurrentTemperature(payload.getTemperature());
        }
        if (payload.getHeartRate() != null) {
            device.setCurrentHeartRate(payload.getHeartRate());
        }
        if (payload.getActivityLevel() != null) {
            device.setCurrentActivity(clampPercent(payload.getActivityLevel()));
        }
        if (payload.getBatteryLevel() != null) {
            device.setBatteryLevel(clampPercent(payload.getBatteryLevel()));
        }
        if (payload.getSignalStrength() != null) {
            device.setSignalStrength(payload.getSignalStrength());
        }
        deviceRepository.save(device);

        if (payload.getActivityLevel() != null
            || payload.getRuminationTime() != null
            || payload.getFeedingCount() != null
            || payload.getRestingTime() != null) {
            animal.setLastBehaviorUpdateAt(dataTime);
            animalRepository.save(animal);
        }
    }

    private Integer clampPercent(Integer value) {
        if (value == null) {
            return null;
        }
        return Math.max(0, Math.min(100, value));
    }

    private Integer nonNegative(Integer value) {
        if (value == null) {
            return null;
        }
        return Math.max(0, value);
    }

    private LocalDateTime parseTelemetryTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LocalDateTime.now();
        }

        String text = value.trim();
        if (text.matches("\\d+")) {
            long epoch = Long.parseLong(text);
            Instant instant = text.length() <= 10 ? Instant.ofEpochSecond(epoch) : Instant.ofEpochMilli(epoch);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        }

        try {
            return LocalDateTime.parse(text);
        } catch (RuntimeException ignored) {
            return OffsetDateTime.parse(text).toLocalDateTime();
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

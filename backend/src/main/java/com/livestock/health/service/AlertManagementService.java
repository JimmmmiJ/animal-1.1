package com.livestock.health.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livestock.health.model.entity.AnimalEntity;
import com.livestock.health.model.entity.DeviceEntity;
import com.livestock.health.model.entity.HealthDataEntity;
import com.livestock.health.model.vo.AlertEventVO;
import com.livestock.health.model.vo.AlertSummaryVO;
import com.livestock.health.model.vo.FarmBaselineVO;
import com.livestock.health.model.vo.PushChannelVO;
import com.livestock.health.model.vo.PushHistoryVO;
import com.livestock.health.repository.AnimalRepository;
import com.livestock.health.repository.DeviceRepository;
import com.livestock.health.repository.HealthDataRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertManagementService {

    private static final String CUSTOM_RULES_CONFIG_PREFIX = "alert_rules:";
    private static final String PUSH_CHANNEL_CONFIG_PREFIX = "push_channel:";

    private final AnimalRepository animalRepository;
    private final DeviceRepository deviceRepository;
    private final HealthDataRepository healthDataRepository;
    private final FarmSnapshotService farmSnapshotService;
    private final UserPermissionService userPermissionService;
    private final ObjectMapper objectMapper;

    private final Map<String, AlertState> alertStateMap = new ConcurrentHashMap<>();

    public AlertSummaryVO getAlertSummary(Long farmId) {
        FarmBaselineVO baseline = farmSnapshotService.getBaseline(farmId);
        List<AlertEventVO> events = buildDerivedAlerts(farmId);

        long pendingCount = events.stream().filter(event -> "pending".equals(event.getStatus())).count();
        long acknowledgedCount = events.stream().filter(event -> "acknowledged".equals(event.getStatus())).count();
        long resolvedCount = events.stream().filter(event -> "resolved".equals(event.getStatus())).count();
        long urgentCount = events.stream().filter(event -> "high".equals(event.getSeverity())).count();
        long todayNewCount = events.stream()
            .filter(event -> event.getCreatedAt() != null && event.getCreatedAt().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
            .count();
        long todayResolvedCount = events.stream()
            .filter(event -> event.getResolvedAt() != null && event.getResolvedAt().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
            .count();

        return AlertSummaryVO.builder()
            .baseline(baseline)
            .pendingCount(pendingCount)
            .acknowledgedCount(acknowledgedCount)
            .urgentCount(urgentCount)
            .resolvedCount(resolvedCount)
            .totalCount((long) events.size())
            .pushSuccessRate(events.isEmpty() ? 100.0 : 98.0)
            .todayNewCount(todayNewCount)
            .todayResolvedCount(todayResolvedCount)
            .build();
    }

    public List<AlertEventVO> getAlertEvents(Long farmId, String status, String severity, int limit) {
        return buildDerivedAlerts(farmId).stream()
            .filter(event -> status == null || status.isBlank() || status.equals(event.getStatus()))
            .filter(event -> severity == null || severity.isBlank() || severity.equals(event.getSeverity()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public AlertEventVO getAlertDetail(Long farmId, String alertId) {
        return buildDerivedAlerts(farmId).stream()
            .filter(event -> alertId.equals(event.getAlertId()))
            .findFirst()
            .orElseGet(() -> buildDerivedAlerts(null).stream()
                .filter(event -> alertId.equals(event.getAlertId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("告警不存在: " + alertId)));
    }

    public void acknowledgeAlert(String alertId, String acknowledgedBy) {
        AlertState state = alertStateMap.computeIfAbsent(alertId, key -> new AlertState());
        state.setStatus("acknowledged");
        state.setAcknowledgedBy(acknowledgedBy);
        state.setAcknowledgedAt(LocalDateTime.now());
        log.info("Acknowledged alert: alertId={}, acknowledgedBy={}", alertId, acknowledgedBy);
    }

    public void resolveAlert(String alertId, String resolvedBy, String resolutionNote) {
        AlertState state = alertStateMap.computeIfAbsent(alertId, key -> new AlertState());
        if (state.getAcknowledgedAt() == null) {
            state.setAcknowledgedAt(LocalDateTime.now());
            state.setAcknowledgedBy(resolvedBy);
        }
        state.setStatus("resolved");
        state.setResolvedBy(resolvedBy);
        state.setResolvedAt(LocalDateTime.now());
        state.setResolutionNote(resolutionNote == null || resolutionNote.isBlank() ? "已处理完成" : resolutionNote);
        log.info("Resolved alert: alertId={}, resolvedBy={}", alertId, resolvedBy);
    }

    public void batchOperation(List<String> alertIds, String action, String operator) {
        for (String alertId : alertIds) {
            if ("acknowledge".equals(action)) {
                acknowledgeAlert(alertId, operator);
            } else if ("resolve".equals(action)) {
                resolveAlert(alertId, operator, "批量处理");
            }
        }
    }

    public List<PushChannelVO> getPushChannels(Long farmId) {
        List<AlertEventVO> events = buildDerivedAlerts(farmId);
        int successCount = Math.max(1, events.size() - 1);

        return List.of(
            buildPushChannel("wechat", "微信推送", true, successCount, 1, List.of("管理员", "值班兽医")),
            buildPushChannel("sms", "短信推送", true, Math.max(0, successCount - 1), 1, List.of("13800138000")),
            buildPushChannel("app", "App 推送", false, 0, 0, new ArrayList<>())
        );
    }

    public void updatePushChannel(String channel, Map<String, Object> config) {
        Map<String, Object> stored = new LinkedHashMap<>(config);
        stored.putIfAbsent("channel", channel);
        savePushChannelConfig(channel, stored);
        log.info("Updated push channel: channel={}, config={}", channel, config);
    }

    public List<PushHistoryVO> getPushHistory(Long farmId, int limit) {
        return buildDerivedAlerts(farmId).stream()
            .limit(limit)
            .map(event -> PushHistoryVO.builder()
                .id((long) Math.abs(event.getAlertId().hashCode()))
                .alertId(event.getAlertId())
                .channel("wechat")
                .receiver(event.getSeverity().equals("high") ? "管理员, 值班兽医" : "管理员")
                .content(event.getTitle())
                .status("resolved".equals(event.getStatus()) ? "completed" : "success")
                .errorMessage(null)
                .pushedAt(event.getCreatedAt())
                .build())
            .collect(Collectors.toList());
    }

    public void createAlertRule(Map<String, Object> rule) {
        Long farmId = toLong(rule.get("farmId"), 1L);
        List<Map<String, Object>> persistedRules = new ArrayList<>(loadCustomRules(farmId));
        Map<String, Object> stored = new LinkedHashMap<>(rule);
        stored.putIfAbsent("id", System.currentTimeMillis());
        stored.putIfAbsent("enabled", true);
        stored.putIfAbsent("farmId", farmId);
        persistedRules.add(stored);
        saveCustomRules(farmId, persistedRules);
        log.info("Created alert rule: {}", stored);
    }

    public List<Map<String, Object>> getAlertRules(Long farmId) {
        Long resolvedFarmId = farmId == null ? 1L : farmId;
        List<Map<String, Object>> rules = new ArrayList<>();
        rules.add(createRule(1L, "设备故障告警", "device", "status = fault", "high", List.of("wechat", "sms")));
        rules.add(createRule(2L, "体温异常告警", "health", "temperature > " + formatDouble(farmSnapshotService.getTemperatureMaxThreshold()), "high", List.of("wechat")));
        rules.add(createRule(3L, "发情提醒", "estrus", "estrusProbability >= 80", "low", List.of("wechat")));
        rules.addAll(loadCustomRules(resolvedFarmId));
        return rules;
    }

    public Map<String, Object> getAlertTrend(Long farmId, int days) {
        List<AlertEventVO> events = buildDerivedAlerts(farmId);
        List<Map<String, Object>> data = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime day = now.minusDays(i);
            List<AlertEventVO> dailyEvents = events.stream()
                .filter(event -> event.getCreatedAt() != null && event.getCreatedAt().toLocalDate().equals(day.toLocalDate()))
                .collect(Collectors.toList());

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", day.format(DateTimeFormatter.ofPattern("MM-dd")));
            point.put("total", dailyEvents.size());
            point.put("resolved", dailyEvents.stream().filter(event -> "resolved".equals(event.getStatus())).count());
            point.put("pending", dailyEvents.stream().filter(event -> "pending".equals(event.getStatus())).count());
            data.add(point);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        return result;
    }

    private List<AlertEventVO> buildDerivedAlerts(Long farmId) {
        List<AnimalEntity> animals = farmId == null ? animalRepository.findAll() : animalRepository.findByFarmId(farmId);
        List<DeviceEntity> devices = farmId == null ? deviceRepository.findAll() : deviceRepository.findByFarmId(farmId);
        Map<Long, AnimalEntity> animalById = animals.stream().collect(Collectors.toMap(AnimalEntity::getId, animal -> animal));
        Map<Long, DeviceEntity> deviceByAnimalId = devices.stream()
            .filter(device -> device.getAnimalId() != null)
            .collect(Collectors.toMap(DeviceEntity::getAnimalId, device -> device, (first, second) -> first));
        Map<Long, HealthDataEntity> latestHealthMap = farmId == null
            ? healthDataRepository.findAll().stream()
                .sorted(Comparator.comparing(HealthDataEntity::getDataTime).reversed())
                .collect(Collectors.toMap(HealthDataEntity::getAnimalId, record -> record, (first, second) -> first, LinkedHashMap::new))
            : farmSnapshotService.getLatestHealthDataByAnimalId(farmId);

        List<AlertEventVO> events = new ArrayList<>();
        for (DeviceEntity device : devices) {
            AnimalEntity animal = animalById.get(device.getAnimalId());
            if ("fault".equals(device.getStatus())) {
                events.add(buildBaseEvent(
                    "device-fault-" + device.getDeviceId(),
                    animal,
                    device,
                    latestHealthMap.get(device.getAnimalId()),
                    "device",
                    "设备故障告警",
                    "high",
                    "pending",
                    "设备状态异常，请尽快检修。",
                    String.valueOf(device.getStatus()),
                    "fault",
                    device.getLastDataUpdateAt()
                ));
            }
            if ("offline".equals(device.getStatus())) {
                LocalDateTime createdAt = device.getLastOnlineAt() == null ? device.getLastDataUpdateAt() : device.getLastOnlineAt();
                events.add(buildBaseEvent(
                    "device-offline-" + device.getDeviceId(),
                    animal,
                    device,
                    latestHealthMap.get(device.getAnimalId()),
                    "device",
                    "设备离线告警",
                    "medium",
                    "acknowledged",
                    "设备长时间离线，请检查通信链路。",
                    "offline",
                    "online",
                    createdAt
                ));
            }
            if (farmSnapshotService.isLowBattery(device)) {
                events.add(buildBaseEvent(
                    "device-battery-" + device.getDeviceId(),
                    animal,
                    device,
                    latestHealthMap.get(device.getAnimalId()),
                    "device",
                    "设备低电量提醒",
                    "low",
                    "pending",
                    "设备电量已低于阈值，请安排充电或更换。",
                    String.valueOf(device.getBatteryLevel()),
                    String.valueOf(farmSnapshotService.getLowBatteryThreshold(device)),
                    device.getLastDataUpdateAt()
                ));
            }
            if (farmSnapshotService.isWeakSignal(device)) {
                events.add(buildBaseEvent(
                    "device-signal-" + device.getDeviceId(),
                    animal,
                    device,
                    latestHealthMap.get(device.getAnimalId()),
                    "device",
                    "设备弱信号提醒",
                    "low",
                    "pending",
                    "设备信号质量低于阈值，请检查安装位置或通信链路。",
                    farmSnapshotService.calculateSignalQuality(device) + "%",
                    farmSnapshotService.getSignalMinThreshold() + "%",
                    device.getLastDataUpdateAt()
                ));
            }
        }

        for (AnimalEntity animal : animals) {
            HealthDataEntity latestHealth = latestHealthMap.get(animal.getId());
            DeviceEntity device = deviceByAnimalId.get(animal.getId());

            if ("high".equals(animal.getRiskLevel())) {
                events.add(buildBaseEvent(
                    "animal-risk-" + animal.getAnimalId(),
                    animal,
                    device,
                    latestHealth,
                    "health",
                    "高风险牲畜预警",
                    "high",
                    "pending",
                    "风险评分已进入高风险区间，请优先复核。",
                    animal.getRiskLevel(),
                    "normal",
                    animal.getLastBehaviorUpdateAt()
                ));
            }
            if ("abnormal".equals(animal.getBehaviorStatus())) {
                events.add(buildBaseEvent(
                    "animal-behavior-" + animal.getAnimalId(),
                    animal,
                    device,
                    latestHealth,
                    "behavior",
                    "行为异常告警",
                    "high",
                    "pending",
                    "检测到反刍或采食行为异常，需要重点跟踪。",
                    animal.getBehaviorStatus(),
                    "normal",
                    animal.getLastBehaviorUpdateAt()
                ));
            } else if ("warning".equals(animal.getBehaviorStatus())) {
                events.add(buildBaseEvent(
                    "animal-warning-" + animal.getAnimalId(),
                    animal,
                    device,
                    latestHealth,
                    "behavior",
                    "行为预警",
                    "medium",
                    "acknowledged",
                    "行为表现出现波动，建议追加巡检。",
                    animal.getBehaviorStatus(),
                    "normal",
                    animal.getLastBehaviorUpdateAt()
                ));
            }
            if (animal.getEstrusProbability() != null && animal.getEstrusProbability() >= 80) {
                events.add(buildBaseEvent(
                    "animal-estrus-" + animal.getAnimalId(),
                    animal,
                    device,
                    latestHealth,
                    "estrus",
                    "发情窗口提醒",
                    "low",
                    "resolved",
                    String.format("当前发情概率 %.1f%%，建议查看配种计划。", animal.getEstrusProbability()),
                    formatDouble(animal.getEstrusProbability()),
                    "80.0",
                    animal.getLastBehaviorUpdateAt()
                ));
            }
            if (latestHealth != null && farmSnapshotService.isTemperatureAlert(latestHealth, device)) {
                events.add(buildBaseEvent(
                    "animal-temp-" + animal.getAnimalId(),
                    animal,
                    device,
                    latestHealth,
                    "health",
                    "体温异常告警",
                    "high",
                    "pending",
                    "最新体温超出正常范围，请立即复测。",
                    formatDouble(latestHealth.getTemperature()),
                    farmSnapshotService.temperatureThresholdRange(device),
                    latestHealth.getDataTime()
                ));
            }
            if (latestHealth != null && farmSnapshotService.isHeartRateAlert(latestHealth, device)) {
                events.add(buildBaseEvent(
                    "animal-heart-" + animal.getAnimalId(),
                    animal,
                    device,
                    latestHealth,
                    "health",
                    "心率异常告警",
                    "medium",
                    "acknowledged",
                    "心率超出正常范围，建议复核体征。",
                    latestHealth.getHeartRate() == null ? "-" : String.valueOf(latestHealth.getHeartRate()),
                    farmSnapshotService.heartRateThresholdRange(device),
                    latestHealth.getDataTime()
                ));
            }
        }

        events.addAll(buildCustomRuleAlerts(farmId, animals, devices, deviceByAnimalId, latestHealthMap));

        return events.stream()
            .map(this::applyState)
            .sorted(Comparator.comparing(AlertEventVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
    }

    private List<AlertEventVO> buildCustomRuleAlerts(
        Long farmId,
        List<AnimalEntity> animals,
        List<DeviceEntity> devices,
        Map<Long, DeviceEntity> deviceByAnimalId,
        Map<Long, HealthDataEntity> latestHealthMap
    ) {
        List<Map<String, Object>> rules = loadCustomRules(farmId == null ? 1L : farmId).stream()
            .filter(this::isRuleEnabled)
            .collect(Collectors.toList());
        if (rules.isEmpty()) {
            return new ArrayList<>();
        }

        List<AlertEventVO> events = new ArrayList<>();
        for (Map<String, Object> rule : rules) {
            String type = String.valueOf(rule.getOrDefault("type", "health"));
            String name = String.valueOf(rule.getOrDefault("name", "自定义告警规则"));
            String severity = String.valueOf(rule.getOrDefault("severity", "medium"));
            if ("device".equals(type)) {
                for (DeviceEntity device : devices) {
                    if (!matchesDeviceRule(device, rule)) {
                        continue;
                    }
                    AnimalEntity animal = animals.stream()
                        .filter(item -> Objects.equals(item.getId(), device.getAnimalId()))
                        .findFirst()
                        .orElse(null);
                    events.add(buildBaseEvent(
                        "custom-rule-" + rule.get("id") + "-" + device.getDeviceId(),
                        animal,
                        device,
                        device.getAnimalId() == null ? null : latestHealthMap.get(device.getAnimalId()),
                        type,
                        name,
                        severity,
                        "pending",
                        "自定义设备规则已命中。",
                        String.valueOf(device.getStatus()),
                        String.valueOf(rule.getOrDefault("condition", "-")),
                        device.getLastDataUpdateAt()
                    ));
                }
            } else {
                for (AnimalEntity animal : animals) {
                    HealthDataEntity latestHealth = latestHealthMap.get(animal.getId());
                    if (!matchesAnimalRule(type, animal, latestHealth, rule)) {
                        continue;
                    }
                    DeviceEntity device = deviceByAnimalId.get(animal.getId());
                    events.add(buildBaseEvent(
                        "custom-rule-" + rule.get("id") + "-" + animal.getAnimalId(),
                        animal,
                        device,
                        latestHealth,
                        type,
                        name,
                        severity,
                        "pending",
                        "自定义告警规则已命中。",
                        resolveRuleTriggerValue(type, animal, latestHealth),
                        String.valueOf(rule.getOrDefault("condition", "-")),
                        latestHealth == null ? animal.getLastBehaviorUpdateAt() : latestHealth.getDataTime()
                    ));
                }
            }
        }
        return events;
    }

    private AlertEventVO buildBaseEvent(
        String alertId,
        AnimalEntity animal,
        DeviceEntity device,
        HealthDataEntity latestHealth,
        String type,
        String title,
        String severity,
        String status,
        String message,
        String triggerValue,
        String thresholdValue,
        LocalDateTime createdAt
    ) {
        LocalDateTime safeCreatedAt = createdAt == null ? LocalDateTime.now() : createdAt;
        return AlertEventVO.builder()
            .alertId(alertId)
            .animalId(animal != null ? animal.getAnimalId() : "-")
            .animalName(animal != null ? animal.getName() : null)
            .deviceId(device != null ? device.getDeviceId() : null)
            .alertType(type)
            .alertTypeText(resolveAlertTypeText(type))
            .severity(severity)
            .severityText(resolveSeverityText(severity))
            .title(title)
            .message(message)
            .triggerValue(triggerValue)
            .thresholdValue(thresholdValue)
            .currentTemperature(latestHealth != null ? latestHealth.getTemperature() : null)
            .currentHeartRate(latestHealth != null ? latestHealth.getHeartRate() : null)
            .currentActivity(latestHealth != null ? latestHealth.getActivityLevel() : null)
            .animalRiskLevel(animal != null ? animal.getRiskLevel() : null)
            .deviceStatus(device != null ? device.getStatus() : null)
            .handlerSuggestion(buildHandlerSuggestion(type, animal, device, latestHealth))
            .status(status)
            .statusText(resolveStatusText(status))
            .createdAt(safeCreatedAt)
            .acknowledgedAt("acknowledged".equals(status) || "resolved".equals(status) ? safeCreatedAt.plusMinutes(20) : null)
            .acknowledgedBy("acknowledged".equals(status) || "resolved".equals(status) ? "system" : null)
            .resolvedAt("resolved".equals(status) ? safeCreatedAt.plusHours(1) : null)
            .resolvedBy("resolved".equals(status) ? "system" : null)
            .resolutionNote("resolved".equals(status) ? "已纳入巡检闭环" : null)
            .metricCards(buildMetricCards(type, device, latestHealth))
            .processRecords(buildProcessRecords(status, safeCreatedAt))
            .build();
    }

    private AlertEventVO applyState(AlertEventVO event) {
        AlertState state = alertStateMap.get(event.getAlertId());
        if (state == null) {
            return event;
        }
        event.setStatus(state.getStatus() == null ? event.getStatus() : state.getStatus());
        event.setStatusText(resolveStatusText(event.getStatus()));
        event.setAcknowledgedAt(state.getAcknowledgedAt() == null ? event.getAcknowledgedAt() : state.getAcknowledgedAt());
        event.setAcknowledgedBy(state.getAcknowledgedBy() == null ? event.getAcknowledgedBy() : state.getAcknowledgedBy());
        event.setResolvedAt(state.getResolvedAt() == null ? event.getResolvedAt() : state.getResolvedAt());
        event.setResolvedBy(state.getResolvedBy() == null ? event.getResolvedBy() : state.getResolvedBy());
        event.setResolutionNote(state.getResolutionNote() == null ? event.getResolutionNote() : state.getResolutionNote());
        event.setProcessRecords(buildProcessRecords(event.getStatus(), event.getCreatedAt(), event.getAcknowledgedAt(), event.getAcknowledgedBy(), event.getResolvedAt(), event.getResolvedBy(), event.getResolutionNote()));
        return event;
    }

    private List<AlertEventVO.MetricCard> buildMetricCards(String type, DeviceEntity device, HealthDataEntity latestHealth) {
        List<AlertEventVO.MetricCard> cards = new ArrayList<>();
        if (latestHealth != null && latestHealth.getTemperature() != null) {
            cards.add(AlertEventVO.MetricCard.builder()
                .label("体温")
                .value(String.format("%.1f℃", latestHealth.getTemperature()))
                .status(farmSnapshotService.isTemperatureAlert(latestHealth, device) ? "异常" : "正常")
                .build());
        }
        if (latestHealth != null && latestHealth.getHeartRate() != null) {
            cards.add(AlertEventVO.MetricCard.builder()
                .label("心率")
                .value(latestHealth.getHeartRate() + " 次/分")
                .status(farmSnapshotService.isHeartRateAlert(latestHealth, device) ? "异常" : "正常")
                .build());
        }
        if (device != null) {
            cards.add(AlertEventVO.MetricCard.builder()
                .label("设备状态")
                .value(device.getStatus())
                .status("fault".equals(device.getStatus()) || "offline".equals(device.getStatus()) ? "异常" : "正常")
                .build());
            cards.add(AlertEventVO.MetricCard.builder()
                .label("电量")
                .value((device.getBatteryLevel() == null ? 0 : device.getBatteryLevel()) + "%")
                .status(farmSnapshotService.isLowBattery(device) ? "低电量" : "正常")
                .build());
            cards.add(AlertEventVO.MetricCard.builder()
                .label("信号质量")
                .value(farmSnapshotService.calculateSignalQuality(device) + "%")
                .status(farmSnapshotService.isWeakSignal(device) ? "弱信号" : "正常")
                .build());
        }
        if (cards.isEmpty()) {
            cards.add(AlertEventVO.MetricCard.builder().label("状态").value(type).status("已生成").build());
        }
        return cards;
    }

    private List<AlertEventVO.ProcessRecord> buildProcessRecords(String status, LocalDateTime createdAt) {
        return buildProcessRecords(status, createdAt, null, null, null, null, null);
    }

    private List<AlertEventVO.ProcessRecord> buildProcessRecords(
        String status,
        LocalDateTime createdAt,
        LocalDateTime acknowledgedAt,
        String acknowledgedBy,
        LocalDateTime resolvedAt,
        String resolvedBy,
        String resolutionNote
    ) {
        List<AlertEventVO.ProcessRecord> records = new ArrayList<>();
        records.add(AlertEventVO.ProcessRecord.builder()
            .stage("告警触发")
            .operator("system")
            .time(createdAt)
            .note("系统根据统一规则自动派生告警。")
            .build());
        if (acknowledgedAt != null) {
            records.add(AlertEventVO.ProcessRecord.builder()
                .stage("告警确认")
                .operator(acknowledgedBy == null ? "system" : acknowledgedBy)
                .time(acknowledgedAt)
                .note("告警已纳入处理队列。")
                .build());
        }
        if ("resolved".equals(status) && resolvedAt != null) {
            records.add(AlertEventVO.ProcessRecord.builder()
                .stage("告警解决")
                .operator(resolvedBy == null ? "system" : resolvedBy)
                .time(resolvedAt)
                .note(resolutionNote == null ? "已闭环处理。" : resolutionNote)
                .build());
        }
        return records;
    }

    private boolean isRuleEnabled(Map<String, Object> rule) {
        return getBoolean(rule.get("enabled"), true);
    }

    private boolean matchesDeviceRule(DeviceEntity device, Map<String, Object> rule) {
        String condition = String.valueOf(rule.getOrDefault("condition", "")).toLowerCase();
        if (condition.contains("offline")) {
            return "offline".equals(device.getStatus());
        }
        if (condition.contains("fault")) {
            return "fault".equals(device.getStatus());
        }
        if (condition.contains("battery")) {
            Double threshold = extractThreshold(rule, farmSnapshotService.getLowBatteryThreshold(device) * 1.0);
            return device.getBatteryLevel() != null && device.getBatteryLevel() <= threshold;
        }
        return "fault".equals(device.getStatus()) || "offline".equals(device.getStatus()) || farmSnapshotService.isLowBattery(device);
    }

    private boolean matchesAnimalRule(String type, AnimalEntity animal, HealthDataEntity latestHealth, Map<String, Object> rule) {
        if ("estrus".equals(type)) {
            Double threshold = extractThreshold(rule, 80.0);
            return animal.getEstrusProbability() != null && animal.getEstrusProbability() >= threshold;
        }
        if ("behavior".equals(type)) {
            return "abnormal".equals(animal.getBehaviorStatus()) || "warning".equals(animal.getBehaviorStatus());
        }
        if (latestHealth == null) {
            return false;
        }
        Double threshold = extractThreshold(rule, farmSnapshotService.getTemperatureMaxThreshold());
        String condition = String.valueOf(rule.getOrDefault("condition", "")).toLowerCase();
        if (condition.contains("heart")) {
            return latestHealth.getHeartRate() != null && latestHealth.getHeartRate() >= threshold;
        }
        return latestHealth.getTemperature() != null && latestHealth.getTemperature() >= threshold;
    }

    private String resolveRuleTriggerValue(String type, AnimalEntity animal, HealthDataEntity latestHealth) {
        if ("estrus".equals(type)) {
            return formatDouble(animal.getEstrusProbability());
        }
        if ("behavior".equals(type)) {
            return animal.getBehaviorStatus();
        }
        if (latestHealth == null) {
            return "-";
        }
        return latestHealth.getTemperature() == null ? "-" : formatDouble(latestHealth.getTemperature());
    }

    private Double extractThreshold(Map<String, Object> rule, Double fallback) {
        for (String key : List.of("threshold", "minValue", "maxValue", "value")) {
            Object value = rule.get(key);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value instanceof String str && !str.isBlank()) {
                try {
                    return Double.parseDouble(str);
                } catch (NumberFormatException ignored) {
                    // Try the textual condition next.
                }
            }
        }
        String condition = String.valueOf(rule.getOrDefault("condition", ""));
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(condition);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : fallback;
    }

    private PushChannelVO buildPushChannel(String channel, String name, boolean enabled, int successCount, int failCount, List<String> receivers) {
        Map<String, Object> config = loadPushChannelConfig(channel);
        boolean resolvedEnabled = getBoolean(config.get("enabled"), enabled);
        List<String> resolvedReceivers = getStringList(config.get("receivers"), receivers);
        int total = Math.max(1, successCount + failCount);
        return PushChannelVO.builder()
            .channel(channel)
            .name(name)
            .enabled(resolvedEnabled)
            .status(resolvedEnabled ? "正常" : "未启用")
            .successCount(successCount)
            .failCount(failCount)
            .successRate(Math.round(successCount * 1000.0 / total) / 10.0)
            .receivers(resolvedReceivers)
            .build();
    }

    private boolean getChannelEnabled(String channel, boolean defaultValue) {
        return getBoolean(loadPushChannelConfig(channel).get("enabled"), defaultValue);
    }

    private Map<String, Object> loadPushChannelConfig(String channel) {
        String raw = userPermissionService.getSystemConfigValue(PUSH_CHANNEL_CONFIG_PREFIX + channel);
        if (raw == null || raw.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> config = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
            return config == null ? new LinkedHashMap<>() : config;
        } catch (JsonProcessingException e) {
            log.warn("Failed to read persisted push channel: channel={}", channel, e);
            return new LinkedHashMap<>();
        }
    }

    private void savePushChannelConfig(String channel, Map<String, Object> config) {
        try {
            userPermissionService.upsertSystemConfig(
                PUSH_CHANNEL_CONFIG_PREFIX + channel,
                objectMapper.writeValueAsString(config),
                "json",
                "Persisted push channel config for " + channel,
                null
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to save push channel config", e);
        }
    }

    private boolean getBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String str && !str.isBlank()) {
            return Boolean.parseBoolean(str);
        }
        return fallback;
    }

    private List<String> getStringList(Object value, List<String> fallback) {
        if (!(value instanceof List<?> list)) {
            return fallback;
        }
        return list.stream()
            .filter(Objects::nonNull)
            .map(String::valueOf)
            .collect(Collectors.toList());
    }

    private Map<String, Object> createRule(Long id, String name, String type, String condition, String severity, List<String> channels) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("id", id);
        rule.put("name", name);
        rule.put("type", type);
        rule.put("condition", condition);
        rule.put("severity", severity);
        rule.put("enabled", true);
        rule.put("pushChannels", channels);
        return rule;
    }

    private List<Map<String, Object>> loadCustomRules(Long farmId) {
        String raw = userPermissionService.getSystemConfigValue(getCustomRulesKey(farmId));
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }

        try {
            List<Map<String, Object>> rules = objectMapper.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
            return rules == null ? new ArrayList<>() : rules;
        } catch (JsonProcessingException e) {
            log.warn("Failed to read persisted alert rules: farmId={}", farmId, e);
            return new ArrayList<>();
        }
    }

    private void saveCustomRules(Long farmId, List<Map<String, Object>> rules) {
        try {
            userPermissionService.upsertSystemConfig(
                getCustomRulesKey(farmId),
                objectMapper.writeValueAsString(rules),
                "json",
                "Custom alert rules for farm " + farmId,
                farmId
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("保存告警规则失败", e);
        }
    }

    private String getCustomRulesKey(Long farmId) {
        return CUSTOM_RULES_CONFIG_PREFIX + (farmId == null ? 1L : farmId);
    }

    private Long toLong(Object value, Long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String resolveAlertTypeText(String type) {
        return switch (type) {
            case "device" -> "设备告警";
            case "behavior" -> "行为告警";
            case "estrus" -> "发情提醒";
            case "health" -> "健康告警";
            default -> "系统告警";
        };
    }

    private String resolveSeverityText(String severity) {
        return switch (severity) {
            case "high" -> "高风险";
            case "medium" -> "中风险";
            case "low" -> "低风险";
            default -> "一般";
        };
    }

    private String resolveStatusText(String status) {
        return switch (status) {
            case "pending" -> "待处理";
            case "acknowledged" -> "已确认";
            case "resolved" -> "已解决";
            default -> "未知";
        };
    }

    private String buildHandlerSuggestion(String type, AnimalEntity animal, DeviceEntity device, HealthDataEntity latestHealth) {
        if ("device".equals(type) && device != null) {
            return "建议检查设备供电、通信链路和固定状态。";
        }
        if ("behavior".equals(type)) {
            return "建议联动查看行为分析详情，确认反刍和采食变化。";
        }
        if ("estrus".equals(type)) {
            return "建议查看配种计划，确认窗口与执行人。";
        }
        if (latestHealth != null && farmSnapshotService.isTemperatureAlert(latestHealth, device)) {
            return "建议优先复测体温，并结合心率、活动情况综合判断。";
        }
        if (animal != null && "high".equals(animal.getRiskLevel())) {
            return "建议安排兽医复核并加入重点巡检列表。";
        }
        return "建议保持跟踪处理，并在详情页记录闭环。";
    }

    private String formatDouble(Double value) {
        return value == null ? "-" : String.format("%.1f", value);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class AlertState {
        private String status;
        private LocalDateTime acknowledgedAt;
        private String acknowledgedBy;
        private LocalDateTime resolvedAt;
        private String resolvedBy;
        private String resolutionNote;
    }
}

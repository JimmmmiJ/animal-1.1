package com.livestock.health.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livestock.health.model.entity.AnimalEntity;
import com.livestock.health.model.entity.HealthDataEntity;
import com.livestock.health.model.vo.ActivityPatternVO;
import com.livestock.health.model.vo.EstrusAnimalVO;
import com.livestock.health.model.vo.EstrusProbabilityTrendVO;
import com.livestock.health.model.vo.EstrusSummaryVO;
import com.livestock.health.model.vo.FarmBaselineVO;
import com.livestock.health.repository.AnimalRepository;
import com.livestock.health.repository.HealthDataRepository;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstrusDetectionService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final String BREEDING_PLAN_CONFIG_PREFIX = "breeding_plan:";

    private final AnimalRepository animalRepository;
    private final HealthDataRepository healthDataRepository;
    private final FarmSnapshotService farmSnapshotService;
    private final UserPermissionService userPermissionService;
    private final ObjectMapper objectMapper;

    public EstrusSummaryVO getEstrusSummary(Long farmId) {
        FarmBaselineVO baseline = farmSnapshotService.getBaseline(farmId);
        List<AnimalEntity> femaleAnimals = animalRepository.findByFarmId(farmId).stream()
            .filter(animal -> Integer.valueOf(2).equals(animal.getGender()))
            .collect(Collectors.toList());

        if (femaleAnimals.isEmpty()) {
            return createEmptySummary(baseline);
        }

        long totalCount = femaleAnimals.size();
        long inEstrusCount = femaleAnimals.stream().filter(animal -> "estrus".equals(resolveEstrusStatus(animal))).count();
        long approachingCount = femaleAnimals.stream().filter(animal -> "approaching".equals(resolveEstrusStatus(animal))).count();
        long pregnantCount = femaleAnimals.stream().filter(animal -> "pregnant".equals(resolveEstrusStatus(animal))).count();
        long normalCount = femaleAnimals.stream().filter(animal -> "normal".equals(resolveEstrusStatus(animal))).count();

        return EstrusSummaryVO.builder()
            .baseline(baseline)
            .inEstrusCount(inEstrusCount)
            .inEstrusPercent(round1(inEstrusCount * 100.0 / totalCount))
            .approachingEstrusCount(approachingCount)
            .approachingEstrusPercent(round1(approachingCount * 100.0 / totalCount))
            .pregnantCount(pregnantCount)
            .pregnantPercent(round1(pregnantCount * 100.0 / totalCount))
            .normalCount(normalCount)
            .normalPercent(round1(normalCount * 100.0 / totalCount))
            .totalCount(totalCount)
            .build();
    }

    public List<EstrusAnimalVO> getEstrusAnimalList(Long farmId, String statusFilter) {
        List<AnimalEntity> animals = animalRepository.findByFarmId(farmId).stream()
            .filter(animal -> Integer.valueOf(2).equals(animal.getGender()))
            .filter(animal -> statusFilter == null || statusFilter.isBlank() || statusFilter.equals(resolveEstrusStatus(animal)))
            .collect(Collectors.toList());

        Map<Long, HealthDataEntity> latestHealthMap = farmSnapshotService.getLatestHealthDataByAnimalId(farmId);

        return animals.stream()
            .sorted(Comparator.comparing((AnimalEntity animal) -> calculateMovementEstrusProbability(animal)).reversed())
            .map(animal -> buildEstrusAnimalVO(animal, latestHealthMap.get(animal.getId()), false))
            .collect(Collectors.toList());
    }

    public EstrusAnimalVO getAnimalEstrusDetail(String animalId) {
        AnimalEntity animal = resolveAnimal(animalId);
        HealthDataEntity latestHealth = healthDataRepository.findByAnimalIdOrderByDataTimeDesc(animal.getId()).stream()
            .findFirst()
            .orElse(null);
        return buildEstrusAnimalVO(animal, latestHealth, true);
    }

    public ActivityPatternVO getActivityPattern(String animalId) {
        AnimalEntity animal = resolveAnimal(animalId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusDays(7);

        List<HealthDataEntity> records = healthDataRepository
            .findByAnimalIdAndDataTimeBetweenOrderByDataTimeDesc(animal.getId(), startTime, now);
        if (records.isEmpty()) {
            return createEmptyActivityPattern();
        }

        Map<Integer, List<Integer>> hourlyData = new LinkedHashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            hourlyData.put(hour, new ArrayList<>());
        }

        Map<String, List<Integer>> dailyData = new LinkedHashMap<>();
        records.forEach(record -> {
            if (record.getActivityLevel() == null) {
                return;
            }
            hourlyData.get(record.getDataTime().getHour()).add(record.getActivityLevel());
            dailyData.computeIfAbsent(record.getDataTime().toLocalDate().format(DATE_FORMATTER), key -> new ArrayList<>())
                .add(record.getActivityLevel());
        });

        List<ActivityPatternVO.PatternPoint> hourlyPattern = hourlyData.entrySet().stream()
            .map(entry -> ActivityPatternVO.PatternPoint.builder()
                .label(String.format("%02d:00", entry.getKey()))
                .value((int) Math.round(entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0)))
                .category(entry.getKey() >= 6 && entry.getKey() < 18 ? "白天" : "夜间")
                .build())
            .collect(Collectors.toList());

        List<ActivityPatternVO.PatternPoint> dailyPattern = dailyData.entrySet().stream()
            .map(entry -> ActivityPatternVO.PatternPoint.builder()
                .label(entry.getKey())
                .value((int) Math.round(entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0)))
                .category("日趋势")
                .build())
            .collect(Collectors.toList());

        int peakHour = hourlyPattern.stream()
            .max(Comparator.comparingInt(ActivityPatternVO.PatternPoint::getValue))
            .map(item -> Integer.parseInt(item.getLabel().substring(0, 2)))
            .orElse(12);

        return ActivityPatternVO.builder()
            .hourlyPattern(hourlyPattern)
            .dailyPattern(dailyPattern)
            .peakActivityHour(peakHour)
            .avgActivityLevel((int) Math.round(records.stream()
                .map(HealthDataEntity::getActivityLevel)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0)))
            .build();
    }

    public EstrusProbabilityTrendVO getEstrusProbabilityTrend(String animalId, int days) {
        AnimalEntity animal = resolveAnimal(animalId);
        List<HealthDataEntity> recentRecords = healthDataRepository.findByAnimalIdOrderByDataTimeDesc(animal.getId()).stream()
            .limit(Math.max(72, days * 12L))
            .collect(Collectors.toList());
        double currentProbability = calculateMovementEstrusProbability(recentRecords);

        List<EstrusProbabilityTrendVO.TrendPoint> trend = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime targetDay = now.minusDays(i);
            List<HealthDataEntity> dayRecords = recentRecords.stream()
                .filter(record -> record.getDataTime().toLocalDate().equals(targetDay.toLocalDate()))
                .collect(Collectors.toList());
            double value = dayRecords.isEmpty() ? Math.max(0.0, currentProbability - i * 2.5) : calculateMovementEstrusProbability(dayRecords);
            trend.add(EstrusProbabilityTrendVO.TrendPoint.builder()
                .date(targetDay.format(DATE_FORMATTER))
                .probability(round1(value))
                .status(resolveEstrusStage(value))
                .build());
        }

        return EstrusProbabilityTrendVO.builder()
            .animalId(animal.getAnimalId())
            .trend(trend)
            .currentProbability(round1(currentProbability))
            .prediction(buildPredictionText(currentProbability))
            .build();
    }

    public List<Map<String, Object>> getEstrusAlertEvents(Long farmId, int limit) {
        Map<Long, HealthDataEntity> latestHealthMap = farmSnapshotService.getLatestHealthDataByAnimalId(farmId);
        return animalRepository.findByFarmId(farmId).stream()
            .filter(animal -> Integer.valueOf(2).equals(animal.getGender()))
            .filter(animal -> calculateMovementEstrusProbability(animal) >= getEstrusNoticeThreshold())
            .sorted(Comparator.comparing((AnimalEntity animal) -> calculateMovementEstrusProbability(animal)).reversed())
            .limit(limit)
            .map(animal -> {
                double probability = calculateMovementEstrusProbability(animal);
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("animalId", animal.getAnimalId());
                event.put("name", animal.getName());
                event.put("probability", round1(probability));
                event.put("status", resolveEstrusStatus(animal, probability));
                event.put("eventTime", animal.getLastBehaviorUpdateAt());
                event.put("eventType", probability >= getEstrusThreshold() ? "发情中" : "临近发情");
                event.put("description", String.format("%s 发情概率 %.1f%%，当前体温 %s",
                    animal.getAnimalId(),
                    probability,
                    latestHealthMap.get(animal.getId()) != null && latestHealthMap.get(animal.getId()).getTemperature() != null
                        ? String.format("%.1f℃", latestHealthMap.get(animal.getId()).getTemperature())
                        : "-"));
                return event;
            })
            .collect(Collectors.toList());
    }

    public void updateBreedingPlan(String animalId, Map<String, Object> planData) {
        AnimalEntity animal = resolveAnimal(animalId);
        Map<String, Object> stored = new LinkedHashMap<>(planData);
        stored.putIfAbsent("planDate", LocalDateTime.now().plusDays(1).toString());
        stored.putIfAbsent("operator", "系统");
        saveBreedingPlan(animal.getAnimalId(), stored);
        log.info("Updated breeding plan: animalId={}, planData={}", animal.getAnimalId(), stored);
    }

    private EstrusAnimalVO buildEstrusAnimalVO(AnimalEntity animal, HealthDataEntity latestHealth, boolean includeDetail) {
        List<HealthDataEntity> records = healthDataRepository.findByAnimalIdOrderByDataTimeDesc(animal.getId()).stream()
            .limit(72)
            .collect(Collectors.toList());
        Map<String, Object> plan = loadBreedingPlan(animal.getAnimalId());
        double movementProbability = calculateMovementEstrusProbability(records);
        String estrusStatus = resolveEstrusStatus(animal, movementProbability);
        List<HealthDataEntity> detectionRecords = records.stream().limit(12).collect(Collectors.toList());

        return EstrusAnimalVO.builder()
            .animalId(animal.getAnimalId())
            .name(animal.getName())
            .breed(animal.getBreed())
            .age(animal.getAge())
            .weight(animal.getWeight())
            .estrusStatus(estrusStatus)
            .estrusProbability(round1(movementProbability))
            .estrusStage(resolveEstrusStage(movementProbability))
            .activityIndex(calculateActivityIndex(records))
            .dailyRuminationTime(animal.getDailyRuminationTime())
            .dailyFeedingCount(animal.getDailyFeedingCount())
            .lastEstrusAt(animal.getLastEstrusAt())
            .nextEstrusPredictedAt(animal.getNextEstrusPredictedAt())
            .currentTemperature(latestHealth != null ? latestHealth.getTemperature() : null)
            .currentHeartRate(latestHealth != null ? latestHealth.getHeartRate() : null)
            .lastUpdateAt(animal.getLastBehaviorUpdateAt())
            .suggestion(buildSuggestion(animal, movementProbability))
            .breedingRecommendation(plan == null
                ? buildBreedingRecommendation(animal, movementProbability)
                : "已生成配种计划，建议按计划时间组织复核与配种。")
            .breedingPlan(plan == null ? new LinkedHashMap<>() : plan)
            .recentDetections(includeDetail ? buildDetectionRecords(detectionRecords) : new ArrayList<>())
            .build();
    }

    private AnimalEntity resolveAnimal(String animalId) {
        return animalRepository.findByAnimalId(animalId)
            .orElseThrow(() -> new RuntimeException("牲畜不存在: " + animalId));
    }

    private int calculateActivityIndex(Long animalDbId) {
        return calculateActivityIndex(healthDataRepository.findByAnimalIdOrderByDataTimeDesc(animalDbId).stream()
            .limit(12)
            .collect(Collectors.toList()));
    }

    private int calculateActivityIndex(List<HealthDataEntity> records) {
        return (int) Math.round(records.stream()
            .limit(12)
            .map(HealthDataEntity::getActivityLevel)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0));
    }

    private double calculateMovementEstrusProbability(AnimalEntity animal) {
        return calculateMovementEstrusProbability(healthDataRepository.findByAnimalIdOrderByDataTimeDesc(animal.getId()).stream()
            .limit(72)
            .collect(Collectors.toList()));
    }

    private double calculateMovementEstrusProbability(List<HealthDataEntity> records) {
        List<Integer> activities = records.stream()
            .map(HealthDataEntity::getActivityLevel)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (activities.isEmpty()) {
            return 0.0;
        }

        List<Integer> recent = activities.stream().limit(12).collect(Collectors.toList());
        List<Integer> baseline = activities.stream().skip(12).limit(48).collect(Collectors.toList());
        if (baseline.isEmpty()) {
            baseline = activities;
        }

        double recentAvg = average(recent);
        double baselineAvg = average(baseline);
        double recentPeak = recent.stream().mapToInt(Integer::intValue).max().orElse((int) Math.round(recentAvg));
        double highActivityRatio = recent.stream().filter(value -> value >= 70).count() / (double) Math.max(1, recent.size());
        double activityRise = recentAvg - baselineAvg;

        double score = 12.0
            + recentAvg * 0.38
            + recentPeak * 0.28
            + clamp(activityRise * 1.3, -12.0, 24.0)
            + highActivityRatio * 18.0;
        return round1(clamp(score, 0.0, 100.0));
    }

    private double average(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private List<EstrusAnimalVO.DetectionRecord> buildDetectionRecords(List<HealthDataEntity> records) {
        return records.stream()
            .map(record -> EstrusAnimalVO.DetectionRecord.builder()
                .time(record.getDataTime())
                .item("综合检测")
                .value(String.format("体温 %.1f℃ / 活动 %s / 采食 %s",
                    record.getTemperature() == null ? 0.0 : record.getTemperature(),
                    record.getActivityLevel() == null ? "-" : record.getActivityLevel(),
                    record.getFeedingCount() == null ? "-" : record.getFeedingCount()))
                .status(resolveDetectionStatus(record))
                .build())
            .collect(Collectors.toList());
    }

    private String resolveDetectionStatus(HealthDataEntity record) {
        if (record.getActivityLevel() != null && record.getActivityLevel() >= 80) {
            return "高活跃";
        }
        if (farmSnapshotService.isTemperatureAlert(record)) {
            return "待关注";
        }
        return "正常";
    }

    private Map<String, Object> loadBreedingPlan(String animalId) {
        String raw = userPermissionService.getSystemConfigValue(BREEDING_PLAN_CONFIG_PREFIX + animalId);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> plan = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
            return plan == null ? null : plan;
        } catch (JsonProcessingException e) {
            log.warn("Failed to read persisted breeding plan: animalId={}", animalId, e);
            return null;
        }
    }

    private void saveBreedingPlan(String animalId, Map<String, Object> plan) {
        try {
            userPermissionService.upsertSystemConfig(
                BREEDING_PLAN_CONFIG_PREFIX + animalId,
                objectMapper.writeValueAsString(plan),
                "json",
                "Persisted breeding plan for " + animalId,
                null
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to save breeding plan", e);
        }
    }

    private String resolveEstrusStatus(AnimalEntity animal) {
        return resolveEstrusStatus(animal, calculateMovementEstrusProbability(animal));
    }

    private String resolveEstrusStatus(AnimalEntity animal, double probability) {
        if ("pregnant".equals(animal.getEstrusStatus())) {
            return "pregnant";
        }
        if (probability >= getEstrusThreshold()) {
            return "estrus";
        }
        if (probability >= getApproachingEstrusThreshold()) {
            return "approaching";
        }
        return "normal";
    }

    private int getEstrusThreshold() {
        return getIntConfig("estrus_threshold", 80);
    }

    private int getEstrusNoticeThreshold() {
        return getIntConfig("breeding_notice_probability", getApproachingEstrusThreshold());
    }

    private int getApproachingEstrusThreshold() {
        int estrusThreshold = getEstrusThreshold();
        int configured = getIntConfig("breeding_probability_threshold", Math.max(0, estrusThreshold - 20));
        return configured >= estrusThreshold ? Math.max(0, estrusThreshold - 20) : configured;
    }

    private int getObservationThreshold() {
        return Math.max(0, getApproachingEstrusThreshold() - 20);
    }

    private int getIntConfig(String key, int fallback) {
        String raw = userPermissionService.getSystemConfigValue(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String resolveEstrusStage(Double probability) {
        if (probability == null) {
            return "未知";
        }
        if (probability >= getEstrusThreshold()) {
            return "发情中";
        }
        if (probability >= getApproachingEstrusThreshold()) {
            return "临近发情";
        }
        if (probability >= getObservationThreshold()) {
            return "观察期";
        }
        return "正常";
    }

    private String buildPredictionText(double currentProbability) {
        if (currentProbability >= getEstrusThreshold()) {
            return "预计 24 小时内维持高发情概率，适合安排配种计划。";
        }
        if (currentProbability >= getApproachingEstrusThreshold()) {
            return "预计 1-2 天内进入重点观察窗口，建议提前准备配种资源。";
        }
        return "当前未进入高概率发情窗口，建议继续监测活动模式与体征变化。";
    }

    private String buildSuggestion(AnimalEntity animal, double probability) {
        String status = resolveEstrusStatus(animal, probability);
        if ("estrus".equals(status)) {
            return "已进入重点窗口，建议结合活动峰值和连续高活跃时段复核。";
        }
        if ("approaching".equals(status)) {
            return "建议连续监测未来 48 小时的活动峰值变化。";
        }
        if ("pregnant".equals(status)) {
            return "已怀孕，当前以稳态监测和营养保障为主。";
        }
        return "维持常规监测，重点关注下一次预测窗口。";
    }

    private String buildSuggestion(AnimalEntity animal) {
        return buildSuggestion(animal, calculateMovementEstrusProbability(animal));
    }

    private String buildBreedingRecommendation(AnimalEntity animal, double probability) {
        if (probability >= getEstrusThreshold()) {
            return "建议在未来 12-24 小时内执行配种，并安排复核记录。";
        }
        if (probability >= getApproachingEstrusThreshold()) {
            return "建议先生成配种计划，进入连续观察阶段。";
        }
        return "暂不建议立即配种，可继续保持监测。";
    }

    private String buildBreedingRecommendation(AnimalEntity animal) {
        return buildBreedingRecommendation(animal, calculateMovementEstrusProbability(animal));
    }

    private EstrusSummaryVO createEmptySummary(FarmBaselineVO baseline) {
        return EstrusSummaryVO.builder()
            .baseline(baseline)
            .inEstrusCount(0L)
            .inEstrusPercent(0.0)
            .approachingEstrusCount(0L)
            .approachingEstrusPercent(0.0)
            .pregnantCount(0L)
            .pregnantPercent(0.0)
            .normalCount(0L)
            .normalPercent(0.0)
            .totalCount(0L)
            .build();
    }

    private ActivityPatternVO createEmptyActivityPattern() {
        List<ActivityPatternVO.PatternPoint> hourlyPattern = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            hourlyPattern.add(ActivityPatternVO.PatternPoint.builder()
                .label(String.format("%02d:00", hour))
                .value(0)
                .category(hour >= 6 && hour < 18 ? "白天" : "夜间")
                .build());
        }
        return ActivityPatternVO.builder()
            .hourlyPattern(hourlyPattern)
            .dailyPattern(new ArrayList<>())
            .peakActivityHour(12)
            .avgActivityLevel(0)
            .build();
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}

package com.livestock.health.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livestock.health.model.entity.AnimalEntity;
import com.livestock.health.model.entity.HealthDataEntity;
import com.livestock.health.model.vo.BehaviorAnalysisReportVO;
import com.livestock.health.model.vo.BehaviorEventVO;
import com.livestock.health.model.vo.BehaviorSummaryVO;
import com.livestock.health.model.vo.FarmBaselineVO;
import com.livestock.health.model.vo.SheepBehaviorDetailVO;
import com.livestock.health.repository.AnimalRepository;
import com.livestock.health.repository.HealthDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
public class FeedingBehaviorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final String DIET_PLAN_CONFIG_PREFIX = "diet_plan:";

    private final AnimalRepository animalRepository;
    private final HealthDataRepository healthDataRepository;
    private final FarmSnapshotService farmSnapshotService;
    private final UserPermissionService userPermissionService;
    private final ObjectMapper objectMapper;

    public BehaviorSummaryVO getBehaviorSummary(Long farmId) {
        FarmBaselineVO baseline = farmSnapshotService.getBaseline(farmId);
        List<AnimalEntity> animals = animalRepository.findByFarmId(farmId);
        if (animals.isEmpty()) {
            return createEmptySummary(baseline);
        }

        double avgRuminationTime = animals.stream()
            .map(AnimalEntity::getDailyRuminationTime)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
        double avgFeedingCount = animals.stream()
            .map(AnimalEntity::getDailyFeedingCount)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
        long abnormalCount = animals.stream()
            .filter(animal -> !"normal".equals(resolveBehaviorStatus(animal)))
            .count();
        double abnormalPercent = round1(abnormalCount * 100.0 / animals.size());
        int ruminationThreshold = getRuminationThreshold();
        int feedingThreshold = getFeedingThreshold();

        return BehaviorSummaryVO.builder()
            .baseline(baseline)
            .avgRuminationTime(Math.round(avgRuminationTime))
            .ruminationStandardRange(ruminationThreshold + "-360")
            .avgFeedingCount(Math.round(avgFeedingCount))
            .feedingStandardRange(feedingThreshold + "-25")
            .abnormalAnimalCount((int) abnormalCount)
            .totalAnimalCount(animals.size())
            .abnormalPercent(abnormalPercent)
            .normalPercent(round1(100.0 - abnormalPercent))
            .digestiveHealthScore(round1(calculateDigestiveHealthScore(animals)))
            .build();
    }

    public List<SheepBehaviorDetailVO> getSheepBehaviorList(Long farmId, String statusFilter) {
        Map<Long, HealthDataEntity> latestHealthMap = farmSnapshotService.getLatestHealthDataByAnimalId(farmId);
        return animalRepository.findByFarmId(farmId).stream()
            .filter(animal -> statusFilter == null || statusFilter.isBlank() || statusFilter.equals(resolveBehaviorStatus(animal)))
            .map(animal -> buildSheepBehaviorVO(animal, latestHealthMap.get(animal.getId()), false))
            .sorted(Comparator.comparing(SheepBehaviorDetailVO::getRuminationTime, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
    }

    public SheepBehaviorDetailVO getSheepBehaviorDetail(String animalId) {
        AnimalEntity animal = resolveAnimal(animalId);
        HealthDataEntity latestHealth = healthDataRepository.findByAnimalIdOrderByDataTimeDesc(animal.getId()).stream()
            .findFirst()
            .orElse(null);
        return buildSheepBehaviorVO(animal, latestHealth, true);
    }

    public List<Map<String, Object>> get24HourBehaviorPattern(Long farmId) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(24);
        List<HealthDataEntity> records = healthDataRepository.findByFarmIdAndDataTimeBetweenOrderByDataTimeDesc(farmId, startTime, endTime);

        List<Map<String, Object>> pattern = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            final int bucket = hour;
            List<HealthDataEntity> hourRecords = records.stream()
                .filter(record -> record.getDataTime().getHour() == bucket)
                .collect(Collectors.toList());

            double rumination = average(hourRecords.stream().map(HealthDataEntity::getRuminationTime).collect(Collectors.toList()));
            double feeding = average(hourRecords.stream().map(HealthDataEntity::getFeedingCount).collect(Collectors.toList())) * 10.0;
            double activity = average(hourRecords.stream().map(HealthDataEntity::getActivityLevel).collect(Collectors.toList()));
            double resting = average(hourRecords.stream().map(HealthDataEntity::getRestingTime).collect(Collectors.toList()));
            double total = Math.max(1.0, rumination + feeding + activity + resting);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("hour", String.format("%02d:00", hour));
            item.put("rumination", (int) Math.round(rumination / total * 100));
            item.put("feeding", (int) Math.round(feeding / total * 100));
            item.put("resting", (int) Math.round(resting / total * 100));
            item.put("activity", (int) Math.round(activity / total * 100));
            pattern.add(item);
        }
        return pattern;
    }

    public List<Map<String, Object>> getRuminationEfficiencyTrend(Long farmId, int days) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay().minusSeconds(1);
            List<HealthDataEntity> records = healthDataRepository.findByFarmIdAndDataTimeBetweenOrderByDataTimeDesc(farmId, start, end);

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date.format(DATE_FORMATTER));
            point.put("efficiency", round1(calculateDailyEfficiency(records)));
            point.put("avgRuminationTime", Math.round(average(records.stream().map(HealthDataEntity::getRuminationTime).collect(Collectors.toList()))));
            trend.add(point);
        }
        return trend;
    }

    public List<BehaviorEventVO> getAbnormalBehaviorEvents(Long farmId, int limit) {
        List<AnimalEntity> animals = animalRepository.findByFarmId(farmId);
        return animals.stream()
            .filter(animal -> !"normal".equals(resolveBehaviorStatus(animal)))
            .sorted(Comparator
                .comparing((AnimalEntity animal) -> "abnormal".equals(resolveBehaviorStatus(animal)) ? 2 : 1)
                .reversed()
                .thenComparing(animal -> animal.getLastBehaviorUpdateAt() == null ? LocalDateTime.MIN : animal.getLastBehaviorUpdateAt(), Comparator.reverseOrder()))
            .limit(limit)
            .map(animal -> BehaviorEventVO.builder()
                .eventId("BEH-" + animal.getAnimalId())
                .animalId(animal.getAnimalId())
                .animalName(animal.getName())
                .eventType("abnormal".equals(resolveBehaviorStatus(animal)) ? "行为异常" : "行为预警")
                .eventTime(animal.getLastBehaviorUpdateAt() == null ? LocalDateTime.now().minusHours(1) : animal.getLastBehaviorUpdateAt())
                .duration(60)
                .description(buildBehaviorEventDescription(animal))
                .ruminationTime(animal.getDailyRuminationTime())
                .feedingCount(animal.getDailyFeedingCount())
                .restingTime(Math.max(0, 480 - safeInt(animal.getDailyRuminationTime()) - safeInt(animal.getDailyFeedingCount()) * 10))
                .severity("abnormal".equals(resolveBehaviorStatus(animal)) ? "high" : "medium")
                .build())
            .collect(Collectors.toList());
    }

    public void saveDietPlan(String animalId, Map<String, Object> dietPlan) {
        AnimalEntity animal = resolveAnimal(animalId);
        Map<String, Object> stored = new LinkedHashMap<>(dietPlan);
        stored.putIfAbsent("updatedAt", LocalDateTime.now().toString());
        saveDietPlanConfig(animal.getAnimalId(), stored);
        log.info("Saved diet plan: animalId={}, dietPlan={}", animalId, stored);
    }

    public List<String> getNutritionAdvice(Long farmId) {
        List<AnimalEntity> animals = animalRepository.findByFarmId(farmId);
        double avgRumination = animals.stream()
            .map(AnimalEntity::getDailyRuminationTime)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
        double avgFeeding = animals.stream()
            .map(AnimalEntity::getDailyFeedingCount)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);

        List<String> advice = new ArrayList<>();
        advice.add("保持日常青粗饲料与精料配比稳定，优先保证饮水和清洁度。");
        if (avgRumination < getRuminationThreshold()) {
            advice.add("建议提升纤维含量并增加优质粗饲料比例，以改善整体反刍时长。");
        } else {
            advice.add("当前反刍表现稳定，可继续维持现有粗饲料结构。");
        }
        if (avgFeeding < getFeedingThreshold()) {
            advice.add("建议调整投喂时间窗口，增加分次饲喂频率。");
        } else {
            advice.add("采食次数处于合理范围，可重点关注个体差异。");
        }
        advice.add("对行为异常个体建议单独补充电解质和益生菌，并连续跟踪 3 天。");
        return advice;
    }

    public BehaviorAnalysisReportVO generateAnalysisReport(Long farmId) {
        List<AnimalEntity> animals = animalRepository.findByFarmId(farmId);
        List<Map<String, Object>> pattern = get24HourBehaviorPattern(farmId);

        Map<String, Integer> behaviorPattern = new LinkedHashMap<>();
        behaviorPattern.put("反刍", averagePercent(pattern, "rumination"));
        behaviorPattern.put("采食", averagePercent(pattern, "feeding"));
        behaviorPattern.put("休息", averagePercent(pattern, "resting"));
        behaviorPattern.put("活动", averagePercent(pattern, "activity"));

        List<BehaviorAnalysisReportVO.HealthTrendPoint> healthTrend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay().minusSeconds(1);
            List<HealthDataEntity> records = healthDataRepository.findByFarmIdAndDataTimeBetweenOrderByDataTimeDesc(farmId, start, end);
            double dailyScore = records.isEmpty()
                ? animals.stream().map(AnimalEntity::getHealthScore).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0.0)
                : calculateDailyHealthScore(records);
            healthTrend.add(BehaviorAnalysisReportVO.HealthTrendPoint.builder()
                .date(date.format(DATE_FORMATTER))
                .score(round1(dailyScore))
                .build());
        }

        long abnormalCount = animals.stream()
            .filter(animal -> !"normal".equals(resolveBehaviorStatus(animal)))
            .count();

        return BehaviorAnalysisReportVO.builder()
            .behaviorPattern(behaviorPattern)
            .healthTrend(healthTrend)
            .analysisSummary(buildAnalysisSummary(animals))
            .abnormalAnimalCount(abnormalCount)
            .avgRuminationTime(round1(animals.stream()
                .map(AnimalEntity::getDailyRuminationTime)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0)))
            .avgFeedingCount(round1(animals.stream()
                .map(AnimalEntity::getDailyFeedingCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0)))
            .recommendations(getNutritionAdvice(farmId))
            .build();
    }

    private SheepBehaviorDetailVO buildSheepBehaviorVO(AnimalEntity animal, HealthDataEntity latestHealth, boolean includeDetail) {
        List<HealthDataEntity> recentRecords = healthDataRepository.findByAnimalIdOrderByDataTimeDesc(animal.getId()).stream()
            .limit(includeDetail ? 48 : 12)
            .collect(Collectors.toList());

        List<SheepBehaviorDetailVO.PatternPoint> hourlyPattern = includeDetail
            ? buildHourlyPattern(recentRecords)
            : new ArrayList<>();
        List<SheepBehaviorDetailVO.HistoryRecord> history = includeDetail
            ? buildHistory(animal.getId())
            : new ArrayList<>();

        List<String> nutritionAdvice = includeDetail
            ? buildAnimalNutritionAdvice(animal)
            : new ArrayList<>();

        Map<String, Object> dietPlan = loadDietPlan(animal.getAnimalId());

        return SheepBehaviorDetailVO.builder()
            .animalId(animal.getAnimalId())
            .name(animal.getName())
            .breed(animal.getBreed())
            .age(animal.getAge())
            .weight(animal.getWeight())
            .healthScore(animal.getHealthScore())
            .ruminationTime(animal.getDailyRuminationTime())
            .feedingCount(animal.getDailyFeedingCount())
            .activityLevel(latestHealth != null ? latestHealth.getActivityLevel() : null)
            .restingTime(latestHealth != null ? latestHealth.getRestingTime() : null)
            .ruminationEfficiency(Math.round(animal.getRuminationEfficiency() == null ? 0.0 : animal.getRuminationEfficiency()))
            .feedingQuality(Math.round(animal.getFeedingQuality() == null ? 0.0 : animal.getFeedingQuality()))
            .status(resolveBehaviorStatus(animal))
            .lastUpdate(animal.getLastBehaviorUpdateAt())
            .lastDietAdjustAt(animal.getLastDietAdjustAt())
            .summary(dietPlan == null ? buildBehaviorSummaryText(animal) : "已保存调整饲喂方案，建议按计划跟踪执行效果。")
            .nutritionAdvice(nutritionAdvice)
            .hourlyPattern(hourlyPattern)
            .history(history)
            .build();
    }

    private List<SheepBehaviorDetailVO.PatternPoint> buildHourlyPattern(List<HealthDataEntity> records) {
        Map<Integer, List<HealthDataEntity>> grouped = new LinkedHashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            grouped.put(hour, new ArrayList<>());
        }
        records.forEach(record -> grouped.get(record.getDataTime().getHour()).add(record));

        return grouped.entrySet().stream()
            .map(entry -> SheepBehaviorDetailVO.PatternPoint.builder()
                .label(String.format("%02d:00", entry.getKey()))
                .rumination((int) Math.round(average(entry.getValue().stream().map(HealthDataEntity::getRuminationTime).collect(Collectors.toList()))))
                .feeding((int) Math.round(average(entry.getValue().stream().map(HealthDataEntity::getFeedingCount).collect(Collectors.toList()))))
                .activity((int) Math.round(average(entry.getValue().stream().map(HealthDataEntity::getActivityLevel).collect(Collectors.toList()))))
                .resting((int) Math.round(average(entry.getValue().stream().map(HealthDataEntity::getRestingTime).collect(Collectors.toList()))))
                .build())
            .collect(Collectors.toList());
    }

    private List<SheepBehaviorDetailVO.HistoryRecord> buildHistory(Long animalDbId) {
        List<HealthDataEntity> records = healthDataRepository.findByAnimalIdOrderByDataTimeDesc(animalDbId);
        List<SheepBehaviorDetailVO.HistoryRecord> history = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            List<HealthDataEntity> dailyRecords = records.stream()
                .filter(record -> record.getDataTime().toLocalDate().equals(date))
                .collect(Collectors.toList());
            history.add(SheepBehaviorDetailVO.HistoryRecord.builder()
                .date(date.format(DATE_FORMATTER))
                .ruminationTime((int) Math.round(average(dailyRecords.stream().map(HealthDataEntity::getRuminationTime).collect(Collectors.toList()))))
                .feedingCount((int) Math.round(average(dailyRecords.stream().map(HealthDataEntity::getFeedingCount).collect(Collectors.toList()))))
                .ruminationEfficiency((int) Math.round(calculateDailyEfficiency(dailyRecords)))
                .status(resolveHistoryStatus(dailyRecords))
                .build());
        }
        return history;
    }

    private String resolveHistoryStatus(List<HealthDataEntity> records) {
        double avgRumination = average(records.stream().map(HealthDataEntity::getRuminationTime).collect(Collectors.toList()));
        if (avgRumination == 0) {
            return "无数据";
        }
        return avgRumination < getRuminationThreshold() ? "预警" : "正常";
    }

    private AnimalEntity resolveAnimal(String animalId) {
        return animalRepository.findByAnimalId(animalId)
            .orElseThrow(() -> new RuntimeException("牲畜不存在: " + animalId));
    }

    private Map<String, Object> loadDietPlan(String animalId) {
        String raw = userPermissionService.getSystemConfigValue(DIET_PLAN_CONFIG_PREFIX + animalId);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> plan = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
            return plan == null ? null : plan;
        } catch (JsonProcessingException e) {
            log.warn("Failed to read persisted diet plan: animalId={}", animalId, e);
            return null;
        }
    }

    private void saveDietPlanConfig(String animalId, Map<String, Object> plan) {
        try {
            userPermissionService.upsertSystemConfig(
                DIET_PLAN_CONFIG_PREFIX + animalId,
                objectMapper.writeValueAsString(plan),
                "json",
                "Persisted diet plan for " + animalId,
                null
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to save diet plan", e);
        }
    }

    private String resolveBehaviorStatus(AnimalEntity animal) {
        boolean ruminationLow = animal.getDailyRuminationTime() != null && animal.getDailyRuminationTime() < getRuminationThreshold();
        boolean feedingLow = animal.getDailyFeedingCount() != null && animal.getDailyFeedingCount() < getFeedingThreshold();
        if ("abnormal".equals(animal.getBehaviorStatus()) || (ruminationLow && feedingLow)) {
            return "abnormal";
        }
        if ("warning".equals(animal.getBehaviorStatus()) || ruminationLow || feedingLow) {
            return "warning";
        }
        return "normal";
    }

    private int getRuminationThreshold() {
        return getIntConfig("behavior_rumination_threshold", 280);
    }

    private int getFeedingThreshold() {
        return getIntConfig("behavior_feeding_threshold", 15);
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

    private List<String> buildAnimalNutritionAdvice(AnimalEntity animal) {
        List<String> advice = new ArrayList<>();
        if (animal.getDailyRuminationTime() != null && animal.getDailyRuminationTime() < getRuminationThreshold()) {
            advice.add("建议提高粗纤维比例，并增加夜间反刍观察。");
        }
        if (animal.getDailyFeedingCount() != null && animal.getDailyFeedingCount() < getFeedingThreshold()) {
            advice.add("建议拆分饲喂批次，提升单日采食频次。");
        }
        advice.add("保持饮水、槽位清洁，并连续记录 3 天效果。");
        return advice;
    }

    private String buildBehaviorEventDescription(AnimalEntity animal) {
        List<String> issues = new ArrayList<>();
        if (animal.getDailyRuminationTime() != null && animal.getDailyRuminationTime() < getRuminationThreshold()) {
            issues.add("反刍时长不足");
        }
        if (animal.getDailyFeedingCount() != null && animal.getDailyFeedingCount() < getFeedingThreshold()) {
            issues.add("采食次数下降");
        }
        if (issues.isEmpty()) {
            return "行为波动明显，建议重点跟踪。";
        }
        return String.join("、", issues);
    }

    private String buildBehaviorSummaryText(AnimalEntity animal) {
        String behaviorStatus = resolveBehaviorStatus(animal);
        if ("abnormal".equals(behaviorStatus)) {
            return "当前行为异常，建议重点跟踪近 24 小时反刍和采食变化。";
        }
        if ("warning".equals(behaviorStatus)) {
            return "当前处于预警状态，建议增加巡检频次并准备调整饲喂。";
        }
        return "行为表现整体稳定，可继续维持当前饲喂节奏。";
    }

    private String buildAnalysisSummary(List<AnimalEntity> animals) {
        long abnormalCount = animals.stream()
            .filter(animal -> !"normal".equals(resolveBehaviorStatus(animal)))
            .count();
        double avgRumination = animals.stream()
            .map(AnimalEntity::getDailyRuminationTime)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);

        if (abnormalCount == 0) {
            return String.format("当前羊群反刍采食行为整体稳定，平均反刍时长 %.0f 分钟，未发现明显异常个体。", avgRumination);
        }
        return String.format("当前羊群平均反刍时长 %.0f 分钟，存在 %d 只需要重点关注的个体，建议结合详情页制定针对性饲喂方案。", avgRumination, abnormalCount);
    }

    private double calculateDigestiveHealthScore(List<AnimalEntity> animals) {
        return animals.stream()
            .mapToDouble(animal -> {
                double ruminationEfficiency = animal.getRuminationEfficiency() == null ? 80.0 : animal.getRuminationEfficiency();
                double feedingQuality = animal.getFeedingQuality() == null ? 80.0 : animal.getFeedingQuality();
                return ruminationEfficiency * 0.6 + feedingQuality * 0.4;
            })
            .average()
            .orElse(0.0);
    }

    private double calculateDailyEfficiency(List<HealthDataEntity> records) {
        if (records.isEmpty()) {
            return 0.0;
        }
        double avgRumination = average(records.stream().map(HealthDataEntity::getRuminationTime).collect(Collectors.toList()));
        double avgFeeding = average(records.stream().map(HealthDataEntity::getFeedingCount).collect(Collectors.toList()));
        return Math.min(100.0, avgRumination / 3.6 + avgFeeding * 2.5);
    }

    private double calculateDailyHealthScore(List<HealthDataEntity> records) {
        if (records.isEmpty()) {
            return 0.0;
        }
        double avgTemp = average(records.stream().map(HealthDataEntity::getTemperature).collect(Collectors.toList()));
        double avgActivity = average(records.stream().map(HealthDataEntity::getActivityLevel).collect(Collectors.toList()));
        double tempScore = avgTemp == 0.0 ? 80.0 : Math.max(60.0, 100.0 - Math.abs(avgTemp - 39.0) * 20.0);
        double activityScore = Math.min(100.0, avgActivity);
        return tempScore * 0.6 + activityScore * 0.4;
    }

    private int averagePercent(List<Map<String, Object>> pattern, String key) {
        return (int) Math.round(pattern.stream()
            .map(item -> item.get(key))
            .filter(Objects::nonNull)
            .mapToInt(item -> ((Number) item).intValue())
            .average()
            .orElse(0.0));
    }

    private double average(List<? extends Number> values) {
        List<? extends Number> validValues = values.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (validValues.isEmpty()) {
            return 0.0;
        }
        return validValues.stream().mapToDouble(Number::doubleValue).average().orElse(0.0);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private BehaviorSummaryVO createEmptySummary(FarmBaselineVO baseline) {
        return BehaviorSummaryVO.builder()
            .baseline(baseline)
            .avgRuminationTime(0L)
            .ruminationStandardRange("280-360")
            .avgFeedingCount(0L)
            .feedingStandardRange("15-25")
            .abnormalAnimalCount(0)
            .totalAnimalCount(0)
            .abnormalPercent(0.0)
            .normalPercent(0.0)
            .digestiveHealthScore(0.0)
            .build();
    }
}

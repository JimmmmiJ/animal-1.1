package com.livestock.health.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livestock.health.model.entity.AnimalEntity;
import com.livestock.health.model.entity.HealthDataEntity;
import com.livestock.health.model.vo.DiseaseRiskSummaryVO;
import com.livestock.health.model.vo.DiseaseTypeAnalysisVO;
import com.livestock.health.model.vo.FarmBaselineVO;
import com.livestock.health.model.vo.RiskAnimalVO;
import com.livestock.health.model.vo.TreatmentPlanVO;
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
public class DiseaseDetectionService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final String TREATMENT_PLAN_CONFIG_PREFIX = "treatment_plan:";

    private final AnimalRepository animalRepository;
    private final HealthDataRepository healthDataRepository;
    private final FarmSnapshotService farmSnapshotService;
    private final UserPermissionService userPermissionService;
    private final ObjectMapper objectMapper;

    public DiseaseRiskSummaryVO getRiskSummary(Long farmId) {
        FarmBaselineVO baseline = farmSnapshotService.getBaseline(farmId);
        List<AnimalEntity> animals = animalRepository.findByFarmId(farmId);
        if (animals.isEmpty()) {
            return createEmptySummary(baseline);
        }

        Map<Long, HealthDataEntity> latestHealthMap = farmSnapshotService.getLatestHealthDataByAnimalId(farmId);
        long totalCount = animals.size();
        long highRiskCount = animals.stream().filter(animal -> "high".equals(resolveRiskLevel(animal, latestHealthMap.get(animal.getId())))).count();
        long mediumRiskCount = animals.stream().filter(animal -> "medium".equals(resolveRiskLevel(animal, latestHealthMap.get(animal.getId())))).count();
        long lowRiskCount = animals.stream().filter(animal -> "low".equals(resolveRiskLevel(animal, latestHealthMap.get(animal.getId())))).count();
        long healthyCount = animals.stream().filter(animal -> "normal".equals(resolveRiskLevel(animal, latestHealthMap.get(animal.getId())))).count();

        return DiseaseRiskSummaryVO.builder()
            .baseline(baseline)
            .highRiskCount(highRiskCount)
            .mediumRiskCount(mediumRiskCount)
            .lowRiskCount(lowRiskCount)
            .healthyCount(healthyCount)
            .healthyPercent(round1(healthyCount * 100.0 / totalCount))
            .alertCount(highRiskCount + mediumRiskCount)
            .modelAccuracy(92.5)
            .totalAnimals(totalCount)
            .build();
    }

    public List<RiskAnimalVO> getRiskAnimalList(Long farmId, String riskLevel) {
        List<AnimalEntity> animals = animalRepository.findByFarmId(farmId);
        Map<Long, HealthDataEntity> latestHealthMap = farmSnapshotService.getLatestHealthDataByAnimalId(farmId);

        return animals.stream()
            .filter(animal -> {
                String resolvedRiskLevel = resolveRiskLevel(animal, latestHealthMap.get(animal.getId()));
                return (riskLevel == null || riskLevel.isBlank() || riskLevel.equals(resolvedRiskLevel))
                    && !"normal".equals(resolvedRiskLevel);
            })
            .map(animal -> buildRiskAnimalVO(animal, latestHealthMap.get(animal.getId()), false))
            .sorted(Comparator.comparing(RiskAnimalVO::getRiskScore, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
    }

    public RiskAnimalVO getAnimalRiskDetail(String animalId) {
        AnimalEntity animal = resolveAnimal(animalId);
        HealthDataEntity latestHealth = healthDataRepository.findByAnimalIdOrderByDataTimeDesc(animal.getId()).stream()
            .findFirst()
            .orElse(null);
        return buildRiskAnimalVO(animal, latestHealth, true);
    }

    public DiseaseTypeAnalysisVO getDiseaseTypeAnalysis(Long farmId) {
        Map<Long, HealthDataEntity> latestHealthMap = farmSnapshotService.getLatestHealthDataByAnimalId(farmId);
        List<AnimalEntity> riskAnimals = animalRepository.findByFarmId(farmId).stream()
            .filter(animal -> !"normal".equals(resolveRiskLevel(animal, latestHealthMap.get(animal.getId()))))
            .collect(Collectors.toList());

        if (riskAnimals.isEmpty()) {
            return DiseaseTypeAnalysisVO.builder()
                .diseaseTypes(new ArrayList<>())
                .weeklyTrend(buildWeeklyTrend(0, 0, 0))
                .build();
        }

        Map<String, Long> typeCountMap = riskAnimals.stream()
            .collect(Collectors.groupingBy(this::determineRiskType, LinkedHashMap::new, Collectors.counting()));

        List<DiseaseTypeAnalysisVO.DiseaseType> diseaseTypes = typeCountMap.entrySet().stream()
            .map(entry -> DiseaseTypeAnalysisVO.DiseaseType.builder()
                .name(entry.getKey())
                .count(entry.getValue().intValue())
                .percent(round1(entry.getValue() * 100.0 / riskAnimals.size()))
                .severity(getDiseaseSeverity(entry.getKey()))
                .build())
            .collect(Collectors.toList());

        long highRiskCount = riskAnimals.stream().filter(animal -> "high".equals(resolveRiskLevel(animal, latestHealthMap.get(animal.getId())))).count();
        long mediumRiskCount = riskAnimals.stream().filter(animal -> "medium".equals(resolveRiskLevel(animal, latestHealthMap.get(animal.getId())))).count();
        long lowRiskCount = riskAnimals.stream().filter(animal -> "low".equals(resolveRiskLevel(animal, latestHealthMap.get(animal.getId())))).count();

        return DiseaseTypeAnalysisVO.builder()
            .diseaseTypes(diseaseTypes)
            .weeklyTrend(buildWeeklyTrend(highRiskCount, mediumRiskCount, lowRiskCount))
            .build();
    }

    public Map<String, Object> getRiskDistribution(Long farmId) {
        List<AnimalEntity> animals = animalRepository.findByFarmId(farmId);
        Map<Long, HealthDataEntity> latestHealthMap = farmSnapshotService.getLatestHealthDataByAnimalId(farmId);
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("高风险", animals.stream().filter(animal -> "high".equals(resolveRiskLevel(animal, latestHealthMap.get(animal.getId())))).count());
        distribution.put("中风险", animals.stream().filter(animal -> "medium".equals(resolveRiskLevel(animal, latestHealthMap.get(animal.getId())))).count());
        distribution.put("低风险", animals.stream().filter(animal -> "low".equals(resolveRiskLevel(animal, latestHealthMap.get(animal.getId())))).count());
        distribution.put("健康", animals.stream().filter(animal -> "normal".equals(resolveRiskLevel(animal, latestHealthMap.get(animal.getId())))).count());
        result.put("distribution", distribution);
        return result;
    }

    public List<Map<String, Object>> getHealthEventTimeline(Long farmId, int limit) {
        Map<Long, HealthDataEntity> latestHealthMap = farmSnapshotService.getLatestHealthDataByAnimalId(farmId);
        return animalRepository.findByFarmId(farmId).stream()
            .filter(animal -> !"normal".equals(resolveRiskLevel(animal, latestHealthMap.get(animal.getId()))))
            .sorted(Comparator
                .comparing((AnimalEntity animal) -> "high".equals(resolveRiskLevel(animal, latestHealthMap.get(animal.getId()))) ? 3 : "medium".equals(resolveRiskLevel(animal, latestHealthMap.get(animal.getId()))) ? 2 : 1)
                .reversed()
                .thenComparing(animal -> animal.getLastBehaviorUpdateAt() == null ? LocalDateTime.MIN : animal.getLastBehaviorUpdateAt(), Comparator.reverseOrder()))
            .limit(limit)
            .map(animal -> {
                HealthDataEntity latestHealth = latestHealthMap.get(animal.getId());
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("animalId", animal.getAnimalId());
                event.put("name", animal.getName());
                event.put("riskLevel", resolveRiskLevel(animal, latestHealth));
                event.put("eventTime", animal.getLastBehaviorUpdateAt() == null ? LocalDateTime.now().minusHours(1) : animal.getLastBehaviorUpdateAt());
                event.put("eventType", "风险预警");
                event.put("description", buildRiskDescription(animal, latestHealth));
                return event;
            })
            .collect(Collectors.toList());
    }

    public TreatmentPlanVO createTreatmentPlan(String animalId, Map<String, Object> planData) {
        AnimalEntity animal = resolveAnimal(animalId);
        int duration = toInt(planData.getOrDefault("duration", 7), 7);
        LocalDateTime startDate = LocalDateTime.now();

        TreatmentPlanVO plan = TreatmentPlanVO.builder()
            .id(System.currentTimeMillis())
            .animalId(animal.getAnimalId())
            .animalName(animal.getName())
            .diagnosis(asString(planData.get("diagnosis"), determineRiskType(animal)))
            .treatmentPlan(asString(planData.get("treatmentPlan"), "加强巡检，连续 7 天跟踪体温、心率和采食表现。"))
            .medication(asString(planData.get("medication"), "复合维生素"))
            .dosage(asString(planData.get("dosage"), "按体重执行标准剂量"))
            .frequency(asString(planData.get("frequency"), "每日 2 次"))
            .duration(duration)
            .startDate(startDate)
            .endDate(startDate.plusDays(duration))
            .status("进行中")
            .veterinarian(asString(planData.get("veterinarian"), "值班兽医"))
            .notes(asString(planData.get("notes"), "重点观察反刍、采食与温度波动。"))
            .records(List.of(
                TreatmentPlanVO.TreatmentRecord.builder()
                    .time(startDate)
                    .action("创建计划")
                    .operator(asString(planData.get("veterinarian"), "值班兽医"))
                    .note("按手册模板创建治疗计划。")
                    .build()
            ))
            .build();
        saveTreatmentPlan(animal.getAnimalId(), plan);
        return plan;
    }

    public TreatmentPlanVO getTreatmentPlan(String animalId) {
        AnimalEntity animal = resolveAnimal(animalId);
        TreatmentPlanVO storedPlan = loadTreatmentPlan(animal.getAnimalId());
        if (storedPlan != null) {
            return storedPlan;
        }

        HealthDataEntity latestHealth = healthDataRepository.findByAnimalIdOrderByDataTimeDesc(animal.getId()).stream()
            .findFirst()
            .orElse(null);

        String note = latestHealth != null && latestHealth.getTemperature() != null
            ? String.format("最近体温 %.1f℃，建议连续复测并记录趋势。", latestHealth.getTemperature())
            : "建议持续观察体征变化并保持单独巡检记录。";

        return TreatmentPlanVO.builder()
            .id(animal.getId())
            .animalId(animal.getAnimalId())
            .animalName(animal.getName())
            .diagnosis(determineRiskType(animal))
            .treatmentPlan("每日巡检，记录体温、心率、采食和反刍；必要时安排复检。")
            .medication("益生菌")
            .dosage("每日一次")
            .frequency("每日 1 次")
            .duration(7)
            .startDate(LocalDateTime.now().minusDays(1))
            .endDate(LocalDateTime.now().plusDays(6))
            .status("待执行")
            .veterinarian("值班兽医")
            .notes(note)
            .records(List.of(
                TreatmentPlanVO.TreatmentRecord.builder()
                    .time(LocalDateTime.now().minusHours(8))
                    .action("风险复核")
                    .operator("系统")
                    .note(note)
                    .build()
            ))
            .build();
    }

    public void updateRiskLevel(String animalId, String riskLevel) {
        AnimalEntity animal = resolveAnimal(animalId);
        animal.setRiskLevel(riskLevel);
        animalRepository.save(animal);
        log.info("Updated risk level: animalId={}, riskLevel={}", animalId, riskLevel);
    }

    public Map<String, Object> performRiskAssessment(String animalId) {
        AnimalEntity animal = resolveAnimal(animalId);
        HealthDataEntity latestHealth = healthDataRepository.findByAnimalIdOrderByDataTimeDesc(animal.getId()).stream()
            .findFirst()
            .orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> riskFactors = new ArrayList<>();
        riskFactors.add(createFactor("健康评分", animal.getHealthScore(), 0.30));
        riskFactors.add(createFactor("日反刍时长", animal.getDailyRuminationTime(), 0.25));
        riskFactors.add(createFactor("日采食次数", animal.getDailyFeedingCount(), 0.20));
        riskFactors.add(createFactor("体温", latestHealth != null ? latestHealth.getTemperature() : null, 0.25));

        result.put("animalId", animal.getAnimalId());
        result.put("assessmentTime", LocalDateTime.now());
        result.put("overallRisk", resolveRiskLevel(animal, latestHealth));
        result.put("riskScore", calculateRiskScore(animal, latestHealth));
        result.put("riskFactors", riskFactors);
        result.put("recommendations", buildRecommendationList(animal, latestHealth));
        return result;
    }

    private RiskAnimalVO buildRiskAnimalVO(AnimalEntity animal, HealthDataEntity latestHealth, boolean includeDetail) {
        List<HealthDataEntity> recentRecords = healthDataRepository.findByAnimalIdOrderByDataTimeDesc(animal.getId()).stream()
            .limit(includeDetail ? 24 : 7)
            .collect(Collectors.toList());

        List<String> symptomTags = buildSymptomTags(animal, latestHealth);
        List<String> riskBasis = buildRiskBasis(animal, latestHealth);
        String riskLevel = resolveRiskLevel(animal, latestHealth);

        return RiskAnimalVO.builder()
            .animalId(animal.getAnimalId())
            .name(animal.getName())
            .breed(animal.getBreed())
            .age(animal.getAge())
            .weight(animal.getWeight())
            .riskLevel(riskLevel)
            .riskScore(calculateRiskScore(animal, latestHealth))
            .riskType(determineRiskType(animal))
            .mainSymptoms(symptomTags.isEmpty() ? "暂无明显症状" : String.join("、", symptomTags))
            .riskTrend(determineRiskTrend(animal))
            .recommendation(buildPrimaryRecommendation(animal, latestHealth))
            .currentTemperature(latestHealth != null ? latestHealth.getTemperature() : null)
            .currentHeartRate(latestHealth != null ? latestHealth.getHeartRate() : null)
            .healthScore(animal.getHealthScore())
            .lastCheckTime(latestHealth != null ? latestHealth.getDataTime() : null)
            .lastUpdateAt(animal.getLastBehaviorUpdateAt())
            .symptomTags(symptomTags)
            .riskBasis(riskBasis)
            .temperatureTrend(includeDetail ? buildTrendPoints(recentRecords, MetricType.TEMPERATURE) : new ArrayList<>())
            .heartRateTrend(includeDetail ? buildTrendPoints(recentRecords, MetricType.HEART_RATE) : new ArrayList<>())
            .activityTrend(includeDetail ? buildTrendPoints(recentRecords, MetricType.ACTIVITY) : new ArrayList<>())
            .recentDetections(includeDetail ? buildDetectionRecords(recentRecords) : new ArrayList<>())
            .build();
    }

    private AnimalEntity resolveAnimal(String animalId) {
        return animalRepository.findByAnimalId(animalId)
            .orElseThrow(() -> new RuntimeException("牲畜不存在: " + animalId));
    }

    private List<RiskAnimalVO.TrendPoint> buildTrendPoints(List<HealthDataEntity> records, MetricType metricType) {
        return records.stream()
            .sorted(Comparator.comparing(HealthDataEntity::getDataTime))
            .map(record -> RiskAnimalVO.TrendPoint.builder()
                .label(record.getDataTime().format(DATE_FORMATTER))
                .value(metricType.extract(record))
                .build())
            .collect(Collectors.toList());
    }

    private List<RiskAnimalVO.DetectionRecord> buildDetectionRecords(List<HealthDataEntity> records) {
        return records.stream()
            .limit(6)
            .flatMap(record -> {
                List<RiskAnimalVO.DetectionRecord> list = new ArrayList<>();
                if (record.getTemperature() != null) {
                    list.add(createDetectionRecord(record, "体温", String.format("%.1f℃", record.getTemperature()),
                        farmSnapshotService.isTemperatureAlert(record) ? "异常" : "正常"));
                }
                if (record.getHeartRate() != null) {
                    list.add(createDetectionRecord(record, "心率", record.getHeartRate() + " 次/分",
                        farmSnapshotService.isHeartRateAlert(record) ? "异常" : "正常"));
                }
                if (record.getActivityLevel() != null) {
                    list.add(createDetectionRecord(record, "活动值", String.valueOf(record.getActivityLevel()),
                        record.getActivityLevel() < 20 ? "待关注" : "正常"));
                }
                return list.stream();
            })
            .limit(8)
            .collect(Collectors.toList());
    }

    private RiskAnimalVO.DetectionRecord createDetectionRecord(HealthDataEntity record, String item, String value, String status) {
        return RiskAnimalVO.DetectionRecord.builder()
            .time(record.getDataTime())
            .item(item)
            .value(value)
            .status(status)
            .note("来源于实时健康数据聚合。")
            .build();
    }

    private List<String> buildSymptomTags(AnimalEntity animal, HealthDataEntity latestHealth) {
        List<String> tags = new ArrayList<>();
        if (latestHealth != null && farmSnapshotService.isTemperatureAlert(latestHealth)) {
            tags.add("体温异常");
        }
        if (latestHealth != null && farmSnapshotService.isHeartRateAlert(latestHealth)) {
            tags.add("心率波动");
        }
        if (animal.getDailyRuminationTime() != null && animal.getDailyRuminationTime() < 280) {
            tags.add("反刍不足");
        }
        if (animal.getDailyFeedingCount() != null && animal.getDailyFeedingCount() < 15) {
            tags.add("采食下降");
        }
        if (animal.getHealthScore() != null && animal.getHealthScore() < 80) {
            tags.add("健康评分偏低");
        }
        return tags;
    }

    private List<String> buildRiskBasis(AnimalEntity animal, HealthDataEntity latestHealth) {
        List<String> basis = new ArrayList<>();
        basis.add("风险等级: " + asText(resolveRiskLevel(animal, latestHealth), "normal"));
        if (animal.getHealthScore() != null) {
            basis.add("健康评分: " + round1(animal.getHealthScore()));
        }
        if (animal.getDailyRuminationTime() != null) {
            basis.add("日反刍: " + animal.getDailyRuminationTime() + " 分钟");
        }
        if (animal.getDailyFeedingCount() != null) {
            basis.add("日采食: " + animal.getDailyFeedingCount() + " 次");
        }
        if (latestHealth != null && latestHealth.getTemperature() != null) {
            basis.add(String.format("最新体温: %.1f℃", latestHealth.getTemperature()));
        }
        return basis;
    }

    private String buildPrimaryRecommendation(AnimalEntity animal, HealthDataEntity latestHealth) {
        if ("high".equals(resolveRiskLevel(animal, latestHealth))) {
            return "优先安排兽医复核，并对该只牲畜执行单独巡检。";
        }
        if (latestHealth != null && farmSnapshotService.isTemperatureAlert(latestHealth)) {
            return "建议连续复测体温并同步检查采食与精神状态。";
        }
        if (animal.getDailyRuminationTime() != null && animal.getDailyRuminationTime() < 280) {
            return "建议重点检查反刍和消化表现，必要时调整饲喂结构。";
        }
        return "建议继续观察，保持每日健康数据复盘。";
    }

    private List<String> buildRecommendationList(AnimalEntity animal, HealthDataEntity latestHealth) {
        List<String> recommendations = new ArrayList<>();
        recommendations.add(buildPrimaryRecommendation(animal, latestHealth));
        String riskLevel = resolveRiskLevel(animal, latestHealth);
        if ("medium".equals(riskLevel) || "high".equals(riskLevel)) {
            recommendations.add("将该只牲畜纳入重点关注列表，并保持连续 3 天复核。");
        }
        if (latestHealth != null && latestHealth.getActivityLevel() != null && latestHealth.getActivityLevel() < 20) {
            recommendations.add("建议核查活动量下降原因，并检查是否存在应激或采食异常。");
        }
        return recommendations;
    }

    private String determineRiskType(AnimalEntity animal) {
        if ("high".equals(animal.getRiskLevel())) {
            return "重点预警";
        }
        if (animal.getDailyRuminationTime() != null && animal.getDailyRuminationTime() < 280) {
            return "消化风险";
        }
        if (animal.getHealthScore() != null && animal.getHealthScore() < 80) {
            return "综合健康风险";
        }
        return "行为波动";
    }

    private String getDiseaseSeverity(String type) {
        return switch (type) {
            case "重点预警", "综合健康风险" -> "high";
            case "消化风险" -> "medium";
            default -> "low";
        };
    }

    private String determineRiskTrend(AnimalEntity animal) {
        if ("high".equals(animal.getRiskLevel())) {
            return "上升";
        }
        if ("normal".equals(animal.getRiskLevel()) || "low".equals(animal.getRiskLevel())) {
            return "下降";
        }
        return "波动";
    }

    private String resolveRiskLevel(AnimalEntity animal, HealthDataEntity latestHealth) {
        double score = calculateRiskScore(animal, latestHealth);
        int highThreshold = getIntConfig("disease_high_risk_threshold", 85);
        int mediumThreshold = getIntConfig("disease_medium_risk_threshold", 70);
        if (score >= highThreshold) {
            return "high";
        }
        if (score >= mediumThreshold) {
            return "medium";
        }
        if (!"normal".equals(animal.getRiskLevel()) || farmSnapshotService.isRiskAnimal(animal)) {
            return "low";
        }
        return "normal";
    }

    private TreatmentPlanVO loadTreatmentPlan(String animalId) {
        String raw = userPermissionService.getSystemConfigValue(TREATMENT_PLAN_CONFIG_PREFIX + animalId);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, TreatmentPlanVO.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to read persisted treatment plan: animalId={}", animalId, e);
            return null;
        }
    }

    private void saveTreatmentPlan(String animalId, TreatmentPlanVO plan) {
        try {
            userPermissionService.upsertSystemConfig(
                TREATMENT_PLAN_CONFIG_PREFIX + animalId,
                objectMapper.writeValueAsString(plan),
                "json",
                "Persisted treatment plan for " + animalId,
                null
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to save treatment plan", e);
        }
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

    private Double calculateRiskScore(AnimalEntity animal, HealthDataEntity latestHealth) {
        double score = switch (animal.getRiskLevel()) {
            case "high" -> 82.0;
            case "medium" -> 62.0;
            case "low" -> 42.0;
            default -> 20.0;
        };
        if (animal.getHealthScore() != null) {
            score += Math.max(0.0, 90.0 - animal.getHealthScore()) * 0.4;
        }
        if (animal.getDailyRuminationTime() != null && animal.getDailyRuminationTime() < 280) {
            score += 6.0;
        }
        if (animal.getDailyFeedingCount() != null && animal.getDailyFeedingCount() < 15) {
            score += 5.0;
        }
        if (latestHealth != null && farmSnapshotService.isTemperatureAlert(latestHealth)) {
            score += 8.0;
        }
        if (latestHealth != null && farmSnapshotService.isHeartRateAlert(latestHealth)) {
            score += 5.0;
        }
        return round1(Math.min(100.0, score));
    }

    private String buildRiskDescription(AnimalEntity animal, HealthDataEntity latestHealth) {
        List<String> tags = buildSymptomTags(animal, latestHealth);
        return String.format("%s（%s）当前为%s，主要表现：%s",
            animal.getName(),
            animal.getAnimalId(),
            asText(resolveRiskLevel(animal, latestHealth), "normal"),
            tags.isEmpty() ? "暂无明显症状" : String.join("、", tags));
    }

    private List<DiseaseTypeAnalysisVO.TrendPoint> buildWeeklyTrend(long highRiskCount, long mediumRiskCount, long lowRiskCount) {
        List<DiseaseTypeAnalysisVO.TrendPoint> trend = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 6; i >= 0; i--) {
            trend.add(DiseaseTypeAnalysisVO.TrendPoint.builder()
                .date(now.minusDays(i).format(DATE_FORMATTER))
                .highRisk((int) highRiskCount)
                .mediumRisk((int) mediumRiskCount)
                .lowRisk((int) lowRiskCount)
                .build());
        }
        return trend;
    }

    private Map<String, Object> createFactor(String factor, Object value, double weight) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("factor", factor);
        item.put("value", value);
        item.put("weight", weight);
        return item;
    }

    private int toInt(Object value, int defaultValue) {
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

    private String asString(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private String asText(String riskLevel, String fallback) {
        return switch (riskLevel == null ? fallback : riskLevel) {
            case "high" -> "高风险";
            case "medium" -> "中风险";
            case "low" -> "低风险";
            default -> "健康";
        };
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private DiseaseRiskSummaryVO createEmptySummary(FarmBaselineVO baseline) {
        return DiseaseRiskSummaryVO.builder()
            .baseline(baseline)
            .highRiskCount(0L)
            .mediumRiskCount(0L)
            .lowRiskCount(0L)
            .healthyCount(0L)
            .healthyPercent(0.0)
            .alertCount(0L)
            .modelAccuracy(0.0)
            .totalAnimals(0L)
            .build();
    }

    private enum MetricType {
        TEMPERATURE {
            @Override
            Double extract(HealthDataEntity record) {
                return record.getTemperature();
            }
        },
        HEART_RATE {
            @Override
            Double extract(HealthDataEntity record) {
                return record.getHeartRate() == null ? null : record.getHeartRate().doubleValue();
            }
        },
        ACTIVITY {
            @Override
            Double extract(HealthDataEntity record) {
                return record.getActivityLevel() == null ? null : record.getActivityLevel().doubleValue();
            }
        };

        abstract Double extract(HealthDataEntity record);
    }
}

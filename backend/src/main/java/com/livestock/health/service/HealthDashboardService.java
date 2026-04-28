package com.livestock.health.service;

import com.livestock.health.model.entity.AnimalEntity;
import com.livestock.health.model.entity.DeviceEntity;
import com.livestock.health.model.entity.HealthDataEntity;
import com.livestock.health.model.vo.AnimalHealthDetailVO;
import com.livestock.health.model.vo.DashboardActivityHeatmapVO;
import com.livestock.health.model.vo.DashboardChartsVO;
import com.livestock.health.model.vo.DashboardStatusSegmentVO;
import com.livestock.health.model.vo.DashboardTemperatureTrendVO;
import com.livestock.health.model.vo.FarmBaselineVO;
import com.livestock.health.model.vo.HealthDashboardSummaryVO;
import com.livestock.health.model.vo.HealthHeatmapVO;
import com.livestock.health.model.vo.HealthInsightVO;
import com.livestock.health.repository.AnimalRepository;
import com.livestock.health.repository.DeviceRepository;
import com.livestock.health.repository.HealthDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthDashboardService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> DASHBOARD_TIME_BUCKETS = Arrays.asList("00:00", "04:00", "08:00", "12:00", "16:00", "20:00");
    private static final List<DashboardActivityHeatmapVO.MetricMeta> DASHBOARD_HEATMAP_METRICS = Arrays.asList(
        DashboardActivityHeatmapVO.MetricMeta.builder().key("activity").label("活动").unit("点").build(),
        DashboardActivityHeatmapVO.MetricMeta.builder().key("rumination").label("反刍").unit("分钟").build(),
        DashboardActivityHeatmapVO.MetricMeta.builder().key("feeding").label("采食").unit("次").build(),
        DashboardActivityHeatmapVO.MetricMeta.builder().key("resting").label("休息").unit("分钟").build()
    );

    private final AnimalRepository animalRepository;
    private final DeviceRepository deviceRepository;
    private final HealthDataRepository healthDataRepository;
    private final FarmSnapshotService farmSnapshotService;

    public HealthDashboardSummaryVO getDashboardSummary(Long farmId) {
        FarmBaselineVO baseline = farmSnapshotService.getBaseline(farmId);
        List<AnimalEntity> animals = animalRepository.findByFarmId(farmId);
        if (animals.isEmpty()) {
            return createEmptySummary(baseline);
        }

        Map<Long, HealthDataEntity> latestHealthMap = farmSnapshotService.getLatestHealthDataByAnimalId(farmId);
        int totalAnimals = animals.size();
        int healthyCount = (int) animals.stream().filter(farmSnapshotService::isHealthyAnimal).count();
        int attentionCount = (int) animals.stream().filter(this::isDashboardAttention).count();
        int alertCount = (int) animals.stream().filter(this::isDashboardHighRisk).count();
        long highRiskCount = animals.stream().filter(animal -> "high".equals(animal.getRiskLevel())).count();
        long mediumRiskCount = animals.stream().filter(animal -> "medium".equals(animal.getRiskLevel())).count();
        long inEstrusCount = animals.stream().filter(animal -> "estrus".equals(animal.getEstrusStatus())).count();

        double avgHealthScore = farmSnapshotService.averageHealthScore(animals);
        double avgTemperature = latestHealthMap.values().stream()
            .map(HealthDataEntity::getTemperature)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        int avgActivity = (int) Math.round(latestHealthMap.values().stream()
            .map(HealthDataEntity::getActivityLevel)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0));
        long avgRuminationTime = Math.round(animals.stream()
            .map(AnimalEntity::getDailyRuminationTime)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0));
        double ruminationPassRate = round1(
            animals.stream()
                .filter(animal -> animal.getDailyRuminationTime() != null
                    && animal.getDailyRuminationTime() >= 280
                    && animal.getDailyRuminationTime() <= 360)
                .count() * 100.0 / animals.size()
        );

        return HealthDashboardSummaryVO.builder()
            .baseline(baseline)
            .totalAnimals(totalAnimals)
            .healthyCount(healthyCount)
            .healthyPercent(round1(healthyCount * 100.0 / totalAnimals))
            .attentionCount(attentionCount)
            .alertCount(alertCount)
            .highRiskCount(highRiskCount)
            .mediumRiskCount(mediumRiskCount)
            .inEstrusCount(inEstrusCount)
            .avgHealthScore(round1(avgHealthScore))
            .avgTemperature(round1(avgTemperature))
            .avgActivity(avgActivity)
            .avgRuminationTime(avgRuminationTime)
            .ruminationPassRate(ruminationPassRate)
            .weeklyTrend(generateWeeklyTrend(avgHealthScore))
            .build();
    }

    public List<AnimalHealthDetailVO> getFocusAnimals(Long farmId, int limit) {
        Map<Long, HealthDataEntity> latestHealthMap = farmSnapshotService.getLatestHealthDataByAnimalId(farmId);
        return animalRepository.findByFarmId(farmId).stream()
            .filter(animal -> !farmSnapshotService.isHealthyAnimal(animal))
            .sorted(Comparator
                .comparingInt(this::getPriority)
                .reversed()
                .thenComparing(animal -> animal.getHealthScore() == null ? 0.0 : animal.getHealthScore()))
            .limit(limit)
            .map(animal -> buildHealthDetailVO(animal, latestHealthMap.get(animal.getId()), false))
            .collect(Collectors.toList());
    }

    public DashboardChartsVO getDashboardCharts(Long farmId) {
        FarmBaselineVO baseline = farmSnapshotService.getBaseline(farmId);
        List<AnimalEntity> animals = animalRepository.findByFarmId(farmId);
        List<HealthDataEntity> recentHealthData = getRecentDashboardHealthData(farmId);

        return DashboardChartsVO.builder()
            .baseline(baseline)
            .healthStatusDistribution(buildHealthStatusDistribution(animals))
            .temperatureTrend(buildTemperatureTrend(recentHealthData))
            .activityHeatmap(buildActivityHeatmap(recentHealthData))
            .build();
    }

    public HealthHeatmapVO getHealthHeatmap(Long farmId) {
        List<AnimalEntity> animals = animalRepository.findByFarmId(farmId);
        List<HealthHeatmapVO.HeatmapCell> cells = new ArrayList<>();
        int gridSize = 12;

        for (int index = 0; index < animals.size() && index < gridSize * gridSize; index++) {
            AnimalEntity animal = animals.get(index);
            cells.add(HealthHeatmapVO.HeatmapCell.builder()
                .x(index % gridSize)
                .y(index / gridSize)
                .animalId(animal.getAnimalId())
                .healthScore(animal.getHealthScore() == null ? 0.0 : animal.getHealthScore())
                .status(resolveHealthStatus(animal))
                .color(resolveHealthColor(animal.getHealthScore()))
                .build());
        }

        return HealthHeatmapVO.builder()
            .cells(cells)
            .gridSize(gridSize)
            .build();
    }

    public Map<String, Object> getHealthDistribution(Long farmId) {
        List<AnimalEntity> animals = animalRepository.findByFarmId(farmId);
        Map<String, Long> healthDistribution = new LinkedHashMap<>();
        healthDistribution.put("优秀", animals.stream().filter(animal -> animal.getHealthScore() != null && animal.getHealthScore() >= 90).count());
        healthDistribution.put("良好", animals.stream().filter(animal -> animal.getHealthScore() != null && animal.getHealthScore() >= 80 && animal.getHealthScore() < 90).count());
        healthDistribution.put("一般", animals.stream().filter(animal -> animal.getHealthScore() != null && animal.getHealthScore() >= 70 && animal.getHealthScore() < 80).count());
        healthDistribution.put("需关注", animals.stream().filter(animal -> animal.getHealthScore() != null && animal.getHealthScore() < 70).count());

        Map<String, Long> riskDistribution = new LinkedHashMap<>();
        riskDistribution.put("高风险", animals.stream().filter(animal -> "high".equals(animal.getRiskLevel())).count());
        riskDistribution.put("中风险", animals.stream().filter(animal -> "medium".equals(animal.getRiskLevel())).count());
        riskDistribution.put("低风险", animals.stream().filter(animal -> "low".equals(animal.getRiskLevel())).count());
        riskDistribution.put("健康", animals.stream().filter(animal -> "normal".equals(animal.getRiskLevel())).count());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("healthDistribution", healthDistribution);
        result.put("riskDistribution", riskDistribution);
        return result;
    }

    public List<HealthInsightVO> getHealthInsights(Long farmId) {
        List<AnimalEntity> animals = animalRepository.findByFarmId(farmId);
        List<HealthInsightVO> insights = new ArrayList<>();

        long lowScoreCount = animals.stream()
            .filter(animal -> animal.getHealthScore() != null && animal.getHealthScore() < 85)
            .count();
        if (lowScoreCount > 0) {
            insights.add(HealthInsightVO.builder()
                .title("群体健康评分偏低")
                .type("health")
                .severity(lowScoreCount >= 10 ? "high" : "warning")
                .description(String.format("当前有 %d 只牲畜健康评分低于 85 分。", lowScoreCount))
                .affectedAnimals(animals.stream()
                    .filter(animal -> animal.getHealthScore() != null && animal.getHealthScore() < 85)
                    .limit(5)
                    .map(AnimalEntity::getAnimalId)
                    .collect(Collectors.toList()))
                .recommendation("建议优先复核低评分个体的体温、心率和采食表现。")
                .confidence(0.90)
                .build());
        }

        long abnormalBehaviorCount = animals.stream()
            .filter(animal -> "abnormal".equals(animal.getBehaviorStatus()))
            .count();
        if (abnormalBehaviorCount > 0) {
            insights.add(HealthInsightVO.builder()
                .title("行为异常预警")
                .type("behavior")
                .severity("high")
                .description(String.format("检测到 %d 只牲畜存在明显行为异常。", abnormalBehaviorCount))
                .affectedAnimals(animals.stream()
                    .filter(animal -> "abnormal".equals(animal.getBehaviorStatus()))
                    .limit(5)
                    .map(AnimalEntity::getAnimalId)
                    .collect(Collectors.toList()))
                .recommendation("建议结合反刍、采食和活动量详情进行逐只复核。")
                .confidence(0.92)
                .build());
        }

        long estrusCount = animals.stream()
            .filter(animal -> "estrus".equals(animal.getEstrusStatus()) || (animal.getEstrusProbability() != null && animal.getEstrusProbability() >= 80))
            .count();
        if (estrusCount > 0) {
            insights.add(HealthInsightVO.builder()
                .title("发情窗口提醒")
                .type("breeding")
                .severity("info")
                .description(String.format("当前有 %d 只母畜进入发情相关窗口。", estrusCount))
                .affectedAnimals(animals.stream()
                    .filter(animal -> "estrus".equals(animal.getEstrusStatus()) || (animal.getEstrusProbability() != null && animal.getEstrusProbability() >= 80))
                    .limit(5)
                    .map(AnimalEntity::getAnimalId)
                    .collect(Collectors.toList()))
                .recommendation("建议同步查看配种计划并保持连续观察。")
                .confidence(0.88)
                .build());
        }

        if (insights.isEmpty()) {
            insights.add(HealthInsightVO.builder()
                .title("群体健康状态稳定")
                .type("positive")
                .severity("info")
                .description("当前羊群整体健康状态稳定，未发现显著异常。")
                .affectedAnimals(new ArrayList<>())
                .recommendation("建议保持现有巡检频率并继续按日复盘。")
                .confidence(0.95)
                .build());
        }

        return insights;
    }

    public AnimalHealthDetailVO getAnimalHealthDetail(String animalId) {
        AnimalEntity animal = animalRepository.findByAnimalId(animalId)
            .orElseThrow(() -> new RuntimeException("牲畜不存在: " + animalId));
        HealthDataEntity latest = healthDataRepository.findByAnimalIdOrderByDataTimeDesc(animal.getId()).stream()
            .findFirst()
            .orElse(null);
        return buildHealthDetailVO(animal, latest, true);
    }

    public List<Map<String, Object>> getHealthRanking(Long farmId, String orderBy, int limit) {
        Comparator<AnimalEntity> comparator = switch (orderBy) {
            case "ruminationTime" -> Comparator.comparing(animal -> animal.getDailyRuminationTime() == null ? 0 : animal.getDailyRuminationTime());
            case "feedingCount" -> Comparator.comparing(animal -> animal.getDailyFeedingCount() == null ? 0 : animal.getDailyFeedingCount());
            default -> Comparator.comparing(animal -> animal.getHealthScore() == null ? 0.0 : animal.getHealthScore());
        };

        return animalRepository.findByFarmId(farmId).stream()
            .sorted(comparator.reversed())
            .limit(limit)
            .map(animal -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("animalId", animal.getAnimalId());
                item.put("name", animal.getName());
                item.put("breed", animal.getBreed());
                item.put("healthScore", animal.getHealthScore());
                item.put("ruminationTime", animal.getDailyRuminationTime());
                item.put("feedingCount", animal.getDailyFeedingCount());
                return item;
            })
            .collect(Collectors.toList());
    }

    private AnimalHealthDetailVO buildHealthDetailVO(AnimalEntity animal, HealthDataEntity latestHealth, boolean includeDetail) {
        List<HealthDataEntity> recentData = healthDataRepository.findByAnimalIdOrderByDataTimeDesc(animal.getId()).stream()
            .limit(includeDetail ? 16 : 6)
            .collect(Collectors.toList());
        DeviceEntity device = animal.getDeviceId() == null ? null : deviceRepository.findById(animal.getDeviceId()).orElse(null);

        return AnimalHealthDetailVO.builder()
            .animalId(animal.getAnimalId())
            .name(animal.getName())
            .breed(animal.getBreed())
            .deviceId(device != null ? device.getDeviceId() : null)
            .age(animal.getAge())
            .weight(animal.getWeight())
            .healthScore(animal.getHealthScore())
            .healthStatus(resolveHealthStatus(animal))
            .riskLevel(animal.getRiskLevel())
            .behaviorStatus(animal.getBehaviorStatus())
            .estrusStatus(animal.getEstrusStatus())
            .currentTemperature(latestHealth != null ? latestHealth.getTemperature() : null)
            .currentHeartRate(latestHealth != null ? latestHealth.getHeartRate() : null)
            .currentActivity(latestHealth != null ? latestHealth.getActivityLevel() : null)
            .dailyRuminationTime(animal.getDailyRuminationTime())
            .dailyFeedingCount(animal.getDailyFeedingCount())
            .focusReason(buildFocusReason(animal))
            .temperatureTrend(includeDetail ? buildTrendPoints(recentData, MetricType.TEMPERATURE) : new ArrayList<>())
            .activityTrend(includeDetail ? buildTrendPoints(recentData, MetricType.ACTIVITY) : new ArrayList<>())
            .interventionSuggestions(includeDetail ? buildInterventionSuggestions(animal, latestHealth) : new ArrayList<>())
            .recentRecords(includeDetail ? buildHealthRecords(recentData) : new ArrayList<>())
            .build();
    }

    private List<HealthDataEntity> getRecentDashboardHealthData(Long farmId) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(24);
        return healthDataRepository.findByFarmIdAndDataTimeBetweenOrderByDataTimeDesc(farmId, startTime, endTime);
    }

    private List<DashboardStatusSegmentVO> buildHealthStatusDistribution(List<AnimalEntity> animals) {
        long highRiskCount = animals.stream().filter(this::isDashboardHighRisk).count();
        long attentionCount = animals.stream().filter(this::isDashboardAttention).count();
        long healthyCount = Math.max(0, animals.size() - highRiskCount - attentionCount);

        return List.of(
            DashboardStatusSegmentVO.builder().key("healthy").label("健康").value(healthyCount).build(),
            DashboardStatusSegmentVO.builder().key("attention").label("需关注").value(attentionCount).build(),
            DashboardStatusSegmentVO.builder().key("highRisk").label("高风险").value(highRiskCount).build()
        );
    }

    private DashboardTemperatureTrendVO buildTemperatureTrend(List<HealthDataEntity> recentHealthData) {
        Map<String, List<HealthDataEntity>> grouped = groupRecordsByBucket(recentHealthData);
        List<DashboardTemperatureTrendVO.TrendPoint> points = DASHBOARD_TIME_BUCKETS.stream()
            .map(bucket -> {
                List<Double> values = grouped.get(bucket).stream()
                    .map(HealthDataEntity::getTemperature)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                boolean hasData = !values.isEmpty();
                return DashboardTemperatureTrendVO.TrendPoint.builder()
                    .timeBucket(bucket)
                    .value(hasData ? round1(values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0)) : null)
                    .hasData(hasData)
                    .build();
            })
            .collect(Collectors.toList());

        return DashboardTemperatureTrendVO.builder()
            .warningThreshold(39.5)
            .dangerThreshold(40.5)
            .points(points)
            .build();
    }

    private DashboardActivityHeatmapVO buildActivityHeatmap(List<HealthDataEntity> recentHealthData) {
        Map<String, List<HealthDataEntity>> grouped = groupRecordsByBucket(recentHealthData);
        List<DashboardActivityHeatmapVO.HeatmapCell> cells = new ArrayList<>();

        for (DashboardActivityHeatmapVO.MetricMeta metric : DASHBOARD_HEATMAP_METRICS) {
            for (String timeBucket : DASHBOARD_TIME_BUCKETS) {
                Double rawValue = calculateMetricAverage(metric.getKey(), grouped.get(timeBucket));
                boolean hasData = rawValue != null;
                cells.add(DashboardActivityHeatmapVO.HeatmapCell.builder()
                    .timeBucket(timeBucket)
                    .metricKey(metric.getKey())
                    .metricLabel(metric.getLabel())
                    .intensity(hasData ? normalizeMetricIntensity(metric.getKey(), rawValue) : 0)
                    .rawValue(hasData ? round1(rawValue) : null)
                    .unit(metric.getUnit())
                    .hasData(hasData)
                    .build());
            }
        }

        return DashboardActivityHeatmapVO.builder()
            .timeBuckets(new ArrayList<>(DASHBOARD_TIME_BUCKETS))
            .metrics(copyMetricMetas())
            .cells(cells)
            .build();
    }

    private Map<String, List<HealthDataEntity>> groupRecordsByBucket(List<HealthDataEntity> records) {
        Map<String, List<HealthDataEntity>> grouped = new LinkedHashMap<>();
        DASHBOARD_TIME_BUCKETS.forEach(bucket -> grouped.put(bucket, new ArrayList<>()));
        records.forEach(record -> grouped.get(resolveDashboardBucket(record.getDataTime())).add(record));
        return grouped;
    }

    private List<DashboardActivityHeatmapVO.MetricMeta> copyMetricMetas() {
        return DASHBOARD_HEATMAP_METRICS.stream()
            .map(metric -> DashboardActivityHeatmapVO.MetricMeta.builder()
                .key(metric.getKey())
                .label(metric.getLabel())
                .unit(metric.getUnit())
                .build())
            .collect(Collectors.toList());
    }

    private boolean isDashboardHighRisk(AnimalEntity animal) {
        return "high".equals(animal.getRiskLevel()) || "abnormal".equals(animal.getBehaviorStatus());
    }

    private boolean isDashboardAttention(AnimalEntity animal) {
        if (isDashboardHighRisk(animal)) {
            return false;
        }
        return "low".equals(animal.getRiskLevel())
            || "medium".equals(animal.getRiskLevel())
            || "warning".equals(animal.getBehaviorStatus());
    }

    private String resolveDashboardBucket(LocalDateTime dataTime) {
        int bucketHour = (dataTime.getHour() / 4) * 4;
        return String.format("%02d:00", bucketHour);
    }

    private Double calculateMetricAverage(String metricKey, List<HealthDataEntity> records) {
        if (records == null || records.isEmpty()) {
            return null;
        }
        return switch (metricKey) {
            case "activity" -> averageIntegers(records.stream().map(HealthDataEntity::getActivityLevel).collect(Collectors.toList()));
            case "rumination" -> averageIntegers(records.stream().map(HealthDataEntity::getRuminationTime).collect(Collectors.toList()));
            case "feeding" -> averageIntegers(records.stream().map(HealthDataEntity::getFeedingCount).collect(Collectors.toList()));
            case "resting" -> averageIntegers(records.stream().map(HealthDataEntity::getRestingTime).collect(Collectors.toList()));
            default -> null;
        };
    }

    private Double averageIntegers(List<Integer> values) {
        List<Integer> valid = values.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (valid.isEmpty()) {
            return null;
        }
        return valid.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    private Integer normalizeMetricIntensity(String metricKey, Double rawValue) {
        return switch (metricKey) {
            case "activity" -> clampPercent(rawValue);
            case "rumination" -> clampPercent(rawValue / 30.0 * 100.0);
            case "feeding" -> clampPercent(rawValue / 4.0 * 100.0);
            case "resting" -> clampPercent(rawValue / 30.0 * 100.0);
            default -> 0;
        };
    }

    private Integer clampPercent(Double value) {
        return (int) Math.max(0, Math.min(100, Math.round(value)));
    }

    private String resolveHealthStatus(AnimalEntity animal) {
        if (animal.getHealthScore() == null) {
            return "未知";
        }
        if (animal.getHealthScore() >= 90) {
            return "优秀";
        }
        if (animal.getHealthScore() >= 80) {
            return "良好";
        }
        if (animal.getHealthScore() >= 70) {
            return "一般";
        }
        return "需关注";
    }

    private String resolveHealthColor(Double healthScore) {
        if (healthScore == null) {
            return "#94a3b8";
        }
        if (healthScore >= 90) {
            return "#10b981";
        }
        if (healthScore >= 80) {
            return "#3b82f6";
        }
        if (healthScore >= 70) {
            return "#f59e0b";
        }
        return "#ef4444";
    }

    private int getPriority(AnimalEntity animal) {
        int priority = 0;
        if ("high".equals(animal.getRiskLevel())) {
            priority += 3;
        } else if ("medium".equals(animal.getRiskLevel())) {
            priority += 2;
        }
        if ("abnormal".equals(animal.getBehaviorStatus())) {
            priority += 2;
        } else if ("warning".equals(animal.getBehaviorStatus())) {
            priority += 1;
        }
        if ("estrus".equals(animal.getEstrusStatus())) {
            priority += 1;
        }
        return priority;
    }

    private String buildFocusReason(AnimalEntity animal) {
        if ("high".equals(animal.getRiskLevel())) {
            return "高风险个体，需优先复核。";
        }
        if ("abnormal".equals(animal.getBehaviorStatus())) {
            return "行为异常，建议查看行为分析详情。";
        }
        if ("estrus".equals(animal.getEstrusStatus())) {
            return "处于发情窗口，建议关注繁殖计划。";
        }
        return "指标波动，需要持续观察。";
    }

    private List<AnimalHealthDetailVO.TrendPoint> buildTrendPoints(List<HealthDataEntity> records, MetricType metricType) {
        return records.stream()
            .sorted(Comparator.comparing(HealthDataEntity::getDataTime))
            .map(record -> AnimalHealthDetailVO.TrendPoint.builder()
                .label(record.getDataTime().format(TIME_FORMATTER))
                .value(metricType.extract(record))
                .build())
            .collect(Collectors.toList());
    }

    private List<String> buildInterventionSuggestions(AnimalEntity animal, HealthDataEntity latestHealth) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add(buildFocusReason(animal));
        if (latestHealth != null && farmSnapshotService.isTemperatureAlert(latestHealth)) {
            suggestions.add("建议立即复测体温，并排查是否存在应激或感染迹象。");
        }
        if ("warning".equals(animal.getBehaviorStatus()) || "abnormal".equals(animal.getBehaviorStatus())) {
            suggestions.add("建议联动查看行为详情页，确认反刍和采食变化。");
        }
        if ("estrus".equals(animal.getEstrusStatus())) {
            suggestions.add("建议同步查看配种计划或繁殖安排。");
        }
        return suggestions;
    }

    private List<AnimalHealthDetailVO.HealthRecord> buildHealthRecords(List<HealthDataEntity> records) {
        return records.stream()
            .sorted(Comparator.comparing(HealthDataEntity::getDataTime).reversed())
            .flatMap(record -> {
                List<AnimalHealthDetailVO.HealthRecord> items = new ArrayList<>();
                String time = record.getDataTime().format(TIME_FORMATTER);
                if (record.getTemperature() != null) {
                    items.add(AnimalHealthDetailVO.HealthRecord.builder()
                        .time(time)
                        .type("体温")
                        .value(String.format("%.1f℃", record.getTemperature()))
                        .status(farmSnapshotService.isTemperatureAlert(record) ? "异常" : "正常")
                        .build());
                }
                if (record.getHeartRate() != null) {
                    items.add(AnimalHealthDetailVO.HealthRecord.builder()
                        .time(time)
                        .type("心率")
                        .value(record.getHeartRate() + " 次/分")
                        .status(farmSnapshotService.isHeartRateAlert(record) ? "异常" : "正常")
                        .build());
                }
                if (record.getActivityLevel() != null) {
                    items.add(AnimalHealthDetailVO.HealthRecord.builder()
                        .time(time)
                        .type("活动值")
                        .value(String.valueOf(record.getActivityLevel()))
                        .status(record.getActivityLevel() < 20 ? "待关注" : "正常")
                        .build());
                }
                return items.stream();
            })
            .limit(12)
            .collect(Collectors.toList());
    }

    private List<HealthDashboardSummaryVO.HealthTrendPoint> generateWeeklyTrend(double avgHealthScore) {
        List<HealthDashboardSummaryVO.HealthTrendPoint> trend = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        double score = round1(avgHealthScore);
        for (int i = 6; i >= 0; i--) {
            trend.add(HealthDashboardSummaryVO.HealthTrendPoint.builder()
                .date(now.minusDays(i).format(DATE_FORMATTER))
                .score(score)
                .build());
        }
        return trend;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private HealthDashboardSummaryVO createEmptySummary(FarmBaselineVO baseline) {
        return HealthDashboardSummaryVO.builder()
            .baseline(baseline)
            .totalAnimals(0)
            .healthyCount(0)
            .healthyPercent(0.0)
            .attentionCount(0)
            .alertCount(0)
            .highRiskCount(0L)
            .mediumRiskCount(0L)
            .inEstrusCount(0L)
            .avgHealthScore(0.0)
            .avgTemperature(0.0)
            .avgActivity(0)
            .avgRuminationTime(0L)
            .ruminationPassRate(0.0)
            .weeklyTrend(new ArrayList<>())
            .build();
    }

    private enum MetricType {
        TEMPERATURE {
            @Override
            Double extract(HealthDataEntity record) {
                return record.getTemperature();
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

package com.livestock.health.controller;

import com.livestock.health.model.vo.AnimalHealthDetailVO;
import com.livestock.health.model.vo.DashboardChartsVO;
import com.livestock.health.model.vo.HealthDashboardSummaryVO;
import com.livestock.health.model.vo.HealthHeatmapVO;
import com.livestock.health.model.vo.HealthInsightVO;
import com.livestock.health.service.HealthDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class HealthDashboardController {

    private final HealthDashboardService healthDashboardService;

    @GetMapping("/summary")
    public ResponseEntity<HealthDashboardSummaryVO> getDashboardSummary(
            @RequestParam Long farmId) {
        HealthDashboardSummaryVO summary = healthDashboardService.getDashboardSummary(farmId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/focus-animals")
    public ResponseEntity<List<AnimalHealthDetailVO>> getFocusAnimals(
            @RequestParam Long farmId,
            @RequestParam(defaultValue = "10") int limit) {
        List<AnimalHealthDetailVO> animals = healthDashboardService.getFocusAnimals(farmId, limit);
        return ResponseEntity.ok(animals);
    }

    @GetMapping("/charts")
    public ResponseEntity<DashboardChartsVO> getDashboardCharts(
            @RequestParam Long farmId) {
        DashboardChartsVO charts = healthDashboardService.getDashboardCharts(farmId);
        return ResponseEntity.ok(charts);
    }

    @GetMapping("/heatmap")
    public ResponseEntity<HealthHeatmapVO> getHealthHeatmap(
            @RequestParam Long farmId) {
        HealthHeatmapVO heatmap = healthDashboardService.getHealthHeatmap(farmId);
        return ResponseEntity.ok(heatmap);
    }

    @GetMapping("/distribution")
    public ResponseEntity<Map<String, Object>> getHealthDistribution(
            @RequestParam Long farmId) {
        Map<String, Object> distribution = healthDashboardService.getHealthDistribution(farmId);
        return ResponseEntity.ok(distribution);
    }

    @GetMapping("/insights")
    public ResponseEntity<List<HealthInsightVO>> getHealthInsights(
            @RequestParam Long farmId) {
        List<HealthInsightVO> insights = healthDashboardService.getHealthInsights(farmId);
        return ResponseEntity.ok(insights);
    }

    @GetMapping("/animals/{animalId}")
    public ResponseEntity<AnimalHealthDetailVO> getAnimalHealthDetail(
            @PathVariable String animalId) {
        AnimalHealthDetailVO detail = healthDashboardService.getAnimalHealthDetail(animalId);
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<Map<String, Object>>> getHealthRanking(
            @RequestParam Long farmId,
            @RequestParam(defaultValue = "healthScore") String orderBy,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> ranking = healthDashboardService.getHealthRanking(farmId, orderBy, limit);
        return ResponseEntity.ok(ranking);
    }
}

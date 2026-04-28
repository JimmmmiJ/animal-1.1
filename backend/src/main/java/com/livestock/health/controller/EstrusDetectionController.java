package com.livestock.health.controller;

import com.livestock.health.model.vo.*;
import com.livestock.health.service.EstrusDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 发情期智能识别预警 Controller
 */
@RestController
@RequestMapping("/api/estrus")
@RequiredArgsConstructor
public class EstrusDetectionController {

    private final EstrusDetectionService estrusDetectionService;

    /**
     * 获取发情期概览统计
     */
    @GetMapping("/summary")
    public ResponseEntity<EstrusSummaryVO> getEstrusSummary(
            @RequestParam Long farmId) {
        EstrusSummaryVO summary = estrusDetectionService.getEstrusSummary(farmId);
        return ResponseEntity.ok(summary);
    }

    /**
     * 获取发情期牲畜列表
     */
    @GetMapping("/animals")
    public ResponseEntity<List<EstrusAnimalVO>> getEstrusAnimalList(
            @RequestParam Long farmId,
            @RequestParam(required = false) String status) {
        List<EstrusAnimalVO> animals = estrusDetectionService.getEstrusAnimalList(farmId, status);
        return ResponseEntity.ok(animals);
    }

    /**
     * 获取牲畜发情详情
     */
    @GetMapping("/animals/{animalId}")
    public ResponseEntity<EstrusAnimalVO> getAnimalEstrusDetail(
            @PathVariable String animalId) {
        EstrusAnimalVO detail = estrusDetectionService.getAnimalEstrusDetail(animalId);
        return ResponseEntity.ok(detail);
    }

    /**
     * 获取活动模式分析
     */
    @GetMapping("/animals/{animalId}/activity-pattern")
    public ResponseEntity<ActivityPatternVO> getActivityPattern(
            @PathVariable String animalId) {
        ActivityPatternVO pattern = estrusDetectionService.getActivityPattern(animalId);
        return ResponseEntity.ok(pattern);
    }

    /**
     * 获取发情概率趋势
     */
    @GetMapping("/animals/{animalId}/probability-trend")
    public ResponseEntity<EstrusProbabilityTrendVO> getEstrusProbabilityTrend(
            @PathVariable String animalId,
            @RequestParam(defaultValue = "7") int days) {
        EstrusProbabilityTrendVO trend = estrusDetectionService.getEstrusProbabilityTrend(animalId, days);
        return ResponseEntity.ok(trend);
    }

    /**
     * 获取发情预警事件
     */
    @GetMapping("/alert-events")
    public ResponseEntity<List<Map<String, Object>>> getEstrusAlertEvents(
            @RequestParam Long farmId,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> events = estrusDetectionService.getEstrusAlertEvents(farmId, limit);
        return ResponseEntity.ok(events);
    }

    /**
     * 更新配种计划
     */
    @PostMapping("/animals/{animalId}/breeding-plan")
    public ResponseEntity<Void> updateBreedingPlan(
            @PathVariable String animalId,
            @RequestBody Map<String, Object> planData) {
        estrusDetectionService.updateBreedingPlan(animalId, planData);
        return ResponseEntity.ok().build();
    }
}

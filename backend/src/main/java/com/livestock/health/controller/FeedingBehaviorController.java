package com.livestock.health.controller;

import com.livestock.health.model.vo.*;
import com.livestock.health.service.FeedingBehaviorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 反刍采食行为分析 Controller
 */
@RestController
@RequestMapping("/api/behavior")
@RequiredArgsConstructor
public class FeedingBehaviorController {

    private final FeedingBehaviorService feedingBehaviorService;

    /**
     * 获取行为分析概览
     */
    @GetMapping("/summary")
    public ResponseEntity<BehaviorSummaryVO> getBehaviorSummary(
            @RequestParam Long farmId) {
        BehaviorSummaryVO summary = feedingBehaviorService.getBehaviorSummary(farmId);
        return ResponseEntity.ok(summary);
    }

    /**
     * 获取羊只行为列表
     */
    @GetMapping("/sheep-list")
    public ResponseEntity<List<SheepBehaviorDetailVO>> getSheepBehaviorList(
            @RequestParam Long farmId,
            @RequestParam(required = false) String status) {
        List<SheepBehaviorDetailVO> list = feedingBehaviorService.getSheepBehaviorList(farmId, status);
        return ResponseEntity.ok(list);
    }

    /**
     * 获取羊只行为详情
     */
    @GetMapping("/sheep/{animalId}")
    public ResponseEntity<SheepBehaviorDetailVO> getSheepBehaviorDetail(
            @PathVariable String animalId) {
        SheepBehaviorDetailVO detail = feedingBehaviorService.getSheepBehaviorDetail(animalId);
        return ResponseEntity.ok(detail);
    }

    /**
     * 获取24小时行为模式
     */
    @GetMapping("/24hour-pattern")
    public ResponseEntity<List<Map<String, Object>>> get24HourBehaviorPattern(
            @RequestParam Long farmId) {
        List<Map<String, Object>> pattern = feedingBehaviorService.get24HourBehaviorPattern(farmId);
        return ResponseEntity.ok(pattern);
    }

    /**
     * 获取反刍效率趋势
     */
    @GetMapping("/efficiency-trend")
    public ResponseEntity<List<Map<String, Object>>> getRuminationEfficiencyTrend(
            @RequestParam Long farmId,
            @RequestParam(defaultValue = "7") int days) {
        List<Map<String, Object>> trend = feedingBehaviorService.getRuminationEfficiencyTrend(farmId, days);
        return ResponseEntity.ok(trend);
    }

    /**
     * 获取行为异常事件
     */
    @GetMapping("/abnormal-events")
    public ResponseEntity<List<BehaviorEventVO>> getAbnormalBehaviorEvents(
            @RequestParam Long farmId,
            @RequestParam(defaultValue = "10") int limit) {
        List<BehaviorEventVO> events = feedingBehaviorService.getAbnormalBehaviorEvents(farmId, limit);
        return ResponseEntity.ok(events);
    }

    /**
     * 保存饲喂方案
     */
    @PostMapping("/sheep/{animalId}/diet-plan")
    public ResponseEntity<Void> saveDietPlan(
            @PathVariable String animalId,
            @RequestBody Map<String, Object> dietPlan) {
        feedingBehaviorService.saveDietPlan(animalId, dietPlan);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取营养建议
     */
    @GetMapping("/nutrition-advice")
    public ResponseEntity<List<String>> getNutritionAdvice(
            @RequestParam Long farmId) {
        List<String> advice = feedingBehaviorService.getNutritionAdvice(farmId);
        return ResponseEntity.ok(advice);
    }

    /**
     * 生成行为分析报告
     */
    @GetMapping("/analysis-report")
    public ResponseEntity<BehaviorAnalysisReportVO> generateAnalysisReport(
            @RequestParam Long farmId) {
        BehaviorAnalysisReportVO report = feedingBehaviorService.generateAnalysisReport(farmId);
        return ResponseEntity.ok(report);
    }
}

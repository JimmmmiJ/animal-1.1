package com.livestock.health.controller;

import com.livestock.health.model.vo.*;
import com.livestock.health.service.DiseaseDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 疾病早期检测与风险评估 Controller
 */
@RestController
@RequestMapping("/api/disease")
@RequiredArgsConstructor
public class DiseaseDetectionController {

    private final DiseaseDetectionService diseaseDetectionService;

    /**
     * 获取疾病风险概览统计
     */
    @GetMapping("/summary")
    public ResponseEntity<DiseaseRiskSummaryVO> getRiskSummary(
            @RequestParam Long farmId) {
        DiseaseRiskSummaryVO summary = diseaseDetectionService.getRiskSummary(farmId);
        return ResponseEntity.ok(summary);
    }

    /**
     * 获取风险牲畜列表
     */
    @GetMapping("/risk-animals")
    public ResponseEntity<List<RiskAnimalVO>> getRiskAnimalList(
            @RequestParam Long farmId,
            @RequestParam(required = false) String riskLevel) {
        List<RiskAnimalVO> animals = diseaseDetectionService.getRiskAnimalList(farmId, riskLevel);
        return ResponseEntity.ok(animals);
    }

    /**
     * 获取牲畜风险详情
     */
    @GetMapping("/animals/{animalId}/risk-detail")
    public ResponseEntity<RiskAnimalVO> getAnimalRiskDetail(
            @PathVariable String animalId) {
        RiskAnimalVO detail = diseaseDetectionService.getAnimalRiskDetail(animalId);
        return ResponseEntity.ok(detail);
    }

    /**
     * 获取疾病类型分析
     */
    @GetMapping("/type-analysis")
    public ResponseEntity<DiseaseTypeAnalysisVO> getDiseaseTypeAnalysis(
            @RequestParam Long farmId) {
        DiseaseTypeAnalysisVO analysis = diseaseDetectionService.getDiseaseTypeAnalysis(farmId);
        return ResponseEntity.ok(analysis);
    }

    /**
     * 获取风险分布
     */
    @GetMapping("/risk-distribution")
    public ResponseEntity<Map<String, Object>> getRiskDistribution(
            @RequestParam Long farmId) {
        Map<String, Object> distribution = diseaseDetectionService.getRiskDistribution(farmId);
        return ResponseEntity.ok(distribution);
    }

    /**
     * 获取健康事件时间轴
     */
    @GetMapping("/health-events")
    public ResponseEntity<List<Map<String, Object>>> getHealthEventTimeline(
            @RequestParam Long farmId,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> events = diseaseDetectionService.getHealthEventTimeline(farmId, limit);
        return ResponseEntity.ok(events);
    }

    /**
     * 创建治疗计划
     */
    @PostMapping("/animals/{animalId}/treatment-plan")
    public ResponseEntity<TreatmentPlanVO> createTreatmentPlan(
            @PathVariable String animalId,
            @RequestBody Map<String, Object> planData) {
        TreatmentPlanVO plan = diseaseDetectionService.createTreatmentPlan(animalId, planData);
        return ResponseEntity.ok(plan);
    }

    /**
     * 获取治疗计划详情
     */
    @GetMapping("/animals/{animalId}/treatment-plan")
    public ResponseEntity<TreatmentPlanVO> getTreatmentPlan(
            @PathVariable String animalId) {
        TreatmentPlanVO plan = diseaseDetectionService.getTreatmentPlan(animalId);
        return ResponseEntity.ok(plan);
    }

    /**
     * 更新风险等级
     */
    @PutMapping("/animals/{animalId}/risk-level")
    public ResponseEntity<Void> updateRiskLevel(
            @PathVariable String animalId,
            @RequestParam String riskLevel) {
        diseaseDetectionService.updateRiskLevel(animalId, riskLevel);
        return ResponseEntity.ok().build();
    }

    /**
     * 执行风险评估
     */
    @PostMapping("/animals/{animalId}/assess")
    public ResponseEntity<Map<String, Object>> performRiskAssessment(
            @PathVariable String animalId) {
        Map<String, Object> result = diseaseDetectionService.performRiskAssessment(animalId);
        return ResponseEntity.ok(result);
    }
}

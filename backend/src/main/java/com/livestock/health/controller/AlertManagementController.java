package com.livestock.health.controller;

import com.livestock.health.model.vo.*;
import com.livestock.health.service.AlertManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 异常告警推送管理 Controller
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertManagementController {

    private final AlertManagementService alertManagementService;

    /**
     * 获取告警概览统计
     */
    @GetMapping("/summary")
    public ResponseEntity<AlertSummaryVO> getAlertSummary(
            @RequestParam Long farmId) {
        AlertSummaryVO summary = alertManagementService.getAlertSummary(farmId);
        return ResponseEntity.ok(summary);
    }

    /**
     * 获取告警事件列表
     */
    @GetMapping("/events")
    public ResponseEntity<List<AlertEventVO>> getAlertEvents(
            @RequestParam Long farmId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "20") int limit) {
        List<AlertEventVO> events = alertManagementService.getAlertEvents(farmId, status, severity, limit);
        return ResponseEntity.ok(events);
    }

    /**
     * 获取告警详情
     */
    @GetMapping("/events/{alertId}")
    public ResponseEntity<AlertEventVO> getAlertDetail(
            @PathVariable String alertId,
            @RequestParam Long farmId) {
        AlertEventVO detail = alertManagementService.getAlertDetail(farmId, alertId);
        return ResponseEntity.ok(detail);
    }

    /**
     * 确认告警
     */
    @PutMapping("/events/{alertId}/acknowledge")
    public ResponseEntity<Void> acknowledgeAlert(
            @PathVariable String alertId,
            @RequestParam String acknowledgedBy) {
        alertManagementService.acknowledgeAlert(alertId, acknowledgedBy);
        return ResponseEntity.ok().build();
    }

    /**
     * 解决告警
     */
    @PutMapping("/events/{alertId}/resolve")
    public ResponseEntity<Void> resolveAlert(
            @PathVariable String alertId,
            @RequestParam String resolvedBy,
            @RequestParam(required = false) String resolutionNote) {
        alertManagementService.resolveAlert(alertId, resolvedBy, resolutionNote);
        return ResponseEntity.ok().build();
    }

    /**
     * 批量操作
     */
    @PostMapping("/batch")
    public ResponseEntity<Void> batchOperation(
            @RequestParam List<String> alertIds,
            @RequestParam String action,
            @RequestParam String operator) {
        alertManagementService.batchOperation(alertIds, action, operator);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取推送渠道配置
     */
    @GetMapping("/push-channels")
    public ResponseEntity<List<PushChannelVO>> getPushChannels(
            @RequestParam Long farmId) {
        List<PushChannelVO> channels = alertManagementService.getPushChannels(farmId);
        return ResponseEntity.ok(channels);
    }

    /**
     * 更新推送渠道配置
     */
    @PutMapping("/push-channels/{channel}")
    public ResponseEntity<Void> updatePushChannel(
            @PathVariable String channel,
            @RequestBody Map<String, Object> config) {
        alertManagementService.updatePushChannel(channel, config);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取推送历史
     */
    @GetMapping("/push-history")
    public ResponseEntity<List<PushHistoryVO>> getPushHistory(
            @RequestParam Long farmId,
            @RequestParam(defaultValue = "20") int limit) {
        List<PushHistoryVO> history = alertManagementService.getPushHistory(farmId, limit);
        return ResponseEntity.ok(history);
    }

    /**
     * 创建告警规则
     */
    @PostMapping("/rules")
    public ResponseEntity<Void> createAlertRule(
            @RequestBody Map<String, Object> rule) {
        alertManagementService.createAlertRule(rule);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取告警规则列表
     */
    @GetMapping("/rules")
    public ResponseEntity<List<Map<String, Object>>> getAlertRules(
            @RequestParam Long farmId) {
        List<Map<String, Object>> rules = alertManagementService.getAlertRules(farmId);
        return ResponseEntity.ok(rules);
    }

    /**
     * 获取告警趋势
     */
    @GetMapping("/trend")
    public ResponseEntity<Map<String, Object>> getAlertTrend(
            @RequestParam Long farmId,
            @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> trend = alertManagementService.getAlertTrend(farmId, days);
        return ResponseEntity.ok(trend);
    }
}

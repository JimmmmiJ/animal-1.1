package com.livestock.health.controller;

import com.livestock.health.model.vo.DeviceStatusVO;
import com.livestock.health.model.vo.DeviceSummaryVO;
import com.livestock.health.model.vo.TemperatureTrendVO;
import com.livestock.health.service.LivestockHealthDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 牲畜健康数据实时采集管理 Controller
 */
@RestController
@RequestMapping("/api/health-data")
@RequiredArgsConstructor
public class LivestockHealthDataController {

    private final LivestockHealthDataService healthDataService;

    /**
     * 获取设备统计概览
     */
    @GetMapping("/summary")
    public ResponseEntity<DeviceSummaryVO> getDeviceSummary(
            @RequestParam Long farmId) {
        DeviceSummaryVO summary = healthDataService.getDeviceSummary(farmId);
        return ResponseEntity.ok(summary);
    }

    /**
     * 获取设备状态列表
     */
    @GetMapping("/devices")
    public ResponseEntity<List<DeviceStatusVO>> getDeviceStatusList(
            @RequestParam Long farmId,
            @RequestParam(required = false) String status) {
        List<DeviceStatusVO> devices = healthDataService.getDeviceStatusList(farmId, status);
        return ResponseEntity.ok(devices);
    }

    /**
     * 获取设备详情
     */
    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<DeviceStatusVO> getDeviceDetail(
            @PathVariable String deviceId) {
        DeviceStatusVO device = healthDataService.getDeviceDetail(deviceId);
        return ResponseEntity.ok(device);
    }

    /**
     * 获取体温趋势
     */
    @GetMapping("/animals/{animalId}/temperature-trend")
    public ResponseEntity<TemperatureTrendVO> getTemperatureTrend(
            @PathVariable String animalId,
            @RequestParam(defaultValue = "24h") String timeRange) {
        TemperatureTrendVO trend = healthDataService.getTemperatureTrend(animalId, timeRange);
        return ResponseEntity.ok(trend);
    }

    /**
     * 获取活动量统计
     */
    @GetMapping("/animals/{animalId}/activity-stats")
    public ResponseEntity<Map<String, Object>> getActivityStatistics(
            @PathVariable String animalId,
            @RequestParam(defaultValue = "24h") String timeRange) {
        Map<String, Object> stats = healthDataService.getActivityStatistics(animalId, timeRange);
        return ResponseEntity.ok(stats);
    }

    /**
     * 更新设备配置
     */
    @PutMapping("/devices/{deviceId}/config")
    public ResponseEntity<Void> updateDeviceConfig(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> config) {
        healthDataService.updateDeviceConfig(deviceId, config);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取数据采集日志
     */
    @GetMapping("/devices/{deviceId}/logs")
    public ResponseEntity<List<Map<String, Object>>> getDataCollectionLogs(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "20") int limit) {
        List<Map<String, Object>> logs = healthDataService.getDataCollectionLogs(deviceId, limit);
        return ResponseEntity.ok(logs);
    }
}

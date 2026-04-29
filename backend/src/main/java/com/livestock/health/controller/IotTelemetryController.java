package com.livestock.health.controller;

import com.livestock.health.model.dto.TelemetryPayload;
import com.livestock.health.service.TelemetryIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/iot")
@RequiredArgsConstructor
public class IotTelemetryController {

    private final TelemetryIngestionService telemetryIngestionService;

    @Value("${iot.telemetry.api-key:}")
    private String telemetryApiKey;

    @PostMapping("/telemetry")
    public ResponseEntity<Map<String, Object>> receiveTelemetry(
            @RequestHeader(value = "X-IOT-KEY", required = false) String apiKey,
            @RequestBody TelemetryPayload payload) {
        if (telemetryApiKey != null && !telemetryApiKey.isBlank() && !telemetryApiKey.equals(apiKey)) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", "Invalid telemetry API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        return ResponseEntity.ok(telemetryIngestionService.ingest(payload, "HTTP"));
    }
}

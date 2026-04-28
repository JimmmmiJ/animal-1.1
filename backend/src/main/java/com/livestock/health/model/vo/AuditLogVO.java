package com.livestock.health.model.vo;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 审计日志 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogVO {

    private Long id;

    private String username;

    private String operationType;

    private String operationModule;

    private String operationDesc;

    private String ipAddress;

    private Integer responseStatus;

    private Integer responseTime;

    private LocalDateTime createdAt;
}

package com.livestock.health.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 智能项圈设备实体
 */
@Entity
@Table(name = "device")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", unique = true, nullable = false, length = 50)
    private String deviceId;

    @Column(name = "device_model", length = 50)
    private String deviceModel;

    @Column(name = "device_sn", length = 100)
    private String deviceSn;

    @Column(name = "farm_id")
    private Long farmId;

    @Column(name = "animal_id")
    private Long animalId;

    // 状态信息
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "signal_strength")
    private Integer signalStrength;

    // 实时数据
    @Column(name = "current_temperature")
    private Double currentTemperature;

    @Column(name = "current_heart_rate")
    private Integer currentHeartRate;

    @Column(name = "current_activity")
    private Integer currentActivity;

    @Column(name = "last_online_at")
    private LocalDateTime lastOnlineAt;

    @Column(name = "last_data_update_at")
    private LocalDateTime lastDataUpdateAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

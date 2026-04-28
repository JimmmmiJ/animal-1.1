package com.livestock.health.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 健康数据记录实体 (时序数据)
 */
@Entity
@Table(name = "health_data", indexes = {
    @Index(name = "idx_device_id", columnList = "device_id"),
    @Index(name = "idx_animal_id", columnList = "animal_id"),
    @Index(name = "idx_farm_id", columnList = "farm_id"),
    @Index(name = "idx_data_time", columnList = "data_time"),
    @Index(name = "idx_farm_time", columnList = "farm_id,data_time")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "animal_id", nullable = false)
    private Long animalId;

    @Column(name = "farm_id", nullable = false)
    private Long farmId;

    // 生理数据
    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "activity_level")
    private Integer activityLevel;

    // 行为数据
    @Column(name = "rumination_time")
    private Integer ruminationTime;

    @Column(name = "feeding_count")
    private Integer feedingCount;

    @Column(name = "resting_time")
    private Integer restingTime;

    // 数据时间
    @Column(name = "data_time", nullable = false)
    private LocalDateTime dataTime;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

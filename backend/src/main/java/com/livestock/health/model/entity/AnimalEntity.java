package com.livestock.health.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 牲畜档案实体
 */
@Entity
@Table(name = "animal")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "animal_id", unique = true, nullable = false, length = 50)
    private String animalId;

    @Column(name = "farm_id", nullable = false)
    private Long farmId;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "breed", length = 50)
    private String breed;

    @Column(name = "gender")
    private Integer gender;

    @Column(name = "age")
    private Integer age;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "group_id")
    private Long groupId;

    // 健康指标
    @Column(name = "health_score")
    private Double healthScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    // 行为数据
    @Column(name = "daily_rumination_time")
    private Integer dailyRuminationTime;

    @Column(name = "daily_feeding_count")
    private Integer dailyFeedingCount;

    @Column(name = "rumination_efficiency")
    private Double ruminationEfficiency;

    @Column(name = "feeding_quality")
    private Double feedingQuality;

    @Column(name = "behavior_status", length = 20)
    private String behaviorStatus;

    // 发情期相关
    @Column(name = "estrus_status", length = 20)
    private String estrusStatus;

    @Column(name = "estrus_probability")
    private Double estrusProbability;

    @Column(name = "last_estrus_at")
    private LocalDateTime lastEstrusAt;

    @Column(name = "next_estrus_predicted_at")
    private LocalDateTime nextEstrusPredictedAt;

    // 设备相关
    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "last_behavior_update_at")
    private LocalDateTime lastBehaviorUpdateAt;

    @Column(name = "last_diet_adjust_at")
    private LocalDateTime lastDietAdjustAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

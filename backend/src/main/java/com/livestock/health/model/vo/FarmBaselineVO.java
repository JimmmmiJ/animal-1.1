package com.livestock.health.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 全场统一基线 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmBaselineVO {

    private Long farmId;

    private LocalDateTime snapshotAt;

    private Long totalAnimals;

    private Long totalDevices;

    private Long femaleAnimalCount;

    private Long onlineDevices;

    private Long offlineDevices;

    private Long faultDevices;

    private Long abnormalBehaviorAnimalCount;

    private Long riskAnimalCount;
}

package com.livestock.health.model.vo;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 治疗计划 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentPlanVO {

    private Long id;

    private String animalId;

    private String animalName;

    private String diagnosis;

    private String treatmentPlan;

    private String medication;

    private String dosage;

    private String frequency;

    private Integer duration;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private String status;

    private String veterinarian;

    private String notes;

    private List<TreatmentRecord> records;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TreatmentRecord {
        private LocalDateTime time;
        private String action;
        private String operator;
        private String note;
    }
}

package com.livestock.health.model.vo;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 推送历史记录 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushHistoryRecordVO {

    private Long id;

    private String alertId;

    private String channel;

    private String receiver;

    private String content;

    private String status;

    private String errorMessage;

    private LocalDateTime pushedAt;
}
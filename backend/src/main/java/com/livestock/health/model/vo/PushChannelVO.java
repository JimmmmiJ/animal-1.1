package com.livestock.health.model.vo;

import lombok.*;

import java.util.List;

/**
 * 推送渠道 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushChannelVO {

    private String channel;

    private String name;

    private Boolean enabled;

    private String status;

    private Integer successCount;

    private Integer failCount;

    private Double successRate;

    private List<String> receivers;
}
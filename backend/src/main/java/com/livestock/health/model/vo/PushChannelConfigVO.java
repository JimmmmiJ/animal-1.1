package com.livestock.health.model.vo;

import lombok.*;

import java.util.List;

/**
 * 推送渠道配置 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushChannelConfigVO {

    private String channel;

    private String name;

    private Boolean enabled;

    private List<String> receivers;

    private String config;
}
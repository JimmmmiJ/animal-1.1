package com.livestock.health.model.vo;

import lombok.*;

/**
 * 系统配置信息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigInfoVO {

    private String key;

    private String value;

    private String type;

    private String description;

    private String category;
}
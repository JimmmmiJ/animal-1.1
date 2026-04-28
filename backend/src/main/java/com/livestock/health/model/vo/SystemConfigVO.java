package com.livestock.health.model.vo;

import lombok.*;

/**
 * 系统配置 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigVO {

    private String key;

    private String value;

    private String type;

    private String description;

    private String category;
}

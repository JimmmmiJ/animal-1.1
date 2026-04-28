package com.livestock.health.model.vo;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色信息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleInfoVO {

    private Long id;

    private String name;

    private String code;

    private String description;

    private Integer userCount;

    private List<String> permissions;

    private LocalDateTime createdAt;
}
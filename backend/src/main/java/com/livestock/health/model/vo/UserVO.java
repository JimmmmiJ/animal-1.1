package com.livestock.health.model.vo;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 用户信息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private String email;

    private String roleName;

    private Long roleId;

    private String farmName;

    private Long farmId;

    private Integer status;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;
}

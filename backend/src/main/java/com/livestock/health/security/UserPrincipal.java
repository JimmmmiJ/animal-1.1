package com.livestock.health.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 安全认证用户主体
 */
@Data
@AllArgsConstructor
public class UserPrincipal {
    
    private Long userId;
    private String username;
    private String role;
}

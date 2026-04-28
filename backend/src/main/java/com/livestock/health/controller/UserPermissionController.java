package com.livestock.health.controller;

import com.livestock.health.model.vo.AuditLogVO;
import com.livestock.health.model.vo.RoleVO;
import com.livestock.health.model.vo.SystemConfigVO;
import com.livestock.health.model.vo.UserVO;
import com.livestock.health.service.UserPermissionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class UserPermissionController {

    private final UserPermissionService userPermissionService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> user = validateUser(request.getUsername(), request.getPassword());

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Invalid username or password"
            ));
        }

        String token = "mock-token-" + System.currentTimeMillis();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Login successful");
        response.put("token", token);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.get("userId"));
        userInfo.put("username", user.get("username"));
        userInfo.put("realName", user.get("realName"));
        userInfo.put("role", user.get("role"));
        response.put("user", userInfo);

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> validateUser(String username, String password) {
        Map<String, Map<String, Object>> users = new HashMap<>();

        Map<String, Object> admin = new HashMap<>();
        admin.put("userId", 1L);
        admin.put("username", "admin");
        admin.put("password", "admin123");
        admin.put("role", "ADMIN");
        admin.put("realName", "Admin");
        users.put("admin", admin);

        Map<String, Object> vet = new HashMap<>();
        vet.put("userId", 2L);
        vet.put("username", "vet");
        vet.put("password", "vet123");
        vet.put("role", "VET");
        vet.put("realName", "Veterinarian");
        users.put("vet", vet);

        Map<String, Object> feeder = new HashMap<>();
        feeder.put("userId", 3L);
        feeder.put("username", "feeder");
        feeder.put("password", "feeder123");
        feeder.put("role", "FEEDER");
        feeder.put("realName", "Feeder");
        users.put("feeder", feeder);

        Map<String, Object> user = users.get(username);
        if (user == null) {
            return null;
        }

        if (!password.equals(user.get("password"))) {
            return null;
        }

        return user;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserVO>> getUserList(
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) String keyword) {
        List<UserVO> users = userPermissionService.getUserList(farmId, keyword);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserVO> getUserDetail(@PathVariable Long userId) {
        UserVO user = userPermissionService.getUserDetail(userId);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users")
    public ResponseEntity<Void> createUser(@RequestBody Map<String, Object> userData) {
        userPermissionService.createUser(userData);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<Void> updateUser(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> userData) {
        userPermissionService.updateUser(userId, userData);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userPermissionService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long userId) {
        userPermissionService.resetPassword(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RoleVO>> getRoleList() {
        List<RoleVO> roles = userPermissionService.getRoleList();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<Map<String, Object>>> getPermissionList() {
        List<Map<String, Object>> permissions = userPermissionService.getPermissionList();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/config")
    public ResponseEntity<List<SystemConfigVO>> getSystemConfig(
            @RequestParam(required = false) String category) {
        List<SystemConfigVO> configs = userPermissionService.getSystemConfig(category);
        return ResponseEntity.ok(configs);
    }

    @PutMapping("/config/{key}")
    public ResponseEntity<Void> updateSystemConfig(
            @PathVariable String key,
            @RequestParam String value) {
        userPermissionService.updateSystemConfig(key, value);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogVO>> getAuditLogs(
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) String operationType,
            @RequestParam(defaultValue = "50") int limit) {
        List<AuditLogVO> logs = userPermissionService.getAuditLogs(farmId, operationType, limit);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        Map<String, Object> stats = userPermissionService.getSystemStats();
        return ResponseEntity.ok(stats);
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}

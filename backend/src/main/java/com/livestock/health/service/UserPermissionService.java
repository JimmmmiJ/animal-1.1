package com.livestock.health.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livestock.health.model.vo.AuditLogVO;
import com.livestock.health.model.vo.RoleVO;
import com.livestock.health.model.vo.SystemConfigVO;
import com.livestock.health.model.vo.UserVO;
import com.livestock.health.model.entity.SystemConfigEntity;
import com.livestock.health.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPermissionService {

    private static final String USER_OVERRIDES_CONFIG_PREFIX = "user_overrides:";
    private static final String ROLE_PERMISSIONS_CONFIG_PREFIX = "role_permissions:";
    private static final List<String> HIDDEN_CONFIG_PREFIXES = List.of(
        "device_config:",
        "alert_rules:",
        "breeding_plan:",
        "treatment_plan:",
        "diet_plan:",
        "push_channel:",
        USER_OVERRIDES_CONFIG_PREFIX,
        ROLE_PERMISSIONS_CONFIG_PREFIX
    );

    private final SystemConfigRepository systemConfigRepository;
    private final ObjectMapper objectMapper;

    public List<UserVO> getUserList(Long farmId, String keyword) {
        List<UserVO> users = new ArrayList<>();
        String[] roles = {"超级管理员", "农场管理员", "兽医", "饲养员"};
        String[] names = {"张三", "李四", "王五", "赵六", "钱七", "孙八"};

        for (int i = 0; i < 15; i++) {
            users.add(UserVO.builder()
                .id((long) i + 1)
                .username("user" + (i + 1))
                .realName(names[i % names.length])
                .phone("138" + String.format("%08d", i))
                .email("user" + (i + 1) + "@example.com")
                .roleName(roles[i % roles.length])
                .roleId((long) (i % roles.length) + 1)
                .farmName("示范牧场")
                .farmId(farmId == null ? 1L : farmId)
                .status(i % 10 == 0 ? 0 : 1)
                .lastLoginAt(LocalDateTime.now().minusDays(i % 5))
                .createdAt(LocalDateTime.now().minusMonths(i % 12))
                .build());
        }

        users = applyPersistedUserOverrides(users, farmId == null ? 1L : farmId);

        if (keyword != null && !keyword.isBlank()) {
            return users.stream()
                .filter(user -> user.getUsername().contains(keyword) || user.getRealName().contains(keyword))
                .collect(Collectors.toList());
        }
        return users;
    }

    public UserVO getUserDetail(Long userId) {
        return getUserList(1L, null).stream()
            .filter(user -> userId.equals(user.getId()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    public void createUser(Map<String, Object> userData) {
        Long farmId = toLong(userData.get("farmId"), 1L);
        Map<String, Object> overrides = loadUserOverrides(farmId);
        List<Map<String, Object>> created = getListMap(overrides, "created");
        Map<String, Object> stored = new LinkedHashMap<>(userData);
        stored.putIfAbsent("id", System.currentTimeMillis());
        stored.putIfAbsent("farmId", farmId);
        stored.putIfAbsent("status", 1);
        stored.putIfAbsent("createdAt", LocalDateTime.now().toString());
        created.removeIf(item -> Objects.equals(toLong(item.get("id"), null), toLong(stored.get("id"), null)));
        created.add(stored);
        overrides.put("created", created);
        saveUserOverrides(farmId, overrides);
        log.info("Create user: {}", stored);
    }

    public void updateUser(Long userId, Map<String, Object> userData) {
        Long farmId = toLong(userData.get("farmId"), 1L);
        Map<String, Object> overrides = loadUserOverrides(farmId);
        List<Map<String, Object>> created = getListMap(overrides, "created");
        Map<String, Object> updated = getObjectMap(overrides, "updated");
        Map<String, Object> stored = new LinkedHashMap<>(userData);
        stored.put("id", userId);
        stored.putIfAbsent("farmId", farmId);

        boolean updatedCreated = false;
        for (int i = 0; i < created.size(); i++) {
            if (Objects.equals(toLong(created.get(i).get("id"), null), userId)) {
                created.set(i, stored);
                updatedCreated = true;
                break;
            }
        }
        if (!updatedCreated) {
            updated.put(String.valueOf(userId), stored);
        }
        overrides.put("created", created);
        overrides.put("updated", updated);
        saveUserOverrides(farmId, overrides);
        log.info("Update user: userId={}, data={}", userId, stored);
    }

    public void deleteUser(Long userId) {
        Long farmId = 1L;
        Map<String, Object> overrides = loadUserOverrides(farmId);
        List<Map<String, Object>> created = getListMap(overrides, "created");
        Map<String, Object> updated = getObjectMap(overrides, "updated");
        List<Long> deletedIds = getLongList(overrides, "deletedIds");
        created.removeIf(item -> Objects.equals(toLong(item.get("id"), null), userId));
        updated.remove(String.valueOf(userId));
        if (!deletedIds.contains(userId)) {
            deletedIds.add(userId);
        }
        overrides.put("created", created);
        overrides.put("updated", updated);
        overrides.put("deletedIds", deletedIds);
        saveUserOverrides(farmId, overrides);
        log.info("Delete user: userId={}", userId);
    }

    public void resetPassword(Long userId) {
        log.info("Reset password: userId={}", userId);
    }

    public List<RoleVO> getRoleList() {
        return applyPersistedRolePermissions(List.of(
            RoleVO.builder()
                .id(1L)
                .name("超级管理员")
                .code("SUPER_ADMIN")
                .description("拥有系统所有权限。")
                .userCount(2)
                .permissions(List.of("*"))
                .createdAt(LocalDateTime.now().minusYears(1))
                .build(),
            RoleVO.builder()
                .id(2L)
                .name("农场管理员")
                .code("FARM_ADMIN")
                .description("负责农场全量管理工作。")
                .userCount(5)
                .permissions(List.of("animal:*", "device:*", "alert:*", "user:view", "report:view"))
                .createdAt(LocalDateTime.now().minusYears(1))
                .build(),
            RoleVO.builder()
                .id(3L)
                .name("兽医")
                .code("VETERINARIAN")
                .description("负责健康巡检和治疗工作。")
                .userCount(8)
                .permissions(List.of("animal:view", "animal:treat", "disease:*", "report:view"))
                .createdAt(LocalDateTime.now().minusYears(1))
                .build(),
            RoleVO.builder()
                .id(4L)
                .name("饲养员")
                .code("FEEDER")
                .description("负责日常饲喂和巡检。")
                .userCount(15)
                .permissions(List.of("animal:view", "feeding:*", "behavior:view"))
                .createdAt(LocalDateTime.now().minusYears(1))
                .build()
        ));
    }

    public List<Map<String, Object>> getPermissionList() {
        List<Map<String, Object>> permissions = new ArrayList<>();
        String[][] permissionData = {
            {"牲畜管理", "animal:view", "查看牲畜"},
            {"牲畜管理", "animal:edit", "编辑牲畜"},
            {"牲畜管理", "animal:delete", "删除牲畜"},
            {"设备管理", "device:view", "查看设备"},
            {"设备管理", "device:config", "配置设备"},
            {"告警管理", "alert:view", "查看告警"},
            {"告警管理", "alert:handle", "处理告警"},
            {"用户管理", "user:view", "查看用户"},
            {"用户管理", "user:manage", "管理用户"},
            {"报表管理", "report:view", "查看报表"},
            {"报表管理", "report:export", "导出报表"},
            {"系统配置", "system:config", "系统配置"}
        };
        for (String[] row : permissionData) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("module", row[0]);
            item.put("code", row[1]);
            item.put("name", row[2]);
            permissions.add(item);
        }
        return permissions;
    }

    public List<SystemConfigVO> getSystemConfig(String category) {
        Map<String, SystemConfigEntity> storedByKey = systemConfigRepository.findAll().stream()
            .collect(Collectors.toMap(SystemConfigEntity::getConfigKey, Function.identity(), (left, right) -> right));

        List<SystemConfigVO> defaults = createBaseSystemConfigs();
        Set<String> defaultKeys = defaults.stream()
            .map(SystemConfigVO::getKey)
            .collect(Collectors.toCollection(HashSet::new));

        List<SystemConfigVO> configs = defaults.stream()
            .map(config -> mergeStoredConfig(config, storedByKey.get(config.getKey())))
            .collect(Collectors.toCollection(ArrayList::new));

        storedByKey.values().stream()
            .filter(entity -> !defaultKeys.contains(entity.getConfigKey()))
            .filter(entity -> !isHiddenConfigKey(entity.getConfigKey()))
            .map(this::toConfigVO)
            .forEach(configs::add);

        if (category != null && !category.isBlank()) {
            return configs.stream()
                .filter(config -> category.equals(config.getCategory()))
                .collect(Collectors.toList());
        }
        return configs;
    }

    @Transactional
    public void updateSystemConfig(String key, String value) {
        SystemConfigVO defaults = createBaseSystemConfigs().stream()
            .filter(config -> config.getKey().equals(key))
            .findFirst()
            .orElse(null);

        upsertSystemConfig(
            key,
            value,
            defaults == null ? inferConfigType(value) : defaults.getType(),
            defaults == null ? "Dynamic system configuration" : defaults.getDescription(),
            null
        );
        log.info("Update system config: key={}, value={}", key, value);
    }

    @Transactional
    public void upsertSystemConfig(String key, String value, String type, String description, Long farmId) {
        SystemConfigEntity entity = systemConfigRepository.findByConfigKey(key)
            .orElseGet(() -> SystemConfigEntity.builder().configKey(key).build());

        entity.setConfigValue(value);
        entity.setConfigType(type);
        entity.setDescription(description);
        entity.setFarmId(farmId);
        systemConfigRepository.save(entity);
    }

    public String getSystemConfigValue(String key) {
        return systemConfigRepository.findByConfigKey(key)
            .map(SystemConfigEntity::getConfigValue)
            .orElse(null);
    }

    public List<AuditLogVO> getAuditLogs(Long farmId, String operationType, int limit) {
        List<AuditLogVO> logs = new ArrayList<>();
        String[] users = {"admin", "zhangsan", "lisi", "wangwu"};
        String[] types = {"登录", "创建", "更新", "删除", "导出"};
        String[] modules = {"用户管理", "牲畜管理", "设备管理", "告警管理", "系统配置"};
        Random random = new Random(20260423L);

        for (int i = 0; i < limit; i++) {
            logs.add(AuditLogVO.builder()
                .id((long) i + 1)
                .username(users[random.nextInt(users.length)])
                .operationType(types[random.nextInt(types.length)])
                .operationModule(modules[random.nextInt(modules.length)])
                .operationDesc("执行了系统操作")
                .ipAddress("192.168.1." + random.nextInt(255))
                .responseStatus(200)
                .responseTime(50 + random.nextInt(200))
                .createdAt(LocalDateTime.now().minusMinutes(random.nextInt(1440)))
                .build());
        }

        if (operationType != null && !operationType.isBlank()) {
            logs = logs.stream()
                .filter(log -> operationType.equals(log.getOperationType()))
                .collect(Collectors.toList());
        }

        return logs.stream()
            .sorted(Comparator.comparing(AuditLogVO::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }

    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", 30L);
        stats.put("activeUsers", 25L);
        stats.put("totalRoles", 4L);
        stats.put("todayLoginCount", 45L);
        stats.put("avgResponseTime", 125.5);
        stats.put("systemUptime", "15天 8小时 32分钟");
        return stats;
    }

    private List<UserVO> applyPersistedUserOverrides(List<UserVO> baseUsers, Long farmId) {
        Map<String, Object> overrides = loadUserOverrides(farmId);
        List<Long> deletedIds = getLongList(overrides, "deletedIds");
        Map<String, Object> updated = getObjectMap(overrides, "updated");
        List<Map<String, Object>> created = getListMap(overrides, "created");

        List<UserVO> merged = baseUsers.stream()
            .filter(user -> !deletedIds.contains(user.getId()))
            .map(user -> {
                Object override = updated.get(String.valueOf(user.getId()));
                return override instanceof Map<?, ?> map ? mapToUserVO(castMap(map), user) : user;
            })
            .collect(Collectors.toCollection(ArrayList::new));

        created.stream()
            .map(item -> mapToUserVO(item, null))
            .filter(user -> !deletedIds.contains(user.getId()))
            .forEach(merged::add);

        return merged.stream()
            .sorted(Comparator.comparing(UserVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
    }

    private UserVO mapToUserVO(Map<String, Object> data, UserVO fallback) {
        Long roleId = toLong(data.get("roleId"), fallback == null ? 2L : fallback.getRoleId());
        return UserVO.builder()
            .id(toLong(data.get("id"), fallback == null ? System.currentTimeMillis() : fallback.getId()))
            .username(asString(data.get("username"), fallback == null ? "user" : fallback.getUsername()))
            .realName(asString(data.get("realName"), fallback == null ? asString(data.get("username"), "用户") : fallback.getRealName()))
            .phone(asString(data.get("phone"), fallback == null ? "" : fallback.getPhone()))
            .email(asString(data.get("email"), fallback == null ? "" : fallback.getEmail()))
            .roleName(asString(data.get("roleName"), resolveRoleName(roleId)))
            .roleId(roleId)
            .farmName(asString(data.get("farmName"), fallback == null ? "示范牧场" : fallback.getFarmName()))
            .farmId(toLong(data.get("farmId"), fallback == null ? 1L : fallback.getFarmId()))
            .status(toInt(data.get("status"), fallback == null ? 1 : fallback.getStatus()))
            .lastLoginAt(parseDateTime(data.get("lastLoginAt"), fallback == null ? null : fallback.getLastLoginAt()))
            .createdAt(parseDateTime(data.get("createdAt"), fallback == null ? LocalDateTime.now() : fallback.getCreatedAt()))
            .build();
    }

    private List<RoleVO> applyPersistedRolePermissions(List<RoleVO> roles) {
        return roles.stream()
            .map(role -> {
                String raw = getSystemConfigValue(ROLE_PERMISSIONS_CONFIG_PREFIX + role.getCode());
                if (raw == null || raw.isBlank()) {
                    return role;
                }
                try {
                    List<String> permissions = objectMapper.readValue(raw, new TypeReference<List<String>>() {});
                    role.setPermissions(permissions == null ? new ArrayList<>() : permissions);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to read role permissions: role={}", role.getCode(), e);
                }
                return role;
            })
            .collect(Collectors.toList());
    }

    private Map<String, Object> loadUserOverrides(Long farmId) {
        String raw = getSystemConfigValue(USER_OVERRIDES_CONFIG_PREFIX + (farmId == null ? 1L : farmId));
        if (raw == null || raw.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> overrides = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
            return overrides == null ? new LinkedHashMap<>() : overrides;
        } catch (JsonProcessingException e) {
            log.warn("Failed to read persisted user overrides: farmId={}", farmId, e);
            return new LinkedHashMap<>();
        }
    }

    private void saveUserOverrides(Long farmId, Map<String, Object> overrides) {
        try {
            upsertSystemConfig(
                USER_OVERRIDES_CONFIG_PREFIX + (farmId == null ? 1L : farmId),
                objectMapper.writeValueAsString(overrides),
                "json",
                "Persisted demo user overrides",
                farmId
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to save user overrides", e);
        }
    }

    private boolean isHiddenConfigKey(String key) {
        return key != null && HIDDEN_CONFIG_PREFIXES.stream().anyMatch(key::startsWith);
    }

    private List<Map<String, Object>> getListMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        return list.stream()
            .filter(Map.class::isInstance)
            .map(item -> castMap((Map<?, ?>) item))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private Map<String, Object> getObjectMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value instanceof Map<?, ?> map ? new LinkedHashMap<>(castMap(map)) : new LinkedHashMap<>();
    }

    private List<Long> getLongList(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        return list.stream()
            .map(item -> toLong(item, null))
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private String resolveRoleName(Long roleId) {
        if (roleId == null) {
            return "农场管理员";
        }
        return switch (roleId.intValue()) {
            case 1 -> "超级管理员";
            case 3 -> "兽医";
            case 4 -> "饲养员";
            default -> "农场管理员";
        };
    }

    private LocalDateTime parseDateTime(Object value, LocalDateTime fallback) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return LocalDateTime.parse(str);
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private Long toLong(Object value, Long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private Integer toInt(Object value, Integer fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private String asString(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private SystemConfigVO mergeStoredConfig(SystemConfigVO defaults, SystemConfigEntity stored) {
        if (stored == null) {
            return defaults;
        }
        return SystemConfigVO.builder()
            .key(defaults.getKey())
            .value(stored.getConfigValue())
            .type(stored.getConfigType() == null ? defaults.getType() : stored.getConfigType())
            .description(stored.getDescription() == null ? defaults.getDescription() : stored.getDescription())
            .category(defaults.getCategory())
            .build();
    }

    private SystemConfigVO toConfigVO(SystemConfigEntity entity) {
        return SystemConfigVO.builder()
            .key(entity.getConfigKey())
            .value(entity.getConfigValue())
            .type(entity.getConfigType() == null ? inferConfigType(entity.getConfigValue()) : entity.getConfigType())
            .description(entity.getDescription())
            .category(inferConfigCategory(entity.getConfigKey()))
            .build();
    }

    private String inferConfigCategory(String key) {
        if (key == null) {
            return "custom";
        }
        if (key.startsWith("dashboard_")) {
            return "dashboard";
        }
        if (key.startsWith("breeding_") || key.startsWith("estrus_")) {
            return "breeding";
        }
        if (key.startsWith("disease_")) {
            return "disease";
        }
        if (key.startsWith("behavior_")) {
            return "behavior";
        }
        if (key.startsWith("device_")) {
            return "device";
        }
        if (key.contains("notice") || key.contains("push") || key.endsWith("_enabled")) {
            return "notification";
        }
        return "custom";
    }

    private String inferConfigType(String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return "boolean";
        }
        if (value != null && value.matches("-?\\d+(\\.\\d+)?")) {
            return "number";
        }
        return "text";
    }

    private List<SystemConfigVO> createBaseSystemConfigs() {
        return Arrays.asList(
            SystemConfigVO.builder().key("data_retention_days").value("365").type("number").description("健康数据保留天数").category("basic").build(),
            SystemConfigVO.builder().key("alert_push_enabled").value("true").type("boolean").description("是否启用告警推送").category("basic").build(),
            SystemConfigVO.builder().key("data_upload_interval").value("60").type("number").description("数据上传间隔(分钟)").category("basic").build(),

            SystemConfigVO.builder().key("temperature_min").value("38.0").type("number").description("体温下限(℃)").category("threshold").build(),
            SystemConfigVO.builder().key("temperature_max").value("41.0").type("number").description("体温上限(℃)").category("threshold").build(),
            SystemConfigVO.builder().key("heart_rate_min").value("60").type("number").description("心率下限(次/分)").category("threshold").build(),
            SystemConfigVO.builder().key("heart_rate_max").value("120").type("number").description("心率上限(次/分)").category("threshold").build(),
            SystemConfigVO.builder().key("rumination_min").value("280").type("number").description("反刍时长下限(分钟)").category("threshold").build(),
            SystemConfigVO.builder().key("feeding_min").value("15").type("number").description("采食次数下限(次)").category("threshold").build(),

            SystemConfigVO.builder().key("wechat_enabled").value("true").type("boolean").description("微信推送开关").category("notification").build(),
            SystemConfigVO.builder().key("sms_enabled").value("true").type("boolean").description("短信推送开关").category("notification").build(),
            SystemConfigVO.builder().key("email_enabled").value("false").type("boolean").description("邮件推送开关").category("notification").build(),

            SystemConfigVO.builder().key("device_protocol").value("MQTT").type("text").description("设备通信协议").category("device").build(),
            SystemConfigVO.builder().key("device_model").value("LH-COLLAR-X1").type("text").description("默认设备型号").category("device").build(),
            SystemConfigVO.builder().key("device_low_battery").value("20").type("number").description("低电量阈值(%)").category("device").build(),
            SystemConfigVO.builder().key("device_signal_min").value("40").type("number").description("最小信号阈值").category("device").build(),

            SystemConfigVO.builder().key("estrus_threshold").value("80").type("number").description("发情判定阈值").category("breeding").build(),
            SystemConfigVO.builder().key("breeding_window_hours").value("24").type("number").description("重点配种窗口(小时)").category("breeding").build(),
            SystemConfigVO.builder().key("breeding_notice_hours").value("12").type("number").description("提前提醒时长(小时)").category("breeding").build(),
            SystemConfigVO.builder().key("breeding_probability_threshold").value("80").type("number").description("发情预警概率阈值").category("breeding").build(),
            SystemConfigVO.builder().key("breeding_prediction_cycle").value("高频(每小时)").type("text").description("发情预测周期").category("breeding").build(),
            SystemConfigVO.builder().key("breeding_strategy").value("自动推荐").type("text").description("配种策略").category("breeding").build(),
            SystemConfigVO.builder().key("breeding_config_note").value("").type("text").description("配种配置备注").category("breeding").build(),
            SystemConfigVO.builder().key("breeding_notice_channel").value("system").type("text").description("配种通知渠道").category("breeding").build(),
            SystemConfigVO.builder().key("breeding_notice_probability").value("80").type("number").description("配种通知概率阈值").category("breeding").build(),
            SystemConfigVO.builder().key("breeding_notice_frequency").value("realtime").type("text").description("配种通知频率").category("breeding").build(),

            SystemConfigVO.builder().key("disease_high_risk_threshold").value("85").type("number").description("疾病高风险阈值").category("disease").build(),
            SystemConfigVO.builder().key("disease_medium_risk_threshold").value("70").type("number").description("疾病中风险阈值").category("disease").build(),
            SystemConfigVO.builder().key("disease_assessment_cycle").value("实时评估").type("text").description("疾病评估周期").category("disease").build(),
            SystemConfigVO.builder().key("disease_alert_strategy").value("均衡").type("text").description("疾病告警策略").category("disease").build(),
            SystemConfigVO.builder().key("disease_model_update").value("2025-12-20").type("text").description("模型最近更新时间").category("disease").build(),

            SystemConfigVO.builder().key("behavior_rumination_threshold").value("280").type("number").description("反刍时长预警阈值").category("behavior").build(),
            SystemConfigVO.builder().key("behavior_feeding_threshold").value("15").type("number").description("采食次数预警阈值").category("behavior").build(),
            SystemConfigVO.builder().key("behavior_sensitivity").value("中").type("text").description("行为分析敏感度").category("behavior").build(),
            SystemConfigVO.builder().key("behavior_analysis_cycle").value("每日").type("text").description("行为分析周期").category("behavior").build(),

            SystemConfigVO.builder().key("dashboard_refresh_seconds").value("60").type("number").description("看板自动刷新间隔(秒)").category("dashboard").build(),
            SystemConfigVO.builder().key("dashboard_show_heatmap").value("true").type("boolean").description("显示行为热力图").category("dashboard").build(),
            SystemConfigVO.builder().key("dashboard_show_focus").value("true").type("boolean").description("显示重点关注列表").category("dashboard").build(),
            SystemConfigVO.builder().key("dashboard_chart_type").value("line").type("text").description("看板趋势图类型").category("dashboard").build(),
            SystemConfigVO.builder().key("dashboard_default_group").value("全部羊群").type("text").description("看板默认分组").category("dashboard").build()
        );
    }
}

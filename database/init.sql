-- 畜牧健康监测与预警管理平台 V1.0 - 数据库初始化脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS livestock_health CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE livestock_health;

-- ============================================
-- 基础数据表
-- ============================================

-- 农场信息表
CREATE TABLE farm (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '农场名称',
    code VARCHAR(50) UNIQUE NOT NULL COMMENT '农场编码',
    address VARCHAR(255) COMMENT '农场地址',
    contact_name VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农场信息表';

-- 用户表
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码 (加密)',
    real_name VARCHAR(50) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    role_id BIGINT COMMENT '角色 ID',
    farm_id BIGINT COMMENT '所属农场 ID',
    status TINYINT DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    last_login_at DATETIME COMMENT '最后登录时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_farm_id (farm_id),
    FOREIGN KEY (farm_id) REFERENCES farm(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) UNIQUE NOT NULL COMMENT '角色名称',
    code VARCHAR(50) UNIQUE NOT NULL COMMENT '角色编码',
    description VARCHAR(255) COMMENT '角色描述',
    permissions JSON COMMENT '权限列表',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限表
CREATE TABLE permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '权限名称',
    code VARCHAR(100) UNIQUE NOT NULL COMMENT '权限编码',
    resource_type VARCHAR(20) COMMENT '资源类型 menu/button/api',
    parent_id BIGINT DEFAULT 0 COMMENT '父级 ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ============================================
-- 牲畜相关表
-- ============================================

-- 牲畜档案表
CREATE TABLE animal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    animal_id VARCHAR(50) UNIQUE NOT NULL COMMENT '牲畜编号 (耳标号)',
    farm_id BIGINT NOT NULL COMMENT '所属农场 ID',
    name VARCHAR(50) COMMENT '牲畜名称',
    breed VARCHAR(50) COMMENT '品种',
    gender TINYINT COMMENT '性别 1-公 2-母',
    age INT COMMENT '年龄 (月)',
    weight DECIMAL(5,1) COMMENT '体重 (kg)',
    group_id BIGINT COMMENT '所属群组 ID',
    
    -- 健康指标
    health_score DECIMAL(5,2) DEFAULT 100.00 COMMENT '健康评分',
    risk_level VARCHAR(20) DEFAULT 'normal' COMMENT '风险等级 normal/low/medium/high',
    
    -- 行为数据
    daily_rumination_time INT DEFAULT 0 COMMENT '日均反刍时间 (分钟)',
    daily_feeding_count INT DEFAULT 0 COMMENT '日均采食次数',
    rumination_efficiency DECIMAL(5,2) COMMENT '反刍效率 (%)',
    feeding_quality DECIMAL(5,2) COMMENT '采食质量 (%)',
    behavior_status VARCHAR(20) DEFAULT 'normal' COMMENT '行为状态 normal/warning/abnormal',
    
    -- 发情期相关
    estrus_status VARCHAR(20) DEFAULT 'normal' COMMENT '发情状态 normal/estrus/pregnant',
    estrus_probability DECIMAL(5,2) DEFAULT 0.00 COMMENT '发情概率 (%)',
    last_estrus_at DATETIME COMMENT '上次发情时间',
    next_estrus_predicted_at DATETIME COMMENT '预计下次发情时间',
    
    -- 设备相关
    device_id BIGINT COMMENT '绑定设备 ID',
    
    last_behavior_update_at DATETIME COMMENT '最后行为数据更新时间',
    last_diet_adjust_at DATETIME COMMENT '最后饲喂调整时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_animal_id (animal_id),
    INDEX idx_farm_id (farm_id),
    INDEX idx_behavior_status (behavior_status),
    INDEX idx_risk_level (risk_level),
    INDEX idx_estrus_status (estrus_status),
    FOREIGN KEY (farm_id) REFERENCES farm(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='牲畜档案表';

-- 牲畜群组表
CREATE TABLE animal_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    farm_id BIGINT NOT NULL COMMENT '所属农场 ID',
    name VARCHAR(100) NOT NULL COMMENT '群组名称',
    description VARCHAR(255) COMMENT '群组描述',
    animal_count INT DEFAULT 0 COMMENT '牲畜数量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (farm_id) REFERENCES farm(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='牲畜群组表';

-- ============================================
-- 智能设备相关表
-- ============================================

-- 智能项圈设备表
CREATE TABLE device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id VARCHAR(50) UNIQUE NOT NULL COMMENT '设备编号',
    device_model VARCHAR(50) COMMENT '设备型号',
    device_sn VARCHAR(100) COMMENT '设备序列号',
    farm_id BIGINT COMMENT '所属农场 ID',
    animal_id BIGINT COMMENT '绑定牲畜 ID',
    
    -- 状态信息
    status VARCHAR(20) DEFAULT 'offline' COMMENT '状态 online/offline/fault',
    battery_level INT DEFAULT 100 COMMENT '电量百分比',
    signal_strength INT COMMENT '信号强度',
    
    -- 实时数据
    current_temperature DECIMAL(5,2) COMMENT '当前体温 (°C)',
    current_heart_rate INT COMMENT '当前心率 (次/分)',
    current_activity INT COMMENT '当前活动量',
    
    last_online_at DATETIME COMMENT '最后在线时间',
    last_data_update_at DATETIME COMMENT '最后数据更新时间',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_device_id (device_id),
    INDEX idx_farm_id (farm_id),
    INDEX idx_status (status),
    FOREIGN KEY (farm_id) REFERENCES farm(id),
    FOREIGN KEY (animal_id) REFERENCES animal(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能项圈设备表';

-- 设备配置表
CREATE TABLE device_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    farm_id BIGINT NOT NULL COMMENT '所属农场 ID',
    device_model VARCHAR(50) COMMENT '适用设备型号',
    
    -- 采集配置
    data_upload_interval INT DEFAULT 60 COMMENT '数据上传间隔 (秒)',
    temperature_threshold_low DECIMAL(5,2) DEFAULT 38.0 COMMENT '体温下限阈值',
    temperature_threshold_high DECIMAL(5,2) DEFAULT 41.0 COMMENT '体温上限阈值',
    heart_rate_threshold_low INT DEFAULT 60 COMMENT '心率下限阈值',
    heart_rate_threshold_high INT DEFAULT 120 COMMENT '心率上限阈值',
    
    -- 告警配置
    enable_temperature_alert BOOLEAN DEFAULT TRUE COMMENT '启用体温告警',
    enable_heart_rate_alert BOOLEAN DEFAULT TRUE COMMENT '启用心率告警',
    enable_behavior_alert BOOLEAN DEFAULT TRUE COMMENT '启用行为告警',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (farm_id) REFERENCES farm(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备配置表';

-- ============================================
-- 健康数据表
-- ============================================

-- 健康数据记录表 (时序数据，数据量大)
CREATE TABLE health_data (
    id BIGINT AUTO_INCREMENT,
    device_id BIGINT NOT NULL COMMENT '设备 ID',
    animal_id BIGINT NOT NULL COMMENT '牲畜 ID',
    farm_id BIGINT NOT NULL COMMENT '农场 ID',
    
    -- 生理数据
    temperature DECIMAL(5,2) COMMENT '体温 (°C)',
    heart_rate INT COMMENT '心率 (次/分)',
    activity_level INT COMMENT '活动量',
    
    -- 行为数据
    rumination_time INT COMMENT '反刍时间 (分钟)',
    feeding_count INT COMMENT '采食次数',
    resting_time INT COMMENT '休息时间 (分钟)',
    
    -- 数据时间
    data_time DATETIME NOT NULL COMMENT '数据采集时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (id, data_time),
    INDEX idx_device_id (device_id),
    INDEX idx_animal_id (animal_id),
    INDEX idx_farm_id (farm_id),
    INDEX idx_data_time (data_time),
    INDEX idx_farm_time (farm_id, data_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康数据记录表'
PARTITION BY RANGE (YEAR(data_time)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_max VALUES LESS THAN MAXVALUE
);

-- ============================================
-- 告警相关表
-- ============================================

-- 告警规则表
CREATE TABLE alert_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    farm_id BIGINT NOT NULL COMMENT '所属农场 ID',
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    rule_type VARCHAR(50) NOT NULL COMMENT '规则类型 temperature/heart_rate/behavior/estrus/disease',
    
    -- 规则配置
    condition_field VARCHAR(50) COMMENT '条件字段',
    condition_operator VARCHAR(10) COMMENT '操作符 >/</=/>=/<=/in',
    condition_value VARCHAR(100) COMMENT '条件值',
    
    -- 告警配置
    severity VARCHAR(20) DEFAULT 'medium' COMMENT '严重等级 low/medium/high/critical',
    enable BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    
    -- 推送配置
    push_channels JSON COMMENT '推送渠道 ["wechat","sms","app"]',
    push_receivers JSON COMMENT '推送接收者',
    
    created_by BIGINT COMMENT '创建人 ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (farm_id) REFERENCES farm(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则表';

-- 告警事件表
CREATE TABLE alert_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alert_id VARCHAR(50) UNIQUE NOT NULL COMMENT '告警 ID',
    farm_id BIGINT NOT NULL COMMENT '所属农场 ID',
    animal_id BIGINT COMMENT '相关牲畜 ID',
    device_id BIGINT COMMENT '相关设备 ID',
    rule_id BIGINT COMMENT '触发规则 ID',
    
    -- 告警信息
    alert_type VARCHAR(50) NOT NULL COMMENT '告警类型',
    severity VARCHAR(20) NOT NULL COMMENT '严重等级',
    title VARCHAR(200) NOT NULL COMMENT '告警标题',
    message TEXT COMMENT '告警内容',
    
    -- 告警数据
    trigger_value VARCHAR(100) COMMENT '触发值',
    threshold_value VARCHAR(100) COMMENT '阈值',
    
    -- 状态
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态 pending/acknowledged/resolved',
    acknowledged_by BIGINT COMMENT '确认人 ID',
    acknowledged_at DATETIME COMMENT '确认时间',
    resolved_by BIGINT COMMENT '解决人 ID',
    resolved_at DATETIME COMMENT '解决时间',
    resolution_note TEXT COMMENT '解决说明',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_alert_id (alert_id),
    INDEX idx_farm_id (farm_id),
    INDEX idx_status (status),
    INDEX idx_severity (severity),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (farm_id) REFERENCES farm(id),
    FOREIGN KEY (animal_id) REFERENCES animal(id),
    FOREIGN KEY (device_id) REFERENCES device(id),
    FOREIGN KEY (rule_id) REFERENCES alert_rule(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警事件表';

-- 告警推送记录表
CREATE TABLE alert_push_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alert_event_id BIGINT NOT NULL COMMENT '告警事件 ID',
    channel VARCHAR(20) NOT NULL COMMENT '推送渠道',
    receiver VARCHAR(100) NOT NULL COMMENT '接收者',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态 pending/success/failed',
    error_message TEXT COMMENT '错误信息',
    pushed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (alert_event_id) REFERENCES alert_event(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警推送记录表';

-- ============================================
-- 行为分析相关表
-- ============================================

-- 行为分析配置表
CREATE TABLE behavior_analysis_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    farm_id BIGINT NOT NULL COMMENT '所属农场 ID',
    
    -- 反刍配置
    rumination_threshold INT DEFAULT 280 COMMENT '反刍时间阈值 (分钟)',
    rumination_weight VARCHAR(20) DEFAULT 'high' COMMENT '反刍效率权重',
    
    -- 采食配置
    feeding_threshold INT DEFAULT 15 COMMENT '采食次数阈值',
    feeding_weight VARCHAR(20) DEFAULT 'high' COMMENT '采食质量权重',
    
    -- 模型配置
    model_version VARCHAR(20) DEFAULT 'v1.0' COMMENT '算法模型版本',
    
    is_latest BOOLEAN DEFAULT TRUE COMMENT '是否最新配置',
    notes TEXT COMMENT '备注说明',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (farm_id) REFERENCES farm(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行为分析配置表';

-- 行为事件表
CREATE TABLE behavior_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(50) UNIQUE NOT NULL COMMENT '事件 ID',
    farm_id BIGINT NOT NULL COMMENT '所属农场 ID',
    animal_id BIGINT NOT NULL COMMENT '牲畜 ID',
    
    -- 事件信息
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型 BEHAVIOR_ABNORMAL/RUMINATION_LOW/FEEDING_LOW',
    event_time DATETIME NOT NULL COMMENT '事件发生时间',
    duration INT COMMENT '持续时间 (分钟)',
    description TEXT COMMENT '事件描述',
    
    -- 事件数据
    rumination_time INT COMMENT '反刍时间 (分钟)',
    feeding_count INT COMMENT '采食次数',
    resting_time INT COMMENT '休息时间 (分钟)',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_event_id (event_id),
    INDEX idx_farm_id (farm_id),
    INDEX idx_animal_id (animal_id),
    INDEX idx_event_time (event_time),
    FOREIGN KEY (farm_id) REFERENCES farm(id),
    FOREIGN KEY (animal_id) REFERENCES animal(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行为事件表';

-- 饲喂方案表
CREATE TABLE diet_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    farm_id BIGINT NOT NULL COMMENT '所属农场 ID',
    animal_id BIGINT NOT NULL COMMENT '牲畜 ID',
    
    -- 饲喂配置
    forage_type VARCHAR(50) COMMENT '饲草类型',
    concentrate_ratio VARCHAR(10) COMMENT '精料配比',
    daily_amount DECIMAL(5,1) COMMENT '日饲喂量 (kg)',
    frequency INT COMMENT '饲喂频次 (次/天)',
    
    -- 添加剂
    add_probiotics BOOLEAN DEFAULT FALSE COMMENT '益生菌',
    add_vitamins BOOLEAN DEFAULT FALSE COMMENT '复合维生素',
    add_minerals BOOLEAN DEFAULT FALSE COMMENT '矿物质补充剂',
    
    -- 其他
    reason TEXT COMMENT '调整原因',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    
    created_by BIGINT COMMENT '创建人 ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (farm_id) REFERENCES farm(id),
    FOREIGN KEY (animal_id) REFERENCES animal(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饲喂方案表';

-- ============================================
-- 配种相关表
-- ============================================

-- 配种计划表
CREATE TABLE breeding_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    farm_id BIGINT NOT NULL COMMENT '所属农场 ID',
    animal_id BIGINT NOT NULL COMMENT '牲畜 ID',
    
    -- 配种信息
    plan_type VARCHAR(20) COMMENT '计划类型 natural/artificial',
    planned_date DATE COMMENT '计划配种日期',
    actual_date DATE COMMENT '实际配种日期',
    
    -- 配种对象
    male_animal_id VARCHAR(50) COMMENT '配种公畜编号',
    semen_batch VARCHAR(50) COMMENT '精液批次',
    
    -- 结果
    result VARCHAR(20) COMMENT '结果 success/failed/pending',
    pregnancy_check_date DATE COMMENT '孕检日期',
    pregnancy_status VARCHAR(20) COMMENT '孕检结果',
    
    -- 其他
    notes TEXT COMMENT '备注',
    created_by BIGINT COMMENT '创建人 ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (farm_id) REFERENCES farm(id),
    FOREIGN KEY (animal_id) REFERENCES animal(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配种计划表';

-- ============================================
-- 系统配置表
-- ============================================

-- 系统配置表
CREATE TABLE system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) UNIQUE NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type VARCHAR(20) COMMENT '配置类型 string/number/boolean/json',
    description VARCHAR(255) COMMENT '配置描述',
    farm_id BIGINT COMMENT '所属农场 ID (NULL 为全局配置)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 审计日志表
CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT '操作用户 ID',
    username VARCHAR(50) COMMENT '操作用户名',
    
    -- 操作信息
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    operation_module VARCHAR(50) COMMENT '操作模块',
    operation_desc VARCHAR(500) COMMENT '操作描述',
    
    -- 请求信息
    request_method VARCHAR(10) COMMENT '请求方法',
    request_uri VARCHAR(500) COMMENT '请求 URI',
    request_params TEXT COMMENT '请求参数',
    ip_address VARCHAR(50) COMMENT 'IP 地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    
    -- 响应信息
    response_status INT COMMENT '响应状态码',
    response_time INT COMMENT '响应时间 (ms)',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- ============================================
-- 初始化数据
-- ============================================

-- 初始化农场
INSERT INTO farm (name, code, address, contact_name, contact_phone) VALUES
('示范牧场', 'DEMO_FARM_001', '内蒙古自治区呼和浩特市', '张三', '13800138000');

-- 初始化角色
INSERT INTO role (name, code, description, permissions) VALUES
('超级管理员', 'SUPER_ADMIN', '系统超级管理员，拥有所有权限', '["*"]'),
('农场管理员', 'FARM_ADMIN', '农场管理员，管理农场所有事务', '["animal:*", "device:*", "alert:*", "user:view", "report:view"]'),
('兽医', 'VETERINARIAN', '兽医角色，负责健康管理', '["animal:view", "animal:treat", "disease:*", "report:view"]'),
('饲养员', 'FEEDER', '饲养员角色，负责日常饲养', '["animal:view", "feeding:*", "behavior:view"]');

-- 初始化默认用户 (密码：admin123)
INSERT INTO user (username, password, real_name, phone, role_id, farm_id, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', '13800138000', 1, 1, 1);

-- 初始化系统配置
INSERT INTO system_config (config_key, config_value, config_type, description) VALUES
('data_retention_days', '365', 'number', '健康数据保留天数'),
('alert_push_enabled', 'true', 'boolean', '是否启用告警推送'),
('default_temperature_low', '38.0', 'number', '默认体温下限 (°C)'),
('default_temperature_high', '41.0', 'number', '默认体温上限 (°C)');

-- 初始化告警规则
INSERT INTO alert_rule (farm_id, name, rule_type, condition_field, condition_operator, condition_value, severity, enable, push_channels) VALUES
(1, '体温过高告警', 'temperature', 'temperature', '>', '40.5', 'high', TRUE, '["wechat","sms"]'),
(1, '体温过低告警', 'temperature', 'temperature', '<', '37.5', 'high', TRUE, '["wechat","sms"]'),
(1, '反刍时间不足告警', 'behavior', 'rumination_time', '<', '280', 'medium', TRUE, '["wechat"]'),
(1, '发情期识别告警', 'estrus', 'estrus_probability', '>', '80', 'medium', TRUE, '["wechat"]');

-- 初始化设备配置
INSERT INTO device_config (farm_id, device_model, data_upload_interval, temperature_threshold_low, temperature_threshold_high) VALUES
(1, 'SMART_COLLAR_V1', 60, 38.0, 41.0);

-- 初始化行为分析配置
INSERT INTO behavior_analysis_config (farm_id, rumination_threshold, feeding_threshold, rumination_weight, feeding_weight, model_version, is_latest) VALUES
(1, 280, 15, 'high', 'high', 'v1.0', TRUE);

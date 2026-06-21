-- =====================================================================
-- 数据库 schema：7 张表
-- 都用 IF NOT EXISTS，Spring Boot 启动时反复跑也不会爆
-- 状态字段都存中文枚举值，方便直接看，约束在应用层 + DB CHECK 共同保证
-- =====================================================================

-- -----------------------------------------------------
-- 表 1: users 用户表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id       INT          NOT NULL AUTO_INCREMENT COMMENT '用户编号，主键',
    username      VARCHAR(50)  NOT NULL                COMMENT '登录账号（学号/工号），系统内唯一',
    password      VARCHAR(100) NOT NULL                COMMENT '登录密码，BCrypt 加密',
    name          VARCHAR(100) NOT NULL                COMMENT '姓名，中文≤20汉字/英文≤100字符',
    role          VARCHAR(20)  NOT NULL DEFAULT 'volunteer' COMMENT 'volunteer/organizer/admin/superadmin',
    phone         CHAR(11)     NULL                    COMMENT '中国大陆手机号',
    is_organizer  TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '是否具备组织者资质',
    is_admin      TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '是否被超管赋权管理员',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_username (username),
    KEY idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -----------------------------------------------------
-- 表 2: activities 活动表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS activities (
    activity_id    INT          NOT NULL AUTO_INCREMENT COMMENT '活动编号，主键',
    title          VARCHAR(100) NOT NULL                COMMENT '活动名称',
    description    VARCHAR(500) NULL                    COMMENT '活动简介',
    location       VARCHAR(100) NOT NULL                COMMENT '活动地点',
    start_time     DATETIME     NOT NULL                COMMENT '开始时间',
    end_time       DATETIME     NOT NULL                COMMENT '结束时间，应晚于 start_time',
    capacity       INT          NOT NULL                COMMENT '招募人数 1~1000',
    organizer_id   INT          NOT NULL                COMMENT '创建组织者，FK→users',
    audit_status   VARCHAR(20)  NOT NULL DEFAULT '待审核' COMMENT '待审核/审核通过/审核不通过',
    publish_status VARCHAR(20)  NOT NULL DEFAULT '已停止' COMMENT '发布中/已停止',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (activity_id),
    KEY idx_activities_organizer (organizer_id),
    KEY idx_activities_status (audit_status, publish_status),
    KEY idx_activities_start (start_time),
    CONSTRAINT fk_activities_organizer FOREIGN KEY (organizer_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='志愿活动表';

-- -----------------------------------------------------
-- 表 3: registrations 报名表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS registrations (
    reg_id        INT         NOT NULL AUTO_INCREMENT COMMENT '报名编号，主键',
    activity_id   INT         NOT NULL                COMMENT 'FK→activities',
    volunteer_id  INT         NOT NULL                COMMENT 'FK→users',
    audit_status  VARCHAR(20) NOT NULL DEFAULT '待审核' COMMENT '待审核/审核通过/审核拒绝/已取消',
    applied_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    audited_at    DATETIME    NULL                    COMMENT '审核完成时间',
    PRIMARY KEY (reg_id),
    UNIQUE KEY uk_reg_act_vol (activity_id, volunteer_id),
    KEY idx_reg_volunteer (volunteer_id),
    KEY idx_reg_act_status (activity_id, audit_status),
    CONSTRAINT fk_reg_activity FOREIGN KEY (activity_id) REFERENCES activities (activity_id),
    CONSTRAINT fk_reg_volunteer FOREIGN KEY (volunteer_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报名表';

-- -----------------------------------------------------
-- 表 4: attendance 签到与志愿时表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS attendance (
    record_id        INT         NOT NULL AUTO_INCREMENT COMMENT '签到记录编号',
    activity_id      INT         NOT NULL                COMMENT 'FK→activities',
    volunteer_id     INT         NOT NULL                COMMENT 'FK→users',
    check_in_time    DATETIME    NULL                    COMMENT '签到时间',
    check_out_time   DATETIME    NULL                    COMMENT '签退时间，应晚于签到',
    service_hours    INT         NOT NULL DEFAULT 0      COMMENT '认证工时-小时',
    service_minutes  INT         NOT NULL DEFAULT 0      COMMENT '认证工时-分钟 0~59',
    status           VARCHAR(20) NOT NULL DEFAULT '未签到' COMMENT '未签到/已签到/已签退/漏签退/异常',
    PRIMARY KEY (record_id),
    UNIQUE KEY uk_att_act_vol (activity_id, volunteer_id),
    KEY idx_att_status (status),
    CONSTRAINT fk_att_activity FOREIGN KEY (activity_id) REFERENCES activities (activity_id),
    CONSTRAINT fk_att_volunteer FOREIGN KEY (volunteer_id) REFERENCES users (user_id),
    CONSTRAINT ck_att_minutes CHECK (service_minutes BETWEEN 0 AND 59),
    CONSTRAINT ck_att_hours CHECK (service_hours >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签到与志愿时表';

-- -----------------------------------------------------
-- 表 5: certificates 证书表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS certificates (
    cert_id       INT          NOT NULL AUTO_INCREMENT COMMENT '证书编号',
    title         VARCHAR(100) NOT NULL                COMMENT '证书标题',
    activity_id   INT          NOT NULL                COMMENT 'FK→activities',
    volunteer_id  INT          NOT NULL                COMMENT 'FK→users',
    start_time    DATETIME     NOT NULL                COMMENT '活动开始时间（冗余）',
    end_time      DATETIME     NOT NULL                COMMENT '活动结束时间（冗余）',
    cert_hours    INT          NOT NULL DEFAULT 0      COMMENT '认证工时-小时',
    cert_minutes  INT          NOT NULL DEFAULT 0      COMMENT '认证工时-分钟 0~59',
    issued_date   DATE         NOT NULL                COMMENT '发放日期',
    status        VARCHAR(20)  NOT NULL DEFAULT '有效'   COMMENT '有效/已失效',
    PRIMARY KEY (cert_id),
    UNIQUE KEY uk_cert_act_vol (activity_id, volunteer_id),
    KEY idx_cert_vol_status (volunteer_id, status),
    CONSTRAINT fk_cert_activity FOREIGN KEY (activity_id) REFERENCES activities (activity_id),
    CONSTRAINT fk_cert_volunteer FOREIGN KEY (volunteer_id) REFERENCES users (user_id),
    CONSTRAINT ck_cert_minutes CHECK (cert_minutes BETWEEN 0 AND 59),
    CONSTRAINT ck_cert_hours CHECK (cert_hours >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='证书表';

-- -----------------------------------------------------
-- 表 6: messages 消息通知表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS messages (
    msg_id        INT          NOT NULL AUTO_INCREMENT COMMENT '消息编号',
    msg_type      VARCHAR(20)  NOT NULL                COMMENT '报名通知/资质审核/活动通知/系统公告',
    title         VARCHAR(100) NOT NULL                COMMENT '消息标题',
    content       VARCHAR(500) NOT NULL                COMMENT '消息正文',
    receiver_id   INT          NULL                    COMMENT '单点通知接收人；公告时为空',
    target_scope  VARCHAR(30)  NULL                    COMMENT '公告范围：全体志愿者/全体组织者，可多选逗号分隔',
    send_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read       TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '0 未读 / 1 已读',
    PRIMARY KEY (msg_id),
    KEY idx_msg_receiver (receiver_id, is_read),
    KEY idx_msg_type_time (msg_type, send_time),
    CONSTRAINT fk_msg_receiver FOREIGN KEY (receiver_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息通知表';

-- -----------------------------------------------------
-- 表 7: organizer_applications 组织者资质申请表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS organizer_applications (
    app_id        INT          NOT NULL AUTO_INCREMENT COMMENT '申请编号',
    applicant_id  INT          NOT NULL                COMMENT 'FK→users',
    reason        VARCHAR(500) NOT NULL                COMMENT '申请理由',
    material_url  VARCHAR(255) NOT NULL                COMMENT '证明材料 URL',
    submitted_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    audit_status  VARCHAR(20)  NOT NULL DEFAULT '待审核' COMMENT '待审核/已通过/已拒绝',
    auditor_id    INT          NULL                    COMMENT '审核管理员 FK→users',
    audited_at    DATETIME     NULL                    COMMENT '审核时间',
    PRIMARY KEY (app_id),
    KEY idx_app_applicant_status (applicant_id, audit_status),
    KEY idx_app_status_time (audit_status, submitted_at),
    CONSTRAINT fk_app_applicant FOREIGN KEY (applicant_id) REFERENCES users (user_id),
    CONSTRAINT fk_app_auditor FOREIGN KEY (auditor_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织者资质申请表';

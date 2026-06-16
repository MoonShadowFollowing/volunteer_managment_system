-- =====================================================================
-- VMS 小规模演示种子数据（S1）
-- 所有 INSERT 都用 IGNORE 防止重启重复插入主键冲突；指定显式 ID 保证幂等
-- 大规模 ≥2000 条数据留到 S2 用 Java DataSeeder 生成
-- 密码 BCrypt 占位：全部 "123456" 的 hash（S2 实装登录时会重新生成）
-- =====================================================================

-- ---------- 1) users (1 超管 + 2 管理员 + 3 组织者 + 10 志愿者) ----------
INSERT IGNORE INTO users (user_id, username, password, name, role, phone, is_organizer, is_admin, created_at) VALUES
  (1, '00000000',   '$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva', '超级管理员', 'superadmin', NULL,           0, 0, '2024-09-01 10:00:00'),
  (2, '12345678',   '$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva', '赵主管',     'admin',      '13800000002', 1, 1, '2024-09-01 10:00:00'),
  (3, '48271936',   '$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva', '钱干事',     'admin',      '13800000003', 1, 1, '2024-09-01 10:00:00'),
  (4, '202400010101','$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva', '李四',       'volunteer',  '13800000004', 1, 0, '2024-09-01 10:00:00'),
  (5, '202411110101','$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva', '王小明',     'volunteer',  '13800000005', 1, 0, '2024-09-01 10:00:00'),
  (6, '202422220201','$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva', '孙七',       'volunteer',  '13800000006', 1, 0, '2024-09-01 10:00:00'),
  (7, '202400020101','$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva', '张三',       'volunteer',  '13812345001', 0, 0, '2024-09-01 10:00:00'),
  (8, '202433330301','$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva', '陈晓',       'volunteer',  '13812345002', 0, 0, '2024-09-01 10:00:00'),
  (9, '202444440401','$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva', '林芳',       'volunteer',  '13812345003', 0, 0, '2024-09-01 10:00:00'),
  (10,'202455550501','$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva', '周琪',       'volunteer',  '13812345004', 0, 0, '2024-09-01 10:00:00'),
  (11,'202466660601','$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva', '吴亮',       'volunteer',  '13812345005', 0, 0, '2024-09-01 10:00:00'),
  (12,'202477770701','$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva', '郑雨',       'volunteer',  '13812345006', 0, 0, '2024-09-01 10:00:00'),
  (13,'202488880801','$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva', '高远',       'volunteer',  '13812345007', 0, 0, '2024-09-01 10:00:00'),
  (14,'202499990901','$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva', '黄敏',       'volunteer',  '13812345008', 0, 0, '2024-09-01 10:00:00'),
  (15,'202410101001','$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva', '徐慧',       'volunteer',  '13812345009', 0, 0, '2024-09-01 10:00:00'),
  (16,'202412121101','$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva', '梁伟',       'volunteer',  '13812345010', 0, 0, '2024-09-01 10:00:00');

-- ---------- 2) activities (5 个，覆盖审核状态 × 发布开关组合) ----------
INSERT IGNORE INTO activities (activity_id, title, description, location, start_time, end_time, capacity, organizer_id, audit_status, publish_status) VALUES
  (1, '南校区清扫活动',       '组织志愿者进行校园清洁，维护公共环境。', '南校区主干道',     '2026-05-01 08:30:00', '2026-05-01 11:30:00', 10, 4, '审核通过', '发布中'),
  (2, '防诈骗宣讲先锋',       '面向新生开展防诈骗知识宣传。',         '图书馆报告厅',     '2026-05-15 14:00:00', '2026-05-15 16:00:00', 10, 4, '审核通过', '已停止'),
  (3, '迎新后勤志愿',         '协助新生报到、搬运行李和现场引导。',   '北门集合点',       '2026-09-01 07:00:00', '2026-09-01 12:00:00', 10, 5, '待审核',   '已停止'),
  (4, '社区敬老陪伴',         '陪伴老人聊天、做简单互动活动。',       '星光社区服务中心', '2026-06-20 09:00:00', '2026-06-20 11:00:00', 10, 5, '审核通过', '发布中'),
  (5, '河岸环保巡查',         '进行河岸垃圾巡查与环保宣传。',         '滨河步道',         '2026-07-12 15:00:00', '2026-07-12 17:30:00', 10, 6, '审核不通过', '已停止');

-- ---------- 3) registrations (8 条，4 种 audit_status 都覆盖) ----------
INSERT IGNORE INTO registrations (reg_id, activity_id, volunteer_id, audit_status, applied_at, audited_at) VALUES
  (1, 1, 7,  '审核通过', '2026-04-25 09:00:00', '2026-04-26 10:00:00'),
  (2, 1, 8,  '审核通过', '2026-04-25 09:10:00', '2026-04-26 10:05:00'),
  (3, 1, 9,  '待审核',   '2026-04-26 09:20:00', NULL),
  (4, 1, 10, '审核拒绝', '2026-04-26 09:30:00', '2026-04-26 11:00:00'),
  (5, 2, 7,  '审核通过', '2026-05-01 14:00:00', '2026-05-02 09:00:00'),
  (6, 4, 11, '审核通过', '2026-06-10 10:00:00', '2026-06-11 09:00:00'),
  (7, 4, 12, '已取消',   '2026-06-10 10:10:00', NULL),
  (8, 4, 13, '待审核',   '2026-06-11 10:00:00', NULL);

-- ---------- 4) attendance (5 条，含正常/漏签退) ----------
INSERT IGNORE INTO attendance (record_id, activity_id, volunteer_id, check_in_time, check_out_time, service_hours, service_minutes, status) VALUES
  (1, 1, 7,  '2026-05-01 08:30:00', '2026-05-01 11:30:00', 3, 0,  '已签退'),
  (2, 1, 8,  '2026-05-01 08:35:00', NULL,                  0, 0,  '漏签退'),
  (3, 2, 7,  '2026-05-15 14:00:00', '2026-05-15 16:00:00', 2, 0,  '已签退'),
  (4, 4, 11, '2026-06-20 09:00:00', '2026-06-20 11:30:00', 2, 30, '已签退'),
  (5, 4, 13, NULL,                  NULL,                  0, 0,  '未签到');

-- ---------- 5) certificates (3 条，含 1 失效) ----------
INSERT IGNORE INTO certificates (cert_id, title, activity_id, volunteer_id, start_time, end_time, cert_hours, cert_minutes, issued_date, status) VALUES
  (1, '南校区清扫服务证明', 1, 7,  '2026-05-01 08:30:00', '2026-05-01 11:30:00', 3, 0,  '2026-05-02', '有效'),
  (2, '防诈骗宣讲先锋',     2, 7,  '2026-05-15 14:00:00', '2026-05-15 16:00:00', 2, 0,  '2026-05-16', '有效'),
  (3, '敬老陪伴志愿证明',   4, 11, '2026-06-20 09:00:00', '2026-06-20 11:30:00', 0, 0,  '2026-06-21', '已失效');

-- ---------- 6) messages (5 条：公告 + 业务通知) ----------
INSERT IGNORE INTO messages (msg_id, msg_type, title, content, receiver_id, target_scope, send_time, is_read) VALUES
  (1, '系统公告', '关于五一假期志愿活动的通知', '请各位志愿者注意假期安全，按活动点要求签到签退。', NULL, '全体志愿者',           '2026-04-20 10:00:00', 0),
  (2, '系统公告', '组织者后台系统升级公告',     '系统将于今晚凌晨进行维护。',                       NULL, '全体组织者',           '2026-04-18 15:30:00', 0),
  (3, '系统公告', '志愿规范季度复盘',           '近期签到异常较多，请组织者关注现场提醒。',         NULL, '全体志愿者,全体组织者', '2026-04-28 10:00:00', 0),
  (4, '报名通知', '报名审核通过',               '您报名的【南校区清扫活动】已审核通过，请按时签到。', 7,    NULL,                   '2026-04-26 10:00:00', 0),
  (5, '资质审核', '组织者资质审核通过',         '恭喜！您的组织者资质已通过审核。',                 4,    NULL,                   '2026-04-20 09:00:00', 1);

-- ---------- 7) organizer_applications (2 条：待审核 + 已通过) ----------
INSERT IGNORE INTO organizer_applications (app_id, applicant_id, reason, material_url, submitted_at, audit_status, auditor_id, audited_at) VALUES
  (1, 4,  '希望发布学校大型活动的宣传与物料设计志愿活动', 'https://example.com/material/scau-org-01.jpg', '2026-04-18 10:00:00', '已通过', 2, '2026-04-20 09:00:00'),
  (2, 14, '希望统筹全校志愿时数录入与志愿者注册',         'https://example.com/material/scau-org-02.jpg', '2026-04-25 09:20:00', '待审核', NULL, NULL);

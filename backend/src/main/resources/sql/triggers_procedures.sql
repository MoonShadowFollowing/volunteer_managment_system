
-- ---------- 触发器 ----------

-- 触发器 1：改工时为 0 → 自动失效对应的证书
DROP TRIGGER IF EXISTS trg_attendance_hours_to_zero;
DELIMITER //
CREATE TRIGGER trg_attendance_hours_to_zero
AFTER UPDATE ON attendance
FOR EACH ROW
BEGIN
    IF NEW.service_hours = 0 AND NEW.service_minutes = 0
       AND (OLD.service_hours > 0 OR OLD.service_minutes > 0) THEN
        UPDATE certificates
        SET cert_hours = 0, cert_minutes = 0, status = '已失效'
        WHERE activity_id = NEW.activity_id AND volunteer_id = NEW.volunteer_id
          AND status = '有效';
    END IF;
END//
DELIMITER ;

-- 触发器 2：改工时 > 0 → 自动复活/补发证书
DROP TRIGGER IF EXISTS trg_attendance_hours_restore;
DELIMITER //
CREATE TRIGGER trg_attendance_hours_restore
AFTER UPDATE ON attendance
FOR EACH ROW
BEGIN
    DECLARE cert_exists INT DEFAULT 0;
    IF NEW.service_hours > 0 OR NEW.service_minutes > 0 THEN
        SELECT COUNT(*) INTO cert_exists FROM certificates
        WHERE activity_id = NEW.activity_id AND volunteer_id = NEW.volunteer_id;
        IF cert_exists > 0 THEN
            UPDATE certificates
            SET cert_hours = NEW.service_hours, cert_minutes = NEW.service_minutes,
                status = '有效', issued_date = CURDATE()
            WHERE activity_id = NEW.activity_id AND volunteer_id = NEW.volunteer_id;
        ELSE
            INSERT INTO certificates (title, activity_id, volunteer_id, start_time, end_time,
                                      cert_hours, cert_minutes, issued_date, status)
            SELECT CONCAT(a.title, '志愿服务证明'),
                   NEW.activity_id, NEW.volunteer_id,
                   a.start_time, a.end_time,
                   NEW.service_hours, NEW.service_minutes,
                   CURDATE(), '有效'
            FROM activities a WHERE a.activity_id = NEW.activity_id;
        END IF;
    END IF;
END//
DELIMITER ;

-- 触发器 3：报名审核通过 → 自动建 attendance 占位行
DROP TRIGGER IF EXISTS trg_registration_approved;
DELIMITER //
CREATE TRIGGER trg_registration_approved
AFTER UPDATE ON registrations
FOR EACH ROW
BEGIN
    DECLARE att_exists INT DEFAULT 0;
    IF NEW.audit_status = '审核通过' AND OLD.audit_status != '审核通过' THEN
        SELECT COUNT(*) INTO att_exists FROM attendance
        WHERE activity_id = NEW.activity_id AND volunteer_id = NEW.volunteer_id;
        IF att_exists = 0 THEN
            INSERT INTO attendance (activity_id, volunteer_id, service_hours, service_minutes, status)
            VALUES (NEW.activity_id, NEW.volunteer_id, 0, 0, '未签到');
        END IF;
    END IF;
END//
DELIMITER ;

-- ---------- 存储过程 ----------

-- 存储过程 1：月度志愿工时汇总报表
DROP PROCEDURE IF EXISTS sp_monthly_hours_report;
DELIMITER //
CREATE PROCEDURE sp_monthly_hours_report(IN p_year INT, IN p_month INT)
BEGIN
    DECLARE start_date DATETIME;
    DECLARE end_date DATETIME;
    SET start_date = CONCAT(p_year, '-', LPAD(p_month, 2, '0'), '-01 00:00:00');
    SET end_date   = DATE_ADD(start_date, INTERVAL 1 MONTH);

    SELECT u.username AS 学号, u.name AS 姓名,
           COUNT(a.record_id) AS 参与活动数,
           COALESCE(SUM(a.service_hours),0) + COALESCE(SUM(a.service_minutes),0) DIV 60 AS 总小时,
           COALESCE(SUM(a.service_minutes),0) % 60 AS 总分钟
    FROM users u
    JOIN attendance a ON u.user_id = a.volunteer_id
    WHERE a.status = '已签退'
      AND a.check_out_time >= start_date AND a.check_out_time < end_date
    GROUP BY u.user_id, u.username, u.name
    ORDER BY 总小时 DESC, 总分钟 DESC;
END//
DELIMITER ;

-- 存储过程 2：活动参与统计
DROP PROCEDURE IF EXISTS sp_activity_participants;
DELIMITER //
CREATE PROCEDURE sp_activity_participants(IN p_activity_id INT)
BEGIN
    SELECT u.username AS 学号, u.name AS 姓名,
           r.audit_status AS 报名状态,
           att.check_in_time AS 签到时间, att.check_out_time AS 签退时间,
           att.service_hours AS 工时小时, att.service_minutes AS 工时分钟,
           att.status AS 签到状态
    FROM registrations r
    JOIN users u ON r.volunteer_id = u.user_id
    LEFT JOIN attendance att ON att.activity_id = r.activity_id AND att.volunteer_id = r.volunteer_id
    WHERE r.activity_id = p_activity_id
    ORDER BY r.applied_at;
END//
DELIMITER ;

-- 存储过程 3：教务用户同步（单条）
DROP PROCEDURE IF EXISTS sp_sync_edu_users;
DELIMITER //
CREATE PROCEDURE sp_sync_edu_users(
    IN p_username VARCHAR(50),
    IN p_name     VARCHAR(100),
    IN p_password VARCHAR(100)
)
BEGIN
    DECLARE exist_id INT DEFAULT NULL;
    SELECT user_id INTO exist_id FROM users WHERE username = p_username;
    IF exist_id IS NOT NULL THEN
        UPDATE users SET name = p_name, password = p_password, source = 'EDU', synced_at = NOW()
        WHERE user_id = exist_id;
    ELSE
        INSERT INTO users (username, password, name, role, source, synced_at)
        VALUES (p_username, p_password, p_name, 'volunteer', 'EDU', NOW());
    END IF;
END//
DELIMITER ;

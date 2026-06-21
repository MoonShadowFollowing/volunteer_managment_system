package edu.scau.vms.config.seed;

import edu.scau.vms.common.constant.AppStatus;
import edu.scau.vms.common.constant.AttendStatus;
import edu.scau.vms.common.constant.AuditStatus;
import edu.scau.vms.common.constant.CertStatus;
import edu.scau.vms.common.constant.MsgType;
import edu.scau.vms.common.constant.PublishStatus;
import edu.scau.vms.common.constant.RegStatus;
import edu.scau.vms.common.constant.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

// 演示用的批量种子数据
// application-dev.yml 里 vms.seed.bulk=true 才会启用
// 二次启动会检测到 bulkxxx 用户已存在直接跳过，不会重复灌
// 目标量：≥200 用户 / ≥100 活动 / ≥2000 报名+签到（撑 SRS §8.2 的数据规模要求）
@Slf4j
@Component
@RequiredArgsConstructor
@Order(100)
@ConditionalOnProperty(prefix = "vms.seed", name = "bulk", havingValue = "true")
public class BulkDataSeeder implements ApplicationRunner {

    private static final String BCRYPT_123456 =
            "$2a$10$H7Mbq2486I0yeclMuoEhqubHwydIVT666w8.oXacS8TOjsh7V3cva";
    private static final String ORG_PREFIX = "bulkorg";
    private static final String VOL_PREFIX = "bulkvol";
    private static final String ACT_TITLE_PREFIX = "[bulk]";

    private static final String[] SURNAMES = {
            "王","李","张","刘","陈","杨","黄","赵","吴","周","徐","孙","马","朱","胡",
            "郭","何","高","林","罗","郑","梁","谢","宋","唐","许","韩","冯","邓","曹"
    };
    private static final String[] GIVEN = {
            "伟","芳","娜","秀英","敏","静","丽","强","磊","军","洋","勇","艳","杰","娟",
            "涛","明","超","霞","平","刚","桂英","文","建国","华","俊","辉","雪梅","琴","小明"
    };
    private static final String[] LOCATIONS = {
            "南校区主干道","图书馆报告厅","北门集合点","启林南宿舍区","教学楼A栋",
            "体育馆","学生活动中心","行政楼一楼","启林北食堂","思政路集合点",
            "翰墨园广场","思修大讲堂"
    };
    private static final String[] THEMES = {
            "校园清扫","防诈骗宣讲","迎新接待","敬老陪伴","环保巡查",
            "图书馆志愿","体测协助","心理健康宣传","失物认领整理","校史讲解",
            "二手书集市","志愿招募宣传"
    };

    private final JdbcTemplate jdbc;
    private final Random rand = new Random(20260615L);

    @Value("${vms.seed.organizers-target:14}") private int organizersTarget;
    @Value("${vms.seed.volunteers-target:200}") private int volunteersTarget;
    @Value("${vms.seed.activities-target:100}") private int activitiesTarget;
    @Value("${vms.seed.regs-target:2000}") private int regsTarget;

    @Override
    public void run(ApplicationArguments args) {
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM activities WHERE title LIKE ?",
                Integer.class, ACT_TITLE_PREFIX + "%");
        if (existing != null && existing > 0) {
            log.info("[BulkDataSeeder] 检测到 {} 个 bulk 活动已存在，跳过批量种子。", existing);
            return;
        }
        log.info("[BulkDataSeeder] 启动批量种子：目标 用户≥{}+{} 活动≥{} 报名≥{}",
                organizersTarget, volunteersTarget, activitiesTarget, regsTarget);
        long t0 = System.currentTimeMillis();

        List<Integer> orgIds = seedOrganizers();
        List<Integer> volIds = seedVolunteers();
        // 额外生成 30 条 8 位学号（username 长度为 8）的志愿者，供超管添加管理员
        List<Integer> shortUsernameVolIds = seedShortUsernameVolunteers();
        // 将短学号志愿者合并到 volIds 中，供后续活动报名等业务使用
        List<Integer> allVolIds = new ArrayList<>(volIds);
        allVolIds.addAll(shortUsernameVolIds);
        List<ActivityRow> activities = seedActivities(orgIds);
        int regs = seedRegistrations(activities, allVolIds);
        int atts = seedAttendance();
        int certs = seedCertificates();
        int msgs = seedMessages(allVolIds, orgIds);
        int apps = seedApplications(allVolIds, orgIds);

        log.info("[BulkDataSeeder] 完成 耗时 {} ms。汇总：org+{} vol+{} (含8位学号 {} 条) act+{} reg+{} att+{} cert+{} msg+{} app+{}",
                System.currentTimeMillis() - t0,
                orgIds.size(), allVolIds.size(), shortUsernameVolIds.size(),
                activities.size(), regs, atts, certs, msgs, apps);
    }

    // ---------- users ----------

    private List<Integer> seedOrganizers() {
        List<Object[]> batch = new ArrayList<>();
        for (int i = 1; i <= organizersTarget; i++) {
            // 学号格式：2024 + 固定专业代码0003 + 班级(2位) + 序号(2位)
            // 从 202400030101 开始
            int classNum = ((i - 1) / 50) + 1;
            int seqNum = ((i - 1) % 50) + 1;
            String username = String.format("20240003%02d%02d", classNum, seqNum);
            String name = randomName();
            String phone = randomPhone();
            // 组织者：role=volunteer + is_organizer=1（双身份模型）
            batch.add(new Object[]{username, BCRYPT_123456, name, Role.VOLUNTEER, phone, 1, 0});
        }
        jdbc.batchUpdate(
                "INSERT INTO users (username,password,name,role,phone,is_organizer,is_admin) VALUES (?,?,?,?,?,?,?)",
                batch);
        return jdbc.queryForList(
                "SELECT user_id FROM users WHERE username LIKE ? ORDER BY user_id",
                Integer.class, "20240003%");
    }

    private List<Integer> seedVolunteers() {
        List<Object[]> batch = new ArrayList<>();
        for (int i = 1; i <= volunteersTarget; i++) {
            // 学号格式：2024 + 固定专业代码0004 + 班级(2位) + 序号(2位)
            // 从 202400040101 开始
            int classNum = ((i - 1) / 50) + 1;
            int seqNum = ((i - 1) % 50) + 1;
            String username = String.format("20240004%02d%02d", classNum, seqNum);
            String name = randomName();
            String phone = randomPhone();
            batch.add(new Object[]{username, BCRYPT_123456, name, Role.VOLUNTEER, phone, 0, 0});
        }
        jdbc.batchUpdate(
                "INSERT INTO users (username,password,name,role,phone,is_organizer,is_admin) VALUES (?,?,?,?,?,?,?)",
                batch);
        return jdbc.queryForList(
                "SELECT user_id FROM users WHERE username LIKE ? ORDER BY user_id",
                Integer.class, "20240004%");
    }

    /**
     * 生成 30 条 username 为 8 位数字的志愿者（学号/工号格式）
     * 用于超管添加管理员时的候选用户
     * username 范围：20240101 ~ 20240130，均为 8 位
     */
    private List<Integer> seedShortUsernameVolunteers() {
        // 检查是否已存在，避免重复插入
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username BETWEEN '20240101' AND '20240130'",
                Integer.class);
        if (existing != null && existing > 0) {
            log.info("[BulkDataSeeder] 8位学号志愿者已存在 {} 条，跳过生成。", existing);
            return jdbc.queryForList(
                    "SELECT user_id FROM users WHERE username BETWEEN '20240101' AND '20240130' ORDER BY user_id",
                    Integer.class);
        }

        List<Object[]> batch = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            // 生成 20240101 ~ 20240130
            String username = String.format("202401%02d", i);
            String name = randomName();
            String phone = randomPhone();
            batch.add(new Object[]{username, BCRYPT_123456, name, Role.VOLUNTEER, phone, 0, 0});
        }
        jdbc.batchUpdate(
                "INSERT IGNORE INTO users (username,password,name,role,phone,is_organizer,is_admin) VALUES (?,?,?,?,?,?,?)",
                batch);
        log.info("[BulkDataSeeder] 成功生成 {} 条 8 位学号志愿者", batch.size());
        return jdbc.queryForList(
                "SELECT user_id FROM users WHERE username BETWEEN '20240101' AND '20240130' ORDER BY user_id",
                Integer.class);
    }

    // ---------- activities ----------

    private List<ActivityRow> seedActivities(List<Integer> orgIds) {
        LocalDate today = LocalDate.now();
        List<Object[]> batch = new ArrayList<>();
        for (int i = 1; i <= activitiesTarget; i++) {
            String theme = THEMES[rand.nextInt(THEMES.length)];
            String title = String.format("%s%s #%03d", ACT_TITLE_PREFIX, theme, i);
            String desc = theme + "志愿活动，欢迎志愿者积极参与，按时签到。";
            String loc = LOCATIONS[rand.nextInt(LOCATIONS.length)];

            // 70% 历史活动（-365 ~ -1 天）；30% 未来活动（+1 ~ +180 天）
            LocalDate day = rand.nextDouble() < 0.7
                    ? today.minusDays(1 + rand.nextInt(365))
                    : today.plusDays(1 + rand.nextInt(180));
            int startHour = 8 + rand.nextInt(10);
            int durHours = 2 + rand.nextInt(4);
            LocalDateTime start = day.atTime(startHour, 0);
            LocalDateTime end = start.plusHours(durHours);

            int capacity = 10 + rand.nextInt(40);
            int organizerId = orgIds.get(rand.nextInt(orgIds.size()));

            // 审核状态分布：75% 通过 / 10% 待审核 / 15% 不通过
            double r = rand.nextDouble();
            String audit = r < 0.75 ? AuditStatus.APPROVED
                    : r < 0.85 ? AuditStatus.PENDING
                    : AuditStatus.REJECTED;
            // 发布状态：仅审核通过的 80% 发布中、20% 已停止
            String publish = AuditStatus.APPROVED.equals(audit) && rand.nextDouble() < 0.8
                    ? PublishStatus.PUBLISHED : PublishStatus.STOPPED;

            batch.add(new Object[]{title, desc, loc,
                    Timestamp.valueOf(start), Timestamp.valueOf(end),
                    capacity, organizerId, audit, publish});
        }
        jdbc.batchUpdate(
                "INSERT INTO activities (title,description,location,start_time,end_time,capacity,organizer_id,audit_status,publish_status) " +
                "VALUES (?,?,?,?,?,?,?,?,?)",
                batch);
        return jdbc.query(
                "SELECT activity_id,organizer_id,audit_status,start_time,end_time " +
                "FROM activities WHERE title LIKE ? ORDER BY activity_id",
                (rs, n) -> new ActivityRow(
                        rs.getInt(1), rs.getInt(2), rs.getString(3),
                        rs.getTimestamp(4).toLocalDateTime(),
                        rs.getTimestamp(5).toLocalDateTime()),
                ACT_TITLE_PREFIX + "%");
    }

    // ---------- registrations ----------

    private int seedRegistrations(List<ActivityRow> activities, List<Integer> volIds) {
        List<ActivityRow> approved = new ArrayList<>();
        for (ActivityRow a : activities) {
            if (AuditStatus.APPROVED.equals(a.audit())) approved.add(a);
        }
        if (approved.isEmpty()) return 0;

        int perActivity = Math.max(8, regsTarget / approved.size() + 4);
        List<Object[]> batch = new ArrayList<>();
        for (ActivityRow a : approved) {
            // 不超过志愿者总数，且每活动随机 ±5 抖动
            int n = Math.min(volIds.size(), Math.max(5, perActivity + rand.nextInt(11) - 5));
            Set<Integer> picked = pickN(volIds, n);

            LocalDateTime appliedBase = a.start().minusDays(7 + rand.nextInt(14));
            for (Integer volId : picked) {
                double r = rand.nextDouble();
                String status = r < 0.78 ? RegStatus.APPROVED
                        : r < 0.90 ? RegStatus.PENDING
                        : r < 0.97 ? RegStatus.REJECTED
                        : RegStatus.CANCELLED;
                LocalDateTime applied = appliedBase.plusMinutes(rand.nextInt(2880));
                Timestamp audited = (RegStatus.APPROVED.equals(status) || RegStatus.REJECTED.equals(status))
                        ? Timestamp.valueOf(applied.plusHours(1 + rand.nextInt(48)))
                        : null;
                batch.add(new Object[]{a.id(), volId, status, Timestamp.valueOf(applied), audited});
            }
        }
        return doBatch(
                "INSERT INTO registrations (activity_id,volunteer_id,audit_status,applied_at,audited_at) VALUES (?,?,?,?,?)",
                batch, new int[]{Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.TIMESTAMP, Types.TIMESTAMP});
    }

    // ---------- attendance ----------

    /**
     * 对所有 (审核通过+已发生) 的 bulk 活动报名记录建签到行。状态分布：
     *  - 65% 已签退（hours = duration ± 抖动）
     *  - 10% 漏签退
     *  - 15% 未签到
     *  - 10% 异常
     */
    private int seedAttendance() {
        List<PairRow> pairs = jdbc.query(
                "SELECT r.activity_id, r.volunteer_id, a.start_time, a.end_time " +
                "FROM registrations r JOIN activities a ON a.activity_id = r.activity_id " +
                "WHERE r.audit_status = ? AND a.end_time < NOW() " +
                "  AND a.title LIKE ? " +
                "  AND NOT EXISTS (SELECT 1 FROM attendance t WHERE t.activity_id=r.activity_id AND t.volunteer_id=r.volunteer_id)",
                (rs, n) -> new PairRow(rs.getInt(1), rs.getInt(2),
                        rs.getTimestamp(3).toLocalDateTime(),
                        rs.getTimestamp(4).toLocalDateTime()),
                RegStatus.APPROVED, ACT_TITLE_PREFIX + "%");

        if (pairs.isEmpty()) return 0;

        List<Object[]> batch = new ArrayList<>();
        for (PairRow p : pairs) {
            int actId = p.activityId(), volId = p.volunteerId();
            LocalDateTime startTime = p.start();
            int durMin = (int) java.time.Duration.between(p.start(), p.end()).toMinutes();

            double r = rand.nextDouble();
            Timestamp ci, co;
            int hours, minutes;
            String status;
            if (r < 0.65) {
                int ciOffset = rand.nextInt(15); // 0~14 分钟早到/迟到
                int coOffset = rand.nextInt(20); // 提前最多 20 分钟离开
                ci = Timestamp.valueOf(startTime.plusMinutes(ciOffset));
                co = Timestamp.valueOf(startTime.plusMinutes(durMin - coOffset));
                int realMin = Math.max(30, durMin - ciOffset - coOffset);
                hours = realMin / 60;
                minutes = realMin % 60;
                status = AttendStatus.CHECKED_OUT;
            } else if (r < 0.75) {
                ci = Timestamp.valueOf(startTime.plusMinutes(rand.nextInt(15)));
                co = null;
                hours = 0; minutes = 0;
                status = AttendStatus.MISSED_CHECKOUT;
            } else if (r < 0.90) {
                ci = null; co = null;
                hours = 0; minutes = 0;
                status = AttendStatus.NOT_CHECKED_IN;
            } else {
                ci = Timestamp.valueOf(startTime.plusMinutes(rand.nextInt(15)));
                co = Timestamp.valueOf(startTime.plusMinutes(durMin + rand.nextInt(60)));
                hours = 0; minutes = 0;
                status = AttendStatus.ABNORMAL;
            }
            batch.add(new Object[]{actId, volId, ci, co, hours, minutes, status});
        }
        return doBatch(
                "INSERT INTO attendance (activity_id,volunteer_id,check_in_time,check_out_time,service_hours,service_minutes,status) VALUES (?,?,?,?,?,?,?)",
                batch, new int[]{Types.INTEGER, Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP, Types.INTEGER, Types.INTEGER, Types.VARCHAR});
    }

    // ---------- certificates ----------

    /**
     * 对所有 attendance(已签退+hours>0) 自动发证。再随机抽 5% 改成已失效。
     */
    private int seedCertificates() {
        List<Object[]> rows = jdbc.query(
                "SELECT t.activity_id, t.volunteer_id, a.title, a.start_time, a.end_time, t.service_hours, t.service_minutes " +
                "FROM attendance t JOIN activities a ON a.activity_id = t.activity_id " +
                "WHERE t.status = ? AND (t.service_hours > 0 OR t.service_minutes > 0) " +
                "  AND a.title LIKE ? " +
                "  AND NOT EXISTS (SELECT 1 FROM certificates c WHERE c.activity_id=t.activity_id AND c.volunteer_id=t.volunteer_id)",
                (rs, n) -> {
                    LocalDateTime st = rs.getTimestamp(4).toLocalDateTime();
                    LocalDateTime et = rs.getTimestamp(5).toLocalDateTime();
                    String certTitle = rs.getString(3).replace(ACT_TITLE_PREFIX, "").trim() + " 志愿服务证明";
                    return new Object[]{
                            certTitle,
                            rs.getInt(1),
                            rs.getInt(2),
                            Timestamp.valueOf(st),
                            Timestamp.valueOf(et),
                            rs.getInt(6),
                            rs.getInt(7),
                            java.sql.Date.valueOf(et.toLocalDate().plusDays(1)),
                            rand.nextDouble() < 0.05 ? CertStatus.INVALID : CertStatus.VALID
                    };
                },
                AttendStatus.CHECKED_OUT, ACT_TITLE_PREFIX + "%");

        return doBatch(
                "INSERT INTO certificates (title,activity_id,volunteer_id,start_time,end_time,cert_hours,cert_minutes,issued_date,status) " +
                "VALUES (?,?,?,?,?,?,?,?,?)",
                rows,
                new int[]{Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP, Types.INTEGER, Types.INTEGER, Types.DATE, Types.VARCHAR});
    }

    // ---------- messages ----------

    private int seedMessages(List<Integer> volIds, List<Integer> orgIds) {
        List<Object[]> batch = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 30 条系统公告（receiver_id=NULL, target_scope 三选一）
        String[] scopes = {"全体志愿者", "全体组织者", "全体志愿者,全体组织者"};
        for (int i = 0; i < 30; i++) {
            String scope = scopes[rand.nextInt(scopes.length)];
            String title = "系统公告 #" + (i + 1) + "：" + pickAnnouncementTopic();
            String content = "本公告由系统管理员发布，详情请关注公告内容。" + (i + 1);
            LocalDateTime send = now.minusDays(rand.nextInt(120));
            batch.add(new Object[]{MsgType.SYS_NOTICE, title, content, null, scope, Timestamp.valueOf(send), rand.nextDouble() < 0.5 ? 1 : 0});
        }
        // 200 条业务通知（receiver_id 指向具体用户）
        List<Integer> allUsers = new ArrayList<>(volIds);
        allUsers.addAll(orgIds);
        String[] bizTypes = {MsgType.REG_NOTICE, MsgType.ACTIVITY_NOTICE, MsgType.QUAL_AUDIT};
        for (int i = 0; i < 200; i++) {
            int recv = allUsers.get(rand.nextInt(allUsers.size()));
            String type = bizTypes[rand.nextInt(bizTypes.length)];
            String title = type + "：编号 " + (i + 1);
            String content = "您有一条 " + type + " 待查阅，请前往消息中心查看详情。";
            LocalDateTime send = now.minusDays(rand.nextInt(60)).minusMinutes(rand.nextInt(1440));
            batch.add(new Object[]{type, title, content, recv, null, Timestamp.valueOf(send), rand.nextDouble() < 0.4 ? 1 : 0});
        }
        return doBatch(
                "INSERT INTO messages (msg_type,title,content,receiver_id,target_scope,send_time,is_read) VALUES (?,?,?,?,?,?,?)",
                batch,
                new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.TIMESTAMP, Types.INTEGER});
    }

    private String pickAnnouncementTopic() {
        String[] topics = {"假期志愿活动安排", "签到规范提醒", "组织者培训通知", "工时认定细则更新",
                "校志愿者表彰公告", "系统维护通知", "新学期招募预告"};
        return topics[rand.nextInt(topics.length)];
    }

    // ---------- organizer applications ----------

    /**
     * 20 条申请：其中 5 条来自 bulkorg 池（视为历史已通过申请），其余 15 条从志愿者池抽样，
     * 状态混合 待审核/已拒绝/已通过。已通过/已拒绝的 auditor_id 取已存在管理员之一。
     */
    private int seedApplications(List<Integer> volIds, List<Integer> orgIds) {
        Integer auditorId = jdbc.queryForObject(
                "SELECT user_id FROM users WHERE role = ? ORDER BY user_id LIMIT 1",
                Integer.class, Role.ADMIN);
        if (auditorId == null) auditorId = 2; // 兜底 demo admin01

        List<Object[]> batch = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 5 条来自 bulkorg：全部已通过
        Set<Integer> orgPicked = pickN(orgIds, Math.min(5, orgIds.size()));
        for (Integer uid : orgPicked) {
            LocalDateTime submit = now.minusDays(30 + rand.nextInt(120));
            batch.add(new Object[]{uid,
                    "希望参与志愿活动的策划与组织，已有相关经验。",
                    "https://example.com/material/bulk-" + uid + ".pdf",
                    Timestamp.valueOf(submit),
                    AppStatus.APPROVED, auditorId,
                    Timestamp.valueOf(submit.plusDays(1 + rand.nextInt(3)))});
        }

        // 15 条来自普通志愿者：8 待审核 / 4 已拒绝 / 3 已通过（但保持 is_organizer 不变以免冲突）
        Set<Integer> volPicked = pickN(volIds, 15);
        int idx = 0;
        for (Integer uid : volPicked) {
            LocalDateTime submit = now.minusDays(rand.nextInt(45));
            String status;
            Integer aud; Timestamp audAt;
            if (idx < 8) {
                status = AppStatus.PENDING; aud = null; audAt = null;
            } else if (idx < 12) {
                status = AppStatus.REJECTED; aud = auditorId;
                audAt = Timestamp.valueOf(submit.plusDays(1 + rand.nextInt(3)));
            } else {
                status = AppStatus.APPROVED; aud = auditorId;
                audAt = Timestamp.valueOf(submit.plusDays(1 + rand.nextInt(3)));
            }
            batch.add(new Object[]{uid,
                    "希望加入组织者队伍，长期参与志愿管理。理由编号 " + (++idx),
                    "https://example.com/material/apply-" + uid + ".jpg",
                    Timestamp.valueOf(submit),
                    status, aud, audAt});
        }

        return doBatch(
                "INSERT INTO organizer_applications (applicant_id,reason,material_url,submitted_at,audit_status,auditor_id,audited_at) " +
                "VALUES (?,?,?,?,?,?,?)",
                batch,
                new int[]{Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.TIMESTAMP, Types.VARCHAR, Types.INTEGER, Types.TIMESTAMP});
    }

    // ---------- helpers ----------

    private int doBatch(String sql, List<Object[]> batch, int[] types) {
        if (batch.isEmpty()) return 0;
        jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override public void setValues(PreparedStatement ps, int i) throws SQLException {
                Object[] row = batch.get(i);
                for (int k = 0; k < row.length; k++) {
                    if (row[k] == null) ps.setNull(k + 1, types[k]);
                    else ps.setObject(k + 1, row[k], types[k]);
                }
            }
            @Override public int getBatchSize() { return batch.size(); }
        });
        return batch.size();
    }

    private Set<Integer> pickN(List<Integer> pool, int n) {
        if (n >= pool.size()) return new HashSet<>(pool);
        List<Integer> copy = new ArrayList<>(pool);
        Collections.shuffle(copy, rand);
        return new HashSet<>(copy.subList(0, n));
    }

    private String randomName() {
        return SURNAMES[rand.nextInt(SURNAMES.length)] + GIVEN[rand.nextInt(GIVEN.length)];
    }

    private String randomPhone() {
        StringBuilder sb = new StringBuilder("138");
        for (int i = 0; i < 8; i++) sb.append(rand.nextInt(10));
        return sb.toString();
    }

    private record ActivityRow(int id, int organizerId, String audit, LocalDateTime start, LocalDateTime end) {}
    private record PairRow(int activityId, int volunteerId, LocalDateTime start, LocalDateTime end) {}
}

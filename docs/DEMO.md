# VMS 答辩演示脚本 V0.5（S5 收尾）

> **目标**：8~12 分钟内走完 4 角色 × FR-01 ~ FR-10 的核心闭环，外加 S5 三个亮点（文件上传 / 证书 PDF / 对外综测 API）。

---

## 0. 演示前 5 分钟准备清单

```text
□ Windows 已装：JDK 17 + MySQL 8 + Node.js 18+ + Chrome
□ MySQL 已建库：CREATE DATABASE vms DEFAULT CHARSET utf8mb4;
□ application-dev.yml 的 vms.seed.bulk: true（首次启动会自动注入 ≥2000 条种子）
□ 后端：cd backend && mvn spring-boot:run  → http://localhost:8080
□ 前端：cd volunteer_managment_system && npm install && npm run serve → http://localhost:8081
□ 启动后立刻访问 http://localhost:8080/api/health/db
        期望返回 {code:0, data:230} 左右（16 demo + 14 bulkorg + 200 bulkvol）
□ Swagger UI：http://localhost:8080/swagger-ui/index.html （备用，演示接口契约）
□ 准备一张测试图片（任意 jpg/png ≤5MB），用于"组织者申请材料上传"演示
□ 演示完成可将 vms.seed.bulk 改回 false，避免下次启动重新统计
```

**测试账号**（密码均为 `123456`）：

| 用户名 | 角色 | 用于 |
|---|---|---|
| `superadmin` | 超管 | FR-08 |
| `admin01` | 管理员 | FR-01 审核 / FR-09 审核 / FR-10 发公告 |
| `org01` | 志愿者 + 组织者（双身份）| FR-01 / FR-02 / FR-03 / FR-04 |
| `vol01` | 普通志愿者 | FR-02 / FR-05 / FR-09 / FR-10 |
| `bulkvol001` | 批量种子志愿者 | 用于展示"累计工时统计"和对外 API |

---

## 1. 流程总览（讲解时一张图）

```
超管授权管理员 → 志愿者申请组织者 → 管理员审核组织者申请
        ↓                                            ↓（通过）
   org 双身份切换 → 发布活动 → 管理员审核活动 → 组织者切换"发布中"
        ↓                                            ↓
                            志愿者报名 → 组织者审核报名
                                              ↓（通过）
                                   签到签退 / 工时认定（小时+分钟）
                                              ↓
                                   证书自动生成 → PDF 下载
                                              ↓
                                   对外综测 API 按学号拉累计工时
```

---

## 2. 详细演示步骤

### Phase A：超管授权（FR-08）　≈ 1 分钟

1. 浏览器进入 http://localhost:8081/login，账号 `superadmin` / `123456`。
2. 自动跳转 `/sys/super-add`（超管专属后台）。
3. **SuperAdd 页**：演示按姓名/学号搜索一个志愿者，点"提升为管理员"，弹窗确认 → 出现成功提示。
4. **SuperDelete 页**：能看到刚提升的管理员，可演示"撤销管理员"按钮（不实际撤销，避免影响后续步骤）。

> **要点**：超管账号走特权通道（前端识别 `superadmin/root`），不显示日常业务菜单。

---

### Phase B：志愿者申请组织者 + 文件上传（FR-09 + S5 ①）　≈ 2 分钟

1. 退出 → 用 `vol01` 登录 → 角色选择 `volunteer`。
2. 进入 **「申请成为组织者」**（VolApplyOrg）。
3. 填写申请理由，点 **「选择文件上传」** 按钮，选一张图片或 PDF。
4. 上传后页面会出现 ✅ "已上传：xxx.jpg" + "查看"链接，可点链接在新窗口看到文件已写入 `backend/uploads/yyyy/MM/`。
5. 点"提交申请" → 出现"申请已提交，等待管理员审核"。下方"我的申请记录"立刻刷新出新行（状态：**待审核**）。

> **要点**：文件上传走 `POST /api/files/upload`（需 JWT），返回相对 URL；读取走 `GET /api/files/static/**`（白名单）；防穿越校验（拒绝 `..` 和绝对路径）；大小≤5MB，类型限 jpg/jpeg/png/gif/webp/pdf。

---

### Phase C：管理员审核申请（FR-09）　≈ 1 分钟

1. 退出 → `admin01` / `123456` 登录 → 进入 **「组织者申请审核」**（AdminOrg）。
2. 找到 vol01 的待审核记录 → 点 **"通过"** → 出现成功提示。
3. **「组织者管理」**（AdminOrgManage）能看到 vol01 已进入"已通过组织者"列表（含已发活动数）。
4. （可选）这里也可顺手撤销一个 bulkorg 组织者，演示"撤销资质"动作 + 给该用户发站内信。

> **要点**：审核通过自动 `users.is_organizer=true` + 站内信通知；撤销同步反操作。

---

### Phase D：组织者发活动 + 管理员审核（FR-01）　≈ 2 分钟

1. 退出 → `org01` / `123456` 登录 → 默认 volunteer 视图，**点右上角"切换为组织者"**。
2. 进入 **「我的活动」**（OrgActivity），点"新建活动"，填表：
   - 活动名称：**【演示】校园清扫活动**
   - 地点、时间、招募人数、简介
3. 提交后表格中出现新活动，状态：**待审核 / 已停止**（两层状态）。
4. **切换 admin01** → **「活动审核」**（AdminAct）→ 找到该活动 → 点"通过"。
5. 切回 org01 → 进入 OrgActivity → 看到状态变成 **审核通过 / 已停止** → 拨动"发布开关" → 变为 **发布中**。

> **要点**：活动有两层独立状态（管理员审核 × 组织者发布开关），修改活动后会自动回到「待审核+已停止」需再次审批。

---

### Phase E：志愿者报名 + 组织者审核（FR-02）　≈ 1.5 分钟

1. 切换 vol01（同浏览器多角色 = 用不同 Chrome 隐身窗口或不同浏览器） → **「活动浏览」**（VolActivity）。
2. 找到刚发的演示活动 → 点 **"我要报名"** → 弹窗确认 → 提交。
3. **「我的报名」**（VolApplied）出现该报名（状态 待审核）。
4. 切回 org01 → **「活动管理」**（OrgManage）→ 选中该活动 → "报名审核" 标签 → 看到 vol01 → 点 **"通过"**。

> **要点**：审核通过时自动建 attendance（未签到）行 + 给志愿者站内信通知。

---

### Phase F：签到签退 + 工时认定（FR-03 + FR-04）　≈ 1.5 分钟

1. 在 OrgManage 的 **"志愿时管理"** 标签下，找到 vol01 → 点 **"手动补签"**（演示用，因为活动还未发生）。
2. 弹窗输入签到时间 / 签退时间 / 工时（如 "2 小时 30 分钟"）→ 提交。
3. 状态变为 **已签退**（前端展示别名"正常"）；同列"工时" 显示 **2 小时 30 分钟**。
4. **观察侧效果**：MySQL 表 `certificates` 自动多了一条该用户的有效证书（后端 AttendanceService.updateHours 内自动 upsert）。
5. 演示"反向"：点击工时编辑，改成 0 小时 0 分钟 → 证书状态自动变 **已失效**（vol01 的 VolCert 页面看到"已失效"标签）。

> **要点**：核心副作用 —— `updateHours(0,0)` 让已发证书自动失效；`updateHours(>0)` 会重新激活/补发证书。

---

### Phase G：证书 PDF 下载（FR-05 + S5 ②）　≈ 1 分钟

1. 切到 vol01 → **「我的证书」**（VolCert）→ 把工时改回 2 小时 30 分钟（让证书重新有效）。
2. 点 **"下载"**：浏览器下载 `xxx.pdf` 文件。
3. 打开 PDF：标准 A4 单页，包含证书编号、姓名、活动信息、时间段、**工时大字体方框（2 小时 30 分钟）**、发证单位与日期、底部水印备注。中文字体使用 iText `STSong-Light`（无需 TTF 文件）。
4. 把工时再改回 0 / 0 → 重新进 VolCert 点下载 → 显示"证书已失效，无法下载"。

> **要点**：iText 7.2.5 + font-asian；后端校验"仅本人 + 状态有效"；前端用 axios 拿 Blob 后触发下载（避开 `<a href>` 没法带 JWT 的问题）。

---

### Phase H：消息与公告（FR-10）　≈ 0.5 分钟

1. 切到 admin01 → **「公告通知」**（AdminNotice）→ 发一条系统公告，目标勾选 **"全体志愿者+全体组织者"**。
2. 切到 vol01 → **「我的消息」**（VolMsg）：能立刻看到该公告"实化"成一条针对自己的消息。

> **要点**：公告实化方案 —— 发公告时保留 1 条 receiver=null 的原始记录 + 给每个目标用户插一条带 receiver_id 的实化条；查询简单，支持每人 `is_read`。

---

### Phase I：对外综测 API（S5 ③）　≈ 1 分钟

> 这是最容易出彩的"亮点"，建议放在最后。

1. 打开新终端（或 Postman / 浏览器），用 `bulkvol001` 这种大种子志愿者的学号查询：
   ```bash
   curl 'http://localhost:8080/api/public/hours?studentId=bulkvol001'
   ```
2. 返回示例：
   ```json
   {
     "code": 0,
     "msg": "ok",
     "data": {
       "studentId": "bulkvol001",
       "studentName": "李娜",
       "totalHours": 8,
       "totalMinutes": 35,
       "formattedDuration": "8小时35分钟",
       "certifiedActivityCount": 3,
       "generatedAt": "2026-06-16 14:23:01"
     }
   }
   ```
3. 演示"学号不存在"：
   ```bash
   curl 'http://localhost:8080/api/public/hours?studentId=NOT_EXIST'
   # → {"code":1001,"msg":"未找到学号对应的志愿者：NOT_EXIST"}
   ```
4. 强调：该接口在白名单中（无须 JWT），是给综测系统等第三方调用的；后续可加 API Key 头校验。

---

### Phase J：仪表盘统计（FR-06）　≈ 0.5 分钟

走查各角色的 Dashboard：
- AdminDash：`totalActivities`/`totalVolunteers`/`totalCertificates`/`pendingActivityAudits`/`totalServiceHours`（在大种子之后这些数应该已破百/破千）
- OrgDash：`myActivityCount`/`publishedActivityCount`/`pendingRegCount`
- VolunteerDash：`totalHours/totalMinutes`/`certificateCount`/`approvedRegCount`
- SuperDash：`totalAdmins`/`totalUsers`

---

## 3. 答辩 Q&A 备料

| 可能被问 | 一句话回答 |
|---|---|
| 为什么用 JWT 不用 Session？ | 课设演示场景单实例本地部署，但 JWT 让前后端解耦，无须服务端 Session 存储；24h 过期；支持 refresh。 |
| 公告设计为什么"实化"为每人一条？ | 查询简单 + 支持个人 `is_read` 状态；课设数据量没有空间压力。 |
| 改志愿时为 0 怎么联动证书失效？ | 在 `AttendanceService.updateHours()` 内事务里联动 `certificates.status`。`>0` 时反向 upsert。 |
| 活动两层状态怎么避免越权发布？ | Service 层断言：`publish_status` 只能在 `audit_status=审核通过` 后改；修改活动会回到「待审核+已停止」。 |
| 文件上传安全吗？ | 类型白名单 + 大小限制 + UUID 文件名（防路径冲突）+ 路径前缀校验（防 `..` 穿越）；上传需 JWT，读取走静态白名单。 |
| 证书 PDF 怎么做的中文？ | iText 7 + font-asian 的 `STSong-Light + UniGB-UCS2-H`，是 Adobe 标准 CJK CIDFont，不需要本地 TTF 文件，跨 OS 一致。 |
| 数据规模为什么是 2000+？ | 由 `BulkDataSeeder` 自动生成 200+ 用户 / 100+ 活动 / 2000+ 报名/签到/工时/证书/消息/申请，满足 PROJECT.md §8.2 非功能要求。 |
| 对外综测 API 怎么保证安全？ | 当前为开放接口（课设演示）；生产建议加 API Key（请求头 `X-API-KEY`）+ IP 白名单。 |

---

## 4. 演示完成后清理

```bash
# 重启会幂等：BulkDataSeeder 检测到 bulk% 用户存在会跳过；schema 是 IF NOT EXISTS；data 是 INSERT IGNORE
# 若要重置：DROP DATABASE vms; CREATE DATABASE vms DEFAULT CHARSET utf8mb4;
```

把 `application-dev.yml` 的 `vms.seed.bulk` 改回 `false`，下次启动不会再尝试批量插入。

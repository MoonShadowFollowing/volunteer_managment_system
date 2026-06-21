# VMS 志愿服务工时认证与活动管理系统 — 项目文档

> 数据库系统课程设计 · 24软工4班第七组
> 需求版本：V1.1（2026-04-29）
> 需求文档：`/data1/user/gjx/260429_VMS需求分析文档_V1.1(5).docx`
> 文档编号：VMS-SRS-20260403

---

## 1. 项目概述

### 1.1 项目定位
面向**高校内部**志愿服务管理场景的数据库应用系统，把原本分散在聊天工具、电子表格、纸质表格中的志愿活动管理、报名审核、签到签退、工时认定、证书发放等业务集中到一个本地运行的应用中完成。

### 1.2 业务核心闭环
```
超管授权管理员
   ↓
组织者申请审核（志愿者→组织者）
   ↓
活动提交（组织者）→ 活动审核（管理员）→ 前台发布开关（组织者）
   ↓
志愿者报名 → 报名审核（组织者）
   ↓
签到签退（活动期间）
   ↓
志愿时认定（组织者可手动改时长/补签）
   ↓
证书自动生成（工时=0时自动失效）
   ↓
查询统计 / PDF导出
```

### 1.3 运行模式
**本地部署 + 联网服务**（V1.1 已不是 V1.0 的纯单机）：
- 应用与 MySQL 部署在本地 PC / 实验室计算机
- 登录需调用学校门户 SSO（可用模拟接口）
- 对外提供 RESTful API 供综合测评系统按学号查累计工时
- 校园网中断时联机功能不可用，本地业务正常

### 1.4 设计创新点与差异化

与国内主流志愿服务平台（如 i志愿）相比，本系统的定位是**校园场景下轻量、自主、可二次开发的补充方案**——不替代全国统一平台，而是在校内本地化数据治理、双身份业务、工时-证书闭环、对外开放接口这些细颗粒场景做扎实。

#### A. 产品定位（"为什么要做"）
| # | 创新点 | 对应章节 / 实现位置 |
|---|---|---|
| A1 | **校内自主部署 + 数据本地化**：学校掌控数据库与流程，可一键改 `application-dev.yml` 切换 MySQL 实例 | §1.3 / §9 / `backend/src/main/resources/application-dev.yml` |
| A2 | **对外开放 RESTful 综测接口**：`/api/public/hours?studentId=` 供综合测评系统按学号查累计工时（白名单免 JWT） | §7 / `module/external/PublicApiController` |
| A3 | **互补叙事**：定位为院系/社团小型活动的本地化管理 + i志愿数据回灌渠道，避开正面竞争 | §1.1 |

#### B. 业务流程（评审重点关注）
| # | 创新点 | 对应章节 / 实现位置 |
|---|---|---|
| B1 | **两层活动状态机**（管理员审核 × 组织者前台启停）：可"先批准但延迟开放报名"，比单层审核更灵活 | §4.1 / `ActivityService` |
| B2 | **双身份热切换**：volunteer + organizer 同一账号双面，登录后 `/role-select` 一键切，无须分账号 | §2 / `src/views/RoleSelect.vue` |
| B3 | **工时 ↔ 证书双向联动**：改工时为 0 → 证书 `status=已失效`；改回 >0 → 自动复活/upsert，审计闭环 | §4.2 / `AttendanceService.updateHours` |
| B4 | **管理员撤销权限实时生效**：`/auth/refresh` + 前端 403 拦截后 `/auth/me` 静默同步 → 当前页面立即跳回 volunteer 视图，无须重新登录 | `src/api/index.js:42-69` / `AuthService.refresh` |
| B5 | **4 级 RBAC 权限**（超管 → 管理员 → 组织者 → 志愿者）：清晰分层，超管走专属直登通道 | §2 / §12.5 |

#### C. 技术栈现代化（技术答辩点）
| # | 创新点 | 对应章节 / 实现位置 |
|---|---|---|
| C1 | **现代主流栈**：Vue3 + Spring Boot 3 + JWT 无状态鉴权 + MyBatis-Plus + iText 7，二次开发友好 | §12.1 |
| C2 | **中文 PDF 证书零外部字体依赖**：走 iText 7 内置 `STSong-Light/UniGB-UCS2-H`，部署轻便不挑环境 | `util/PdfGenerator` |
| C3 | **统一契约**：12 个业务模块 + 统一 `Result<T>` + `PageResult<T>` + 中文枚举常量，模块横向扩展友好 | §12.2 / §12.3 |
| C4 | **规模化种子数据**：`vms.seed.bulk: true` 自动灌入 ≥200 用户 / ≥100 活动 / ≥2000 报名+签到+工时，演示与压测就绪 | §8.2 / `BulkDataSeeder` |
| C5 | **安全防护**：BCrypt 密码 + JWT + `@PreAuthorize` + Service 层 `mustOwn` 越权自校验双重防护 + 文件上传路径穿越校验 | §8.3 / `SecurityConfig` |

#### D. 用户体验细节（Demo 容易出彩）
| # | 创新点 | 对应章节 / 实现位置 |
|---|---|---|
| D1 | **消息身份隔离 + 未读红点**：志愿者收"报名通过"、组织者收"新报名"，互不打扰；顶栏未读计数实时刷新 | `MessageService` / `MainLayout` |
| D2 | **4 类身份定制 Dashboard**：每个角色 metrics 不同，超管看赋权数、管理员看待审、组织者看本人活动、志愿者看累计工时 | §12.4 / `StatService` |
| D3 | **身份前缀编号体系**：`VOL-学号 / ORG-学号 / ADM-学号 / SUP-学号`，列表/证书/PDF 一眼分清角色 | §12.6 |
| D4 | **公告实化分发**：发公告时保留 1 条原始（`receiver_id=null`）+ 给每个目标用户插实化条，未读状态可独立跟踪 | §5.6 / `MessageService.sendNotice` |

#### E. 理性边界（避免吹过头）
- 不正面对标"取代 i志愿"：i志愿的全国数据互通、官方背书、用户规模是 VMS 不具备的绝对优势
- 性能/安全的具体数字仅按 SRS §8 中 NFR 给出口径，未做工业级压测
- 移动端不在当前范围内，仅 Web 桌面
- 学校门户 SSO 暂用模拟接口对接，真接入需学校开放权限

> **答辩开场话术建议**：
> "i志愿解决全国志愿服务的统一登记与认证，VMS 是校园内 *轻量、自主、可二次开发* 的补充方案——把校园本地化数据、双身份业务、工时-证书联动、对外开放接口这些细颗粒场景做扎实，与 i志愿不是替代而是互补。"

---

## 2. 用户角色（4 类）

| 角色 | 人数 | 入口 | 主要职责 |
|---|---|---|---|
| **志愿者** | 多 | SSO 登录 + 选身份 | 浏览/报名活动、签到签退、查个人工时与证书、看消息、申请成为组织者 |
| **活动组织者** | 较少 | SSO 登录 + 选身份（需资质） | 发布/修改/删活动（提交审核）、审报名、签到签退管理、改志愿时、手动补签 |
| **系统管理员** | 少 | SSO 登录 + 选身份（需超管赋权） | 活动审核（批准/驳回）、组织者申请审核、组织者资质移除、发公告、看异常签到、全局统计 |
| **超级管理员** | 1（root/superadmin） | 专属账号直登，跳过角色选择 | 仅管理员账号授权与撤销，不参与日常业务 |

**关键关系**：志愿者与组织者是**同一人的双身份**，组织者审核通过后顶栏可热切换。

---

## 3. 功能模块（10 个）

| 编号 | 模块 | 主要参与者 | 核心功能 |
|---|---|---|---|
| **FR-01** | 志愿活动管理 | 组织者 + 管理员 | 活动 CRUD、提交审核、前台发布开关 |
| **FR-02** | 活动报名与审核 | 志愿者 + 组织者 | 报名/取消报名/查记录、审核通过/拒绝 |
| **FR-03** | 签到签退管理 | 组织者 | 在活动管理页统一查看签到/签退时间、状态处理 |
| **FR-04** | 志愿时认定 | 组织者 | "小时+分钟"格式认定工时，可手动改 |
| **FR-05** | 证书管理 | 志愿者 | 工时生效自动发证，预览/下载 PDF |
| **FR-06** | 查询与统计报表 | 三角色 | 个人/活动/全局多维统计 |
| **FR-07** | 统一认证与角色鉴权 | 全体 | 校园 SSO + RBAC，密码 BCrypt 加密、Session 反复制 |
| **FR-08** | 超管账号管理 | 超管 | 添加/移除管理员（专属后台） |
| **FR-09** | 组织者申请与审核 | 志愿者 + 管理员 | 申请理由+证明材料上传 → 审核通过/拒绝 → 资质移除 |
| **FR-10** | 消息与公告管理 | 管理员 + 志愿者 + 组织者 | 管理员发公告（可选范围）+ 业务通知 |
| **FR-11** | 报表导出（S6 扩展）| 志愿者 + 组织者 + 管理员 | 个人工时 xlsx / 活动签到汇总 xlsx / 月度汇总 xlsx |

---

## 4. 关键业务规则（V1.1 口径）

### 4.1 活动的两层状态（V1.1 新增）
活动同时具有两个独立维度，缺一不可：

| 维度 | 取值 | 谁能改 |
|---|---|---|
| **审核状态** | 待审核 / 审核通过 / 审核不通过 | 系统管理员 |
| **前台发布状态** | 发布中 / 已停止 | 活动组织者（仅审核通过后可操作） |

志愿者**只能看到"审核通过 + 发布中"的活动**。

### 4.2 工时与证书的联动（V1.1 新增）
- 工时格式：**"HH小时 mm分钟"**（如 "4小时0分钟"），不再用小数
- 组织者将某志愿者的工时改为 **0小时0分钟** → 系统警告 → 该证书状态自动 **"已失效"**
- 证书状态：**有效 / 已失效**
- 失效证书的"预览/下载"按钮置灰

### 4.3 报名与签到状态枚举
- 报名状态：待审核 / 审核通过 / 审核拒绝 / 已取消
- 签到状态：未签到 / 已签到 / 已签退 / 漏签退 / 异常
- 组织者操作：
  - **修改志愿时**（针对"正常"状态）→ 弹窗调整小时+分钟
  - **手动补签**（针对"漏签退/异常"）→ 系统补全状态并赋默认参考时长

### 4.4 时间精度
- 业务时间统一 `YYYY-MM-DD HH:MM:SS`（精确到秒）
- 仅展示日期时用 `YYYY-MM-DD`
- 工时显示 `HH小时 mm分钟`

---

## 5. 数据库表设计（权威版本）

> **来源**：`VMS数据表设计.docx`（同目录），共 **7 张表**，对齐 V1.1 §2.4.6 数据词典。
> 全部使用 **MySQL 8.0 + utf8mb4**，主键统一 `int` 自增，状态字段使用 `varchar(20)` 存储中文枚举值（不用 MySQL ENUM，便于读取与扩展）。
> 工时统一以 **「小时 + 分钟」** 两字段存储。

### 5.1 users 用户表
覆盖 4 类角色：volunteer / organizer / admin / superadmin。

| 字段 | 类型 | 长度 | PK | FK | NULL | 默认值 | 说明 |
|---|---|---|---|---|---|---|---|
| user_id | int | 11 | ✅ | | NOT NULL | AUTO_INCREMENT | 用户编号 |
| username | varchar | 50 | | | NOT NULL | - | 登录账号（学号/工号），全表唯一 |
| password | varchar | 100 | | | NOT NULL | - | BCrypt 加密，禁明文 |
| name | varchar | 100 | | | NOT NULL | - | 中文≤20汉字 / 英文≤100字符 |
| role | varchar | 20 | | | NOT NULL | `volunteer` | volunteer / organizer / admin / superadmin |
| phone | char | 11 | | | NULL | - | 中国大陆手机号 |
| is_organizer | tinyint | 1 | | | NOT NULL | 0 | 0 否 / 1 是 |
| is_admin | tinyint | 1 | | | NOT NULL | 0 | 是否被超管赋权 |
| created_at | datetime | - | | | NOT NULL | CURRENT_TIMESTAMP | 注册时间 |

**索引建议**：UNIQUE(username)、INDEX(role)。

### 5.2 activities 活动表
两层独立状态：**管理员审核状态 × 组织者前台发布状态**。

| 字段 | 类型 | 长度 | PK | FK | NULL | 默认值 | 说明 |
|---|---|---|---|---|---|---|---|
| activity_id | int | 11 | ✅ | | NOT NULL | AUTO_INCREMENT | |
| title | varchar | 100 | | | NOT NULL | - | 活动名称 |
| description | varchar | 500 | | | NULL | - | 活动简介 |
| location | varchar | 100 | | | NOT NULL | - | 活动地点 |
| start_time | datetime | - | | | NOT NULL | - | `yyyy-MM-dd HH:mm:ss` |
| end_time | datetime | - | | | NOT NULL | - | 必须晚于 start_time |
| capacity | int | 11 | | | NOT NULL | - | 招募人数 1~1000 |
| organizer_id | int | 11 | | ✅ | NOT NULL | - | → users(user_id) |
| audit_status | varchar | 20 | | | NOT NULL | `待审核` | 待审核 / 审核通过 / 审核不通过 |
| publish_status | varchar | 20 | | | NOT NULL | `已停止` | 发布中 / 已停止；仅审核通过后可改 |
| created_at | datetime | - | | | NOT NULL | CURRENT_TIMESTAMP | |

**索引建议**：INDEX(organizer_id)、INDEX(audit_status, publish_status)、INDEX(start_time)。

### 5.3 registrations 报名表

| 字段 | 类型 | 长度 | PK | FK | NULL | 默认值 | 说明 |
|---|---|---|---|---|---|---|---|
| reg_id | int | 11 | ✅ | | NOT NULL | AUTO_INCREMENT | |
| activity_id | int | 11 | | ✅ | NOT NULL | - | → activities |
| volunteer_id | int | 11 | | ✅ | NOT NULL | - | → users |
| audit_status | varchar | 20 | | | NOT NULL | `待审核` | 待审核 / 审核通过 / 审核拒绝 / 已取消 |
| applied_at | datetime | - | | | NOT NULL | CURRENT_TIMESTAMP | |
| audited_at | datetime | - | | | NULL | - | 未审核时空 |

**索引建议**：UNIQUE(activity_id, volunteer_id)（一人对一活动只能一次报名）、INDEX(volunteer_id)、INDEX(activity_id, audit_status)。

### 5.4 attendance 签到与志愿时表

| 字段 | 类型 | 长度 | PK | FK | NULL | 默认值 | 说明 |
|---|---|---|---|---|---|---|---|
| record_id | int | 11 | ✅ | | NOT NULL | AUTO_INCREMENT | |
| activity_id | int | 11 | | ✅ | NOT NULL | - | → activities |
| volunteer_id | int | 11 | | ✅ | NOT NULL | - | → users |
| check_in_time | datetime | - | | | NULL | - | ≥ 活动开始时间 |
| check_out_time | datetime | - | | | NULL | - | > check_in_time |
| service_hours | int | 11 | | | NOT NULL | 0 | 小时部分，≥0 |
| service_minutes | int | 11 | | | NOT NULL | 0 | 分钟部分，0~59 |
| status | varchar | 20 | | | NOT NULL | `未签到` | 未签到 / 已签到 / 已签退 / 漏签退 / 异常 |

**索引建议**：UNIQUE(activity_id, volunteer_id)、INDEX(status)。

### 5.5 certificates 证书表
工时为 0 时 `status` 自动置 `已失效`（由业务逻辑或触发器维护）。

| 字段 | 类型 | 长度 | PK | FK | NULL | 默认值 | 说明 |
|---|---|---|---|---|---|---|---|
| cert_id | int | 11 | ✅ | | NOT NULL | AUTO_INCREMENT | |
| title | varchar | 100 | | | NOT NULL | - | 证书标题 |
| activity_id | int | 11 | | ✅ | NOT NULL | - | → activities |
| volunteer_id | int | 11 | | ✅ | NOT NULL | - | → users |
| start_time | datetime | - | | | NOT NULL | - | 冗余，便于查询 |
| end_time | datetime | - | | | NOT NULL | - | 冗余 |
| cert_hours | int | 11 | | | NOT NULL | 0 | |
| cert_minutes | int | 11 | | | NOT NULL | 0 | 0~59 |
| issued_date | date | - | | | NOT NULL | - | `yyyy-MM-dd` |
| status | varchar | 20 | | | NOT NULL | `有效` | 有效 / 已失效 |

**索引建议**：UNIQUE(activity_id, volunteer_id)、INDEX(volunteer_id, status)。

### 5.6 messages 消息通知表
业务通知（单点推送）与系统公告（按范围群发）合表。

| 字段 | 类型 | 长度 | PK | FK | NULL | 默认值 | 说明 |
|---|---|---|---|---|---|---|---|
| msg_id | int | 11 | ✅ | | NOT NULL | AUTO_INCREMENT | |
| msg_type | varchar | 20 | | | NOT NULL | - | 报名通知 / 资质审核 / 活动通知 / 系统公告 |
| title | varchar | 100 | | | NOT NULL | - | |
| content | varchar | 500 | | | NOT NULL | - | 支持换行 |
| receiver_id | int | 11 | | ✅ | NULL | - | 单点通知填；系统公告时空 |
| target_scope | varchar | 30 | | | NULL | - | 公告范围：全体志愿者 / 全体组织者，可多选（逗号分隔）|
| send_time | datetime | - | | | NOT NULL | CURRENT_TIMESTAMP | |
| is_read | tinyint | 1 | | | NOT NULL | 0 | 0 未读 / 1 已读 |

**索引建议**：INDEX(receiver_id, is_read)、INDEX(msg_type, send_time)。

### 5.7 organizer_applications 组织者资质申请表

| 字段 | 类型 | 长度 | PK | FK | NULL | 默认值 | 说明 |
|---|---|---|---|---|---|---|---|
| app_id | int | 11 | ✅ | | NOT NULL | AUTO_INCREMENT | |
| applicant_id | int | 11 | | ✅ | NOT NULL | - | → users(user_id) |
| reason | varchar | 500 | | | NOT NULL | - | 申请理由 |
| material_url | varchar | 255 | | | NOT NULL | - | 证明材料 URL |
| submitted_at | datetime | - | | | NOT NULL | CURRENT_TIMESTAMP | |
| audit_status | varchar | 20 | | | NOT NULL | `待审核` | 待审核 / 已通过 / 已拒绝 |
| auditor_id | int | 11 | | ✅ | NULL | - | → users(user_id) |
| audited_at | datetime | - | | | NULL | - | |

**索引建议**：INDEX(applicant_id, audit_status)、INDEX(audit_status, submitted_at)。

### 5.8 表间关系总览（ER 概览）

```
users (1) ─── (N) activities             [organizer_id]
users (1) ─── (N) registrations          [volunteer_id]
users (1) ─── (N) attendance             [volunteer_id]
users (1) ─── (N) certificates           [volunteer_id]
users (1) ─── (N) messages               [receiver_id]
users (1) ─── (N) organizer_applications [applicant_id / auditor_id]

activities (1) ─── (N) registrations
activities (1) ─── (N) attendance
activities (1) ─── (N) certificates
```

---

## 6. 用例清单（V1.1 §2.4 全集）

| UC | 用例 | 参与者 |
|---|---|---|
| UC-00-01 | 门户统一登录 | 全体 |
| UC-00-02 | 角色选择与鉴权 | 三角色 |
| UC-01-01 | 发布活动 | 组织者 |
| UC-01-02 | 修改活动 | 组织者 |
| UC-01-03 | 删除活动 | 组织者 |
| UC-01-04 | 查看活动信息 | 全体 |
| UC-01-05 | 审核活动 | 管理员 |
| UC-02-01 | 提交报名申请 | 志愿者 |
| UC-02-02 | 查看报名记录 | 志愿者 |
| UC-02-03 | 查看报名名单 | 组织者 |
| UC-02-04 | 审核报名申请 | 组织者 |
| UC-02-05 | 查看审核结果 | 志愿者 |
| UC-03-01 | 志愿时管理与确认 | 组织者 |
| UC-03-02 | 证书查询与下载 | 志愿者 |
| UC-04-01 | 发布系统公告 | 管理员 |
| UC-04-02 | 查看我的消息 | 志愿者+组织者 |
| UC-05-01 | 组织者资质申请与审核 | 志愿者+管理员 |
| UC-05-02 | 超级管理员权限分配 | 超管 |

---

## 7. 外部系统接口

| 外部系统 | 调用方向 | 交互数据 | 实现方式 | 当前阶段 |
|---|---|---|---|---|
| 学校门户认证 | 本系统 → 门户 | 学号、姓名、登录态 | 认证接口/模拟接口 | 课设可模拟 |
| 综合测评系统 | 综测 → 本系统 | 学号、累计认证服务时长 | RESTful API | 至少做一个模拟接口 |
| i 志愿系统 | 双向 | 活动记录、服务时长 | 预留接口 | 暂不强制 |
| 文件导出 | 本系统输出 | PDF/Excel 志愿证明 | 离线文件 | 建议实现 |

---

## 8. 非功能需求

### 8.1 性能
- 页面响应 ≤ 2s，基本操作 ≤ 2s
- 常规查询 ≤ 3s，统计查询 ≤ 5s
- 外部接口超时 5s 必须给提示

### 8.2 数据规模
≥ 200 用户、≥ 100 活动、≥ 2000 条 报名/签到/工时 记录

### 8.3 安全
- 密码 BCrypt 加密，禁止明文
- Session 反复制（换浏览器/设备需重登）
- RBAC 越权防护
- 关键操作（删活动、改工时、移除管理员）二次确认

### 8.4 数据恢复
管理员手工备份 MySQL 数据文件，损坏时通过备份恢复

---

## 9. 运行环境

| 项 | 配置 |
|---|---|
| OS | Windows 10/11 |
| 数据库 | MySQL 8.0 |
| 后端 | Java(JDK 17 + IDEA/Eclipse) 或 Python 3.10+(Flask/Django/PyQt) |
| 前端 | 浏览器(Chrome/Edge) 或本地 GUI |
| 硬件 | i3/2.0GHz 以上、4GB+ 内存、2GB+ 磁盘 |
| 网络 | 需校园网（SSO + 对外 API） |

---

## 10. PRS ↔ FR 需求跟踪

| PRS | 内容 | FR/NFR |
|---|---|---|
| PRS-001 | 用户登录与权限管理 | FR-07 |
| PRS-002 | 志愿活动发布与管理 | FR-01 |
| PRS-003/004 | 志愿者报名 / 报名审核 | FR-02 |
| PRS-005 | 签到签退管理 | FR-03 |
| PRS-006 | 工时认定与统计 | FR-04 |
| PRS-007/018 | 消息通知与公告管理 | FR-10 |
| PRS-008 | 证书管理 | FR-05 |
| PRS-009/010 | 个人记录查询 / 活动统计报表 | FR-06 |
| PRS-011~015 | 性能/数据/安全/灵活性/环境 | NFR-01~05 |
| PRS-016 | 超管账号管理 | FR-08 |
| PRS-017 | 组织者申请与审核 | FR-09 |
| PRS-019 | 活动审核流程 | FR-01 |

---

## 11. 课设交付物清单（建议）

- [ ] 需求分析文档（V1.1 已有）
- [ ] 概要设计 / ER 图
- [x] 数据库物理设计（建表 SQL + 测试数据）—— 表结构见 §5
- [x] 系统实现（前端 + 后端 + DB）—— 本地全栈跑通（后端 :8080 + MySQL + 前端 :8081），登录→各业务闭环可端到端操作
  - [x] 前端 Vue3 骨架（同事完成）
  - [x] 前端 S2 接入真后端（`origin/develop` 分支，commit `dd06e24`；master 未动）
  - [x] **前端业务页全量接真**（2026-06-16，本地工作树，**尚未推 GitHub**）：GitHub 上 develop 实际只到 S2（仅登录接真，其余 18 个业务页为写死 mock）。本地已补齐 S3~S5 的前端接入——新增 `src/api/*.js` 9 个域模块，18 个 .vue 全部改调真后端（活动 CRUD/审核/发布、报名审核、签到补签/改志愿时、证书+PDF、文件上传申请、组织者审核/撤销、超管增删管理员、消息公告、3 个 Dashboard）。**注意**：实际文件名与本文档命名相反——`OrgActivity.vue`=活动CRUD页、`OrgManage.vue`=报名审核+志愿时tab页。
  - [x] **接入时修复的后端 bug/改动**（本地）：① 5 个 VO（UserInfo/ActivityVO/MessageVO/UserSummaryVO）加 `@JsonProperty("isXxx")`，布尔字段 JSON 保留 `is` 前缀（修好登录双身份）；② `RegistrationService.apply()` 补 `setAppliedAt`、`OrganizerApplicationService.submit()` 补 `setSubmittedAt`——原先这两条写库路径因 NOT NULL 时间戳为空必报 500（报名/申请接口此前从未成功过）；③ 种子密码哈希写错（注释 123456 实为他值），已重置 230 用户密码并改源码 `data.sql`/`BulkDataSeeder`。
  - [ ] 后端 Spring Boot（见 §12）
    - [x] **S1 基础设施**：项目骨架 + 7 张表 DDL + 演示种子 + 统一响应/异常 + Swagger（仓库 `/data1/user/gjx/数据库课设_VMS/backend/`）
    - [x] **S2 鉴权 + 用户**：Spring Security + JWT(jjwt 0.12.5)；`POST /api/auth/login`、`GET /api/auth/me`、`POST /api/auth/logout`；前端 axios + Login/MainLayout/router 已接通真接口
    - [x] **S3 核心业务闭环**（2026-06-15 完成）：activity / registration / attendance / certificate / message / stat 6 个模块；auth refresh；FR-01~06+FR-10；前端 8 个业务页全部接真
    - [x] **S4 申请+超管**（2026-06-15 完成）：organizer-application 模块 + UserController 超管接口 + Organizer 列表/撤销；前端 VolApplyOrg/AdminOrg/AdminOrgManage/SuperAdd/SuperDelete 全部接真
    - [x] **S5 收尾**（2026-06-16 完成）：①文件上传 module/file + ②证书 PDF（iText 7 + font-asian） + ③对外综测 API `/api/public/hours` + ④DataSeeder 开关打开（≥2000 条） + ⑤答辩演示脚本 `DEMO.md`；前端 VolApplyOrg 用 ElUpload 替换 URL 输入，VolCert 用 axios+blob 触发 PDF 下载
- [x] 模拟 SSO 接口 + 至少一个对外 RESTful API（综测查询）—— `GET /api/public/hours?studentId=`，白名单
- [x] 证书 PDF 导出原型 —— `GET /api/certificates/{id}/pdf`（iText 7.2.5）
- [x] 测试用例 + 演示脚本 —— 见 `DEMO.md`
- [ ] 答辩 PPT

---

## 12. 后端实施计划（Spring Boot）

### 12.1 技术栈选型

| 项 | 选型 | 理由 |
|---|---|---|
| JDK | Java 17 LTS | 与运行环境匹配，Spring Boot 3 最低要求 |
| Spring Boot | 3.2.x | 长期支持版，搭配 Jakarta EE 9+ |
| Web | Spring Web (MVC) | 课设规模无须 WebFlux |
| 持久层 | **MyBatis-Plus 3.5.x** | 课设场景写 SQL 直观、调试方便；JPA 抽象过深不利于阅卷 |
| 数据库 | MySQL 8.0 + utf8mb4_unicode_ci | |
| 连接池 | HikariCP（Spring Boot 默认） | |
| 安全 | Spring Security 6 + JWT (jjwt 0.12.x) | Session 反复制改用 JWT，简单清晰 |
| 密码加密 | BCrypt（Spring Security 自带 `BCryptPasswordEncoder`） | 与 NFR 对齐 |
| 参数校验 | `spring-boot-starter-validation` (Hibernate Validator) | |
| 接口文档 | **springdoc-openapi 2.3.x**（Swagger UI） | 便于前后端联调与答辩演示 |
| JSON | Jackson + JavaTimeModule | `LocalDateTime` 序列化为 `yyyy-MM-dd HH:mm:ss` |
| PDF 导出 | iText 7 (community) | 证书导出 |
| 构建 | Maven 3.9+ | |
| 单元测试 | JUnit 5 + Mockito + Spring Boot Test | |
| 日志 | Logback（Spring Boot 默认） | |

### 12.2 项目结构（Maven 标准目录）

```
volunteer_management_backend/
├── pom.xml
├── src/main/java/edu/xxx/vms/
│   ├── VmsApplication.java
│   ├── common/
│   │   ├── Result.java               # 统一响应 { code, msg, data }
│   │   ├── PageResult.java           # 分页结果包装
│   │   ├── exception/                # BizException + GlobalExceptionHandler
│   │   └── constant/                 # Role / AuditStatus / PublishStatus 等枚举常量
│   ├── config/
│   │   ├── MybatisPlusConfig.java    # 分页插件、自动填充 create_at
│   │   ├── SecurityConfig.java       # JWT 过滤器 + RBAC
│   │   ├── JacksonConfig.java        # LocalDateTime 格式
│   │   └── SwaggerConfig.java
│   ├── security/
│   │   ├── JwtUtil.java
│   │   ├── JwtAuthFilter.java
│   │   ├── CurrentUser.java          # ThreadLocal 当前用户信息
│   │   └── annotation/RequireRole.java
│   ├── module/                       # 业务按 FR 模块分包
│   │   ├── auth/                     # FR-07 登录、角色切换（模拟 SSO）
│   │   ├── user/                     # FR-08 超管账号管理
│   │   ├── activity/                 # FR-01 + FR-03 活动 + 签到
│   │   ├── registration/             # FR-02 报名审核
│   │   ├── attendance/               # FR-04 志愿时认定
│   │   ├── certificate/              # FR-05 证书
│   │   ├── message/                  # FR-10 消息公告
│   │   ├── organizer/                # FR-09 组织者申请
│   │   ├── stat/                     # FR-06 查询统计
│   │   ├── external/                 # S5 对外 RESTful API（PublicApiController）
│   │   ├── file/                     # S5 文件上传（FileController）
│   │   └── 每个模块内：controller / service / mapper / entity / dto
│   └── util/
│       └── PdfGenerator.java         # S5 证书 PDF（iText 7 + font-asian）
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml           # 本地开发
│   ├── mapper/                       # MyBatis XML（如需复杂 SQL）
│   └── sql/
│       ├── schema.sql                # 建表 DDL（按 §5）
│       └── data.sql                  # 测试种子数据（≥200 用户 / ≥100 活动 / ≥2000 报名签到）
└── src/test/java/...                 # 关键 Service 单测
```

### 12.3 API 契约约定

- **基础前缀**：`/api`
- **认证**：登录后返回 JWT，前端放 `Authorization: Bearer <token>` 头
- **统一响应**：`{ "code": 0, "msg": "ok", "data": ... }`；失败 code ≠ 0，HTTP 状态码 4xx/5xx
- **分页参数**：`?page=1&pageSize=10`，返回 `{ total, rows }`
- **时间字段**：`yyyy-MM-dd HH:mm:ss`
- **路径风格**：`/api/<module>/<resource>`，遵循 REST 语义（GET/POST/PUT/DELETE）
- **错误码段位**：1xxx 通用、2xxx 鉴权、3xxx 业务、4xxx 数据校验

### 12.4 核心接口清单（按前端页面映射）

| 模块 | Method + 路径 | 用途 | 前端用处 |
|---|---|---|---|
| **认证** | POST `/api/auth/login` | 模拟 SSO 登录 → 返 JWT + 角色信息 | Login.vue |
| | GET `/api/auth/me` | 获取当前用户 + 双身份资格 | MainLayout.vue |
| | POST `/api/auth/refresh` | 凭旧 token 换新 token（避免长会话强制下线） | axios 拦截器或定时刷新 |
| | POST `/api/auth/logout` | 语义占位，前端清 localStorage 即可 | MainLayout |
| | ~~POST `/api/auth/switch-role`~~ | 已废弃：双身份切换由前端纯改 `userRole` 完成，无需后端接口 | MainLayout |
| **活动** | GET `/api/activities` | 分页查活动；可传 `volunteerView=true`（只看已审核+发布中）、`organizerId`、`auditStatus`、`publishStatus`、`name`、`startDate`、`endDate` | VolActivity / OrgActivity / AdminAct |
| | GET `/api/activities/{id}` | 单活动详情 | 详情视图复用 |
| | POST `/api/activities` | 组织者发布活动（默认待审核/已停止） | OrgActivity |
| | PUT `/api/activities/{id}` | 修改活动；修改后自动回到「待审核+已停止」需要重新审批 | OrgActivity |
| | DELETE `/api/activities/{id}` | 组织者删除自己的活动 | OrgActivity |
| | PUT `/api/activities/{id}/audit` | 管理员审核（body: `{approve: bool}`）| AdminAct |
| | PUT `/api/activities/{id}/publish` | 组织者切前台发布开关（仅审核通过可开，body: `{publish: bool}`） | OrgActivity |
| **报名** | POST `/api/registrations` | 志愿者报名（body: `{activityId}`，校验：活动审核通过+发布中+未满+非自办） | VolActivity 详情 |
| | GET `/api/registrations/mine` | 我的已报名活动（含活动信息和签到工时 join） | VolApplied |
| | DELETE `/api/registrations/{regId}` | 取消报名（仅待审核） | VolApplied |
| | GET `/api/registrations?activityId=` | 组织者看活动的报名列表 | OrgManage 报名审核 tab |
| | PUT `/api/registrations/{regId}/audit` | 组织者审核报名；通过时自动创建 attendance 记录 + 给志愿者发通知 | OrgManage |
| **签到/志愿时** | GET `/api/attendance?activityId=` | 组织者看签到记录（仅本人活动可查） | OrgManage 志愿时 tab |
| | PUT `/api/attendance/{recordId}/hours` | 修改志愿时（body: `{hours, minutes}`；hours+minutes=0 → 已发证书自动失效，>0 → 自动发/复活证书） | OrgManage 弹窗 |
| | PUT `/api/attendance/{recordId}/manual` | 手动补签（body: `{checkInTime, checkOutTime, hours, minutes}`） | OrgManage |
| **证书** | GET `/api/certificates/mine` | 我的证书列表（可加 `activityName` 模糊过滤） | VolCert |
| | GET `/api/certificates/{id}/pdf` | 下载证书 PDF（iText 7，S5 ✅，仅本人 + 有效状态，返 application/pdf 流） | VolCert |
| **消息** | GET `/api/messages/mine` | 我的消息（含公告实化条目，可按 `type` 过滤） | VolMsg / OrgMsg |
| | POST `/api/messages/notice` | 管理员发公告（body: `{title, content, targets:["全体志愿者","全体组织者"]}`，会保留 1 条原始 + 实化到每个目标用户） | AdminNotice |
| | GET `/api/messages/notices` | 历史公告列表（原始条目，receiver=NULL） | AdminNotice |
| **组织者申请** | POST `/api/organizer-applications` | 志愿者提交申请（body: `{reason, materialUrl}`；已有待审则拒绝重复提交） | VolApplyOrg |
| | GET `/api/organizer-applications/mine` | 我的申请历史 | VolApplyOrg |
| | GET `/api/organizer-applications?status=` | 管理员看申请列表（可按状态过滤） | AdminOrg |
| | PUT `/api/organizer-applications/{appId}/audit` | 审核（body: `{approve}`，通过时自动把 users.is_organizer=true + 站内信通知） | AdminOrg |
| | GET `/api/organizers` | 已通过组织者列表（含已发活动数 actCount） | AdminOrgManage |
| | DELETE `/api/organizers/{userId}` | 撤销组织者资质（is_organizer=false + 站内信通知） | AdminOrgManage |
| **超管** | GET `/api/users/promotable` | 列可提升的用户（非超管+非管理员），支持 `name`/`userNo` 过滤 | SuperAdd |
| | GET `/api/users/admins` | 当前管理员列表（不含超管） | SuperDelete |
| | PUT `/api/users/{userId}/promote-admin` | 提升为管理员（同时设 role=admin，发站内信） | SuperAdd |
| | PUT `/api/users/{userId}/revoke-admin` | 撤销管理员（role=volunteer，发站内信；不影响超管） | SuperDelete |
| **统计** | GET `/api/stat/dashboard` | 工作台数据（按当前角色返回 metrics map：志愿者 `totalHours/totalMinutes/certificateCount/approvedRegCount`；组织者 `myActivityCount/publishedActivityCount/pendingRegCount`；管理员 `totalActivities/totalVolunteers/totalCertificates/pendingActivityAudits/totalServiceHours`；超管 `totalAdmins/totalUsers`） | 各 Dashboard |
| **对外 API** | GET `/api/public/hours?studentId=` | 综测系统按学号查累计工时（S5 ✅，白名单无须 JWT） | — |
| **文件上传** | POST `/api/files/upload` | 单文件上传（≤5MB，jpg/png/gif/webp/pdf）→ 返回 `{url, filename, originalName, size}`（S5） | VolApplyOrg |
| | GET `/api/files/static/**` | 公开访问上传过的文件（S5，白名单） | 申请记录"查看"链接 |
| **报表导出**（S6）| GET `/api/reports/personal-hours.xlsx[?volunteerId=]` | 个人工时 xlsx（本人或 admin），含合计行 | VolCert |
| | GET `/api/reports/activity-summary.xlsx?activityId=` | 活动签到汇总 xlsx（组织者本人或 admin） | OrgManage |
| | GET `/api/reports/monthly.xlsx?year=&month=` | 月度志愿工时汇总 xlsx（admin），按签退月份归集所有志愿者 | AdminDash |

### 12.4.1 接口契约总览（S3 完工口径）

- **统一响应**：`{ "code": 0, "msg": "ok", "data": <T> }`；axios 拦截器自动拆 `data`；`code !== 0` 走 `ElMessage.error`
- **分页响应**：`PageResult<T> = { total: number, rows: T[] }`（所有列表接口统一）
- **分页参数**：`page`（默认 1）、`pageSize`（默认 10）
- **时间字段**：`LocalDateTime → "yyyy-MM-dd HH:mm:ss"`，`LocalDate → "yyyy-MM-dd"`
- **活动展示编号**：`actNo = yyyyMMdd(start_time) + zero-pad-4(id)`，例如 id=1 + 2026-05-01 → `202605010001`
- **志愿者展示编号**：`volId = "VOL-" + zero-pad-5(userId)`
- **错误码段位**：1xxx 系统/未找到；2xxx 鉴权（2003 凭证错/2004 token 无效）；3xxx 业务规则（3001 活动未审核/3002 证书失效）；4xxx 参数校验
- **认证白名单**：`/api/auth/login`、`/api/health/**`、`/swagger-ui/**`、`/v3/api-docs/**`
- **越权防护**：组织者类操作（活动 CRUD、报名审核、签到/工时维护）都走 `mustOwn` 校验或 `@PreAuthorize` ORG/ADM
- **S5 已实装**：文件上传 (`/api/files/upload`, `/api/files/static/**`)、证书 PDF (`/api/certificates/{id}/pdf` iText 7)、对外综测 API (`/api/public/hours`)、DataSeeder ≥2000 条（`vms.seed.bulk: true`）

### 12.5 RBAC 与 JWT

- 登录后 JWT payload：`{ uid, username, role, isOrganizer, isAdmin, exp }`
- 自定义注解 `@RequireRole({"admin","superadmin"})` 卡控接口
- 双身份切换：用户的 `role` 字段在 JWT 中可切；服务端用 `isOrganizer` 校验是否允许切到 organizer
- **越权防护**：组织者修改/删除活动时校验 `organizer_id == 当前用户 uid`

### 12.6 关键业务规则落地

| 规则 | 实现位置 |
|---|---|
| 工时改 0 → 证书自动失效 | `AttendanceService.updateHours()` 内事务里联动更新 `certificates.status` |
| 工时 > 0 且签到正常 → 自动发证 | `AttendanceService` 改工时后 upsert certificates |
| 活动 publish_status 只能在 audit_status=审核通过 后改 | Service 层断言 |
| 报名：一人一活动唯一 | 数据库 UNIQUE 约束 + Service 层捕获冲突 |
| 公告发布 → 给目标 scope 内每个用户插一条 messages（receiver_id 实化） | `MessageService.broadcast()`；也可保留一条 target_scope 公告 + 查询时合并 |

> **公告实现取舍**：方案 A（实化为每人一条）查询简单；方案 B（保留公告+合并查询）省空间。建议方案 A——课设规模无压力，且支持 is_read 个人化。

### 12.7 里程碑（建议 4 个 Sprint）

| Sprint | 时间 | 交付 |
|---|---|---|
| **S1 基础设施** | 第 1 天 | 项目骨架 + pom + application.yml + schema.sql + 测试种子 + 全局响应/异常 + Swagger 跑通 ✅ |
| **S2 认证 + 用户** | 第 2 天 | FR-07 完成；前端 Login + MainLayout 接通 JWT ✅ |
| **S3 核心业务闭环** | 第 3~4 天（2026-06-15 完成）| FR-01/02/03/04/05/06/10 完成；前端 Vol*/Org*/AdminAct/AdminNotice/Dashboard* 全部接真接口；auth/refresh 上线 ✅ |
| **S4 申请+超管** | 第 5 天（2026-06-15 完成）| FR-08 超管账号管理 + FR-09 组织者资质申请审核 + 撤销组织者；前端 VolApplyOrg/AdminOrg/AdminOrgManage/SuperAdd/SuperDelete 全部接真 ✅ |
| **S5 收尾** | 第 6 天（2026-06-16 完成）| 文件上传（FileController 防穿越）+ 证书 PDF（iText 7 + font-asian）+ 对外综测 API（白名单）+ DataSeeder 开关打开 + DEMO.md 答辩脚本 ✅ |
| **S6 扩展功能** | 第 7 天（2026-06-21 完成）| **报表导出**（`module/report` + Apache POI 5.2.5 + 个人工时 xlsx / 活动签到汇总 xlsx / 月度汇总 xlsx）+ 前端 VolCert / OrgManage / AdminDash 接通 ✅ |

### 12.8 开发与协同约定

- 后端仓库位置：`/data1/user/gjx/数据库课设_VMS/backend/`（与本 PROJECT.md 同目录，与前端仓库 `/data1/user/gjx/volunteer_managment_system/` 不同目录）
- 本地 MySQL：库名 `vms`、字符集 `utf8mb4`、账号 `vms_user/vms_pwd`（可在 `application-dev.yml` 配）
- 前端 `vue.config.js` 增加 `devServer.proxy` 把 `/api` 反代到 `http://localhost:8080`
- 接口契约变动 → 立刻更新本文档 §12.4 + Swagger
- 每次推进同步更新本 PROJECT.md 和 `.claude/memory/project_vms_*.md`

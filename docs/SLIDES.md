# VMS 答辩 PPT 大纲

> **总时长建议**：演讲 9 分钟 + 现场演示 4 分钟 + Q&A 5 分钟，共 18 分钟左右。
> 每页 3-5 个要点；尽量不要把整段段落塞进幻灯片，文字交给讲稿。
> 视觉风格建议：浅红 + 米白主题（跟前端 App 配色一致），衬一张实拍/插画做封底。

---

## 第 1 页：封面

- **项目名称**：志愿服务工时认证与活动管理系统（VMS）
- **课程**：数据库系统课程设计
- **班级 / 第七组** + 成员姓名
- 答辩时间：2026 年 6 月 xx 日

> 视觉：标题居中 + 一张志愿者活动场景图做底图（开源素材即可），右下角小写日期/班级。

---

## 第 2 页：目录

1. 项目背景与定位
2. 需求分析与角色模型
3. 系统总体设计
4. 数据库设计
5. 关键业务实现
6. 设计创新点
7. 测试与演示
8. 总结与展望

> 视觉：左右两栏，1-4 在左，5-8 在右；每条带一个图标。

---

## 第 3 页：项目背景

- 国内现状：i志愿 / 志愿汇等全国级平台，校园场景里有三个痛点
  - 数据掌握在中央，**学校无法自治**
  - 工时数据 **难对接**校内综合测评系统
  - 全国通用 → **细颗粒场景**（双身份、两层活动状态）做不深
- 解题思路：**校园轻量补充方案 + 对外开放接口**，不取代 i志愿

> 视觉：左侧三个痛点用警示色图标，右侧画箭头指向"VMS = 校内补位"。
> 讲稿（30 秒）：开门见山，"我们不是要再造一个 i志愿，而是在校内做精"。

---

## 第 4 页：需求分析 · 4 角色

| 角色 | 入口 | 主要职责 |
|---|---|---|
| **志愿者** | SSO 登录 + 选身份 | 报名活动、签到、查工时/证书、申请组织者 |
| **活动组织者** | 同上（需资质） | 发布/改/删活动、审报名、签到管理、改志愿时 |
| **系统管理员** | 同上（需超管赋权） | 活动审核、组织者申请审核、发公告、全局统计 |
| **超级管理员** | 专属账号直登 | 管理员账号授权与撤销，不参与日常业务 |

- 关键关系：**志愿者 / 组织者 = 同一人的双身份**，可热切换

> 视觉：四个角色用大色块卡片排成 2x2，箭头连接表明上下层级。

---

## 第 5 页：需求分析 · 10+2 功能模块

| 编号 | 模块 | 参与者 |
|---|---|---|
| FR-01 ~ FR-07 | 活动 / 报名 / 签到 / 工时 / 证书 / 查询 / 鉴权 | 三角色 |
| FR-08 | 超管账号管理 | 超管 |
| FR-09 | 组织者申请与审核 | 志愿者 + 管理员 |
| FR-10 | 消息与公告 | 全体 |
| **FR-11**（S6 扩展） | **活动双向评价** | 志愿者 + 组织者 |
| **FR-12**（S6 扩展） | **报表 Excel 导出** | 三角色 |

- 数据规模指标：≥200 用户 / ≥100 活动 / ≥2000 报名+签到（演示种子已就绪）

> 视觉：表格 + S6 两行用 **加粗 + 黄底** 突出"我们做的扩展"。

---

## 第 6 页：系统架构

```
┌──────────────────────────────────────────────────────────┐
│  浏览器（Vue 3 + Element Plus + axios）                  │
└────────────────────┬─────────────────────────────────────┘
                     │ JWT Bearer
┌────────────────────┴─────────────────────────────────────┐
│  Spring Boot 3.2.5 后端                                   │
│  ├─ Controller（@PreAuthorize 切面）                      │
│  ├─ Service（@Transactional + mustOwn 越权防护）          │
│  └─ MyBatis-Plus + LambdaQueryWrapper                    │
└────────────────────┬─────────────────────────────────────┘
                     │ JDBC
┌────────────────────┴─────────────────────────────────────┐
│  MySQL 8.0 / utf8mb4_unicode_ci                          │
└──────────────────────────────────────────────────────────┘
   外延：iText 7 PDF · Apache POI xlsx · /api/public 综测接口
```

- 三层架构 + 无状态 JWT
- 部署模型：**本地部署 + 联网服务**（SSO 模拟、对外开放）

> 视觉：用 PPT 的层叠矩形把架构图复刻一遍，每层右侧标技术栈。

---

## 第 7 页：技术栈选型

| 层 | 选型 | 为什么 |
|---|---|---|
| 前端框架 | Vue 3 + Element Plus | 组合式 API + UI 库成熟，国内文档齐 |
| 后端框架 | Spring Boot 3.2.5 | 主流 + JDK 17 LTS |
| ORM | MyBatis-Plus 3.5.5 | 不写 XML，链式 LambdaQueryWrapper |
| 数据库 | MySQL 8.0 | 课程要求 + CHECK 约束 |
| 鉴权 | jjwt 0.12.5（JWT） | 无状态、移动端友好 |
| **PDF** | **iText 7.2.5 + font-asian** | 内置中文字体，部署不带 TTF |
| **Excel** | **Apache POI 5.2.5** | xlsx 写起来直观 |
| API 文档 | springdoc-openapi 2.3.0（Swagger UI） | 自动生成，演示能直接点接口 |

> 视觉：表格用图标列代替文字，整页留白多一点。

---

## 第 8 页：数据库设计 · ER 概览

```
users (1) ─── (N) activities             [organizer_id]
users (1) ─── (N) registrations          [volunteer_id]
users (1) ─── (N) attendance             [volunteer_id]
users (1) ─── (N) certificates           [volunteer_id]
users (1) ─── (N) messages               [receiver_id]
users (1) ─── (N) organizer_applications [applicant_id / auditor_id]
users (1) ─── (N) activity_reviews       [reviewer_id / target_id]  ← S6
activities (1) ─── (N) registrations / attendance / certificates / activity_reviews
```

- **8 张表**（S6 新增 activity_reviews）
- 状态字段统一 `varchar(20)` 存中文枚举（如"待审核""审核通过"），不用 MySQL ENUM 类型——便于阅读和扩展

> 视觉：把这段 ASCII 图画成正式 ER 图（PowerDesigner / DataGrip 都能导出）。
> 讲稿（45 秒）：先指 users 是中心节点，再讲 activities 是另一个枢纽，最后说 reviews 是双向 FK 同表自连。

---

## 第 9 页：数据库设计 · 关键表

### users（用户）
`user_id PK · username UNIQUE · password(BCrypt) · role · is_organizer · is_admin`

### activities（活动）— 两层状态
`audit_status: 待审核/审核通过/审核不通过` × `publish_status: 发布中/已停止`

### attendance（签到/工时）
`UNIQUE(activity_id, volunteer_id)` 防一人重复；`CHECK(service_minutes 0~59)`

### certificates（证书）
`status: 有效/已失效` —— 由 AttendanceService 副作用驱动

### activity_reviews（S6 评价）
`UNIQUE(activity_id, reviewer_id, target_id)` 防重复评 + `CHECK(rating 1~5)`

> 视觉：每张表用一个圆角矩形，关键字段加粗，外键画红色虚线。

---

## 第 10 页：核心业务闭环

```
超管授权初始管理员
    ↓
管理员审核组织者申请 ──→ 用户拿到 ORG 资格
    ↓
组织者发活动 → 管理员审核 → 组织者开"发布中"
    ↓
志愿者报名 → 组织者审核 → 自动建 attendance 占位
    ↓
组织者改工时 / 手动补签 → 证书自动 upsert 或失效
    ↓
志愿者下载 PDF 证书 + 评价活动
    ↓
综测系统通过 /api/public/hours 取累计工时
```

- 横跨 7 个业务模块，10 步全闭环

> 视觉：用箭头链做时间线，每步配一张迷你前端截图。
> 讲稿（1 分钟）：跟着箭头讲一遍，强调"每一步都有数据沉淀"。

---

## 第 11 页：关键实现 1 — 工时与证书双向联动

```java
// AttendanceService.updateHours()
if (hours > 0 || minutes > 0) {
    if (cert == null) certificateMapper.insert(...);   // 自动发证
    else updateCert(VALID);                            // 复活已失效证书
} else {
    if (cert != null) updateCert(INVALID);             // 工时归零 → 失效
}
messageService.sendDirect(volunteerId, ...);           // 同时发消息通知
```

- **改一次工时 = 三处副作用**（attendance / certificates / messages）
- 全部包在 `@Transactional` 里，要么都成要么都回滚

> 视觉：左侧贴代码片段，右侧画"工时改 0 → 证书右上角变灰"的前端截图对比。

---

## 第 12 页：关键实现 2 — 鉴权与权限实时同步

- **JWT** 无状态：登录返 token，每请求带 `Bearer`，过期时间 24h
- **双层越权防护**：
  - 接口层：`@PreAuthorize("hasAuthority('ORG')")`
  - Service 层：`mustOwn(activityId, currentUserId)` 二次校验
- **权限撤销实时生效**：
  - 后端 `AuthService.me()` / `refresh()` 每次重查 DB
  - 前端 axios 拦截器：403 → 静默拉 `/auth/me` → 同步本地状态 → 跳回家页

```javascript
// src/api/index.js
} else if (status === 403) {
  const me = await http.get('/auth/me')
  localStorage.setItem('isOrganizerQualified', String(me.isOrganizerQualified))
  if (!me.isOrganizerQualified && isOrgRoute) router.push('/sys/dashboard-volun')
}
```

> 视觉：流程图——左：管理员撤销组织者；右：志愿者页面 1 秒内自动跳走。

---

## 第 13 页：关键实现 3 — 公告"实化"分发

- 管理员一次发公告 → 后端在 `messages` 表写：
  - 1 条 `receiver_id=NULL` 的原始留底
  - N 条对每个目标用户实化（带 `receiver_id`）
- 收益：**每人独立 `is_read` 状态 + 红点能精准到人**
- 取舍：200 人发一次公告 = 201 条记录，课设规模无压力

> 视觉：左侧画 1 条原始公告 → 中间箭头分叉 → 右侧 N 条实化记录的表格。

---

## 第 14 页：S6 创新点 ① 双向评价

- **谁能评谁**：
  - 志愿者只能评：自己已签退活动的组织者
  - 组织者只能评：自己活动里已签退的志愿者
- **防重复**：DB `UNIQUE(activity_id, reviewer_id, target_id)` 三元组
- **role 字段后端推断**：`reviewerId == activity.organizerId` 自动判断
- 接口：5 个 REST（提交 / 我评的 / 评我的 / 活动评价 / 平均分聚合）

```java
// 服务端逻辑（节选）
boolean reviewerIsOrganizer = a.getOrganizerId().equals(reviewerId);
if (reviewerIsOrganizer) {
    // 必须有 target 在我这活动的签退记录
} else {
    // target 必须就是这场活动的 organizer + 我自己签退过
}
```

> 视觉：前端弹窗截图（el-rate 5 星 + 评语）+ 后端校验流程图。

---

## 第 15 页：S6 创新点 ② Excel 报表导出

- 3 个端点，共享 `ResponseEntity<ByteArrayResource>` xlsx 流：

| 接口 | 用户 | 用途 |
|---|---|---|
| `/api/reports/personal-hours.xlsx` | 本人或 admin | 个人工时表（带累计） |
| `/api/reports/activity-summary.xlsx` | 组织者或 admin | 活动签到汇总 |
| `/api/reports/monthly.xlsx?year=&month=` | admin | 月度全院按工时降序排行 |

- 技术栈：**Apache POI 5.2.5 `XSSFWorkbook`**
- 数据源：直接复用 `attendance` 表 + `selectBatchIds` 拼名字
- 权限：路由层 `@PreAuthorize` + Service 层 `mustOwn`

> 视觉：右下角放真实导出的 xlsx 截图（用 Excel 打开），展示标题/表头/合计行。

---

## 第 16 页：S6 创新点 ③ 对外开放接口

```bash
curl http://localhost:8081/api/public/hours?studentId=202400040101
```

```json
{
  "code": 0,
  "data": {
    "studentId": "202400040101",
    "studentName": "李四",
    "totalHours": 18,
    "totalMinutes": 30,
    "formattedDuration": "18小时30分钟",
    "certifiedActivityCount": 6
  }
}
```

- **白名单无 JWT** —— 综测系统按学号直接查
- 跟 i志愿对照：**他们没这种开放接口**
- 真上线建议补一个 API Key 校验

> 视觉：左侧 curl + JSON，右侧画"综测系统 → VMS"的对接关系图。

---

## 第 17 页：S6 创新点 ④ 现代化技术栈实战

- **中文 PDF 零外部依赖**：iText 7 + font-asian 的 `STSong-Light` 内置 CJK 字体，部署不用带 TTF
- **种子数据自动灌**：`vms.seed.bulk: true` 启动时灌 ≥230 用户 / ≥100 活动 / ≥2200 报名签到（撑 SRS §8.2 数据规模指标）
- **三处防护**：
  - 文件上传：UUID 命名 + `normalize()` + `startsWith(root)` 防路径穿越
  - SQL 防注入：全程 LambdaQueryWrapper 参数化（**今天 fix 了一处遗留的字符串拼接**）
  - NPE 防御：admin 改孤儿记录时友好报错而非崩溃

> 视觉：三个盾牌图标 + 三处实战防护点。

---

## 第 18 页：现场演示流程（4 分钟）

> 提前把 4 个浏览器窗口（Chrome 无痕） 排好：超管 / 管理员 / 组织者+志愿者双身份 / 普通志愿者

| 时长 | 步骤 | 体现的功能 |
|---|---|---|
| 30s | 超管登录 → 提升 `12345678` 为管理员 | FR-08 |
| 30s | 组织者发新活动 → 管理员审核通过 → 组织者点"发布中" | FR-01 两层状态 |
| 30s | 志愿者报名 → 组织者审核 → 自动建 attendance | FR-02 副作用 |
| 30s | 组织者手动补签 → 证书自动出现 | FR-03/04/05 联动 |
| 30s | 把工时改 0 → 证书右上角变灰（已失效） | 工时-证书双向 |
| 30s | 志愿者下载 PDF 证书 → 评价活动 | FR-11 |
| 30s | 管理员选 6 月 → 导出月度全院 Excel | FR-12 |
| 30s | curl 调 `/api/public/hours` 拿累计工时 | 对外接口 |

> 演示 Tips：
> - 演示前把"导出 Excel"和"PDF 下载"提前点过一次，确保浏览器允许自动下载
> - 准备一个 fallback 视频（手机录屏），万一现场网络不行直接放
> - 评价那一步可以演示"重复评价被后端拒绝"作为加分项

---

## 第 19 页：测试

| 测试维度 | 方法 | 结果 |
|---|---|---|
| 前端 build | `npm run build` | ✅ 全过，47 个 chunk |
| 前端 lint | `npx vue-cli-service lint` | 4 个非阻塞死代码警告 |
| 前后端 API 一致性 | 静态扫描 axios 调用 vs Controller @Mapping | 39 + 7 全对得上 |
| 后端单元测试 | （可选，时间允许补上） | — |
| 集成测试 | 现场演示完整闭环 | 见演示页 |
| 数据规模 | bulk seed | ≥230 用户、≥100 活动、≥2200 报名签到 |

> 视觉：用 ✅ / ⚠️ 标记每行；最后一行展示一张 Excel 截图（230 行用户的导出）。

---

## 第 20 页：与 i志愿的对比（差异化总结）

| 维度 | i志愿 | VMS |
|---|---|---|
| 部署 | 中央云 | **校园本地** |
| 工时数据归属 | 平台 | **学校** |
| 综测对接 | 闭环 | **开放 RESTful** |
| 双身份切换 | 分账号 | **同账号热切换** |
| 工时-证书联动 | 单向 | **双向（改 0 失效 / 复活）** |
| 权限撤销 | 需重新登录 | **403 实时同步** |
| 公告未读追踪 | 全员通用 | **实化到人** |

> 视觉：双栏表格，VMS 列加浅色背景突出。
> 讲稿（45 秒）：**强调"互补而非替代"**——避免老师追问"你们怎么比官方平台还好"。

---

## 第 21 页：开发统计

- **代码量**（粗略）：
  - 后端 Java：~70 个 .java 文件，约 6000 行
  - 前端 Vue/JS：~50 个文件，约 7000 行
  - SQL：8 张表 DDL + 演示种子约 500 行
- **里程碑**：S1 骨架 → S2 鉴权 → S3 核心业务 → S4 超管+申请 → S5 上传+PDF+对外接口 → **S6 评价+报表**
- **协作**：前端骨架 + 后端实现 + 文档双线

> 视觉：横向时间轴，每个 S 节点标日期 + 关键交付物。

---

## 第 22 页：项目展望

- **A 类（短期可落地）**：
  - 信用分体系（迟到扣分 / 信用门槛影响报名优先级）
  - 班级 / 学院工时排行榜
- **B 类（中期）**：
  - 二维码自助签到（消除"组织者全权代填"）
  - 个性化推荐（基于历史 tag 协同过滤）
- **C 类（远期）**：
  - 微信小程序（复用现有 REST API）
  - 证书 SHA-256 链式存证（轻量级"区块链"演示）

> 视觉：三个上升的台阶 + 时间标尺。

---

## 第 23 页：致谢

- 感谢 xxx 老师的指导
- 感谢小组成员通力协作
- 感谢 i志愿等先行平台的启发

> 视觉：简洁居中，留白多一点。

---

## 第 24 页：Q&A

> 准备常见追问：

**Q1：你们为什么不用 MySQL 的 ENUM 类型存状态？**
A：可扩展、可读、迁移友好。`varchar(20)` + 应用层常量约束足够。

**Q2：JWT 怎么实现注销？**
A：JWT 无状态，前端清 token 即可；如果要服务端注销需要 Redis 黑名单（现阶段未做）。

**Q3：CORS 这么宽松不会有问题？**
A：dev 期 `allowedOriginPattern("*")` + `allowCredentials(true)` 方便调试；演示前可以收紧到 `localhost:8080`。

**Q4：现在数据 200 用户没压力，10 万人怎么办？**
A：①表都加了索引；②MyBatis-Plus 内置分页；③公告实化策略可以改成"按 scope 查询"；④`attendance` 写多查少可上读写分离。

**Q5：i志愿已经存在为什么还做？**
A：定位是"校园本地化补充"而非替代，参见第 20 页对比。

**Q6：S6 评价模块有什么核心约束？**
A：①UNIQUE 防重复；②必须已签退；③role 后端推断不信前端；④CHECK rating 1~5。

**Q7：怎么保证组织者不会改自己审核的报名？**
A：报名审核的 organizerId 与 activity.organizer_id 必须匹配；Service 里 `mustOwn` 二次校验。

---

## 第 25 页：封底

```
谢 谢 聆 听
```

> 视觉：跟封面呼应；底部小字 GitHub 链接 + 答辩日期。

---

## 附录 A：现场截图清单（准备时存到 `slides_assets/`）

- 登录页（封面/演示用）
- 角色选择页（双身份切换）
- 志愿者活动列表（含报名按钮）
- 组织者活动管理（含两层状态 chip）
- 组织者签到管理（含"评价"按钮）
- 志愿者证书列表（含"已失效"灰色 chip）
- PDF 证书实物（用浏览器打开放截图）
- 月度报表 Excel 实物
- Swagger UI 界面（13 + 2 个 tag 分组）
- curl 调对外 API 的命令行截图

## 附录 B：演讲分段时长

| 段落 | 页数 | 时长 |
|---|---|---|
| 开场（封面+目录+背景） | 1-3 | 1 分钟 |
| 需求与设计 | 4-9 | 2 分钟 |
| 关键实现 | 10-13 | 2 分钟 |
| S6 创新点 | 14-17 | 2 分钟 |
| 现场演示 | 18 | 4 分钟 |
| 测试 & 对比 & 展望 | 19-22 | 1.5 分钟 |
| 致谢 & Q&A | 23-25 | 5 分钟 |
| **合计** | **25** | **17.5 分钟** |

## 附录 C：避坑提醒

- 别说"我们做得比 i志愿好"——评审会追问规模、用户量、合规
- 别承诺"上线就能用"——明确是课设原型
- PPT 上不要写真密码（演示账号 `123456` 可以提，但别贴 BCrypt hash）
- 截图前把浏览器 URL 栏的 `localhost:8081` 该模糊就模糊（避免暴露 API 细节）
- 演示电脑提前关掉企业微信、QQ、微信弹窗，免得社死

# VMS 志愿服务工时认证与活动管理系统（前后端一体）

> 数据库系统课程设计 · 24软工4班第七组
> `develop1.1` 分支 = **前端（Vue3）+ 后端（Spring Boot）+ 文档** 全套，clone 一个分支即可运行整个项目。

## 目录结构

```
volunteer_managment_system/        ← 仓库根 = 前端（Vue3 + Element Plus）
├── src/                           前端源码（views/api/router/layout…）
├── package.json
├── backend/                       ← 后端（Spring Boot 3.2.5 + MyBatis-Plus + MySQL）
│   ├── src/main/java/edu/scau/vms/
│   ├── src/main/resources/        application*.yml + sql/schema.sql + sql/data.sql
│   └── pom.xml
└── docs/                          PROJECT.md（总设计）/ DEMO.md（答辩脚本）/ VMS数据表设计.docx
```

## 环境要求

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | **17**（必须，不能用 21/25）| Spring Boot 3.2.5 只支持 17~21 |
| Maven | 3.9+ | 命令行构建后端；或直接用 IDEA 内置 Maven |
| MySQL | 8.0 | 数据库 |
| Node.js | 18+ | 跑前端 |

---

## 一、准备数据库

```sql
CREATE DATABASE vms DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
> 7 张表会在后端首次启动时按 `backend/src/main/resources/sql/schema.sql` 自动建好，
> 并自动灌入 **≥2000 条**演示数据（`vms.seed.bulk: true`）。

## 二、配置后端数据库密码

打开 `backend/src/main/resources/application-dev.yml`，把 `password` 改成**你本机 MySQL root 密码**：

```yaml
spring:
  datasource:
    username: root
    password: 123456   # ← 改成你自己的 MySQL root 密码
```

## 三、启动后端（端口 8080）

```bash
cd backend
mvn -DskipTests spring-boot:run
```
启动成功后访问 <http://localhost:8080/api/health/db>，应返回 `{"code":0,"data":230}` 左右。
接口文档（Swagger）：<http://localhost:8080/swagger-ui/index.html>

> **国内下载依赖慢 / 报"程序包不存在"？** 在 `C:\Users\你\.m2\settings.xml` 加阿里云镜像：
> ```xml
> <settings><mirrors><mirror>
>   <id>aliyun</id><mirrorOf>central</mirrorOf>
>   <url>https://maven.aliyun.com/repository/public</url>
> </mirror></mirrors></settings>
> ```

## 四、启动前端（端口 8081）

```bash
# 回到仓库根目录
npm config set registry https://registry.npmmirror.com   # 可选，国内加速
npm install
npm run serve
```
浏览器打开 <http://localhost:8081>。前端 axios 直连 `http://localhost:8080/api`（后端已开 CORS，无需配代理）。

---

## 五、测试账号（密码均为 `123456`）

> 💡 实际用户名以数据库为准，执行 `SELECT user_id, username, name, role FROM users;` 可查看所有账号。

| 用户名 | 角色 | 用途 |
|---|---|---|
| `00000000` | 超级管理员 | 增/删管理员 |
| `12345678` | 管理员 | 活动审核、组织者申请审核、发公告 |
| `202400010101` | 志愿者+组织者（双身份）| 发活动、审报名、签到、改志愿时 |
| `202400020101` | 普通志愿者 | 报名、查证书、申请组织者 |
| `202400040101` | 批量种子志愿者 | 累计工时统计、对外 API 演示 |

> **多角色同时操作**：同一浏览器只能登一个账号（token 会互相覆盖），
> 请用不同浏览器或 Chrome 无痕窗口分别登录不同角色。

## 六、核心业务闭环（可照着点一遍）

`org01` 发活动 → `admin01` 审核通过 → `org01` 开「发布中」 → `vol01` 报名 →
`org01` 审核报名 → `org01` 改志愿时/补签 → `vol01` 我的证书出现并可下载 PDF →
把工时改 0 → 证书自动失效。

更详细的演示脚本见 `docs/DEMO.md`，总体设计见 `docs/PROJECT.md`。

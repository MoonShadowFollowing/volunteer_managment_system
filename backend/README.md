# VMS 后端 — Sprint 1 骨架

志愿服务工时认证与活动管理系统（VMS）的 Spring Boot 后端。
配套前端：`/data1/user/gjx/volunteer_managment_system/`（Vue3 + Element Plus）。
配套需求/表结构：`/data1/user/gjx/数据库课设_VMS/PROJECT.md`。

## S1 范围

本次只搭基础设施：项目骨架、7 张表 DDL、小规模演示数据、统一响应/异常、Swagger。
**不含**鉴权与业务接口。

## Windows 启动步骤

### 1. 环境准备
- JDK 17（推荐 Eclipse Temurin / Oracle JDK 17）
- MySQL 8.0（默认端口 3306）
- Maven 3.9+（或直接用 IntelliJ IDEA 自带的 Maven，无需额外装）

### 2. 建库
打开 MySQL 命令行或 Workbench，执行：
```sql
CREATE DATABASE vms DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 配置数据库账号
编辑 `src/main/resources/application-dev.yml`，把 `username` 和 `password` 改成你本机 MySQL 的账号。
默认：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/vms?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
```

### 4. 启动
**方式 A：IDEA**（推荐）
- File → Open → 选项目根目录
- 等 Maven 自动导入依赖
- 右键 `VmsApplication` → Run

**方式 B：命令行**
项目根目录打开 PowerShell：
```cmd
mvn spring-boot:run
```

第一次启动会自动：
- 下载依赖（约 5~10 分钟）
- 在 `vms` 库里建 7 张表
- 灌入小规模演示种子数据

控制台见 `Started VmsApplication in X.X seconds (process running for Y.Y)` 即成功。

### 5. 验证

| URL | 预期返回 |
|---|---|
| http://localhost:8080/api/health/ping | `{"code":0,"msg":"ok","data":"pong"}` |
| http://localhost:8080/api/health/db | `{"code":0,"msg":"ok","data":<用户数>}` |
| http://localhost:8080/swagger-ui/index.html | Swagger UI 首页，能看到 Health 接口 |

## 项目结构

```
src/main/java/edu/scau/vms/
├── VmsApplication.java          入口
├── common/
│   ├── Result.java              统一响应 { code, msg, data }
│   ├── PageResult.java          分页返回
│   ├── exception/               BizException + 全局拦截器
│   └── constant/                ErrorCode + 7 类状态枚举常量
├── config/
│   ├── JacksonConfig.java       时间格式
│   ├── MybatisPlusConfig.java   分页插件 + 自动填充
│   ├── SwaggerConfig.java       OpenAPI 元信息
│   └── CorsConfig.java          跨域（与前端 8081 联调）
└── module/health/
    └── HealthController.java    ping + db 健康检查

src/main/resources/
├── application.yml              公共配置
├── application-dev.yml          开发环境 DB 连接
└── sql/
    ├── schema.sql               7 张表 DDL
    └── data.sql                 小规模演示数据
```

## 接续开发

下一个 Sprint（S2）：JWT 鉴权 + 用户管理 + 超管模块。
完整路线图见 `/data1/user/gjx/数据库课设_VMS/PROJECT.md §12.7`。

## 端口与跨域

- 后端：8080
- 前端 dev：8081（Vue CLI 默认；如冲突可改 `vue.config.js`）
- 已开放 CORS 给所有 origin 模式，便于联调；上线前请收紧

## 重启与清库

`spring.sql.init.mode=always` 表示**每次启动都执行 schema.sql / data.sql**。
DDL 用 `CREATE TABLE IF NOT EXISTS`、`data.sql` 用 `INSERT IGNORE`，重启不会重复出错。
如需清空：MySQL 中 `DROP DATABASE vms;` 然后重新 `CREATE DATABASE vms ...`。

演示前可改 `application.yml` 把 `mode` 设为 `never`。

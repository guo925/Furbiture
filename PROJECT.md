# 项目信息文档（供 AI 阅读）

> 本文件是 Furbiture 项目的权威技术参考。AI 在新会话中处理本项目任务前，应优先阅读本文件与 `.claude/CLAUDE.md`（协作规范），再结合具体代码做决策。

---

## 1. 项目概述

**Furbiture（橙家优选）** 是一个家具电商系统，前后端分离：

- **后端**：Java + Spring Boot，提供 RESTful API
- **前端**：Vue 3 + Vite 单页应用
- **角色体系**：普通用户（USER）、商家（MERCHANT）、管理员（ADMIN）三方，各自独立端
- **定位**：教学/学习性质项目，代码遵循企业级规范（Controller-Service-Mapper 分层、DTO/VO、统一返回、统一异常）

## 2. 技术栈与依赖

### 后端（Maven，`pom.xml`）

| 技术 | 版本 | 用途 |
|---|---|---|
| Java | 17（release；本地 JVM 实际为 21） | 语言 |
| Spring Boot | 3.3.4 | 框架 |
| Spring Security + JWT | jjwt 0.12.6 | 认证鉴权（Bearer Token） |
| MyBatis-Plus | 3.5.5 | ORM（`mybatis-plus-spring-boot3-starter`） |
| MySQL | mysql-connector-j 8.x | 数据库 |
| Redis | spring-boot-starter-data-redis | 缓存（当前主要配置存在） |
| WebSocket | spring-boot-starter-websocket | 实时通知 |
| AOP | spring-boot-starter-aop | 操作日志切面 |
| Validation | starter-validation | 参数校验 |
| Lombok | 1.18.36 | 简化代码 |
| springdoc-openapi | 2.6.0 | Swagger UI（`/swagger-ui.html`） |
| 阿里云 OSS | 3.17.3 | 文件上传（开发环境用本地存储） |
| 测试 | spring-boot-starter-test | 单元测试 |

### 前端（npm，`frontend/package.json`）

| 依赖 | 版本 | 用途 |
|---|---|---|
| Vue | ^3.4.21 | 框架 |
| Vite | ^5.1.6 | 构建/开发服务器 |
| Element Plus | ^2.6.1 | UI 组件库 |
| Pinia | ^2.1.7 | 状态管理 |
| Vue Router | ^4.3.0 | 路由 |
| Axios | ^1.6.7 | HTTP 请求 |
| ECharts | ^6.1.0 | 图表（仪表板） |

### 运行端口与环境

| 项 | 值 |
|---|---|
| 后端端口 | `9090` |
| 前端开发端口 | `3003`（Vite 代理 `/api` 和 `/uploads` → `localhost:9090`） |
| 数据库 | `furniture_db`，MySQL `localhost:3306`，用户 `root` / 密码 `123456` |
| 前端 API 基础路径 | `/api`（见 `frontend/.env*`，`VITE_API_BASE_URL`） |

## 3. 项目结构

```
Furbiture/
├── pom.xml                     # Maven 后端构建
├── src/main/java/com/gjx/      # 后端源码
│   ├── FurnitureApplication.java
│   ├── annotation/             # OperationLog 注解
│   ├── aspect/                 # OperationLogAspect 操作日志切面
│   ├── common/                 # R<T> 统一返回、ResultCode、BusinessException
│   ├── config/                 # Security/Redis/WebSocket/MyBatis/CORS/调度等配置
│   ├── controller/
│   │   ├── user/               # 用户端（Auth/Product/Cart/Order/Review/Favorite/...）
│   │   ├── admin/              # 管理端（商品/订单/用户/分类/仪表板/审计）
│   │   └── merchant/           # 商家端（商品/订单/分类/资料/仪表板）
│   │   └── FileController.java # 文件上传
│   ├── dto/                    # request/ 与 response/（VO）
│   ├── entity/                 # 13 个实体
│   ├── enums/                  # OrderStatusEnum、UserRoleEnum
│   ├── exception/              # GlobalExceptionHandler、ErrorResponse
│   ├── mapper/                 # MyBatis-Plus Mapper 接口（含自定义 SQL）
│   ├── security/               # JwtTokenProvider、JwtAuthenticationFilter、SecurityConfig
│   ├── service/                # 接口 + Impl/ 实现
│   ├── task/                   # OrderTimeoutTask（订单超时）
│   └── util/                   # AuthenticationUtil、ValidationUtil
├── src/test/java/              # 单元测试（当前仅一个启动测试）
├── database/                   # SQL 脚本（schema.sql、migration_*.sql、reset_data.sql）
├── frontend/
│   ├── vite.config.js          # 端口 3003 + /api 代理
│   └── src/
│       ├── main.js             # 入口（ElementPlus 全量注册）
│       ├── App.vue
│       ├── router/index.js     # 三级角色路由
│       ├── stores/             # Pinia（user.js 存 token，cart.js）
│       ├── api/                # axios 实例 + modules/（auth/admin/merchant/order/...）
│       ├── views/
│       │   ├── user/           # 前台页面
│       │   ├── admin/          # 管理后台（含 backup/ 旧版备份）
│       │   └── merchant/       # 商家后台
│       ├── components/         # 通用组件 + common/（DataTable、FormDialog）
│       ├── composables/        # usePagination/useForm/useLoading/useTheme 等
│       ├── directives/         # lazyLoad
│       └── assets/styles/      # main.css、design-tokens.css
└── .claude/                    # AI 协作配置（见 §9）
```

## 4. 数据库设计（15 张表）

核心表（`database/schema.sql`）：

| 表 | 说明 | 关键字段 |
|---|---|---|
| `user` | 用户 | `role`（USER/ADMIN/MERCHANT） |
| `category` | 商品分类 | 支持多级（parent_id、level） |
| `product` | 商品 | `merchant_id`（商家归属）、`price`、`stock`、`status`、`sales` |
| `product_image` | 商品图 | |
| `product_spec` | 商品规格 | |
| `address` | 收货地址 | |
| `cart` | 购物车 | |
| `order` | 订单 | **MySQL 关键字，实体用 `` `order` `` 转义**；`status` 0-5 |
| `order_item` | 订单项 | **冗余存储 `product_name` 快照** |

扩展表（`database/migration_v2.sql`、`migration_v3_planb.sql`）：
`favorite`、`review`、`notification`、`operation_log`、`merchant_audit`、`discount`

> **注意**：`merchant_audit` 和 `discount` 表存在但无对应实体，暂未接入 ORM。

### 订单状态（`OrderStatusEnum`，对应 `order.status`）

| code | 含义 |
|---|---|
| 0 | 待付款（PENDING_PAYMENT，可取消/可支付） |
| 1 | 已付款（PAID，可发货） |
| 2 | 已发货（DELIVERED，可确认收货） |
| 3 | 已完成（COMPLETED） |
| 4 | 已取消（CANCELLED） |
| 5 | 已退款（REFUNDED） |

## 5. 后端架构与规范

- **分层**：Controller（请求接收/校验）→ Service（业务）→ Mapper（数据访问），接口 + Impl 分离
- **统一返回**：`R<T>`（`{code, msg, data}`，code 200 成功），`ResultCode` 定义错误码
- **统一异常**：`GlobalExceptionHandler` 捕获业务异常与校验异常，返回 `ErrorResponse`
- **认证鉴权**：JWT Bearer Token；`JwtAuthenticationFilter` 解析并注入 SecurityContext；`SecurityConfig` 按角色控制 `/api/admin/**`、`/api/merchant/**` 权限
- **当前用户**：`AuthenticationUtil.getUsernameFromRequest(request)` 从 JWT 取用户名
- **日志**：`@Slf4j`；自定义 `@OperationLog` 注解 + AOP 切面记录操作日志
- **自动填充**：`MyMetaObjectHandler` 处理 createTime/updateTime
- **调度任务**：`OrderTimeoutTask` 处理订单超时
- **分页**：MyBatis-Plus `Page`；自定义 SQL 用 `@Select` 注解（如 `OrderItemMapper.getHotProducts`）

## 6. 前端架构与规范

- **路由**：`/user`、`/admin`、`/merchant` 三套布局与子路由；路由守卫校验登录与角色
- **状态**：Pinia `stores/user.js`（token 存 `localStorage` 键 `furniture_token`、用户存 `furniture_user`）、`stores/cart.js`
- **请求**：`api/request.js` 创建 axios 实例，`baseURL='/api'`，拦截器自动带 `Authorization: Bearer <token>`；HTTP 200 + code!=200 时统一弹错
- **API 模块**：`api/modules/`（auth、admin、merchant、product、order、cart、address、user、file、favorite）
- **通用组件**：`components/common/DataTable.vue`、`FormDialog.vue`
- **图标**：Element Plus 图标全量注册为全局组件

## 7. 核心业务模块与接口

### 用户端（`controller/user/`）
登录注册、商品浏览/搜索、购物车、下单支付（模拟）、订单管理、收货地址、评价、收藏、通知。

### 商家端（`controller/merchant/`）
- 商品/分类/订单管理、店铺资料
- **仪表板**（`MerchantDashboardController`，均返回真实数据）：

| 接口 | 返回结构 |
|---|---|
| `GET /api/merchant/dashboard` | `{productCount, pendingOrderCount, totalSales, todayOrderCount}` |
| `GET /api/merchant/dashboard/revenue-trend` | `[{date:"MM-DD", orders, sales}]`（近 7 天） |
| `GET /api/merchant/dashboard/category-distribution` | `[{name, value}]` |
| `GET /api/merchant/dashboard/order-funnel` | `{pending, paid, delivered, completed}` |
| `GET /api/merchant/dashboard/top-products` | `[{id, name, price, salesCount}]` |

### 管理端（`controller/admin/`）
商品/分类/订单/用户全量管理、**仪表板**（`AdminDashboardController`）：

| 接口 | 返回结构 |
|---|---|
| `GET /api/admin/dashboard` | `{totalOrderCount, todayOrderCount, totalSales, todaySales, userCount, productCount}` |
| `GET /api/admin/dashboard/sales-trend` | `[{date:"yyyy-MM-dd", sales, orderCount}]`（近 7 天） |
| `GET /api/admin/dashboard/hot-products` | `[{id, name, price, salesCount}]` |

## 8. 测试账号与数据初始化

所有账号密码均为 `123456`（BCrypt 哈希预置）：

| 用户名 | 角色 |
|---|---|
| `admin` | ADMIN |
| `user1` | USER |
| `merchant1` | MERCHANT（商品 merchant_id=3） |
| `merchant2` | MERCHANT（商品 merchant_id=4） |

初始化：`database/schema.sql`（建表 + 种子数据）→ `merchant_update.sql`（商家字段/账号）→ `migration_v2.sql`、`migration_v3_planb.sql`（扩展表）。

## 9. AI 协作工作流（`.claude/`）

- **提交门禁**：禁止直接 `git commit`（pre-commit hook 拦截）。必须用 `/git-save` 命令，流程：`tester` 单元测试全过 → `quality-engineer` 质量审查（零严重 + 零高危 + 评分≥60）→ 自动生成 commit message 并推送。紧急可 `/git-save --force`。
- **Agent**：`tester`（单元测试）、`quality-engineer`（质量审查）、`gitcommit-agent`（提交代理）、`Plan`、`Explore` 等
- **Skills/Commands**：`/restart`（重启前后端）、`unit-test`、`comments-check`
- **规范**：详见 `.claude/CLAUDE.md`（12 节协作规范：开发前理解上下文、双方案设计、企业级代码规范、修改前说明影响范围、调试问题分析等）

## 10. 本地开发与运行

```bash
# 后端（默认 dev 环境，读 application-dev.yml，MySQL + Redis 需本地启动）
mvn spring-boot:run

# 前端
cd frontend
npm install
npm run dev   # http://localhost:3003
```

- 数据库/Redis 需预先启动；Redis 配置见 `application-dev.yml`
- Swagger：`http://localhost:9090/swagger-ui.html`（分用户/管理/商家三组）

## 11. AI 辅助要点（补充信息）

以下为本会话探索发现的注意事项，供后续 AI 快速定位问题：

1. **仪表板已全量真实化**：商家 5 个接口、管理员 3 个接口均返回真实数据，前端对应图表已接入。新增图表时复用 `frontend/src/views/merchant/MerchantDashboard.vue` 的 echarts 模式（init → setOption → dispose + resize 监听）。
2. **字段命名不一致陷阱**：管理员 `/admin/dashboard` 用 `totalOrderCount`/`todayOrderCount`，商家 `/merchant/dashboard` 用 `productCount`/`pendingOrderCount`/`totalSales`/`todayOrderCount`。前端取数前先核对后端返回键名。
3. **销售额口径不同**：管理员 `getTotalSales`/`getTodaySales` 只统计 `PAID` 状态订单；商家 `calcTotalSales` 统计 `PAID + DELIVERED + COMPLETED`。若两个仪表板数据对不上，是口径差异而非 bug。
4. **日期统计**：`todayOrderCount` 依赖服务器日期（`DATE(create_time)=CURDATE()`），跨天后数值会变化，属正常行为。
5. **`order` 表名**是 MySQL 关键字，写 SQL 必须用反引号转义。
6. **`frontend/dist` 被 git 跟踪**：执行 `npm run build` 会改写 dist 下大量哈希文件，提交前注意还原或确认是否需要提交构建产物（本会话曾误改后 `git checkout -- frontend/dist` 还原）。
7. **`merchant_audit`、`discount`** 两张表无实体，改动时注意是否影响 ORM 扫描。
8. **单元测试目前只有** `FurnitureApplicationTests.java` 一个启动测试，`/git-save` 的 tester 阶段运行它。
9. **启动入口**：后端 `com.gjx.FurnitureApplication`；前端 `frontend/src/main.js`。
10. **登录接口**：`POST /api/auth/login` 请求体 `{username, password}`，返回 `{user, token}`；token 存 localStorage `furniture_token`。

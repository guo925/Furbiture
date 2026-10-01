# Furbiture 项目现状说明书（AI 交接文档）

> **本文件的用途**：任何 AI 会话（或新加入的人）在动这个项目之前，**先读本文件**。
> 它回答三个问题：**这是个什么项目**、**现在是什么状态**、**有哪些已知的坑不能踩**。
>
> - 最后核实日期：**2026-10-01**（本文件中的数字均为当日实测，非估算）
> - 配套文档：[`PROJECT.md`](../PROJECT.md)（技术细节）· [`docs/code-review-2026-10-01.md`](code-review-2026-10-01.md)（全量体检报告）
> - 数据库相关：[`database/README.md`](../database/README.md)（**改表结构前必读**）
> - 若本文件与代码不一致，**以代码为准**，并顺手修掉本文件。

---

## 一、这是什么项目

**Furbiture（橙家优选）** —— 家具电商系统，前后端分离，三角色（USER / MERCHANT / ADMIN）各自独立前端。**教学 / 学习性质**，但按企业级规范组织。

| 项 | 值 |
|---|---|
| 后端 | Spring Boot **3.3.4** / **Java 17** / MyBatis-Plus 3.5.5 / MySQL 8.0 / Redis / WebSocket |
| 前端 | Vue 3.4 + Vite 5 + Element Plus + Pinia + Vue Router 4 + Axios + ECharts 6 |
| 端口 | 后端 `9090` ｜ 前端开发 `3003` ｜ Vite 代理 `/api`、`/uploads` → 9090 |
| 数据库 | `furniture_db` @ `localhost:3306` ｜ Redis @ `localhost:6379` |
| 规模 | 后端 134 个源文件（另 8 个测试类）｜ 前端 `src/` 68 个文件（38 `.vue` + 27 `.js` + 3 样式）｜ `scripts/` 4 个检查脚本 |

> ⚠️ **Java 17 而非 21**。同一台机器上其他项目多为 21，切项目时务必看 `pom.xml` 的 `<java.version>`。

---

## 二、当前状态快照（2026-10-01 实测）

| 检查项 | 结果 | 命令 |
|---|---|---|
| 编译 | ✅ **BUILD SUCCESS**（135 个源文件，仅 2 条既有 `RedisConfig` 过期 API 警告） | `mvn -B -DskipTests clean compile` |
| 测试 | ✅ **70 通过 / 0 失败 / 0 跳过** | `mvn -B test` |
| 覆盖率 | ⚠️ 行 **11%** / 分支 **8%**（JaCoCo 已接入，**刻意未设阈值**——设了必红） | `mvn -B test`（自动产出报告） |
| 架构规则 | ✅ **ArchUnit 6 条规则全部启用**，0 违规 | `mvn -B test` |
| 前端 lint | ✅ 退出码 0，零错误零警告 | `cd frontend && npm run lint` |
| 前端构建 | ✅ 成功，**JS 合计 1,439 kB / gzip 488 kB**（改造前 2,376 kB / 791 kB） | `cd frontend && npm run build` |
| 产物无冗余组件 | ✅ 未混入未使用的 EP 组件 | `node scripts/check-bundle.mjs` |
| 图标 import 完整 | ✅ 模板用到的图标都有显式 import | `node scripts/check-icon-imports.mjs` |
| API 契约 | ✅ 0 错误 | `node scripts/check-api-contract.mjs` |
| 实体-表结构 | ✅ 一致（无漂移） | `node scripts/check-entity-schema.mjs` |
| CI | ✅ `.github/workflows/ci.yml`（backend: mvn verify；frontend: lint + build + 产物两项检查） | — |

### ⚠️ 提交门禁：已恢复但**尚未生成通过标记**

`.claude/` 曾被整体删除，已于本次恢复。因此：

- `.git/hooks/pre-commit` 的软链**不再断开**，提交时**会**执行质检门禁
- 但 `.claude/checks/` 下**没有通过标记** → **现在直接 `git commit` 会被拦下**

**处理方式**（三选一）：
1. 走 `/git-save`（会跑 tester + quality-engineer，通过后自动提交）
2. 临时 `git commit --no-verify` 跳过（**不推荐**，门禁会完全失效）
3. 若决定弃用这套工作流，删除 `.git/hooks/pre-commit` 软链与 `.claude/` 相应内容

> **门禁自身的 4 个已知漏洞**（若要继续用，建议修）：
> 1. 只对 `*.java` 生效 → 前端 / SQL / YAML 变更**完全跳过质检**
> 2. hook 的提示文案里**自己打印 `git commit --no-verify`**
> 3. 只检查本地，无 CI 兜底（现在 CI 已建，可作为第二道防线）
> 4. **指纹算法在「新增 Java 文件」时必然失配**（2026-10-01 实测踩到，见下）

#### 漏洞 4 详解：指纹算法与 `git add` 顺序冲突

指纹算法（`.claude/hooks/pre-commit`，与 `gitcommit-agent` 约定一致）是：

```bash
git diff HEAD -- '*.java' ':!*Test*.java' | sha256sum
```

`git diff HEAD` **不包含未跟踪文件**。于是：

| 阶段 | 新增的 Java 文件状态 | 指纹 |
|---|---|---|
| 质检时（尚未 `git add`） | 未跟踪 → **不进 diff** | `dae969ac…` ← 写进标记文件 |
| 提交时（已 `git add`） | 已入索引 → **进 diff** | `f108e635…` ← hook 算出，与标记不符 → **阻止提交** |

**后果：只要改动里新增了任何 Java 文件，这个门禁永远不可能通过。** 而那正是最需要质检的改动类型。
本次实测：29 个新增 Java 文件 → 指纹必然变化。注意此时「被质检的代码」与「被提交的代码」其实是同一份，
属**误报**。

**正确用法（不改 hook 也能过）**：**先 `git add -A`，再跑门禁，最后提交**——让质检与 hook 在同一状态下算指纹。

**根治方案**（任选其一）：
- 把算法改成包含未跟踪文件，如 `git add -A -N` 后再算，或改用
  `git diff HEAD -- '*.java' ':!*Test*.java'` + `git ls-files -o --exclude-standard -- '*.java'` 一并入哈希；
- 或让 hook 改用「暂存区」口径：`git diff --cached -- '*.java' ':!*Test*.java' | sha256sum`
  （与提交动作天然同状态，最简洁）。

> ⚠️ 改完务必做**负向对照**：改一行已暂存的 Java 源码后提交，门禁必须变红——否则等于把门禁关掉了。

---

## 三、快速上手

```bash
# 后端（需本地 MySQL + Redis 已启动）
mvn spring-boot:run            # → http://localhost:9090
# 前端
cd frontend && npm install && npm run dev   # → http://localhost:3003

# 改完必跑的四件套
mvn -B -DskipTests compile          # 编译
mvn -B test                         # 测试 + 架构规则 + 覆盖率
node scripts/check-api-contract.mjs # 前端 API 契约
node scripts/check-entity-schema.mjs # 实体 vs 表结构
cd frontend && npm run lint && npx vite build
```

**本地中间件无需单独启动** —— Docker 常驻，标准端口。容器名带 `seckill-` 前缀但**不是 seckill 专属**：

| 服务 | 端口 | 容器名 |
|---|---|---|
| MySQL 8.0 | 3306 | `seckill-mysql` |
| Redis 7.2 | 6379 | `seckill-redis` |

**测试账号**（密码均为 `123456`）：`admin`(ADMIN) · `user1`(USER) · `merchant1`(MERCHANT) · `merchant2`(MERCHANT)

> ⚠️ **不要硬编码商家 ID**。种子数据里 merchant1=5、merchant2=6，但你的开发库是手工拼的、可能是 3/4。
> 代码必须经 `product.merchant_id` 或当前登录用户推导。

---

## 四、目录地图（已与磁盘核对）

```
Furbiture/
├── .claude/                      # AI 工作流（agents / skills / hooks / commands）
├── .editorconfig                 # 仓库级缩进与换行约定（Java 4 空格 / 前端 2 空格）
├── docs/
│   ├── AI-CONTEXT.md             # ← 本文件
│   └── code-review-2026-10-01.md # 全量体检报告
├── database/                     # SQL 脚本（⚠️ 改表结构前必读 database/README.md）
│   ├── schema.sql                # 权威建表脚本 + 种子数据
│   ├── merchant_update.sql       # 商家字段与账号
│   ├── migration_v2 ~ v7.sql     # 幂等增量迁移
│   └── reset_data.sql            # ⚠️ 破坏性：清空并重灌
├── scripts/
│   ├── check-api-contract.mjs    # 前端 API 契约校验
│   └── check-entity-schema.mjs   # 实体 vs 表结构漂移校验
├── .github/workflows/ci.yml      # 最小 CI
├── src/main/java/com/gjx/
│   ├── annotation/  aspect/      # @OperationLog + AOP 切面
│   ├── common/                   # R<T>、ResultCode、BusinessException
│   ├── config/                   # Security/Redis/WebSocket/MyBatis/CORS/TraceIdFilter
│   ├── controller/{user,admin,merchant}/
│   ├── dto/{request,response}/
│   ├── entity/                   # 13 个实体
│   ├── enums/                    # OrderStatusEnum（含状态迁移规则）、UserRoleEnum
│   ├── exception/                # GlobalExceptionHandler
│   ├── mapper/                   # 13 个 Mapper（含自定义 SQL）
│   ├── security/                 # JwtTokenProvider / JwtAuthenticationFilter / SecurityConfig / LoginAttemptService
│   ├── service/ + service/Impl/
│   ├── task/                     # OrderTimeoutTask
│   └── util/                     # AuthenticationUtil、ValidationUtil
├── src/test/java/com/gjx/
│   ├── architecture/ArchitectureRulesTest.java   # ArchUnit 6 条架构规则
│   ├── enums/OrderStatusEnumTest.java            # 状态迁移规则（33 个用例）
│   └── security/PasswordSerializationTest.java   # 密码序列化防护（4 个用例）
└── frontend/src/
    ├── api/                      # request.js（axios + 401 闸门）+ modules/（11 个 API 模块）
    ├── stores/                   # user.js、cart.js
    ├── router/index.js           # 三级角色路由 + 守卫（fail-close）+ 404 catch-all
    ├── composables/              # useBreakpoint、useSearchHistory、useTheme、useConfirm、useDashboardData
    ├── constants/                # orderStatus.js、images.js
    ├── utils/                    # format.js
    ├── components/               # UserLayout、MerchantLayout、ProductCard、merchant/dashboard/*
    └── views/{user,admin,merchant}/
```

**❌ 以下路径在旧文档里被提到，但实际不存在**：
`components/common/`（DataTable、FormDialog）· `composables/usePagination|useForm|useLoading` · `views/admin/backup/` · `directives/`（`lazyLoad` 指令不存在）

---

## 五、当前阻断项

**无。** 上一次交接文档列出的三条阻断项已全部解决：

| 原阻断项 | 现状 |
|---|---|
| 提交门禁失效（软链断裂） | ✅ `.claude/` 已恢复；但需先跑一次 `/git-save` 生成通过标记，否则提交仍会被拦 |
| 一键部署链路断裂（3 个脚本报 1064） | ✅ `merchant_update.sql` / `reset_data.sql` 已修，**全新建库链路 7 个脚本实测无报错且幂等** |
| `deploy/` 目录被删 | ✅ 已恢复 |

---

## 六、必须知道的坑位

1. **`order` 是 MySQL 关键字** —— 写 SQL 必须反引号 `` `order` ``（实体已标 `` @TableName("`order`") ``）。

2. **商家归属只能经 `product.merchant_id` 判断** —— `order` 表虽有 `merchant_id` 列，但**未映射、无索引、代码不使用**。必须走 `order_item → product.merchant_id`。

3. **销售额口径不一致（不是 bug）** —— 管理端只统计 `PAID`；商家端统计 `PAID + DELIVERED + COMPLETED`。但**商家端的金额必须按 `order_item` 聚合**（`SUM(oi.price * oi.quantity)`），**不是**整单 `total_amount`（一张订单含多商家商品会重复计数）。这是最近才修对的，别改回去。

4. **`R.error(String)` 固定返回 code 500** —— 已知缺陷。客户端错误请显式传 `ResultCode.PARAM_ERROR` / `NOT_FOUND`。

5. **JWT 密钥必须 ≥ 32 字节** —— 否则应用**拒绝启动**（有意设计）。`application-dev.yml` 内置的开发密钥长 52 字节能通过校验——**生产若忘记设 `SPRING_PROFILES_ACTIVE=prod`，可用公开密钥自签 admin 令牌**。

6. **`frontend/dist` 已移出版本控制** —— 不要提交构建产物。

7. **MySQL 8.0 不支持 `ALTER TABLE ... ADD ... IF NOT EXISTS`** —— 那是 MariaDB 方言，会报 `ERROR 1064`。写迁移必须用「查 `information_schema` 判存在的存储过程」写法，详见 `database/README.md`。

8. **schema 有两个真相来源** —— 全新安装路径（`schema.sql` + compose 挂载的 4 个脚本）与存量升级路径（`migration_vN`）。**改表结构必须两边都改**，否则全新部署会和开发机不一样。

9. **实体与表结构漂移会让整个模块挂掉** —— MyBatis-Plus 把实体所有字段拼进 SQL，库里少一列就是 `Unknown column`。改完 schema 必跑 `node scripts/check-entity-schema.mjs`。历史上 `Category.icon/status` 就踩过这个坑。

10. **`logic-delete-field` 配了但实体没字段时静默失效** —— 现在 `User`/`Product`/`Category` 已加 `@TableLogic`，软删除真正生效。副作用：**被删用户名无法再注册**（唯一键仍占着）。

11. **通知域已接线，但新加通知点要注意事务顺序** —— `INotificationService.push()` 已接 6 处（支付/发货/确认收货/退款/超时取消/商家审核）。`type` 值一律走 `NotificationTypeEnum`，不要自己拼字符串（`notification.type` 是有索引的列）。WebSocket 推送注册为 **`afterCommit`**：事务回滚时通知既不入库也不推送。**新增调用点时若不在事务内，推送会立即发出**——这是有意设计，但要知道差别。

12. **`merchant_audit` / `discount` 两张表无对应实体** —— `merchant_audit` 在 `AdminAuditServiceImpl` 里用 `JdbcTemplate` 访问（合规，Service 层）。

13. **`deleted` 列在你的开发库上次是手工补的** —— 本次已通过迁移补齐。若你在别的机器上建库，记得跑完整迁移链。

---

## 七、红线（12 条）—— 其中多条已由机器强制

| # | 红线 | 强制方式 |
|---|---|---|
| 1 | 禁止在 Controller 里 try-catch 后仍返回 HTTP 200 | 人工审查 |
| 2 | **任何按 ID 操作资源的接口必须校验归属**（`userId`/`merchantId`） | skill `furbiture-authorization` + agent `security-auditor` |
| 3 | SQL 一律 `#{}`/`?` 占位符，禁止 `${}` 拼接 | 人工审查（当前 0 处） |
| 4 | 新接口默认需要认证，白名单需明确理由 | 人工审查 |
| 5 | 文件上传必须校验扩展名 + MIME 白名单 | 人工审查 |
| 6 | **事务边界在 Service 层**，多表写用 `@Transactional(rollbackFor = Exception.class)` | **ArchUnit 规则** |
| 7 | 入参 DTO / 出参 VO / Entity 只对应表 | 部分（ArchUnit 只保证 Entity 在 entity 包） |
| 8 | **新写的 `@RequestBody` 必须加 `@Valid`** | 人工审查（当前 31/31 全带） |
| 9 | 日志用 `@Slf4j`，禁止 `System.out.println` | **ArchUnit 规则** |
| 10 | 禁止把凭据写进日志 / `console.log` | 人工审查（前端 `no-console` 放行 error/warn） |
| 11 | 不要大范围重构，除非提前说明并获批准 | 人工审查 |
| 12 | 提交用 `/git-save`，禁止直接 `git commit` | pre-commit hook |

**架构规则（ArchUnit，全部启用，0 违规）**——见 `src/test/java/com/gjx/architecture/ArchitectureRulesTest.java`：
Controller 不得有 `@Transactional` ｜ Controller 不得依赖 `..mapper..`/`JdbcTemplate` ｜ 非 controller 类不得依赖 `..controller..` ｜ `..mapper..` 下必须是接口 ｜ `@TableName` 类必须在 `..entity..` ｜ 禁止 `System.out/err`、`printStackTrace`

**前端红线（ESLint，`plugin:vue/vue3-essential`）**：
`vue/no-v-html`（**error**，项目当前零 `v-html`，这是最重要的护城河）｜ `vue/no-mutating-props`（error）｜ `no-unused-vars`（error）｜ `no-console`（warn，放行 error/warn）

---

## 八、剩余已知问题

> 上一次体检的 60+ 项问题**绝大部分已修复**（见 §九）。下面是**仍未解决**的，按优先级：

### 🟠 应处理

| # | 问题 | 位置 |
|---|---|---|
| 1 | **后端从不返回 401** —— `SecurityConfig` 未配 `authenticationEntryPoint`，Spring Security 默认用 `Http403ForbiddenEntryPoint`，令牌过期/无效一律 403。后果：前端 `request.js` 里那套 **401 处理（并发去重闸门 + SPA 跳转 + redirect 参数 + 开放重定向防护）从未执行过**，是死代码——令牌过期时用户只会看到「请求失败 (403)」提示，**不会被引导重新登录**。属本项目最高发的「看起来实现了、实际没生效」类型 | `security/SecurityConfig.java`、`api/request.js` |
| 2 | 上一条**本次刻意未修**：补 `authenticationEntryPoint` 会把「未认证」的状态码从 403 改成 401，属**对外 API 契约变更**（会同时激活前端那套逻辑）。项目规则要求这类改动先获批，故只记录不动手 | 同上 |

> ✅ 原 4 条应处理项已全部处理完毕：订单列表一次性拉取（§九「批次 10」）、
> 商家仪表板假数据（§九「批次 11」）、开发库 `merchant_id` 为 NULL（§九「批次 12」）、
> 前端路由守卫（**已实测证实无安全影响**，见下）。

**关于「前端路由守卫可篡改」的实测结论**（此前只是断言，现已取证）：

**关于「前端路由守卫可篡改」的实测结论**（此前只是断言，现已取证）：

后端 `SecurityConfig` 把 `/api/admin/**` 收紧到 ADMIN、`/api/merchant/**` 收紧到 MERCHANT、
其余 `/api/**` 需登录；角色来自 **JWT 签名载荷**而非 localStorage。实测：

| 请求方 | `/api/admin/orders` | `/api/merchant/dashboard` | `/api/orders` |
|---|---|---|---|
| user1 的 token | **403** | **403** | 200 |
| merchant1 的 token | **403** | 200 | 200 |

即：前端把 `localStorage.furniture_user.role` 改成 `ADMIN` 只能让页面外壳渲染出来，
所有数据请求仍会被后端按 token 里的真实角色拒绝。**结论：不是缺陷，无需修改**，
但要知道 SPA 会短暂渲染出一个空壳页面。

### 🟡 技术债

| # | 问题 |
|---|---|
| 3 | **未引入 Flyway** —— 迁移靠手工执行 + 幂等脚本。引入需先给现有库 baseline、停用 docker-entrypoint 挂载、搬迁文件，属独立变更（详见本文末「已主动 defer 的项」） |
| 4 | `ProductServiceImpl.listProducts/searchProducts` 的 `LIKE '%kw%'` 三列 OR 是全表扫。**已实测决定不改**：换 ngram FULLTEXT 会让单字搜索失效（详见 §九「搜索为什么保留 LIKE」） |
| 5 | `OrderTimeoutTask` 用 Redis `SET NX` 做了多实例互斥（fail-open），但**未显式释放锁**，靠 TTL 过期 —— 单次扫描若超过 4 分钟会与下一轮重叠（幂等性由条件更新保证，不会写坏数据） |
| 6 | `OrderVO.statusText` / `OrderDetailVO.statusText` 由后端 `OrderStatusEnum.getDescription()` 生成，但**前端从未读取**（前端用自己的 `constants/orderStatus.js`）。且两者用词不同（后端「已付款」vs 前端「待发货」）——存在**两个真相来源**，谁将来渲染了它就会出现同一订单两个词。**本次未改**，见下方说明 |

### 🟢 已知但可接受

- Token 存 `localStorage`（XSS 可窃取）。**已评估并决定不改架构**：项目零 `v-html`/`innerHTML`/`eval`，注入面极小；改为 httpOnly Cookie 属架构级变更，风险高于收益。已通过 `vue/no-v-html`（error 级）+ ESLint 守住注入面。
- `esbuild.drop` 只 drop `debugger` 不 drop console —— 源码 `console.log` 已是 0 处，drop console 唯一会删的是 21 处**刻意保留的 `console.error/warn`**。
- Element Plus 仍是**全量 CSS**（360 kB / gzip 48 kB）。项目有 24 处 `import { ElMessage } from 'element-plus'` 显式导入 JS API，逐个改造成本高且容易漏；全量 CSS 换来"不可能出现无样式组件"的确定性。JS 侧已按需（见 §九）。

---

## 八·补、开发库的两处偏差（**不是代码缺陷，是数据状态**）

这两条是本轮验证时顺带发现的，**不要当成 bug 去改代码**：

### 1. `product.merchant_id` 大面积 NULL —— ✅ 已于 2026-10-01 回填

开发库里 10 个商品曾有 9 个 `merchant_id IS NULL`（只有手工添加的 `aa` 是 3）。
原因是 **`database/merchant_update.sql` 从未在这个库上执行过** —— 它是 compose 的 02 脚本，
只在**数据卷为空时**由 MySQL 容器自动跑；本机这个库是手工拼出来的。

**已处置**：执行了 `UPDATE product SET merchant_id = 3 WHERE merchant_id IS NULL;`（回填 9 行）。
回填前已备份到 `~/furniture_db-backup-2026-10-01.sql`。
**刻意没有整脚本执行**，也没插入那 5 个种子商品——它们的 `main_image` 指向 `/images/*.jpg`，
而后端只映射 `/uploads`，插进来只会多 5 张坏图（见下面第 2 条）。

现状：merchant1 有 10 个商品、merchant2 有 0 个（脚本原意是"全部归 merchant1"，属 crude 回填）。

> ⚠️ 回填用的是硬编码 `merchant_id = 3`。**你的开发库恰好 merchant1=3**（`schema.sql` 只种
> admin+user1，商家靠自增拿到 3/4），所以成立；换一台机器建库前先核对商家真实 id。

**原始修法**（保留备查，属数据操作，执行前请备份）：

```bash
# 方式 A：跑一遍回填（会把所有 NULL 商品归给 merchant1，crude 但就是脚本原意）
docker exec -i seckill-mysql mysql -uroot -p123456 furniture_db < database/merchant_update.sql

# 方式 B：直接重置为种子数据（破坏性，会清空订单/购物车）
docker exec -i seckill-mysql mysql -uroot -p123456 furniture_db < database/reset_data.sql
```

### 2. 商品主图指向不存在的路径

`product.main_image` 存的是 `/images/sofa2.jpg` 这类路径，但后端没有 `/images` 静态映射
（只有 `/uploads`），因此前端列表页商品图全部是坏图。属种子数据问题。

---

## 八·补二、`statusText` 为什么**没有**动

`OrderVO`/`OrderDetailVO` 的 `statusText` 前端零引用，是同一信息的第二份副本，
且与前端 `constants/orderStatus.js` 用词不一致（后端 1=「已付款」，前端 1=「待发货」）。
这是**有意义的分歧**，不是笔误：

- **后端枚举是领域语言**：`PAID` 这个状态就是"已付款"，Service 层的报错
  「只有已付款订单可以发货」用的也是它。
- **前端是展示语言**：`constants/orderStatus.js` 的注释写明了三端统一采用「动作视角」
  （1=「待发货」，回答"我还需要做什么"），这符合电商惯例。

本轮**修掉的是前端内部的自相矛盾**（同一用户在同一笔订单上，列表页显示「待收货」、
详情页显示「已发货」）——现在两端都走 `getOrderStatusMeta`，文案一致。

**没动后端**是因为：删字段是破坏性 API 变更，改枚举描述又会让
「只有已付款订单可以发货」这类领域报错变得拗口（「只有待发货订单可以发货」）。
留待需要时统一决策：要么后端删掉 `statusText`（推荐，YAGNI），
要么前端改为直接渲染它并删掉自己的映射表。


---

## 九、修复进度

- [x] **批次 0**：恢复 `.claude/` `deploy/` `docs/`
- [x] **批次 1**：零风险机械修复
- [x] **批次 2**：P0 业务正确性
- [x] **批次 3**：P1 正确性与性能
- [x] **批次 4**：验证基础设施
- [x] **批次 5**：P2 技术债
- [x] **批次 6**：P3 打磨 + 前端抽象与拆分
- [x] **批次 7**：`AuthenticationUtil` 去静态化 + 通知域接线
- [x] **批次 8**：搜索规范化 + 定时任务多实例互斥
- [x] **批次 9**：前端工具函数收敛 + 状态文案统一 + 包体瘦身
- [x] **批次 10**：用户端订单列表分页改造（状态计数 / 关键词 / 日期筛选全部下沉服务端）
- [x] **批次 11**：商家仪表板去假数据（待办清单 / 店铺健康全部由真实统计派生）
- [x] **批次 12**：死代码清理 + 鉴权链路取证 + 开发库数据回填

### 批次 12 的关键修复

| 原问题 | 状态 |
|---|---|
| `OrderVO` / `OrderDetailVO` 里 `statusText` 与前端 `constants/orderStatus.js` 是**同一信息的两个副本**且用词不同 | ✅ **整个类删除**——取证发现不是"字段零引用"，而是**两个类全仓库零引用**（详见下），两个真相来源从根上消失 |
| 畸形 / 过期 token 打 `GET /api/users/current` → **500** | ✅ 已修。根因：该端点**在放行清单里**，请求能到达 Controller，而 `AuthenticationUtil` 会**第二次解析**原始 token 且无任何保护。已按该类 javadoc 既有的承诺（"不在此处抛异常"）补上兜底 |
| 开发库 9 个商品 `merchant_id` 为 NULL → 商家端商品列表几乎为空 | ✅ 已回填给 merchant1（脚本原意）。**只回填，未插入那 5 个种子商品**——它们的 `main_image` 指向 `/images/*.jpg`，而后端只映射 `/uploads`，插进来只是多 5 张坏图 |
| 商家仪表板新增统计缺后端数据 | ✅ `lowStockCount` / `offShelfCount` 已由 `/merchant/dashboard` 提供 |

#### 取证纠正：`code-review-2026-10-01.md` 里的一处错误结论

该报告称「`OrderVO`、`OrderDetailVO`、`ProductVO`、`ProductDetailVO`、`CreateProductRequest`、
`ProductQueryRequest`、`ChangePasswordRequest`、`LoginRequest`、`LoginResponse`、`UserVO`
—— **10 个 DTO/VO 全部零引用**」。**逐个核对后，这个结论不成立**：

| 实际状态 | 类 |
|---|---|
| **真死**（0 引用） | `ProductVO`、`ProductDetailVO`、`LoginResponse`、`ProductQueryRequest`（**这 4 个仍未删**） |
| 在用 | `CartItemVO`(3)、`UserVO`(1)、`LoginRequest`(2)、`ChangePasswordRequest`(2)、`CreateProductRequest`(2) |

**教训**：体检报告里的"零引用"结论不能直接拿来做删除决策——本次差点删掉 5 个正在使用的类。
删除前必须自己跑一遍引用统计。

### 批次 11 的关键修复

| 原问题 | 状态 |
|---|---|
| 待办「库存巡检」的值是字符串 `'巡检'` —— **一个字塞进了本该是 24px 数字的位子** | ✅ 改为后端统计的 `lowStockCount`（在售商品中 `stock <= 10` 的数量） |
| 待办「资料维护」的值恒为 `'1项'` | ✅ 改为实时计算：手机号 / 邮箱缺失项数（取自 `userStore` 的 UserVO，不额外发请求） |
| 「商品发布」的值是 `'继续上新' / '去发布'`（提示语混在数字位） | ✅ 改为真实统计的 `offShelfCount`（已下架商品数） |
| 店铺健康「经营状态良好」**恒为这句**（40 分也说良好） | ✅ 由分数派生四档：优秀 / 良好 / 一般 / 待改善 |
| 健康列表两条**无条件绿勾**（"商品可售状态正常"、"店铺资料可维护"） | ✅ 改为健康分的**构成项**（履约率 / 商品供给 / 发货待办），警示点由真实数据决定 |
| 面板副标题「基础项越完整，买家信任越高」与公式 `履约率×0.7 + 商品供给×0.3` **对不上** | ✅ 副标题改为「按履约率与商品供给综合评估」。**健康分公式未动**（口径不变，分数与以前可比） |
| 快捷入口「运营设置」指向 `/merchant/profile` —— 既是「店铺资料」的重复目标，**标签还承诺了一个并不存在的设置页** | ✅ 移除；其余 5 项均指向真实存在的页面 |

> **顺带决策**：`shortcuts` 菜单本身 / 「平台提醒」文案 / `ShopOverviewCard` 的引导语是**静态导航与说明文，不是假数据**，保留。
> 原体检报告把 "shortcuts 整块" 一并归入假数据并不准确——菜单天然是静态的，只有那条指向不存在页面的入口才是缺陷。

### 批次 10 的关键修复

| 原问题 | 状态 |
|---|---|
| 用户端订单列表一次性拉 100 条，**订单超 100 笔时页签角标只统计最近 100 笔** | ✅ 改为服务端分页 + `el-pagination`；角标改由 `GET /api/orders/stats` 提供 |
| 关键词 / 日期筛选在客户端做，翻页后同一条件在不同页命中不同订单 | ✅ 条件下沉服务端；关键词命中「订单号 OR 商品名」（商品名走 `EXISTS` 子查询，不用 join、可走 `idx_order_item_order`） |
| 「退款/售后」页签覆盖 4+5 两种状态，服务端原先只支持单状态 | ✅ `status` 参数接受集合（`?status=4,5`） |
| 列表与角标各算各的，口径会漂移 | ✅ 抽出 `OrderServiceImpl.userOrderFilter(...)` 供两者共用；**端到端实测**「角标之和 == 列表 total」在 4 组筛选条件下均成立 |

**验证方式**：后端 4 组筛选条件走真实 HTTP 接口对照数据库核验；前端用浏览器实测页签切换、关键词搜索、翻页、重置、取消订单后刷新（角标与列表同步更新）。

### 批次 7–9 的关键修复

| 原问题 | 状态 |
|---|---|
| `AuthenticationUtil` 静态持有容器依赖（全场最后的字段注入残留） | ✅ 改为真正的 Spring Bean，12 文件 / 34 个调用点；静态残留 **0** |
| 通知域端到端不可用（`push()` 零调用者） | ✅ 接线 6 处 + `NotificationTypeEnum`；**真实 HTTP 端到端验证过**（支付→商家、发货→买家、归属隔离正确） |
| WebSocket 推送先于事务提交（回滚会留下"幽灵通知"） | ✅ 注册为 `afterCommit` 回调 |
| `searchProducts` 不规范化关键词、无缓存（与 `listProducts` 行为不一致） | ✅ 统一走 `normalizeKeyword` + `@Cacheable` |
| 订单超时任务多实例重复扫描 | ✅ Redis `SET NX` 互斥，**fail-open**（取不到锁必须放行）+ 5 个测试**变异验证过** |
| 前端 6 处 `money` / 6 处 `fallbackImage` / 1 处 `formatDate` 重复定义 | ✅ 全部收敛到 `utils/format.js`、`constants/images.js` |
| 同一订单在列表页显示「待收货」、详情页显示「已发货」 | ✅ 两端统一走 `getOrderStatusMeta`（**浏览器实测确认**） |
| 用户端 6 个 `<img>` 无 `loading="lazy"` | ✅ 已补 |
| `RegisterView` 手机号/邮箱仅 `required` | ✅ 对齐项目既有校验写法 |
| `main.js` 全量注册 293 个图标 | ✅ 移除（已验证模板图标全部有显式 import）→ **icons chunk 171 kB → 36 kB** |
| `import * as echarts` 全量引入 | ✅ 收敛到 `utils/echarts.js` 单一注册点 → **echarts chunk 1,125 kB → 544 kB** |
| `app.use(ElementPlus)` 全量注册 | ✅ 改按需（`unplugin-vue-components`） |
| **`manualChunks` 让 Element Plus 桶文件失去 tree-shaking** | ✅ 修复 → **产物 JS 2,376 kB → 1,439 kB（gzip 791 → 488 kB）** |

### 前端包体：实测数字与两个坑

```
                              改前        改后
echarts chunk              1,125 kB      544 kB
element-plus-icons chunk     171 kB       36 kB
element-plus chunk           781 kB     （并入自动分块）
────────────────────────────────────────────────
JS 合计                    2,376 kB    1,439 kB   −937 kB
JS gzip                      791 kB      488 kB   −303 kB
```

**坑 1：`manualChunks` 会让 Element Plus 的桶文件无法 tree-shake。**
`element-plus/es/index.mjs` 是 `export * from` 80+ 个组件的桶文件。
**只要 `manualChunks` 给它返回了 chunk 名，未使用的组件就会全量进入产物**
（实测多出 338 kB）。表现完全静默：源码干净、lint 通过、页面正常。
→ 修法是在 `manualChunks` 最前面提前 `return`（`vite.config.js` 有详细注释）。
→ 护栏：`scripts/check-bundle.mjs`（已接入 CI）。

**坑 2：全量注册图标 / 全量注册组件 各自挡住一半的 tree-shaking。**
两者都要去掉；去掉图标全局注册后，模板里漏 import 的图标**只会在运行时打一条
`Failed to resolve component` 警告**，构建不失败。
→ 护栏：`scripts/check-icon-imports.mjs`（已接入 CI）。

### 搜索为什么保留 `LIKE '%kw%'`

曾计划换 `FULLTEXT ... WITH PARSER ngram` 以走索引。**实测后放弃**（在临时库上做对照实验）：

| 查询 | 现在的 `LIKE '%kw%'` | ngram FULLTEXT |
|---|---|---|
| 两字词「沙发」/「真皮」 | ✅ | ✅ |
| 品牌「顾家」 | ✅ | ✅ |
| **单字「床」** | ✅ | **❌ 返回空** |

`ngram_token_size=2`，**单个汉字不成 token**。用一个真实的功能回退（搜单个汉字失效）
换取当前十位数量级数据上测不出的性能收益，不划算。
→ 数据量到万级、且业务能接受分词语义时再迁移，步骤见 `database/README.md`。

---

## 九·补、本轮新增的测试与护栏

**测试：44 → 49 → 61 → 64 → 70**（`mvn test` 全绿），新增的都做过**变异测试**（改坏必须变红）：

| 测试 | 钉住的属性 | 变异验证 |
|---|---|---|
| `OrderTimeoutTaskTest`（5 个） | Redis 故障时**必须放行**（fail-open），不能因缓存故障让超时任务停摆 | ✅ 把 `return true` 改成 `return false` → 测试变红；去掉锁判断 → 另一个测试变红 |
| `PasswordSerializationTest`（4 个） | 密码不随 JSON 序列化外泄 | ✅ 去掉 `@JsonProperty(WRITE_ONLY)` → 3 个变红 |
| `OrderStatusEnumTest`（33 个） | 10 种非法状态迁移被拒绝 | ✅ |
| `OrderServiceImplTest`（12 个） | 订单关键词**参数化不进 SQL 文本** / OR 块自带括号（防越权）/ 列表与计数条件逐字一致 / 分页上限钳制 / `COUNT` 映射为 `BigInteger` 也能取到值 | ✅ 去掉 OR 块外层 `and(...)` → 只有 `keywordOrBlockIsParenthesised` 变红（证明断言精确、未过度耦合）。含一条**负向对照**证明 `doesNotContain` 不是空断言 |
| `MerchantDashboardServiceImplTest`（3 个） | 低库存统计**含阈值边界**（`<=` 而非 `<`）且**只算在售商品** / 已下架用 `<>` 收敛 / 商品统计只查三次 | ✅ `.le` 改 `.lt` → 边界那条变红；删掉 `eq(status,1)` → 同一条变红 |
| `AuthenticationUtilTest`（6 个） | 畸形 / 过期 / 结构不合法的 token **一律按未认证返回 null，绝不抛异常**（放行接口靠这条约定不 500） | ✅ 去掉 try/catch → 3 条异常路径各变红，3 条正常路径不受影响 |
| `ArchitectureRulesTest`（6 条） | Controller 无事务 / 分层单向 / Mapper 是接口 … | ✅ |

> ⚠️ **写 MyBatis-Plus Wrapper 断言的两个坑**（两个测试类各踩到一个）：
>
> 1. **条件是延迟渲染的**——参数要到 `getSqlSegment()` 求值时才登记进 `paramNameValuePairs`。
>    先读会拿到**空 Map**：`containsValue` 真空失败，而 `doesNotContainValue` 会**真空通过**。
>    两个测试类里统一走 `boundValues(...)` 辅助方法（内部先取一次 SQL 片段）。
> 2. **`LambdaQueryWrapper` 求值 SQL 需要 TableInfo 缓存**，该缓存由 MyBatis 运行时在 Mapper
>    初始化时填充。不启动 Spring 上下文的纯 Mockito 测试里会抛
>    `can not find lambda cache for this entity`——需要手动
>    `TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), X.class)`。
>    （`OrderServiceImplTest` 用的是字符串列名的 `QueryWrapper`，不触发 lambda 解析，故无此问题。）
>    **不要为了测试方便把生产代码的 `LambdaQueryWrapper` 降级成字符串列名**——类型安全更值钱。

**新增护栏脚本**（均已接入 `.github/workflows/ci.yml`，且都用**负向对照**验证过"改坏会报错"）：

| 脚本 | 作用 |
|---|---|
| `scripts/check-bundle.mjs` | 断言产物中不含未使用的 EP 组件（防 `manualChunks` 坑 1 复发） |
| `scripts/check-icon-imports.mjs` | 断言模板用到的图标都有显式 import（防坑 2 复发） |


### 已完成的关键修复（避免重复修）

| 原问题 | 状态 |
|---|---|
| 重复下单无幂等（**资损**） | ✅ `createOrder` 用 DELETE 影响行数做原子认领，置于扣库存之前 |
| 评价可任意伪造刷分 | ✅ 5 步校验链 + `review` 唯一键 `uk_user_order_product` |
| 商家可改任意用户名（可锁死管理员） | ✅ 用户名事实上只读（值变了才拒绝，不误伤正常资料更新） |
| 管理端裸改订单状态 | ✅ 枚举白名单 + **状态迁移规则**（33 个测试用例守着）+ 期望前置状态进 WHERE |
| 通知已读越权（IDOR） | ✅ A 类写法（`userId` 进 WHERE + 检查影响行数） |
| 分类可被任意商家增删改 | ✅ 写接口收归管理员；商家端只读；补环检测 + 子孙 level 重算 |
| `@RequestBody` 漏 `@Valid`（30/33） | ✅ **31/31 全部带上** |
| `User.password` 无序列化保护 | ✅ `@JsonProperty(WRITE_ONLY)` + 4 个测试用例（**变异测试验证过**） |
| 逻辑删除配置空转 | ✅ 三个实体加 `@TableLogic`，软删除真正生效 |
| 金额统计用 `double` | ✅ 全链路 `BigDecimal` |
| 分页无上限（可 DoS） | ✅ `setMaxLimit(100)` |
| `DATE(create_time)=CURDATE()` 索引失效 | ✅ 改半开区间；补 `order.idx_pay_time` |
| 仪表板 4 处数据错误 | ✅ 销售额按 `order_item` 聚合 / 品类分布真填充 / 趋势与热销过滤状态 / 热销先过滤商家再 LIMIT |
| 商家端搜索只过滤当前页 | ✅ 条件下沉服务端 + 重置页码 |
| 前端调用不存在的 API 方法 ×2 | ✅ 已修 + `check-api-contract.mjs` 防复发 |
| 购物车 N+1（21 次请求） | ✅ 后端一次返回商品快照 `CartItemVO` → **1 次请求** |
| 6 处未捕获的 Promise 拒绝 | ✅ 统一到 `useConfirm` composable |
| 401 无并发去重 / 硬跳转 / 静默踢出 | ✅ 模块级闸门 + SPA 跳转 + redirect（含**开放重定向防护**） |
| 路由缺 404 / 守卫 fail-open | ✅ catch-all 路由 + fail-close |
| 门禁只查 `*.java` | ⚠️ **未修**（hook 仍在，但覆盖范围未扩展）；CI 已建可作第二道防线 |
| 全新建库链路断裂 | ✅ 实测 7 个脚本无报错且幂等 |
| 实体-表结构漂移 | ✅ `migration_v4` 补齐 + `check-entity-schema.mjs` 巡检 |
| 数据库缺 FK/CHECK/唯一键 | ✅ `migration_v7`（含**存量脏数据巡检**） |
| 冗余索引 | ✅ 清理 9 个（**带存在性前置条件**，避免删掉唯一可用的索引） |
| Controller 分层违规 | ✅ 30 → **0**；ArchUnit 规则已转正 |
| 字段注入 | ✅ 65 处 → **0 处**（最后的 `AuthenticationUtil` 已改为构造器注入的 Spring Bean） |
| 前端无静态检查 | ✅ ESLint + Prettier + `.editorconfig`（仓库级） |
| 无 CI / 无覆盖率统计 | ✅ `.github/workflows/ci.yml` + JaCoCo |
| `MerchantDashboard.vue` 865 行 | ✅ 拆成 11 个组件 + composable，主文件 **108 行** |

### 已主动 defer 的项（**需要时单独排期，别顺手做**）

**Flyway 迁移版本管理**。理由：
- 引入需先给现有库做 baseline（做错则应用起不来）
- 与 `docker-entrypoint` 初始化路径冲突，必须二选一或精心协调
- 需搬移/重命名全部 SQL，会让 `docker-compose.yml`、两份 README 失效
- 收益部分已被覆盖：现有迁移**全部幂等**、`check-entity-schema.mjs` 能检测漂移

**正确做法**：① 给现有库 baseline ② 停用 docker-entrypoint 的 SQL 挂载 ③ `mvn flyway:baseline` ④ 逐个迁文件并验证。**这是一次独立变更。**

---

## 十、可用的 AI 工具

### subagent（`.claude/agents/`）

| Agent | 用途 |
|---|---|
| `security-auditor` | 专查本项目三类高复发高危：**IDOR 归属校验**、**Controller 吞异常返回 200**、**校验注解空转** |
| `dead-feature-hunter` | 专查「代码看起来实现了、实际从未生效」——本项目最高发的缺陷类型 |
| `tester` | JUnit 5 + Mockito 测试生成与执行 |
| `quality-engineer` | 四维度质量审查（注释 25% / 安全 30% / 规范 25% / 架构 20%） |
| `gitcommit-agent` | 质检流水线编排（由 `/git-save` 调用） |

### skill（`.claude/skills/`）

| Skill | 用途 |
|---|---|
| `furbiture-authorization` | 归属校验的正确写法。**经基线测试验证有效**：无 skill 时会照抄 `NotificationController.markRead` 的越权写法，有 skill 时产出 A 类正确写法 |
| `unit-test`、`comments-check` | 测试生成 / 注释质量 |

> ⚠️ `.claude/agents/quality-engineer-references/security-checklist.md` 里的 IDOR 示例教的是
> **「先查后判断」**（B 类），在"被检查的条件会变"时有竞态。**以 skill `furbiture-authorization` 为准。**

### 脚本（`scripts/`）

| 脚本 | 用途 |
|---|---|
| `check-api-contract.mjs` | 校验前端 `xxxAPI.method(...)` 是否有对应定义。本项目已真实发生 3 次这类 bug（运行时抛 TypeError 且被 catch 静默吞掉）。顺带报告零调用的死方法 |
| `check-entity-schema.mjs` | 校验实体字段 vs 数据库表结构。**全新部署最容易踩的坑**：实体有字段但建表脚本没有 → 整个模块 `Unknown column` |
| `check-bundle.mjs` | 断言构建产物中不含未使用的 Element Plus 组件。防 `manualChunks` 坑（见 §九「坑 1」）复发——这个退化在源码和 lint 里完全看不见 |
| `check-icon-imports.mjs` | 断言模板里用到的 EP 图标都有显式 import。移除图标全局注册后，漏 import 只会在**运行时**打警告、构建不失败 |

### 命令（`.claude/commands/`）

`/git-save`（带门禁的提交）· `/restart`（重启前后端）· `/unit-test <类>` · `/comments-check [文件]`

---

*本文件由 2026-10-01 全量代码体检 + 分批修复生成。所有数字均为当日实测。修改项目结构后请同步更新本文件。*

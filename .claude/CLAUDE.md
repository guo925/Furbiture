# Furbiture AI 协作规范

> **动手前先读根目录 `PROJECT.md`**（技术栈、目录结构、15 张表、接口清单、注意事项）。
> 本文件只写两类内容：**① 本项目特有的事实**、**② 质量门禁要对照的规范清单**。
> 通用编码风格、安全清单、测试要求、Git 规范见 ECC rules 与全局 `~/.claude/CLAUDE.md`，此处不重复。

---

## 一、项目特有事实

| 项 | 值 |
|---|---|
| 后端 / 前端端口 | `9090` / `3003`（Vite 代理 `/api`、`/uploads` → 9090） |
| Java / Spring Boot | **17** / 3.3.4 ⚠️ 本机其他项目多为 21，切项目时留意 |
| 数据库 / 缓存 | `furniture_db` @ localhost:3306 · Redis @ localhost:6379 |
| 本地启动 | 后端 `mvn spring-boot:run`；前端 `cd frontend && npm run dev` |
| 一键起全栈 | `docker compose --env-file deploy/.env up -d --build` |
| 健康检查 | `GET /actuator/health` |

### 坑位清单（踩过的坑，别再踩）

1. **`order` 是 MySQL 关键字** —— 写 SQL 必须用反引号 `` `order` `` 转义（实体已标注 `` @TableName("`order`") ``）。
2. **测试账号** —— `admin` / `user1` / `merchant1`(id=3) / `merchant2`(id=4)，密码均为 `123456`。
3. **商家归属只能经 `product.merchant_id` 判断** —— `order` 表虽有 `merchant_id` 列，但**未映射、无索引、代码不使用**；判断"订单属于哪个商家"必须走 `order_item → product.merchant_id`。
4. **销售额口径不一致** —— 管理端只统计 `PAID`；商家端统计 `PAID + DELIVERED + COMPLETED`。两个仪表板对不上是**口径差异，不是 bug**。
5. **`frontend/dist` 已移出版本控制** —— 不要提交构建产物。
6. **JWT 密钥必须 ≥ 32 字节** —— 否则应用**拒绝启动**（这是有意设计，防止弱密钥上线）。
7. **`R.error(String)` 固定返回 code 500** —— 已知缺陷；客户端错误请显式传 `ResultCode.PARAM_ERROR` / `NOT_FOUND` 等语义码。

---

## 二、规范清单

> `quality-engineer` 的「维度三：代码规范」按本节逐条检查。

### 分层

- Controller 只负责请求接收与参数校验；Service 承载业务逻辑；Mapper 负责数据库访问。
- 入参用 DTO（`dto/request`），出参用 VO（`dto/response`），Entity 只对应数据表。
- **禁止在 Controller 写复杂业务逻辑。**

### 命名与结构

- 变量、方法名需有业务含义；**禁止魔法数字**（提取为具名常量）。
- 方法职责单一，禁止超长方法（> 50 行）；单个类建议不超过 300 行。

### 异常与返回

- 统一走 `GlobalExceptionHandler`；**禁止在 Controller 里 try-catch 后仍返回 HTTP 200**。
- 统一用 `R<T>` 返回，并按语义选择 `ResultCode`。

### 日志

- 使用 `@Slf4j`，**禁止 `System.out.println`**。
- 日志需含关键业务信息（订单号、用户 ID）。
- **禁止打印密码、Token、手机号等敏感数据。**

### 事务

- 涉及多表写操作必须加 `@Transactional(rollbackFor = Exception.class)`。
- 事务边界应位于 **Service 层**。

### 安全红线

- **任何按 ID 操作资源的接口必须校验归属**（`userId` / `merchantId`）—— 这是本项目最大的历史技术债，已修复的坑不要再踩回去。
- SQL 一律用 `#{}` 占位符，**禁止 `${}` 拼接**。
- 新增接口**默认需要认证**；只有确需公开的才加入 `SecurityConfig` 白名单。
- 文件上传必须校验**扩展名 + MIME** 白名单。

---

## 三、工作流

### 改代码前后必须交代

- **改前**：改哪些文件、为什么改、影响范围。
- **改后**：修改内容、解决的问题、可能产生的问题、下一步优化建议。

### 不要大范围重构

除非提前说明并获批准。分层下沉、模块搬迁这类结构性调整需单独提出方案。

### 提交

使用 `/git-save`，详见 [commands/git-save.md](commands/git-save.md)。
**禁止直接 `git commit`** —— 会被 [hooks/pre-commit](hooks/pre-commit) 拦截。

门禁通过条件（三者缺一不可）：

1. 单元测试全部通过
2. 质量审查：零严重 + 零高危 + 综合评分 ≥ 60
3. **变更指纹一致** —— 质检之后又改过代码，原结论即失效，须重新质检

---

## 四、可用工具

| 工具 | 用途 |
|---|---|
| `/restart` | 重启前后端服务 |
| `/git-save [--force]` | 带门禁的提交（--force 需二次确认） |
| `/unit-test <类>` | 生成并执行单元测试 |
| `/comments-check [文件]` | 注释质量检查 |
| `gitcommit-agent` | 质检流水线（通常由 `/git-save` 调用，不自提交） |
| `tester` | 单元测试专家 |
| `quality-engineer` | 四维度质量审查（注释 25% / 安全 30% / 规范 25% / 架构 20%） |

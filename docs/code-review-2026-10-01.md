# Furbiture 全量代码体检报告

> 审查日期：2026-10-01
> 审查范围：后端 115 个 Java 文件 / 7,334 行；前端 48 个文件 / 9,928 行；数据库 6 个 SQL；部署配置 —— 合计约 17,300 行，逐文件通读，非抽样。
> 审查方式：8 条互不重叠的链路并行深挖（安全链路 / 订单购物车 / 商品分类 / 用户地址基础设施 / 前端基础设施 / 前端用户端 / 前端管理商家端 / 数据库部署），关键结论由主审**二次交叉验证**。
> 验证手段：`mvn clean compile`、`mvn test`、直连 `furniture_db` 核对真实表结构、`rg` 定点复核。

---

## 〇、一句话结论

**这是一份"地基比外表好"的代码。**

最危险的地方（电商三大经典翻车点：超卖、注入、前端改价）**全部做对了**，而且注释质量罕见地高——注释解释了"为什么"而不只是"是什么"。历史越权技术债也确实修掉了，`order`/`cart`/`address`/`product`/`merchant` 全链路归属校验都在 SQL 条件里。

问题集中在四个方向：

1. **分层只做了壳，没接线**——DTO/VO 写了却零引用，Controller 直吐 Entity，30/33 个 `@RequestBody` 漏了 `@Valid`，校验注解全部空转；
2. **幂等与并发边界**——单请求内的原子性做得好，两次请求之间的竞态没管（重复下单、重复评价、重复收藏）；
3. **"看起来有、实际全废"的功能**——前端两处调用了不存在的方法、通知只读不写、逻辑删除配置空转、缓存 `allEntries` 自废；
4. **部署链路是断的**——一键启动命令的三个依赖项（`deploy/` 目录、两个 SQL 脚本）全部有问题，**实测报错**。

**门禁判定：BLOCK**（存在可造成资损与数据污染的缺陷）。

---

## 一、⛔ 开工前必须先处理：工作区文件被删 + 提交门禁失效

### 1.1 `.claude/`、`deploy/`、`docs/` 已从工作区消失

```
 D .claude/CLAUDE.md                        D .claude/hooks/pre-commit
 D .claude/agents/gitcommit-agent.md        D .claude/skills/comments-check/SKILL.md
 D .claude/agents/quality-engineer.md       D .claude/skills/comments-check/references/standards.md
 D .claude/agents/quality-engineer-references/security-checklist.md
 D .claude/agents/tester.md                 D .claude/skills/unit-test/SKILL.md
 D .claude/commands/git-save.md             D .claude/skills/unit-test/references/templates.md
 D .claude/commands/restart.md              D deploy/.env.example
                                            D docs/frontend-optimization.md
                                            D docs/plan-b-changelog.md
```

**连带后果（已发生，非假设）：**

| # | 后果 | 证据 |
|---|---|---|
| 1 | **提交门禁彻底失效** | `.git/hooks/pre-commit` 是指向 `../../.claude/hooks/pre-commit` 的软链，目标已删 → **断链**。Git 对断链 hook 静默跳过，`git commit` 现在畅通无阻 |
| 2 | **一键部署命令不存在** | `docker-compose.yml:4` 依赖 `--env-file deploy/.env`，而 `deploy/` 目录已不在 |
| 3 | **`deploy/.env` 永久丢失** | 该文件被 `.gitignore:39` 忽略，不在版本库中。若本地真建过（含数据库口令 + JWT 密钥），只能从 `.env.example` 重建 |

**恢复命令：**

```bash
git restore .claude deploy/ docs/
```

⚠️ 恢复后 pre-commit 钩子立刻生效。若 `.claude/checks/` 下无通过标记，**下一次提交会被直接拦下**——恢复后须先跑一次 `/git-save`。

### 1.2 门禁本身的设计漏洞（即便恢复也要修）

| 漏洞 | 位置 | 说明 |
|---|---|---|
| **只对 `*.java` 生效** | `.claude/hooks/pre-commit` 第 27-31 行、指纹算法 | 只改 Vue / SQL / YAML / Dockerfile 的提交**完全跳过质检**。而本项目前端 9,928 行 > 后端 7,334 行 |
| **hook 自己打印绕过命令** | 同上，`git commit --no-verify` 出现在提示文案里 | 等于把逃生舱口贴在门上 |
| **检查的是空集合** | `src/test/` 只有 1 个不断言任何东西的测试 | "单元测试全部通过"这个条件**永远为真** |

---

## 二、按优先级的问题清单

### 🔴 P0 — 会真的害到你（资损 / 安全事故 / 部署失败）

#### P0-1 重复下单无幂等保护 —— 可生成两笔订单（资损）

`OrderServiceImpl.java:51-54, 122-125`

```java
List<Cart> cartItems = cartMapper.selectBatchIds(cartItemIds);   // 无锁读
...
cartMapper.delete(new LambdaQueryWrapper<Cart>()                  // 返回值被丢弃
        .in(Cart::getId, cartItemIds)
        .eq(Cart::getUserId, userId));
```

**原理**：`selectBatchIds` 是快照读，不加行锁；后面的 `delete` **完全丢弃返回值**，不判定"我是否真的消费掉了这些购物车行"。典型的 check-then-act 竞态。

**时序**：请求 A、B 同时进入 → 都读到同一批 cart 行 → 各自锁商品（B 排在 A 后）→ A 提交（扣库存、建单、删 cart）→ B 继续，**再扣一遍库存、再建一笔订单** → B 的 delete 影响 0 行，但返回值被忽略，静默成功。

**修复**（利用 DELETE 行锁做排他认领）：

```java
int deleted = cartMapper.delete(new LambdaQueryWrapper<Cart>()
        .in(Cart::getId, cartItemIds)
        .eq(Cart::getUserId, userId));
if (deleted != cartItemIds.size()) {
    throw new BusinessException("购物车商品已被处理，请勿重复提交");
}
```

彻底方案：前端传 `idempotencyKey` + 落 `order_idempotency(key UNIQUE)` 表。

#### P0-2 评价可任意伪造、无限刷分

`ReviewServiceImpl.java:36` / `ReviewController.java:45`

只校验了评分区间 1-5，**完全没校验"是否真的买过"**：

```java
review.setProductId(productId);   // 直接落库
review.setOrderId(orderId);       // orderId 客户端可控，不校验归属
save(review);                     // 无唯一约束，可无限次插入
```

`migration_v3_planb.sql:19-32` 的 `review` 表只有普通索引，**无 `UNIQUE(user_id, order_id, product_id)`**。

**后果**：任何登录用户对任意商品 `POST /api/reviews` 刷 5 星，无限次。平均分被污染 → 影响商品排序与转化。

**修复**：校验订单属于当前用户 + 状态为 `COMPLETED` + 订单明细确实含该商品 + 防重复；DDL 补唯一键。

#### P0-3 商家可改任意用户名 → 可锁死管理员账号

`MerchantProfileController.java:53`

```java
if (infoData.containsKey("username")) user.setUsername(infoData.get("username"));
else if (infoData.containsKey("name")) user.setUsername(infoData.get("name"));
```

**原理**：`UserServiceImpl.findByUsername` 用 MyBatis-Plus `getOne(...)`，默认 `throwEx=true` —— 用户名重复即抛 `TooManyResultsException`。而登录、`AuthenticationUtil`、所有商家控制器的 `getMerchantId()` **都依赖 `findByUsername`**。

**攻击**：商家调 `PUT /api/merchant/info {"username":"admin"}` → 此后 `findByUsername("admin")` 命中 2 行 → 抛异常 → **管理员永久无法登录**（登录分支直接 500），商家仪表板/订单接口一并崩溃。成本极低的定向账号锁定。

**修复**：资料接口禁止改用户名；`user.username` 补唯一索引作为最后防线。

> 当前线上库 4 个账号用户名正常，说明未被利用过。

#### P0-4 一键部署链路断裂（已实测报错）

| # | 位置 | 问题 | 实测 |
|---|---|---|---|
| a | `database/merchant_update.sql:7` | `ADD COLUMN IF NOT EXISTS` —— **MariaDB 方言，MySQL 8.0 不支持** | `ERROR 1064` |
| b | `database/merchant_update.sql:13` | `CREATE INDEX IF NOT EXISTS` —— 同上 | `ERROR 1064` |
| c | `database/reset_data.sql:5-6` | `CREATE DATABASE IF NOT EXISTS furniture_db` **缺分号**，直接接 `USE furniture_db;` | `ERROR 1064` |
| d | `deploy/` 目录 | 已删除（见 §1.1） | 命令第一步失败 |

**连锁反应**：`merchant_update.sql` 作为 docker-entrypoint 的 `02-` 脚本执行失败 → `product.merchant_id` 不存在 → `migration_v2.sql:49-51` 对该列建索引报 `1054`（其 `CONTINUE HANDLER` 只捕获 1061，接不住）→ **迁移链断裂，全新部署的数据库残缺**。

**讽刺的是** `migration_v2.sql:86` 的注释自己写着「MySQL 不支持 ADD COLUMN IF NOT EXISTS」——同一项目里两套写法自相矛盾。

**修复**：改用 `migration_v2.sql` 同款"查 `information_schema` 再 ALTER"的存储过程写法；`reset_data.sql` 补分号。

#### P0-5 管理员"更新订单状态"接口裸改 status —— 状态机被捅穿

`AdminOrderController.java:71-77`

```java
order.setStatus(statusRequest.getStatus());   // 想要几号就给几号
orderService.updateById(order);
```

其余所有流转（`mockPay`/`cancelOrder`/`deliverOrder`/`confirmReceive`/`requestRefund`）都用 `eq(Order::getStatus, 期望前置状态)` 做条件更新，服务层状态机严密；**唯独这个后门**绕开全部前置校验。

**后果**：可把 `CANCELLED(4)` 改成 `PAID(1)`、`PENDING_PAYMENT(0)` 改成 `COMPLETED(3)`，甚至写入 `999` 污染数据；破坏对账（销售额按 `status=PAID` 统计）。

**修复**：删除该接口，或改为语义化动作 + `OrderStatusEnum.fromCode()` 白名单校验。

#### P0-6 通知"标记已读"越权（IDOR）

`NotificationController.java:39` / `NotificationServiceImpl.java:53`

```java
@PutMapping("/{id}/read")
public R<?> markRead(@PathVariable Long id) {   // 没有 HttpServletRequest，没有当前用户
    notificationService.markAsRead(id);
```

Service 里只按主键查，**无 `user_id` 条件**。同文件的 `markAllAsRead`/`countUnread` 都正确按 `userId` 过滤，唯独此处漏了。遍历 id 可把全站用户通知标记已读。

---

### 🟠 P1 — 本迭代应修（数据正确性 / 性能 / 安全加固）

#### 分层"只做了壳"：校验注解全部空转 ★ 投入产出比最高

**全项目 33 个 `@RequestBody`，只有 3 个加了 `@Valid`**（都在登录/注册）。

`C-1`：`UpdateUserRequest` 上写了 `@Email`、`@Pattern(regexp="^1[3-9]\\d{9}$")`，因为没有 `@Valid`，用户可以提交 `email="not-an-email"` 并成功入库。**校验注解成了"文档性谎言"，比不写更危险**。

**批量赋值（Mass Assignment）**：`AdminProductController.java:63` 直接 `@RequestBody Product product` → 客户端可塞 `id` / `sales` / `merchantId` / `createTime`。

**同时是死代码重灾区**（写了却零引用）：
`OrderVO`、`OrderDetailVO`、`ProductVO`、`ProductDetailVO`、`CreateProductRequest`、`ProductQueryRequest`、`ChangePasswordRequest`、`LoginRequest`、`LoginResponse`、`UserVO` —— **10 个 DTO/VO 全部零引用**。

**修复优先级最高的一件**：给全部 `@RequestBody` 补 `@Valid`，并把 Entity 入参换成现成 DTO。

#### `User.password` 无 `@JsonIgnore` —— 靠"手动 setPassword(null)"兜底

`entity/User.java:11-28`，`grep JsonIgnore` → 零命中。

`User` 同时用作 ORM 实体、`@RequestBody` 入参、`R<User>` 出参。目前靠 **5 处手工 `user.setPassword(null)`** 兜底（`UserController:42`、`AdminUserController:39/50`、`AuthController:100`）。任何人新增一个返回 User 的接口、或漏写一行，BCrypt 哈希立即外泄。

**修复**：`@JsonProperty(access = Access.WRITE_ONLY)` + 出口统一走 VO。

#### 逻辑删除配置与实体脱节 —— `deleted` 列形同虚设

`application.yml:46-49` 配了 `logic-delete-field: deleted`，`migration_v2.sql:16-24` 给 `user/product/category` 加了该列，**但 13 个实体没有任何一个声明该字段**。

MyBatis-Plus 的逻辑删除只对"实体里存在同名字段"的类生效 → **静默失效**，`removeById` 全是物理删除。

**后果**：`AdminUserController.delete` 是真删除 → 删掉有订单的用户后，`order.user_id` 变悬空，历史订单失去用户信息且不可恢复。且配置的存在会让后来者误以为有软删除。

#### 金额统计用 `double` —— 货币精度错误

`OrderServiceImpl.java:372-397`，接口 `IOrderService.java:110,122,129`

```java
BigDecimal sales = baseMapper.selectTotalSalesByDateAndStatus(...);
return sales != null ? sales.doubleValue() : 0;   // BigDecimal → double，精度丢失
```

Mapper 返回 `BigDecimal`，这里又转回 `double`，等于白算。仪表板销售额出现分位误差，日/周/月对账对不上。

#### 无界查询：分页形同虚设 + N+1

| 位置 | 问题 |
|---|---|
| `MyBatisPlusConfig.java:22` | `PaginationInnerInterceptor` **未设 `maxLimit`** → `size=100000` 可把整表搬进内存（低成本 DoS）。`AdminAuditController:31` 有 `MAX_PAGE_SIZE=100` 兜底，说明作者知道这个风险，只是没推广 |
| `OrderServiceImpl.java:322` | 用户订单列表**无分页**，返回全部订单 |
| `OrderController.java:121-136` | 循环内逐单查明细 → **N+1**（100 单 = 101 条 SQL） |
| `MerchantOrderController.java:59-76` | 分页只加在最后一步，前面三步全量拉取 + **巨型 `IN` 列表**（商家 500 商品时 orderIds 可上万） |
| `FavoriteServiceImpl.java:61-68` | 循环单查商品 → N+1 |
| `cart.js:35-53` | 前端购物车对每项再发一次 `getDetail` → **N+1**（20 件商品 = 21 个请求） |
| `UsersView.vue` / `OrdersView.vue` | 前端一次性拉全量 |

#### 搜索 `LIKE '%关键字%'` → 索引失效

`ProductServiceImpl.java:90-93, 122-125`：三列 `OR like`（`name`/`brand`/`description`），前缀通配让 B+Tree 索引直接失效，`OR` 三列更是全表扫描。`/api/products/suggestions` 每敲一个键触发一次三列全表扫描，可被用来放大 DB 压力。

**修复**：短期改 `likeRight`（前缀匹配，可走索引）或建 `FULLTEXT`；中期上 ES。无论如何给 `suggestions` 加防抖 + 最小关键词长度。

#### `DATE(create_time) = CURDATE()` → 索引失效

`OrderMapper.java:40` 与 `MerchantDashboardController.java:172`。

对列施加函数，`idx_order_create_time` 失效。另外 `MerchantDashboardController:172` 用 `.apply("DATE(create_time) = CURDATE()")` 传原始 SQL 字符串——此处是常量暂无注入，但属危险习惯。

且 `order.pay_time` **根本没有索引**，销售统计同样全表扫。

**修复**：

```sql
-- 反例
WHERE DATE(create_time) = CURDATE()
-- 正例（可用索引，且不受跨天边界影响）
WHERE create_time >= CURDATE() AND create_time < CURDATE() + INTERVAL 1 DAY
```

#### 仪表板数据正确性问题（4 处）

| # | 位置 | 问题 |
|---|---|---|
| a | `MerchantDashboardController.java:159` | `calcTotalSales` 用 **`order.totalAmount` 整单金额**求和 → 一张订单含多商家商品时，**每个商家都算全额**，各商家求和 ≠ 平台真实 GMV。注释自己写着"近似代替" |
| b | `MerchantDashboardController.java:91-94` | 品类分布恒为"未分类" —— `Product.categoryName` 是 `@TableField(exist=false)` 内存字段，只有 `fillCategoryNames()` 才填充，这里走的是裸 `list()` → **图表 100% 显示"未分类"** |
| c | `MerchantDashboardController.java:55-85, 130-145` | 营收趋势/热销**未过滤订单状态** → 已取消、已退款订单仍计入营收和热销榜 |
| d | `MerchantDashboardController.java:140-144` | 商家热销榜**先取全平台 Top10 再过滤本商家** → 中小商家永远看不到自己的热销榜 |

#### 商家端搜索/筛选是假的（前端）

`MerchantProducts.vue:32,157-185` 与 `MerchantOrders.vue:19,110-134`

`loadProducts` / `loadOrders` **只发 `page`/`size`**，`keyword` 与 `statusFilter` 从不进请求；筛选在**客户端**对"服务端当前页 10 条"过滤。

**后果**：搜"床"只在当前页找 → 结果缺失、误报"暂无商品"；切"仓库中"标签若目标在第 2 页则显示空；`total` 是未过滤总数，分页数字与实际列表不符。

> 后端**已经支持** `keyword` / `status` 参数（`MerchantProductController.java:42-43`），纯前端没传。

#### 前端调用了不存在的方法（2 处，功能整块失效）

| 位置 | 调用 | 实际 |
|---|---|---|
| `ProductsView.vue:146` | `productAPI.getCategories()` | `productAPI` 只有 `getList`/`getDetail`/`search`。应属 `categoryAPI.getList()` → 分类筛选只剩「全部」 |
| `ProfileView.vue:168` | `orderAPI.getOrderStats()` | `orderAPI` 只有 `create`/`pay`/`getList`/`getDetail`/`cancel`/`confirmReceipt` → 个人中心四个订单数字**恒为 0** |

两处都被 `catch` 静默吞掉，所以"看起来在、实际全废"。

#### 通知域端到端不可用

`NotificationServiceImpl.java:23` 定义了 `push()`，**全仓库零调用者**。

下单、支付、发货、审核等事件**都没有发通知** → `/api/notifications` 永远返回空、未读数永远 0。这是个"只有读、没有写"的空壳，评审时容易被当成"已实现"。

#### 分类管理越权 + 循环引用

`MerchantCategoryController.java:58, 76, 96`

`category` 表**没有 `merchant_id`，是全局共享数据**，而 `/api/merchant/categories` 只要求 `MERCHANT` 权限 → **任意商家可增删改平台所有分类**，改一次还会清空分类树缓存污染前台。

且 `updateCategory` 允许把 `parentId` 设成**自己或自己的子孙** → 形成环 → `getCategoryTree()` 只从根遍历 → **环上的分类从分类树里静默消失**（不是栈溢出，是"整棵子树人间蒸发"）。

另外 `CategoryServiceImpl.java:37-43` 对 null `parentId` 会 NPE（`Collectors.groupingBy` 的分类函数返回 null 即抛）；而 `AdminCategoryController:97` 创建分类时不设 `parentId` → DDL 允许 NULL → **一次写坏、全站分类树 500**。

#### 默认地址互斥缺数据库约束

`AddressServiceImpl.java:41-61` 是"先全清 0，再置 1"两次独立 UPDATE，`address` 表只有 `idx_user_id`，**没有"每用户至多一条默认"的约束**。

且 `AddressController.add:62` 先把新地址以 `isDefault=1` 直接 `save` 进库，绕开了 `setDefault` 的串行点 → 并发下出现**两条默认地址且永久留存**。

#### 其他 P1

| 问题 | 位置 |
|---|---|
| 商家改商品在 `merchantId` 为 null 时 NPE（管理员创建的商品不设 merchantId → 落 NULL → 商家一碰就 500，且成为"孤儿数据"） | `MerchantProductController.java:77, 93` |
| `@CacheEvict(allEntries=true)` 在事务提交前触发 → 短暂脏缓存窗口（TTL 5 分钟）；且每次库存变更清空**所有**列表缓存 → 缓存近乎无效 + 雪崩窗口 | `ProductServiceImpl.java:27-30, 79` |
| 缓存 key 含任意用户关键词、无限长 → 缓存污染型 DoS | `ProductServiceImpl.java:79` |
| `product.sales` 取消/退款时只回补库存、**不回滚销量** → 销量永久虚高 | `ProductMapper.java:14-19` |
| WebSocket `setAllowedOriginPatterns("*")` + **无 STOMP 鉴权**（当前不可利用：前端无 STOMP 代码，`push` 零调用，属未接线死代码） | `WebSocketConfig.java:28-33` |
| dev 默认 JWT 密钥硬编码在仓库（52 字节，能通过 ≥32 校验）+ 默认 profile 是 `dev` → 若生产忘记设 `SPRING_PROFILES_ACTIVE`，可用公开密钥自签 `admin` 令牌 | `application-dev.yml:33`、`application.yml:10` |
| 登录限流只按用户名计 → 可定向锁定任意账号（含 admin），无需任何凭据 | `LoginAttemptService.java:54-89` |
| 注册接口透传"用户名已存在" → 枚举通道由登录堵住、从注册重新打开 | `AuthController.java:145-147` |
| `TraceIdFilter` 原样接受客户端 `X-Trace-Id` → 日志伪造（可注入换行） | `TraceIdFilter.java:41-47` |
| `R.error(String)` 固定返回 code 500 → "订单不存在"被当成服务器错误 | `R.java:60-65` + 多处调用 |
| `CartController.java:64-75, 87-98` try-catch 后仍返回 HTTP 200，异常被吞且**零日志** → 违反项目红线，且线上无法定位 | 同左 |
| 全库 **0 个外键约束** | 所有 SQL 脚本 |
| `review` 无唯一约束、`category.name` 非唯一、`user.phone/email` 非唯一 | DDL |
| 无 CHECK 约束：价格/库存/数量/评分均允许负数或越界 | DDL |
| 无 CI（`.github/workflows` 不存在）；`Dockerfile:15` 又是 `mvn -DskipTests` → **测试从不在发布路径上** | 同左 |
| nginx 缺安全响应头（`X-Frame-Options`/`CSP`/`nosniff`/`HSTS`）、无 `gzip`、`index.html` 无 `no-cache` | `frontend/nginx/default.conf` |

---

### 🟡 P2 — 技术债（不紧急，但会持续放血）

- **63 处字段注入**（`@Autowired` 私有字段）→ 直接导致这个项目**几乎无法写纯单元测试**（与"测试覆盖为 0"互为因果）
- **`reset_data.sql` 是"无护栏的破坏性脚本"**：只 `TRUNCATE` 了 9 张旧表，**未清理** `favorite`/`review`/`notification`/`operation_log`/`discount`/`merchant_audit`；`TRUNCATE` 重置自增 ID 后，残留旧数据会指向错误的外键（旧 user 2 的收藏挂到新 user 2 头上）。且只插 `admin/user1/...`，**不插商家账号**，重置后商家登录不了
- **迁移无版本管理**：未用 Flyway/Liquibase（同工作区的 GPM 用了），无版本表、无 checksum、无回滚脚本；`migration_v2.sql` 里 `UPDATE product SET merchant_id=3` 这类数据迁移**不幂等**
- **`schema.sql` 与真实库不同源**：真实库 `category` 表有 `icon`/`status` 两列，但**没有任何脚本添加它们** → 照 `schema.sql` 从零建库会缺列（⚠️ 见 §四 误报修正）
- **"分页做了个寂寞"** 之外还有 `AdminAuditController` 直接注入 `JdbcTemplate`/`Mapper` 在 Controller 里写 SQL，跳过 Service 层
- **`@Transactional` 开在 Controller**：`AddressController.add/update`、`MerchantProductController.batchUpdateStatus`
- **`UserServiceImpl.adminListUsers:19-28` 的 OR 未加括号**：当前只有这一个条件所以正确，但将来叠加 `.eq(status, x)` 会变成 `status=x AND username LIKE ? OR phone LIKE ?` —— **OR 优先级低于 AND，过滤条件被绕过**
- **`merchantId` 判断用 `existing.getMerchantId().equals(...)`** → null 时 NPE，应用 `Objects.equals`
- **Redis 连接池配置静默失效**：`application-dev.yml:24-29` 配了 `lettuce.pool.*`，但 `pom.xml` **没有 `commons-pool2`** → 实际退化为单条共享连接
- **`order` 表无 `update_time`** → 状态流转无法从一行看出"最后一次变更"
- **`review.images VARCHAR(1000)` 存 JSON 数组** → 应使用原生 `JSON` 类型
- **布尔/状态字段类型漂移**：`is_default INT`、`selected INT`、`is_read TINYINT`、`status INT` vs `TINYINT`
- **字符集只写 `CHARSET` 未写 `COLLATE`**：docker 用 `utf8mb4_unicode_ci`，本地 MySQL 8 默认 `utf8mb4_0900_ai_ci` → 两套库排序规则不同，跨库比对 VARCHAR 可能触发 "Illegal mix of collations"
- **compose 无资源上限与日志轮转**；健康检查把口令写在命令行（出现在 `docker inspect`）；应用以 `root` 连库
- **`org` 冗余索引一批**（可安全删除）：`cart.idx_user_id`、`favorite.idx_user_id`、`order.idx_user_id`、`order.idx_order_no`、`product.idx_category_id`、`discount.idx_code`，以及 `order_item` 上 base 与 v2 重复的两对
- **`AdminAuditController.reject`** 无 `@Transactional`、不校验存在性、不校验状态机 → 可对已通过的申请再次 reject
- **`MerchantProfile.vue` 把主按钮改成绿色 `#67c23a`**，而商家端品牌色是橙 `#ff7a1a` —— 三端统一品牌色的工作被这一处破坏
- **超大组件**：`MerchantDashboard.vue` 865 行、`UserLayout.vue` 499 行、`OrdersView.vue` 487 行、`AddressView.vue` 448 行（完整拆分方案见附录 A）

---

### 🟢 P3 — 打磨（有更好，没有也能活）

<details>
<summary>展开：约 20 条</summary>

- 生产残留 `console.log`：`stores/user.js:33-34,49` **把登录响应的 token 完整打印到控制台**；`stores/cart.js` **18 处**；`DashboardView.vue:122,133` 等多处 `console.error`
- 前端 401 处理：无并发去重、`window.location.href='/login'` 硬跳转（绕过 router、丢回跳地址）、绕过 `userStore.logout()`、不弹提示
- 路由**缺 404 catch-all** → 未知 URL 纯白屏
- 路由守卫 `catch { next() }` 是 **fail-open** —— 出错时放行所有受保护路由，应改为 fail-close
- `useTheme` **状态分裂**：`currentTheme` 是模块级、`isDark` 是实例级 → A 组件切主题时 B 组件图标不更新（当前三布局互斥挂载，症状被掩盖，属**潜在缺陷**）；且 400ms 定时器从不清理
- 构建全量引入：Element Plus 全量 + **约 290 个图标全量注册** + `import * as echarts`；`vite.config.js` 无 `build` 段、无分包、无 gzip；`main.js` 先 `use(router)` 后 `use(store)`（依赖时序巧合）
- 前端**无任何静态检查**：无 ESLint / Prettier / tsconfig / `.editorconfig`
- 6 处 `ElMessageBox.confirm` 未捕获 reject → 用户点"取消"就抛 `Uncaught (in promise)`
- `MerchantProducts.vue:204-215` 保存商品**无 try/catch、无提交锁** → 双击可创建两条商品
- `MerchantCategories.vue:82-90,263` `parentId` 类型不一致（string `"0"` vs number id）→ 编辑时父分类下拉**回显空白**
- 12 个 `<img>` 只有 1 个加了 `loading="lazy"`；普遍缺 `width`/`height` → 布局抖动
- `index.html` 引用 `/vite.svg` 但 **`public/` 目录不存在** → 必然 404
- `favoriteAPI` 定义完整却**零调用**（`FavoritesView.vue:66,88` 绕过它直连 `request`）；`.env` 里 4 个 `VITE_*` 变量**零引用**
- `useTheme.js:1` 导入了未使用的 `watch`；`AdminCategoryController.java:8` 未使用的 import；`WebMvcConfig.java:3` 未使用 `import jakarta.annotation.PostConstruct`
- `User.java:46` 注释「角色：USER, ADMIN」**漏了 MERCHANT**
- `AddressView.vue:284` `.replace(/市$/, '市')` 是无副作用的空操作
- 无请求取消/竞态处理（搜索联想三次输入乱序返回会覆盖）
- 文件上传沿用全局 15s 超时 → 弱网传 2-5MB 图片极易失败，且错误提示是"网络连接异常"（误导）
- 无全局错误边界（`app.config.errorHandler`）
- `.gitattributes` 仅 2 条，缺 `* text=auto`
- `scripts/` 目录为空
- `docker-compose` / `Dockerfile` 镜像 tag 未固定 digest

</details>

---

## 三、✅ 做得好的地方（改动时别改坏）

这部分不是客套，是**明确列出哪些代码已经是对的**，避免后续重构误伤：

### 安全（核心红线守住了）

- **SQL 注入：全仓库零命中**。所有 `@Select`/`@Update` 与 `JdbcTemplate` 一律 `#{}`/`?` 占位符，`grep '\${'` 零命中
- **`order` 关键字转义正确**：`Order.java:12` `` @TableName("`order`") ``，SQL 全部带反引号
- **提权防护到位**：`UserController.updateUser:64-73` 逐字段白名单赋值（`role`/`password` 无法通过请求体篡改）；`AuthController.register:151-156` 硬编码 `role=USER`；`AdminAuthController:92` 校验 `role==ADMIN`
- **认证基础设施扎实**：BCrypt 统一失败文案防用户名枚举、`dummyPasswordHash` 消除时序侧信道、JWT 密钥 <32 字节**拒绝启动**、`jjwt` 只走 `parseSignedClaims`（无 `alg=none` 绕过）
- **文件上传**：扩展名白名单 + UUID 重命名（不可预测不可覆盖）+ `normalize` + `startsWith` 阻断路径穿越
- **无 `v-html` / `innerHTML` / `eval`** —— 全项目零命中，这是当前最重要的护城河
- **Redis 反序列化用白名单** `PolymorphicTypeValidator` 替代危险的 `LaissezFaireSubTypeValidator`
- **`TraceIdFilter` 的 MDC 在 `finally` 中清理**（避免 Tomcat 线程复用导致日志串号）—— 很多新手会忘
- **越权历史债确实修掉了**：订单/购物车/地址/收藏全部按 `userId` 落 SQL 条件；商家商品批量操作用"数量比对+整体拒绝"防越权

### 业务正确性

- **扣库存这条最危险的路走对了**：`selectForUpdate` 悲观锁 + **按 productId 排序防死锁** + `stock >= quantity` 条件更新兜底
- **金额完全由后端用 `BigDecimal` 按加锁后读到的 `product.price` 计算，不信任前端** —— 避免了前端改价
- **订单状态流转用条件更新**（`eq(Order::getStatus, 期望前置状态)`）实现乐观并发控制，`mockPay`/`cancelOrder`/`deliverOrder` 都不会重复执行

### 工程质量

- **注释质量罕见地高**：准确、解释"为什么"、不腐烂。例如 `OrderServiceImpl:76-77`「负数量会让 `stock >= quantity` 恒真，扣减 SQL 变成反向加库存」—— 这是真正理解代码才会写的注释
- **`.gitignore` 写得比多数企业项目用心**，每条规则都写了为什么（如解释 `/dist/` 锚根导致匹配不到前端产物）
- **`fillCategoryNames`**（`ProductServiceImpl:165-191`）用"收集 id → 一次批量查 → 映射"正确避免 N+1
- **`useBreakpoint`** 用 `matchMedia` 而非 `resize` 监听，并在 `onUnmounted` 正确摘除
- **`design-tokens.css` → `element-theme.css` 令牌体系完整**，EP 变量真正接了 `--color-*` 令牌
- **后端 Dockerfile**：多阶段 + 非 root `USER app` + JRE 基础镜像 + HEALTHCHECK
- **compose**：mysql/redis/backend **未对外发布端口**（仅 frontend 发布 8080）；依赖用 `condition: service_healthy` 串起启动顺序
- **前端门禁设计**（指纹防绕过）思路相当扎实 —— 可惜现在整套断链了

---

## 四、我修正的 1 处误报

审查过程中有一条 HIGH 结论**经核实不成立**，特此更正，避免误导：

> ❌ 「`Category` 实体有 `icon`/`status` 但建表脚本没有 → **所有分类查询直接抛 `Unknown column 'icon'`**，分类功能整块 500」

**实测结果**：

```
真实库 furniture_db.category 的列：
id / name / parent_id / level / sort_order / icon / status / create_time   ← icon 和 status 都在
```

真实库**确实有**这两列，**所以运行时不会报错**，分类功能正常。

**但问题真实存在，只是性质不同**：`schema.sql` 里 `CREATE TABLE category` **只有 6 列**，且**没有任何脚本**添加 `icon`/`status`（已确认全库无 `ALTER TABLE category ADD COLUMN icon/status`）。

→ 真正的问题是 **「建库脚本与真实库不同源」**：照着仓库里的脚本从零建库，会得到一个缺列的库；而当前开发库是手工补过列的。这属于**脚本漂移**（P2），不是运行时故障（P0/P1）。

**修复**：补一条迁移脚本，或在 `schema.sql` 里直接加上这两列。

---

## 五、建议的修复路线图

### 第 0 步（今天，10 分钟）

```bash
git restore .claude deploy/ docs/     # 恢复 AI 工作流与部署文件
```

然后决定：`.claude/` 是恢复还是彻底移除。若恢复，先跑一次 `/git-save` 让门禁重新有通过标记。

### 第 1 批（本周，代价小收益大）

1. 给全部 `@RequestBody` 补 `@Valid`（一次性消灭一整类校验失效）
2. `User.password` 加 `@JsonProperty(WRITE_ONLY)`
3. 删掉 `stores/user.js` 与 `stores/cart.js` 的全部 `console.*`，并打开 `esbuild.drop: ['console']`
4. 修前端两个"调用不存在的方法"
5. 修 `reset_data.sql` 缺分号 + `merchant_update.sql` 的 MariaDB 方言
6. 通知 `markAsRead` 加 `userId` 归属校验

### 第 2 批（本月）

7. 下单幂等（`delete` 返回行数校验）
8. 评价防伪 + DDL 唯一键
9. 商家资料接口禁止改用户名 + `user.username` 唯一索引
10. `PaginationInnerInterceptor.setMaxLimit(100)`
11. 金额全链路 `BigDecimal`
12. `DATE(create_time)=CURDATE()` 改范围查询 + 补 `pay_time` 索引
13. 仪表板 4 处数据正确性问题

### 第 3 批（后续迭代）

14. 分层落地：Entity 出参换 VO、Controller 事务下沉 Service
15. 引入 Flyway + 建最小 CI
16. 补测试（先补安全用例）
17. 前端抽象：`DataTable` / `useConfirm` / `useCrudDialog` / `utils/format`
18. 拆分超长组件

---

## 附录 A：超长组件拆分清单

| 文件 | 行数 | 方案 |
|---|---|---|
| `MerchantDashboard.vue` | 865（样式就占 455） | 拆到 `components/merchant/dashboard/`：`ShopOverviewCard` / `MetricGrid` / `TodoPanel` / `ShopHealthPanel` / `ShortcutPanel` / `TrendPanel` / `RevenueChart` / `CategoryChart` / `HotProductsPanel` + `composables/useDashboardData.js`。抽出后主文件 < 120 行 |
| `UserLayout.vue` | 499 | 拆 `ShopTopBar` / `ShopHeader`（含 `SearchBox`）/ `ShopFooter`；搜索逻辑抽 `useSearchSuggest` |
| `OrdersView.vue` | 487 | 抽 `OrderCard.vue` + `utils/order.js` + `OrderFilterBar.vue` |
| `AddressView.vue` | 448 | 抽 `AddressFormDialog.vue` + `AddressCard.vue` |
| `ProductDetailView.vue` | 385 | 拆 `ProductGallery` / `ProductInfoPanel` |
| `HomeView.vue` | 374 | 拆 `HeroSection` / `CategoryPanel` / `ServicePanel` / `ChannelGrid` |
| `CartView.vue` | 366 | 抽 `CartItemRow` / `CartSettlementBar` |
| `CheckoutView.vue` | 360 | 抽 `AddressSelector` / `OrderSummaryPanel` |

**admin/merchant 重复代码可抽象点：**
`components/common/DataTable.vue`、`composables/useCrudDialog.js`、`composables/useConfirm.js`、`utils/format.js`、`constants/orderStatus.js`、`composables/useCategoryForm.js`、`composables/useCharts.js`

---

## 附录 B：全表索引现状 + 建议

| 表 | 现有索引（最终状态） | 建议新增 | 建议删除（冗余） |
|---|---|---|---|
| `user` | PK、UNIQUE(username) | UNIQUE(phone)、UNIQUE(email) | — |
| `category` | PK、idx_category_parent | UNIQUE(name,parent_id) | — |
| `product` | PK、idx_category_id、idx_status、idx_product_name、idx_product_category_status、idx_product_merchant、idx_product_sales、idx_merchant_id | 复合(merchant_id,status)；搜索需 `FULLTEXT` | `idx_category_id`（被复合最左前缀覆盖）、`idx_merchant_id` 与 `idx_product_merchant` 二选一 |
| `product_image` | PK、idx_product_id | 复合(product_id,sort_order) | — |
| `product_spec` | PK、idx_product_id | 复合(product_id,spec_name) | — |
| `address` | PK、idx_user_id | 复合(user_id,is_default) | — |
| `cart` | PK、UNIQUE uk_user_product、idx_user_id | — | **idx_user_id**（唯一键最左前缀覆盖） |
| `order` | PK、UNIQUE(order_no)、idx_user_id、idx_order_no、idx_status、idx_order_user_status、idx_order_create_time | **idx_order_pay_time**、复合(status,create_time) | **idx_order_no**（与 UNIQUE 重复）、**idx_user_id**（被复合覆盖） |
| `order_item` | PK、idx_order_id、idx_product_id、idx_order_item_order、idx_order_item_product | — | v2 重复建的那一对，任删其一 |
| `favorite` | PK、UNIQUE uk_user_product、idx_user_id | idx_product_id | **idx_user_id** |
| `review` | PK、idx_product_id、idx_user_id、idx_order_id | **UNIQUE(user_id,order_id,product_id)**、复合(product_id,create_time) | — |
| `notification` | PK、idx_user_read、idx_type | 复合(user_id,is_read,create_time) | idx_type |
| `operation_log` | PK、idx_user_id、idx_action、idx_create_time | 视查询 | — |
| `discount` | PK、UNIQUE(code)、idx_merchant_id、idx_code | UNIQUE(merchant_id,code) | **idx_code** |
| `merchant_audit` | PK、idx_audit_user、idx_audit_status | 复合(user_id,status) | — |

---

## 附录 C：验证记录

| 验证项 | 命令 | 结果 |
|---|---|---|
| 编译 | `mvn -B -DskipTests clean compile` | **BUILD SUCCESS**，编译 114 个源文件，2 条过期 API 警告（均在 `RedisConfig` 的 `setObjectMapper`） |
| 测试 | `mvn -B test` | **通过**，1 test / 0 failures，上下文加载 6.78s（真实 MySQL+Redis） |
| 真实表结构 | `information_schema` 查询 | 确认 `category` 含 `icon`/`status`（见 §四） |
| `markAsRead` 越权 | `rg` 定点 | 属实 |
| `cartMapper.delete` 返回值丢弃 | `rg` 定点 | 属实 |
| 管理员裸改 status | `rg` 定点 | 属实 |
| 通知 `push()` 零调用 | `rg '\.push\('` | 属实 |
| 评价未校验订单 | `rg` 定点 | 属实 |
| `productAPI.getCategories` 不存在 | 读 `api/modules/product.js` | 属实 |
| `orderAPI.getOrderStats` 不存在 | 读 `api/modules/order.js` | 属实 |
| `merchant_update.sql` MariaDB 方言 | 读文件 | 属实（:7、:13） |
| `reset_data.sql` 缺分号 | 读文件 | 属实（:5-6） |
| 无 CI | `ls .github/workflows` | 不存在 |

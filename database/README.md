# 数据库初始化与迁移指南

> **改表结构之前，请先读完本文件。** 本项目 schema 有**两个真相来源**，只改一边会导致
> "全新部署的库和开发机不一样"，而 MyBatis-Plus 会把实体所有字段拼进 SQL —— 缺一列就是
> `Unknown column`，整个模块不可用。

---

## 一、两个真相来源（核心约定）

| | (A) 全新安装路径 | (B) 存量升级路径 |
|---|---|---|
| 位置 | 被 `docker-compose.yml` 挂载的 4 个脚本 | `migration_vN_*.sql` |
| 谁执行 | MySQL 容器**首次**启动（数据卷为空时）自动执行 | 手工执行 |
| 内容 | `schema.sql`（权威建表脚本）+ `merchant_update` + `migration_v2` + `migration_v3_planb` | v4 / v5 / v6 / v7 ... |
| 要求 | 必须**完整**——从零建库出来的库要能直接跑应用 | 必须**幂等**——可重复执行不报错 |

**改表结构时两边都要改：**
- 新增的列/索引若对"从零建库"是必需的 → **必须进 (A)**（通常加进 `schema.sql`）
- 同时写一条 (B) 的幂等迁移，供已有库升级

> 判断标准很简单：**把 `schema.sql` 导进一个空库，应用能不能跑起来？** 不能就说明 (A) 缺东西。

---

## 二、快速初始化

### 方式 1：Docker（推荐）

```bash
# 1. 复制环境变量模板并填写
cp deploy/.env.example deploy/.env

# 2. 启动全栈（MySQL 首次启动会自动执行 01~04 脚本）
docker compose --env-file deploy/.env up -d --build
```

### 方式 2：手工

```bash
docker exec -i seckill-mysql mysql -uroot -p123456 < database/schema.sql
# 注意 schema.sql 不含 USE，需要指定库：
docker exec -i seckill-mysql mysql -uroot -p123456 furniture_db < database/schema.sql
docker exec -i seckill-mysql mysql -uroot -p123456 furniture_db < database/merchant_update.sql
docker exec -i seckill-mysql mysql -uroot -p123456 furniture_db < database/migration_v2.sql
docker exec -i seckill-mysql mysql -uroot -p123456 furniture_db < database/migration_v3_planb.sql
# 存量库继续跑 v4 起的迁移
docker exec -i seckill-mysql mysql -uroot -p123456 furniture_db < database/migration_v4_category_icon_status.sql
docker exec -i seckill-mysql mysql -uroot -p123456 furniture_db < database/migration_v5_review_unique.sql
docker exec -i seckill-mysql mysql -uroot -p123456 furniture_db < database/migration_v6_order_pay_time_index.sql
docker exec -i seckill-mysql mysql -uroot -p123456 furniture_db < database/migration_v7_integrity_constraints.sql
```

---

## 三、脚本清单

| 脚本 | 作用 | 幂等 | 被 compose 挂载 |
|---|---|---|---|
| `schema.sql` | **权威建表脚本** + 种子数据 | 否（含 `DROP TABLE`） | ✅ 01 |
| `merchant_update.sql` | 加 `product.merchant_id` + 商家账号与商品 | 是 | ✅ 02 |
| `migration_v2.sql` | 给 user/product/category 加 `deleted`；补索引 | 是 | ✅ 03 |
| `migration_v3_planb.sql` | `favorite`/`review`/`notification`/`operation_log`/`discount` | 是 | ✅ 04 |
| `migration_v4_category_icon_status.sql` | `category.icon/status` 补列 + `parent_id` 收紧为 NOT NULL | 是 | ❌ |
| `migration_v5_review_unique.sql` | `review` 唯一键 `uk_user_order_product`（含存量重复清理） | 是 | ❌ |
| `migration_v6_order_pay_time_index.sql` | `order.idx_pay_time` | 是 | ❌ |
| `migration_v7_integrity_constraints.sql` | CHECK 约束 + 核心外键 + 唯一键 + 冗余索引清理 | 是 | ❌ |
| `reset_data.sql` | **破坏性**：清空并重灌种子数据 | — | ❌ |

> v4 起故意**不挂载**到 compose：entrypoint 只在空卷首启执行，那时 `schema.sql` 已经包含了
> 这些内容，挂上去是多余的。它们的存在意义是给**已有库**升级。

---

## 四、写迁移脚本的硬性要求

### 1. MySQL 8.0 不支持 `IF NOT EXISTS`（这是本项目踩过的坑）

```sql
-- ❌ 报 ERROR 1064（这是 MariaDB 方言）
ALTER TABLE `product` ADD COLUMN IF NOT EXISTS `merchant_id` BIGINT;
CREATE INDEX IF NOT EXISTS `idx_x` ON `product` (`merchant_id`);
```

**正确写法**（查 `information_schema` 判存在）：
```sql
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_xxx()
BEGIN
  IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'product' AND COLUMN_NAME = 'merchant_id') THEN
    ALTER TABLE `product` ADD COLUMN `merchant_id` BIGINT COMMENT '商家ID';
  END IF;
END //
DELIMITER ;
CALL add_xxx();
DROP PROCEDURE IF EXISTS add_xxx();
```

判索引用 `information_schema.STATISTICS`，判约束用 `information_schema.TABLE_CONSTRAINTS`。

### 2. 必须能在**任意库状态**下安全执行

**不要假设目标库处于"所有脚本都跑过"的理想状态。** 真实开发库常常是手工拼出来的中间态。

典型例子：删除"冗余索引"前，必须先确认**覆盖它的索引确实存在** ——
否则会把唯一可用的索引删掉。本项目 v7 就是这么写的（每条删除都带存在性前置条件）。

### 3. 加约束前先巡检存量数据

加 CHECK 前查违规行、加 FK 前查悬空引用、加唯一键前查重复。有脏数据时 `ALTER` 会直接失败。

### 4. 幂等

脚本要能被重复执行。`reset_data.sql` 之外的迁移脚本都必须满足这一点。

---

## 五、验证工具

```bash
# 实体字段 vs 数据库表结构 是否漂移（改完 schema 必跑）
node scripts/check-entity-schema.mjs
DB_NAME=furbiture_verify node scripts/check-entity-schema.mjs   # 指定库
```

**这个检查很重要**：`Category.icon/status` 曾经就是"实体有、建表脚本没有"，
现有开发库因手工补过列而一直正常，但**全新部署出来的实例一查分类就报 Unknown column**。
这类问题在开发机上永远看不见。

---

## 六、数据库配置

| 项 | 值 |
|---|---|
| 数据库名 | `furniture_db` |
| 端口 | `3306`（容器 `seckill-mysql`） |
| 用户名 / 密码 | `root` / `123456`（开发环境默认值） |
| 字符集 | `utf8mb4` |

> 生产环境请用 `deploy/.env` 覆盖，**不要**把真实口令提交进仓库。

---

## 七、测试账号

密码均为 `123456`（BCrypt 哈希预置）。

| 用户名 | 角色 | 说明 |
|---|---|---|
| `admin` | ADMIN | 管理员 |
| `user1` / `user2` / `user3` | USER | 普通用户 |
| `merchant1` | MERCHANT | 商家（种子商品归属它） |
| `merchant2` | MERCHANT | 商家 |

> ⚠️ **自增 ID 按插入顺序分配，不要硬编码商家 ID**。代码里必须经
> `product.merchant_id` 或当前登录用户推导，而不是写死 `merchantId = 3`。

---

## 八、表清单（15 张）

**基础表（`schema.sql`）**：`user`、`category`、`product`、`product_image`、`product_spec`、
`address`、`cart`、`order`、`order_item`

**扩展表（`migration_v2` / `migration_v3_planb`）**：`merchant_audit`、`favorite`、`review`、
`notification`、`operation_log`、`discount`

> ⚠️ `order` 是 MySQL 关键字，写 SQL 必须反引号转义 `` `order` ``。
> `merchant_audit` 与 `discount` 两张表**无对应实体**，未接入 ORM。

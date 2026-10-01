---
name: furbiture-authorization
description: Use when writing or reviewing any Furbiture endpoint, service method, or mapper query that operates on a resource by id — order, cart, address, product, category, notification, review, favorite. Especially when the task says "照现有风格写", "参考 xxx 模块", "快速实现", or when you are about to copy a neighbouring method as a template.
---

# Furbiture 资源归属校验

## Overview

**项目里已有的归属校验写法，有一半是错的。照抄隔壁方法 = 照抄漏洞。**

本项目 `CLAUDE.md` 称之为「最大的历史技术债」，2026-10-01 全量体检仍有漏网（`NotificationController.markRead`）。

**核心规则：写操作（新增/修改/删除某个具体资源）时，归属条件必须写进 SQL 的 `WHERE`，用影响行数判断成败。禁止"先查出来，再在 Java 里比对，然后再操作"。**

## When to Use

- 任何形如 `PUT /xxx/{id}`、`POST /xxx/{id}/action`、`DELETE /xxx/{id}` 的接口
- 任何按 id 读/改/删资源的 Service 方法
- 商家端接口中"这个商品/订单是不是我的"的判断
- 你正准备把某个已有方法当模板来抄的时候 ← **最危险，先读这条**

## Core Pattern

```java
// ❌ B 类：先查后判断 —— 项目现有代码里最常见的写法
Notification n = getById(id);
if (n != null && n.getUserId().equals(userId)) {
    n.setIsStarred(1);
    updateById(n);
}
```

```java
// ✅ A 类：条件下沉进 WHERE，看影响行数
boolean updated = update(new LambdaUpdateWrapper<Notification>()
        .eq(Notification::getId, id)
        .eq(Notification::getUserId, userId)   // ← 归属进 WHERE
        .set(Notification::getIsStarred, 1));
if (!updated) {
    throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在");
}
```

**为什么必须用 A 类**：B 类的检查和操作之间有窗口。当**被检查的条件会变**时它就是真漏洞 ——
本项目 P0 资损 bug（重复下单）正是这个模式：先 `selectBatchIds` 查出 cart 行，再 `delete` 且丢弃返回值 → 两个并发请求都通过检查 → **生成两笔订单**。

**B 类什么时候不算漏洞**：只读操作（GET），或归属字段本身不可转移（`address.user_id` 没有转移机制）。
即便如此，**新代码也没理由用更差的写法。**

## Quick Reference

| 场景 | 正确写法 |
|---|---|
| 改/删某资源 | `update/delete(new LambdaUpdateWrapper<X>().eq(X::getId, id).eq(X::getUserId, uid)...)`，判断返回值 |
| 条件状态流转 | 把**前置状态**也写进 WHERE：`.eq(Order::getStatus, PENDING_PAYMENT)` |
| 商家归属 | 归属只能经 `order_item → product.merchant_id`；`order.merchant_id` 列**未映射、代码不使用** |
| 创建资源 | 服务端**强制覆写** `setUserId(当前用户)`，忽略请求体里的 userId |
| 校验 null | 用 `Objects.equals(a, b)`，**不要** `a.getMerchantId().equals(x)`（null 时 NPE） |
| 错误码 | 用 `ResultCode.NOT_FOUND` / `FORBIDDEN`；**不要** `R.error(String)`（固定返回 500） |
| 不存在 vs 无权 | 返回**同一条消息**，避免用 404/403 差异探测他人数据 |

## Red Flags — 出现任意一条，停下来改成 A 类

- 我正在写 `getById(...)` / `getOne(...)`，接下来打算 `if (!x.getUserId().equals(cur))`，然后再 `update` / `delete`
- 我在"照抄"某个已有方法的写法，**但没有先确认那个方法本身是对的**
- 我打算在报告/回复里写一段"安全提醒"或"建议后续修复"，而**不是直接把代码改对**
- 我用「项目规范说不做大范围重构」来为"保留一个已知漏洞"开脱
- 接口签名里没有 `HttpServletRequest`（或没有取 `userId`），但它在改一个属于某人的资源

**最后一条尤其致命**：没有 `userId` 就意味着这条链路从头到尾没打算校验归属。

## 常见合理化（都已被实测验证过）

| 借口 | 现实 |
|---|---|
| 「照着现有代码风格写，保持一致」 | 现有代码里**有已知的越权洞**（`NotificationController.markRead`）。风格一致 ≠ 安全。**抄之前先判断邻居对不对** |
| 「我在回复里加了安全提醒，用户会自己决定」 | 提醒不是修复。**交付出去的那份代码就是有洞的代码**，而"注意到问题"恰恰证明你有能力改对 |
| 「项目规范说不做大范围重构，所以我不动老代码」 | **不改老代码 ≠ 新代码可以复制老 bug。** 这条规范禁止的是重构，不是禁止你把**自己新写的**那行写对 |
| 「`getById` 后判断 userId 也算校验了」 | 算，但用了一个更差的写法。新代码没有理由不用更稳的 |
| 「赶时间，先把功能跑通」 | A 类比 B 类**更短**（少一次查询）。A 类不是额外成本，是更省 |

## 自检（改完自问）

1. 这个接口改的资源，**归属条件在 WHERE 里**吗？
2. 如果它涉及状态流转，**前置状态在 WHERE 里**吗？
3. 我**检查了影响行数**吗，还是丢弃了返回值？
4. Controller 里**取到 userId 并传下去了**吗？
5. 如果我是抄来的——**我确认过被抄的那个方法本身正确吗？**

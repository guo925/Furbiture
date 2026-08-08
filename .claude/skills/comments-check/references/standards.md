# 注释质量标准 — 好的注释 vs 不好的注释

## 原则

> 好注释解释 **Why**（为什么这样做），坏注释只重复 **What**（做了什么）。

---

## 维度一：注释密度

### ❌ 坏例子：注释严重不足

```java
public Result<UserVO> login(LoginDTO dto) {
    String username = dto.getUsername();
    String password = dto.getPassword();
    User user = userMapper.selectByUsername(username);
    if (user == null) {
        throw new BusinessException("用户不存在");
    }
    if (!passwordEncoder.matches(password, user.getPassword())) {
        throw new BusinessException("密码错误");
    }
    if (user.getStatus() == 1) {
        throw new BusinessException("账号已被禁用");
    }
    String token = jwtUtil.generateToken(user.getId(), user.getUsername());
    UserVO vo = new UserVO();
    BeanUtils.copyProperties(user, vo);
    vo.setToken(token);
    redisTemplate.opsForValue().set("token:" + user.getId(), token, 24, TimeUnit.HOURS);
    return Result.success(vo);
}
```

这段代码 18 行，0 行注释。新人看到会困惑：
- status == 1 是什么意思？
- 为什么要存 Redis？
- JWT 里放了什么？

### ✅ 好例子：恰到好处的注释

```java
/**
 * 用户登录
 * 流程：验证账号密码 → 检查账号状态 → 生成 JWT → 缓存 Token → 返回用户信息
 */
public Result<UserVO> login(LoginDTO dto) {
    String username = dto.getUsername();
    String password = dto.getPassword();

    // 1. 根据用户名查询用户
    User user = userMapper.selectByUsername(username);
    if (user == null) {
        throw new BusinessException("用户不存在");
    }

    // 2. 校验密码（使用 BCrypt 加密对比，不存储明文密码）
    if (!passwordEncoder.matches(password, user.getPassword())) {
        throw new BusinessException("密码错误");
    }

    // 3. 检查账号状态：1=禁用，0=正常
    if (user.getStatus() == 1) {
        throw new BusinessException("账号已被禁用");
    }

    // 4. 生成 JWT Token，包含用户 ID 和用户名，有效期 24 小时
    String token = jwtUtil.generateToken(user.getId(), user.getUsername());

    // 5. 组装返回数据
    UserVO vo = new UserVO();
    BeanUtils.copyProperties(user, vo);
    vo.setToken(token);

    // 6. 将 Token 存入 Redis，用于后续登录状态校验和踢人功能
    redisTemplate.opsForValue().set("token:" + user.getId(), token, 24, TimeUnit.HOURS);

    return Result.success(vo);
}
```

---

## 维度二：注释准确性

### ❌ 坏例子：注释与代码不一致

```java
// 查询所有未禁用的用户
List<User> users = userMapper.selectList(
    new LambdaQueryWrapper<User>()
        .eq(User::getStatus, 1)  // 实际查的是 status=1（禁用），不是"未禁用"！
);
```

### ✅ 好例子：注释准确匹配

```java
// 查询所有已禁用的用户（status=1 表示禁用）
List<User> users = userMapper.selectList(
    new LambdaQueryWrapper<User>()
        .eq(User::getStatus, 1)
);
```

### ❌ 坏例子：重构后注释未更新

```java
// 发送短信验证码  ← 代码已改成发邮件，但注释没改！
emailService.sendVerificationCode(email);
```

---

## 维度三：小白可读性

### ❌ 坏例子：只有 What 没有 Why

```java
// 使用 CompletableFuture 异步处理
CompletableFuture.runAsync(() -> {
    orderService.createOrder(order);
});
```

小白看完不知道为什么要异步、异步有什么好处。

### ✅ 好例子：解释了 Why

```java
// 异步创建订单，避免用户等待
// CompletableFuture 会在后台线程执行，主线程立即返回响应给前端
CompletableFuture.runAsync(() -> {
    orderService.createOrder(order);
});
```

### ❌ 坏例子：行业术语无解释

```java
// 生成 SKU 编码
String skuCode = generateSkuCode(product);
```

### ✅ 好例子：解释了术语

```java
// 生成 SKU 编码（SKU = Stock Keeping Unit，库存量单位，用于唯一标识商品规格）
String skuCode = generateSkuCode(product);
```

### ❌ 坏例子：技术名词无解释

```java
// 使用 AOP 记录日志
@Around("logPointcut()")
public Object logAround(ProceedingJoinPoint joinPoint) { ... }
```

### ✅ 好例子：解释了技术名词

```java
/**
 * 使用 AOP（面向切面编程）自动记录 Controller 层的请求日志
 * @Around 表示在目标方法执行前后都会进入此方法
 * ProceedingJoinPoint 代表被拦截的目标方法（如具体的 Controller 接口）
 */
@Around("logPointcut()")
public Object logAround(ProceedingJoinPoint joinPoint) { ... }
```

---

## 快速自查清单

写注释时问自己三个问题：

1. **密度够吗？** — 10 行代码里有没有 3 行注释？
2. **准确吗？** — 改代码时有没有同步改注释？
3. **小白能看懂吗？** — 如果我是半年前的自己，现在能看懂吗？

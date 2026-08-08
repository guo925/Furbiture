# 安全检查清单

## 🔴 严重风险

### SQL 注入

```java
// ❌ 危险：字符串拼接 SQL
@Select("SELECT * FROM user WHERE username = '${username}'")
User findByUsername(String username);

// ❌ 危险：手写 SQL 用 ${}
userMapper.execute("DELETE FROM order WHERE id = ${orderId}");

// ✅ 安全：MyBatis 参数绑定
@Select("SELECT * FROM user WHERE username = #{username}")
User findByUsername(String username);
```

**检查方法：** grep `${` 在 mapper XML 和注解 SQL 中。

### 认证缺失

```java
// ❌ 危险：敏感操作无认证
@PostMapping("/api/admin/user/delete")
public Result<Void> deleteUser(@RequestParam Long id) { ... }
// → 任何人知道 URL 就能删用户！

// ✅ 安全：加权限校验
@PostMapping("/api/admin/user/delete")
@PreAuthorize("hasRole('ADMIN')")
public Result<Void> deleteUser(@RequestParam Long id) { ... }
```

**检查方法：** 找到所有 `@PostMapping`、`@DeleteMapping`、`@PutMapping`，确保敏感接口有 `@PreAuthorize` 或在 SecurityConfig 中配置了拦截规则。

### 敏感数据泄露

```java
// ❌ 危险：日志打印敏感信息
log.info("用户注册成功: {}", dto);  // dto 含密码！
log.info("登录: phone={}, pwd={}", phone, password);

// ❌ 危险：返回 Entity 含密码字段
return Result.success(user);  // user.getPassword() 返回给前端了

// ✅ 安全：
log.info("用户注册成功: phone={}", maskPhone(dto.getPhone()));
return Result.success(userVO);  // VO 不含 password
```

**检查方法：** 
- 搜索 `log\.(info|debug|warn).*password|phone|token|secret|key`
- 检查 Controller 返回值是否用 VO 而非 Entity

---

## 🟠 高危风险

### 越权风险（IDOR）

```java
// ❌ 危险：未校验数据归属
public Result<Void> cancelOrder(Long orderId) {
    orderMapper.deleteById(orderId);  // 用户 A 删了用户 B 的订单！
}

// ✅ 安全：校验归属
public Result<Void> cancelOrder(Long orderId, Long currentUserId) {
    Order order = orderMapper.selectById(orderId);
    if (!order.getUserId().equals(currentUserId)) {
        throw new BusinessException("无权操作此订单");
    }
    orderMapper.deleteById(orderId);
}
```

**检查方法：** 找所有 update/delete 操作，确认是否校验了数据归属（userId 匹配）。

### 输入校验不足

```java
// ❌ 危险：未校验直接入参
@PostMapping("/category")
public Result<Void> save(@RequestBody CategoryDTO dto) {
    categoryService.save(dto);  // name 为 null 也直接存了
}

// ✅ 安全：参数校验
@PostMapping("/category")
public Result<Void> save(@Valid @RequestBody CategoryDTO dto) {
    // DTO 中: @NotBlank String name; @NotNull Integer sort;
}
```

**检查方法：** 检查 Controller 方法参数是否有 `@Valid` 注解，DTO 字段是否有校验注解（`@NotNull`, `@NotBlank`, `@Size` 等）。

---

## 🟡 中危风险

### XSS / HTML 注入

```java
// ❌ 潜在风险：用户输入未转义直接返回
@GetMapping("/search")
public Result<List<Product>> search(@RequestParam String keyword) {
    // keyword 可能是 <script>alert('xss')</script>
}

// ✅ 安全：前端做转义，后端可加 @Size 限制长度
@GetMapping("/search")
public Result<List<Product>> search(@RequestParam @Size(max=50) String keyword) { ... }
```

### 文件上传安全

```java
// ❌ 危险：未校验文件类型
public String upload(MultipartFile file) {
    String filename = file.getOriginalFilename();  // 可能是 shell.jsp
    Files.copy(file.getInputStream(), uploadPath.resolve(filename));
}

// ✅ 安全：校验文件类型和大小
public String upload(MultipartFile file) {
    String ext = FilenameUtils.getExtension(file.getOriginalFilename());
    if (!ALLOWED_EXTENSIONS.contains(ext)) {
        throw new BusinessException("不支持的文件类型");
    }
    // 使用 UUID 重命名，防止路径穿越
    String newName = UUID.randomUUID() + "." + ext;
}
```

### 配置安全

```java
// ❌ 危险：硬编码密钥
private static final String SECRET_KEY = "mySecretKey123";  // 泄露到 git！

// ✅ 安全：从配置读取
@Value("${jwt.secret}")
private String secretKey;
```

**检查方法：** 搜索 `password\s*=|secret\s*=|key\s*=|token\s*=` 等硬编码模式。

---

## 🟢 低危风险

### 依赖安全

```bash
# 检查是否有已知漏洞的依赖
mvn dependency:tree | grep -E "log4j|fastjson|jackson|shiro|struts"
```

关注这些常见漏洞库：
- log4j-core < 2.17.0
- fastjson < 1.2.83
- jackson-databind 旧版本
- spring-framework < 5.3.31

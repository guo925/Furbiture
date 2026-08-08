# 单元测试分层模板

## 1. Service 层测试模板

最常见、最重要的测试。使用纯 Mockito，不启动 Spring 上下文。

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // ========== 正常流程 ==========

    @Test
    @DisplayName("正常流程 - 用户注册成功")
    void shouldRegisterUserSuccessfullyWhenValidInput() {
        // Given
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("testuser");
        dto.setPassword("123456");
        dto.setEmail("test@example.com");

        when(userMapper.selectByUsername("testuser")).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("encoded_password");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        // When
        Result<UserVO> result = userService.register(dto);

        // Then
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getUsername()).isEqualTo("testuser");
        verify(userMapper, times(1)).insert(any(User.class));
    }

    // ========== 边界条件 ==========

    @Test
    @DisplayName("边界条件 - 用户名为空时抛出异常")
    void shouldThrowExceptionWhenUsernameIsEmpty() {
        // Given
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("");
        dto.setPassword("123456");

        // When & Then
        assertThrows(BusinessException.class, () -> userService.register(dto));
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("边界条件 - 用户名已存在时返回错误")
    void shouldReturnErrorWhenUsernameAlreadyExists() {
        // Given
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("existing");
        dto.setPassword("123456");

        when(userMapper.selectByUsername("existing")).thenReturn(new User());

        // When
        Result<UserVO> result = userService.register(dto);

        // Then
        assertThat(result.getCode()).isNotEqualTo(200);
        verify(userMapper, never()).insert(any());
    }

    // ========== 异常场景 ==========

    @Test
    @DisplayName("异常场景 - 数据库插入失败时抛出异常")
    void shouldThrowExceptionWhenDatabaseInsertFails() {
        // Given
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("testuser");
        dto.setPassword("123456");

        when(userMapper.selectByUsername("testuser")).thenReturn(null);
        when(userMapper.insert(any(User.class)))
                .thenThrow(new DataAccessException("DB error") {});

        // When & Then
        assertThrows(DataAccessException.class, () -> userService.register(dto));
    }
}
```

---

## 2. Controller 层测试模板

使用 `@WebMvcTest` 测试 Controller 层，Mock Service 依赖。

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("正常流程 - 注册接口返回200")
    void shouldReturn200WhenRegisterWithValidInput() throws Exception {
        // Given
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("test");
        dto.setPassword("123456");

        Result<UserVO> mockResult = Result.success(new UserVO());
        when(userService.register(any(RegisterDTO.class))).thenReturn(mockResult);

        // When & Then
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("异常场景 - Service 抛异常时返回错误")
    void shouldReturnErrorWhenServiceThrowsException() throws Exception {
        // Given
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("test");
        dto.setPassword("123456");

        when(userService.register(any()))
                .thenThrow(new BusinessException("注册失败"));

        // When & Then
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
```

如果项目使用 Security，需要在测试中处理认证：

```java
@WebMvcTest(UserController.class)
@WithMockUser(username = "admin", roles = {"ADMIN"})
class UserControllerTest { ... }
```

---

## 3. Mapper 层测试模板

使用 MyBatis-Plus，直接用 `@SpringBootTest` + 真实数据库或 H2。

```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 用真实数据库
// 或 @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // 用 H2
@Transactional  // 测试完自动回滚
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    @DisplayName("正常流程 - 根据用户名查询用户")
    void shouldFindUserByUsername() {
        // Given: 先插入一条数据
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("123456");
        userMapper.insert(user);

        // When
        User result = userMapper.selectByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
    }
}
```

---

## 4. 工具类测试模板

不需要 Mock，直接 new 或调静态方法。

```java
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("test-secret-key-for-unit-test-12345678", 3600000L);
    }

    @Test
    @DisplayName("正常流程 - 生成并解析 Token")
    void shouldGenerateAndParseToken() {
        // Given
        String username = "testuser";

        // When
        String token = jwtUtil.generateToken(username);
        String parsed = jwtUtil.getUsernameFromToken(token);

        // Then
        assertThat(parsed).isEqualTo(username);
    }

    @Test
    @DisplayName("边界条件 - 过期 Token 解析失败")
    void shouldFailWhenTokenIsExpired() {
        // Given
        JwtUtil shortLived = new JwtUtil("test-secret", 1L); // 1ms 过期
        String token = shortLived.generateToken("testuser");

        // 等待过期
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        // When & Then
        assertThrows(ExpiredJwtException.class, () -> shortLived.getUsernameFromToken(token));
    }
}
```

---

## 常用 Mockito 速查

| 场景 | 代码 |
|------|------|
| Mock 返回值 | `when(dep.method(arg)).thenReturn(value)` |
| Mock 多个返回值 | `when(dep.method(arg)).thenReturn(v1, v2, v3)` |
| Mock 抛异常 | `when(dep.method(arg)).thenThrow(new XxxException())` |
| Mock void 方法 | `doNothing().when(dep).voidMethod()` |
| 匹配任意参数 | `any(User.class)`, `anyString()`, `anyLong()` |
| 精确匹配 | `eq("value")` |
| 匹配 null | `isNull()` |
| 验证调用次数 | `verify(dep, times(2)).method()` |
| 验证从未调用 | `verify(dep, never()).method()` |
| 捕获参数 | `ArgumentCaptor<User> captor = ...; verify(dep).method(captor.capture())` |
| 断言异常 | `assertThrows(XxxException.class, () -> service.method())` |
| 断言集合 | `assertThat(list).hasSize(3).contains(item)` |

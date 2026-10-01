package com.gjx.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gjx.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 密码字段序列化防护测试。
 *
 * <p><b>为什么需要这个测试：</b>
 * {@code User.password} 承载的是 BCrypt 哈希。它一旦被序列化进接口响应，攻击者就能拿到
 * 哈希做离线爆破。历史实现靠「在每个返回 User 的地方手工写一行 {@code setPassword(null)}」
 * 兜底——共 4 处。这种"约定式安全"极度脆弱：任何人新增一个返回 User 的接口、或漏写一行，
 * 哈希立刻外泄，而且**不会有任何测试失败**来提醒你。
 *
 * <p>现在改为在实体上声明 {@code @JsonProperty(access = WRITE_ONLY)}，从序列化层根治。
 * 本测试把这个不变量**变成可执行的断言**——如果将来有人误删该注解，
 * 这些用例会立刻变红，而不是等哈希泄露到线上才发现。
 *
 * <p>覆盖的三种出口形态，正是项目里真实存在的：
 * <ol>
 *   <li>直接返回 {@code User}（如 {@code UserController.getCurrentUser}）</li>
 *   <li>包在 {@code Map} 里返回（如 {@code AuthController.login} 的 {@code result.put("user", user)}）</li>
 *   <li>包在集合/分页里返回（如 {@code AdminUserController.list} 的 {@code Page<User>}）</li>
 * </ol>
 */
class PasswordSerializationTest {

    /** 故意用一个一眼能认出的假哈希，便于断言与排查 */
    private static final String FAKE_HASH = "$2a$10$FAKEHASHFORTESTINGONLYaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User userWithPassword() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword(FAKE_HASH);
        user.setRole("ADMIN");
        return user;
    }

    @Test
    @DisplayName("直接序列化 User 时，password 不应出现在 JSON 中")
    void shouldNotSerializePasswordWhenSerializingUserDirectly() throws Exception {
        String json = objectMapper.writeValueAsString(userWithPassword());

        assertFalse(json.contains(FAKE_HASH), "BCrypt 哈希泄露到响应中：" + json);
        assertFalse(json.contains("password"), "响应 JSON 中不应出现 password 字段：" + json);
        // 同时确认其它字段仍正常输出——避免"把整个对象都序列化没了"这种过度修复
        assertTrue(json.contains("admin"), "正常字段应保留：" + json);
        assertTrue(json.contains("ADMIN"), "正常字段应保留：" + json);
    }

    @Test
    @DisplayName("把 User 放进 Map 返回时（AuthController.login 的形态），password 不应泄露")
    void shouldNotSerializePasswordWhenWrappedInMap() throws Exception {
        User user = userWithPassword();

        Map<String, Object> result = new HashMap<>();
        result.put("token", "fake.jwt.token");
        result.put("user", user);

        String json = objectMapper.writeValueAsString(result);

        assertFalse(json.contains(FAKE_HASH), "BCrypt 哈希泄露到响应中：" + json);
        assertTrue(json.contains("fake.jwt.token"), "token 应正常输出：" + json);
    }

    @Test
    @DisplayName("把 User 放进集合返回时（AdminUserController.list 的分页形态），password 不应泄露")
    void shouldNotSerializePasswordWhenInsideCollection() throws Exception {
        // Page<User> 在 Jackson 眼里就是一个带 records 列表的可序列化对象，
        // 用 List 足以覆盖"User 嵌在集合里"这一序列化路径
        String json = objectMapper.writeValueAsString(List.of(userWithPassword(), userWithPassword()));

        assertFalse(json.contains(FAKE_HASH), "BCrypt 哈希泄露到响应中：" + json);
    }

    @Test
    @DisplayName("toString() 不得带出密码哈希 —— 日志打印实体是 JSON 防线拦不住的另一条路")
    void shouldNotExposePasswordViaToString() {
        // @JsonProperty(WRITE_ONLY) 只管 Jackson 序列化。@Data 生成的 toString() 会拼上每个字段，
        // 而 log.info("{}", user) 这种写法在项目里很常见——一旦有人这么写，密码哈希就进日志文件了。
        // 本断言钉住 @ToString.Exclude 不被误删。
        String text = userWithPassword().toString();

        assertFalse(text.contains(FAKE_HASH), "密码哈希出现在 toString() 中：" + text);
        // 同时确认不是把整个 toString 砍掉了（过度修复）
        assertTrue(text.contains("admin"), "其余字段应保留：" + text);
    }

    @Test
    @DisplayName("反序列化（写入）方向必须仍然可用：注册/建用户时 password 要能正常接收")
    void shouldStillDeserializePasswordOnWrite() throws Exception {
        // WRITE_ONLY 的语义是"只写不读"。若误改成 @JsonIgnore，注册与改密功能会静默失效——
        // 这个断言就是防止有人"为了安全"把它改成 @JsonIgnore 而破坏写入链路。
        String body = "{\"username\":\"newuser\",\"password\":\"plainSecret123\"}";

        User user = objectMapper.readValue(body, User.class);

        assertTrue("plainSecret123".equals(user.getPassword()),
                "password 必须仍能从请求体反序列化进来，否则注册/改密会失效");
    }
}

---
name: unit-test
description: AI 智能单元测试 — 生成、执行、报告一条龙。当用户说"写测试"/"单元测试"/"测试这个类"时，AI 应主动调用此技能。
---

# AI 智能单元测试

为指定类生成 JUnit 5 + Mockito 单元测试，执行并输出报告。

---

## 参数

用户可以通过 `/unit-test <target>` 指定测试目标：
- 类全限定名：`com.gjx.controller.UserController`
- 方法级：`com.gjx.service.UserService#login`
- 不指定：自动选择最近修改或用户当前正在查看的类

---

## 执行流程

### 第一步：确定测试目标

如果用户指定了 `<target>`，直接定位该类。

如果未指定，按优先级自动选择：
1. IDE 中当前打开的文件
2. 最近 git 修改的 Java 源文件（非测试文件）
3. 提示用户指定

找到目标类后，读取源码完整内容。

### 第二步：分析待测代码

分析以下内容：

1. **类的基本信息**
   - 类名、包路径、父类、实现的接口
   - 类上的注解（`@Service`、`@RestController`、`@Component` 等）
   - 所有依赖注入字段（`@Autowired`、`@Resource`、构造器注入）

2. **方法清单**
   - public 方法的签名、参数、返回值
   - 每个方法的业务逻辑分析
   - 方法中的条件分支、循环、异常处理

3. **依赖链分析**
   - 每个依赖（Mapper、其他 Service、工具类）的作用
   - 依赖方法调用的上下文和返回类型
   - 确定哪些依赖需要 Mock

4. **测试场景识别**
   对每个方法，列出：
   - ✅ 正常流程（happy path）
   - ⚠️ 边界条件（null、空集合、极值等）
   - ❌ 异常场景（依赖失败、数据不存在、参数非法等）

将分析结果**简要展示给用户确认**，然后进入生成阶段。

### 第三步：生成测试代码

按照以下规范生成 JUnit 5 + Mockito 测试类。**模板和常见模式参考 `references/templates.md`。**

**文件位置：** `src/test/java/{package}/{ClassName}Test.java`

**编写规范：**
- 测试方法命名：`should{预期行为}When{条件}`
- 使用 `@DisplayName` 写中文描述
- Given/When/Then 结构清晰分块
- 所有 Mock 调用使用 `when().thenReturn()` 或 `when().thenThrow()`
- 异常测试使用 `assertThrows()`
- 需要验证 Mock 调用次数时使用 `verify(dep, times(n)).method()`
- 使用 `ArgumentCaptor` 验证复杂参数

**覆盖要求：**
- 每个 public 方法至少 1 个正常流程测试
- 有条件分支的方法，覆盖所有分支
- 有数据库操作的，覆盖成功/失败两种场景
- 有外部依赖调用的，覆盖依赖异常场景

生成完成后，将测试代码写入 `src/test/java/{package}/{ClassName}Test.java`。

### 第四步：编译检查

```bash
cd <project_root> && mvn test-compile -q 2>&1 | tail -30
```

如果编译失败，分析错误并修复测试代码，重新写入后再次编译。最多尝试修复 3 次。

### 第五步：执行测试

```bash
cd <project_root> && mvn test -Dtest="{ClassName}Test" -q 2>&1
```

如果整个项目编译失败（与测试类无关），则：
```bash
cd <project_root> && mvn test -Dtest="{ClassName}Test" -DfailIfNoTests=false -pl . -am 2>&1 | tail -60
```

### 第六步：生成测试报告

执行结束后，读取并解析 `target/surefire-reports/{ClassName}Test.txt` 和对应 XML 文件。

输出结构化报告：

```
╔══════════════════════════════════════════════════════════════╗
║                    📋 单元测试报告                             ║
╠══════════════════════════════════════════════════════════════╣
║  被测类   │ com.gjx.service.UserService                      ║
║  测试类   │ UserServiceTest                                  ║
║  测试数   │ 8  |  通过 ✅ 7  |  失败 ❌ 1  |  跳过 ⏭️ 0       ║
║  耗时     │ 3.2s                                             ║
╠══════════════════════════════════════════════════════════════╣

  通过的测试:
  ✅ 正常流程 - 用户登录成功
  ✅ 正常流程 - 注册新用户
  ✅ 边界条件 - 用户名为空
  ✅ 边界条件 - 密码长度不足
  ✅ 边界条件 - 用户名已存在
  ✅ 异常场景 - 数据库连接失败
  ✅ 异常场景 - 密码加密异常

  失败的测试:
  ❌ 异常场景 - Token 生成失败
     错误: Expected exception of type 'TokenException' but got null
     位置: UserServiceTest.java:145
     原因分析: login() 方法中 Token 生成异常未被正确抛出，检查 generateToken() 方法的异常处理
     建议: 在 UserService.login() 中添加 try-catch 或确保 generateToken() 抛出 Runtime 异常

╚══════════════════════════════════════════════════════════════╝
```

### 第七步：失败分析（如有失败）

如果存在失败测试，逐项分析：

1. **是测试写错了还是业务代码有 Bug？**
   - 核对 Mock 设置是否匹配实际调用参数
   - 核对断言期望值是否与源码逻辑一致
   - 检查是否遗漏了 Mock 依赖

2. **给出修复方向**
   - 测试写错 → 修正测试代码
   - 业务代码有 Bug → 指出 Bug 位置和修复建议

3. **询问用户是否要自动修复并重跑**

---

## 注意事项

- 生成的测试文件路径遵循 Maven 标准目录结构
- 如果测试文件已存在，先展示已有内容，询问是覆盖还是追加
- 测试代码中不要使用 `Thread.sleep()`、`System.out.println()`
- 使用项目已有的测试依赖（JUnit 5 + Mockito），不引入新依赖
- Controller 层测试参考 `references/templates.md` 中的分层模板

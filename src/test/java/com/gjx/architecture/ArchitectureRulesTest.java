package com.gjx.architecture;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 架构规则测试（ArchUnit）。
 *
 * <p><b>这个类为什么存在：</b>
 * 本项目把「Controller 不写事务」「分层单向依赖」「Mapper 必须是接口」等规范只写在了散文文档里
 * （{@code CLAUDE.md}、{@code docs/AI-CONTEXT.md} §七）。AI 读了会「尽量遵守」，但没有任何东西
 * 在它违反时拦住它——规范是软的。本类把这些规范编译成**可执行断言**：一旦有人（人或 AI）踩线，
 * {@code mvn test} 立刻变红，且失败信息里直接给出「违反时应该怎么改」。
 *
 * <p><b>为什么用显式 {@code check(classes)} 而不是 {@code @ArchTest} 静态字段：</b>
 * 每条规则单独一个 {@code @Test}，失败时能精确指认是哪条红线被破，报告也更可读。
 *
 * <p><b>为什么先做「非空」断言：</b>
 * ArchUnit 的规则若因包名写错而匹配到 0 个类，会**静默通过**——这正是本项目 pre-commit 门禁
 * 「检查的是空集合所以永远为真」的同一个坑（见 AI-CONTEXT §五-1）。故对关键规则先断言导入集合非空。
 *
 * <p><b>本批次已放行全部规则</b>（含此前 @Disabled 的「Controller 不得直接依赖 Mapper」）。
 * 若将来又探测到需要大重构才能满足的规则，仍沿用 {@code @Disabled} + 违规清单的写法留待后续批次，
 * 不为让构建变绿而放宽规则。
 */
class ArchitectureRulesTest {

    /** 只导入主代码（target/classes），排除测试类，避免测试自身的写法干扰架构判断。 */
    private static JavaClasses classes;

    @BeforeAll
    static void importMainClasses() {
        classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.gjx");
    }

    /**
     * 红线 §七-6：事务边界在 Service 层。
     *
     * <p>Controller 上标 {@code @Transactional} 会把「HTTP 解析 → 事务提交」耦在一起：
     * 事务时长包含了参数绑定甚至序列化，且多数时候只是为包住一次 Service 调用而空转
     * （Service 自己已有事务）。正确的改法是把注解下沉到 Service 方法
     * （多表写用 {@code rollbackFor = Exception.class}），Controller 只做校验与转发。
     *
     * <p><b>注意：</b>违规几乎都出现在**方法**上（{@code @Transactional} 标在 Controller 的
     * 某个 handler 方法上），而不是类上。所以这里同时检查类级与方法级——只查类级会让规则
     * 对真实的违规「静默通过」（本规则第一版就踩过这个坑）。
     */
    @Test
    @DisplayName("红线#6 — @RestController（及其方法）不得标注 @Transactional（事务边界属于 Service 层）")
    void controllersMustNotBeTransactional() {
        String why = "红线 §七-6：事务边界应在 Service 层。"
                + "违反时：删掉 Controller 上的 @Transactional，把需要原子性的多步写操作"
                + "下沉成一个带 @Transactional(rollbackFor = Exception.class) 的 Service 方法。";

        ArchRule classLevel = classes()
                .that().areAnnotatedWith(RestController.class)
                .should().notBeAnnotatedWith(Transactional.class)
                .because(why);

        ArchRule methodLevel = methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .should().notBeAnnotatedWith(Transactional.class)
                .because(why);

        classLevel.check(classes);
        methodLevel.check(classes);
    }

    /**
     * 分层单向依赖：controller → service → mapper 只允许向下。
     *
     * <p>若有 Service / 工具类反向依赖 Controller，说明业务逻辑被写进了 Controller，
     * 或出现了循环依赖——后果是 Service 再也无法脱离 Web 层做单元测试。
     */
    @Test
    @DisplayName("单向依赖 — 除 Controller 外，任何类不得依赖 ..controller.. 包")
    void nothingOutsideControllerDependsOnControllers() {
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..controller..")
                .because("分层要求单向依赖：controller → service → mapper。"
                        + "违反时：把被依赖的逻辑从 Controller 抽到 Service，由 Controller 调用 Service，"
                        + "不要把 Service 反过来「回调」Controller。");
        rule.check(classes);
    }

    /**
     * 分层：Mapper 层是数据库访问**契约**，必须是接口。
     *
     * <p>MyBatis 通过 JDK 动态代理为接口生成实现；写成 class 会让 MyBatis 无法注册
     * （或意外当成普通 Bean），也把实现细节混进了契约层。
     */
    @Test
    @DisplayName("分层 — ..mapper.. 下的类型必须是接口")
    void mappersMustBeInterfaces() {
        long mapperCount = classes.stream()
                .filter(c -> c.getPackageName().contains(".mapper"))
                .count();
        assertTrue(mapperCount > 0,
                "未导入到任何 mapper 类型——规则会空转。请检查 importPackages 范围与 ..mapper.. 包名模式。");

        ArchRule rule = classes()
                .that().resideInAPackage("..mapper..")
                .should().beInterfaces()
                .because("Mapper 层是数据库访问契约，由 MyBatis 动态代理实现。"
                        + "违反时：把该类改成 interface 并继承 BaseMapper<T>。");
        rule.check(classes);
    }

    /**
     * 红线 §七-7：Entity 只对应表，且集中放在 {@code com.gjx.entity}。
     *
     * <p>实体若散落在 controller/service 包里，会助长「Controller 直接吐 Entity」的坏习惯，
     * 也让 DTO / VO / Entity 的边界彻底模糊。以 {@code @TableName} 作为「这是实体」的机器可识别标志。
     */
    @Test
    @DisplayName("红线#7 — @TableName 实体必须位于 ..entity.. 包")
    void entitiesMustLiveInEntityPackage() {
        long entityCount = classes.stream()
                .filter(c -> c.isAnnotatedWith(TableName.class))
                .count();
        assertTrue(entityCount > 0,
                "未导入到任何 @TableName 实体——规则会空转，请检查导入范围。");

        ArchRule rule = classes()
                .that().areAnnotatedWith(TableName.class)
                .should().resideInAPackage("..entity..")
                .because("红线 §七-7：Entity 只对应表。散落各处会让分层边界失效。"
                        + "违反时：把该类移到 com.gjx.entity 包。");
        rule.check(classes);
    }

    /**
     * 红线 §七-9：禁止 {@code System.out.println} / {@code System.err} / {@code printStackTrace}。
     *
     * <p>直接写标准流会绕过日志框架：没有级别、没有 traceId（本项目已有 TraceIdFilter）、
     * 无法按环境开关，还会污染 stdout。统一用 {@code @Slf4j}。
     */
    @Test
    @DisplayName("红线#9 — 禁止访问标准输出/错误流与 printStackTrace")
    void noStandardStreamAccess() {
        GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
                .because("红线 §七-9：日志用 @Slf4j，禁止 System.out.println / 禁止 printStackTrace 吞异常。"
                        + "违反时：改用 log.info/warn/error(...)，异常交给 GlobalExceptionHandler 处理。")
                .check(classes);
    }

    /**
     * 分层红线：Controller 不得直接依赖 {@code ..mapper..} 或 {@code JdbcTemplate}。
     *
     * <p>本规则曾探测到 19 处违规，全部集中在两个商家端 Controller。现已全部下沉：
     * <ul>
     *   <li>{@code admin/AdminAuditController} —— 逻辑下沉到 {@code IAdminAuditService} / {@code AdminAuditServiceImpl}（违规数 30 → 19）</li>
     *   <li>{@code merchant/MerchantOrderController} —— 下沉到 {@code IMerchantOrderService} / {@code MerchantOrderServiceImpl}（订单列表、订单详情）</li>
     *   <li>{@code merchant/MerchantDashboardController} —— 下沉到 {@code IMerchantDashboardService} /
     *       {@code MerchantDashboardServiceImpl}（5 个仪表板接口的统计编排）</li>
     * </ul>
     *
     * <p>现规则实测 **0 处违规**，已去除 {@code @Disabled} 转正。
     * 后续若再有人往 Controller 里注入 Mapper，本规则会立刻把 {@code mvn test} 拖红。
     */
    @Test
    @DisplayName("分层 — Controller 不得直接依赖 ..mapper.. 或 JdbcTemplate")
    void controllersMustNotTouchPersistenceDirectly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..mapper..", "org.springframework.jdbc.core..")
                .because("Controller 只做请求接收、参数校验、转发；数据库访问必须经 Service。"
                        + "违反时：把查询/写入逻辑下沉到 Service，Controller 只调用 Service 方法。");
        rule.check(classes);
    }
}

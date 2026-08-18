package com.zxf.hexagonal.unit;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 六边形架构守护测试：把「依赖方向永远向内」的铁律固化为自动化断言。
 *
 * <p>规则与 docs/SpringBoot六边形架构包结构设计指南.md 的依赖矩阵一一对应，
 * 任何架构腐化（domain 引入框架、application 依赖 infrastructure 等）
 * 都会在单元测试阶段（零容器、毫秒级）被拦截。</p>
 */
@AnalyzeClasses(packages = "com.zxf.hexagonal")
class HexagonalArchitectureTest {

    /** domain 零框架依赖：不引入 Spring / Jakarta / Kafka / Lombok，也不依赖任何外层。 */
    @ArchTest
    static final ArchRule domainZeroFrameworkDependency = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta..",
                    "lombok..",
                    "org.apache.kafka..",
                    "com.zxf.hexagonal.application..",
                    "com.zxf.hexagonal.infrastructure..")
            .as("domain 层必须零框架依赖且不依赖外层");

    /**
     * 领域策略（domain/service）不访问端口：它是纯规则计算，
     * 需要的事实由应用层查好后作为参数传入（见指南 §3.5/误区五）。
     * allowEmptyShould：当前尚无 domain/service 类，首个领域策略落地后规则自动生效。
     */
    @ArchTest
    static final ArchRule domainServicesArePurePolicies = noClasses()
            .that().resideInAPackage("..domain.service..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..port.out..",
                    "..port.in..",
                    "..infrastructure..")
            .allowEmptyShould(true)
            .as("领域策略不得访问端口或基础设施（事实由应用层传入）");

    /** application 不依赖 infrastructure 层。 */
    @ArchTest
    static final ArchRule applicationNotDependOnInfrastructure = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.zxf.hexagonal.infrastructure..")
            .as("application 层不得依赖 infrastructure 层");

    /**
     * application 不依赖具体基础设施技术（JPA / Web / Kafka / Boot）。
     * 允许项（不在禁表）：spring-context/spring-tx 装配注解（@Service、@Transactional）、
     * jakarta.validation 校验注解、spring-data commons 的 Page/Pageable 分页类型、Lombok。
     */
    @ArchTest
    static final ArchRule applicationNotDependOnInfrastructureTechnology = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "org.springframework.boot..",
                    "org.springframework.web..",
                    "org.springframework.data.jpa..",
                    "org.apache.kafka..")
            .as("application 层不得依赖具体基础设施技术（装配注解与分页类型除外）");

    /** 入站适配器不直接依赖出站适配器（交互必须经过 application 层编排）。 */
    @ArchTest
    static final ArchRule inboundAdaptersNotDependOnOutboundAdapters = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter.out..")
            .as("入站适配器不得直接依赖出站适配器");

    /** 出站适配器不依赖入站适配器。 */
    @ArchTest
    static final ArchRule outboundAdaptersNotDependOnInboundAdapters = noClasses()
            .that().resideInAPackage("..adapter.out..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter.in..")
            .as("出站适配器不得依赖入站适配器");

    /** Controller 只依赖入端口（UseCase），不直接触碰到出端口与出站适配器。 */
    @ArchTest
    static final ArchRule controllersOnlyDependOnInPorts = noClasses()
            .that().resideInAPackage("..adapter.in.web.controller..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..port.out..",
                    "..adapter.out..")
            .as("Controller 只依赖 port.in（UseCase），不得依赖 port.out 或出站适配器");
}

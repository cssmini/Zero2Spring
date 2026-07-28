# Zero2Spring

> 从零手写迷你 Spring 框架 —— 纯 JDK 实现 IoC / DI + AOP + MVC

[🇨🇳 中文](README.md) | [🇯🇵 日本語](README.ja.md)

---

## 目录

- [项目介绍](#项目介绍)
- [模块结构](#模块结构)
- [技术栈](#技术栈)
- [源码目录](#源码目录)
- [架构总览](#架构总览)
- [IoC 容器](#ioc-容器)
  - [启动流程](#启动流程)
  - [Bean 生命周期](#bean-生命周期)
- [AOP 切面](#aop-切面)
  - [切面注册流程](#切面注册流程)
  - [通知执行链](#通知执行链)
- [Spring MVC](#spring-mvc)
  - [MVC 启动流程](#mvc-启动流程)
  - [请求处理流程](#请求处理流程)
- [核心注解速查](#核心注解速查)
- [快速开始](#快速开始)
- [设计要点](#设计要点)

---

## 项目介绍

Zero2Spring 是一个**纯 JDK 手写实现**的迷你 Spring 框架，仅使用 Java 反射和动态代理，完整复现 Spring 三大核心能力：

| 能力 | 包路径 | 说明 |
|------|--------|------|
| **IoC / DI** | `com.spring.core` | 控制反转容器 + 依赖注入 |
| **AOP** | `com.spring.aop` | @Before / @After / @Around 切面编程 |
| **MVC** | `com.spring.mvc` | DispatcherServlet + 注解驱动 |

---

## 模块结构

```
Zero2Spring/ (父 POM)
│
├── zero-spring-core/          ← 框架核心 (jar)
│   ├── com.spring.core.*      IoC/DI 容器
│   ├── com.spring.aop.*       AOP 切面框架
│   └── com.spring.mvc.*       MVC 前端控制器
│
├── zero-test-ioc/             ← IoC 测试 (jar, → core)
├── zero-test-mvc/             ← MVC 测试 (war, → core)
└── zero-test-aop/             ← AOP 测试 (jar, → core)
```

```
zero-spring-core ─────────────────────────────┐
  ↑              ↑              ↑             │
  │              │              │             │
zero-test-ioc  zero-test-mvc  zero-test-aop    │
                                              │
javax.servlet-api  jackson-databind  commons-lang3
(provided)         (JSON序列化)      (工具库)
```

---

## 技术栈

- **JDK** 1.8+
- **构建** Maven 多模块
- **Java API** `java.lang.reflect.Proxy` / `InvocationHandler` / Reflection
- **MVC 依赖** `javax.servlet-api` (provided) / `jackson-databind` / `commons-lang3`

---

## 源码目录

```
zero-spring-core/src/main/java/com/spring/
│
├── core/                           ← IoC/DI 容器
│   ├── ApplicationContext.java     # 容器入口
│   ├── annotation/
│   │   ├── Component.java          # @Component
│   │   ├── ComponentScan.java      # @ComponentScan (String[])
│   │   ├── Autowired.java          # @Autowired
│   │   └── Scope.java              # @Scope(singleton/prototype)
│   ├── service/
│   │   ├── BeanPostProcessor.java  # 后处理器接口
│   │   ├── BeanNameAware.java      # BeanName 感知
│   │   └── InitializingBean.java   # 初始化回调
│   ├── entity/
│   │   └── BeanDefinition.java     # Bean 元数据
│   └── BeanPostProcessor/
│       └── Value.java              # @Value
│
├── aop/                            ← AOP 切面
│   ├── Aspect.java                 # @Aspect (元标注 @Component)
│   ├── Before.java                 # @Before
│   ├── After.java                  # @After (finally 语义)
│   ├── Around.java                 # @Around
│   ├── JoinPoint.java              # 连接点
│   └── framework/
│       ├── Advice.java             # 通知封装
│       └── AspectBeanPostProcessor # AOP 核心处理器
│
└── mvc/                            ← MVC 前端控制器
    ├── annotation/
    │   ├── Controller.java         # @Controller (元标注 @Component)
    │   ├── RequestMapping.java     # @RequestMapping
    │   └── ResponseBody.java       # @ResponseBody
    ├── context/
    │   └── WebApplicationContext   # MVC IoC 容器
    ├── handler/
    │   └── MyHandler.java          # URL → Method 映射
    └── servlet/
        └── DispatcherServlet.java  # 前端控制器
```

---

## 架构总览

```
┌──────────────────────────────────────────────────────────────┐
│                     zero-spring-core                          │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  ApplicationContext (IoC 容器 / core)                    │ │
│  │                                                          │ │
│  │  初始化: scan() → doScan() → registerClass()             │ │
│  │  生命周期: 实例化 → @Autowired → Aware → BP → init → BP │ │
│  │  存储:     beanDefinitionMap / singletonObjects          │ │
│  └────────────────────────┬────────────────────────────────┘ │
│                           │                                   │
│  ┌────────────────────────▼────────────────────────────────┐ │
│  │  AspectBeanPostProcessor (AOP / aop)                     │ │
│  │                                                          │ │
│  │  扫描 @Aspect → parseAspect() → before/after/around      │ │
│  │  匹配 Bean  → JDK 动态代理 → AopProxy                    │ │
│  └────────────────────────┬────────────────────────────────┘ │
│                           │                                   │
│  ┌────────────────────────▼────────────────────────────────┐ │
│  │  WebApplicationContext + DispatcherServlet (MVC / mvc)   │ │
│  │                                                          │ │
│  │  复用 core ApplicationContext 进行 IoC                   │ │
│  │  扫描 @Controller → initHandlerMapping()                 │ │
│  │  请求分发: URL → Handler → invoke() → 视图/JSON          │ │
│  └─────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

---

## IoC 容器

### 启动流程

```
                 new ApplicationContext(AppConfig.class)
                              │
                              ▼
              ┌───────────────────────────────┐
              │ scan(configClass)              │
              │                               │
              │ ① 读取 @ComponentScan         │
              │    value() → String[] 多包     │
              │    遍历每个包路径               │
              └───────────────┬───────────────┘
                              │
                              ▼
              ┌───────────────────────────────┐
              │ ② doScan(rootDir, pkg) [递归] │
              │    过滤 .class 文件            │
              │    区分目录递归 / 文件加载     │
              └───────────────┬───────────────┘
                              │
                              ▼
              ┌───────────────────────────────┐
              │ ③ registerClass(className)    │
              │                               │
              │   检查 @Component              │
              │        @Controller            │
              │        @Aspect                │
              │                               │
              │   BeanPostProcessor → 提前注册│
              │   其他 → 注册 BeanDefinition   │
              └───────────────┬───────────────┘
                              │
                              ▼
              ┌───────────────────────────────┐
              │ ④ 遍历 beanDefinitionMap      │
              │    scope="singleton"           │
              │    → createBean()              │
              │    → singletonObjects          │
              └───────────────────────────────┘
```

### Bean 生命周期

```
                  getBean(beanName)
                        │
            ┌───────────▼───────────┐
            │ singletonObjects 有？ │
            ├── 是 → 返回           │
            └── 否 → createBean()   │
                        │
        ════════════ createBean ════════════
                        │
        ┌───────────────┼───────────────┐
        │ singleton 缓存    prototype 新建 │
        └───────────────┼───────────────┘
                        │
                        ▼
        ┌───────────────────────────────┐
        │ ① 反射实例化                   │
        │    clazz.getConstructor()      │
        │      .newInstance()            │
        └───────────────┬───────────────┘
                        │
                        ▼
        ┌───────────────────────────────┐
        │ ② 依赖注入 @Autowired         │
        │    field.set(instance,        │
        │      getBean(field.getName()))│
        └───────────────┬───────────────┘
                        │
                        ▼
        ┌───────────────────────────────┐
        │ ③ BeanNameAware.setBeanName() │
        └───────────────┬───────────────┘
                        │
                        ▼
        ┌───────────────────────────────┐
        │ ④ BeanPostProcessor           │
        │    postProcessBeforeInit()    │
        │    (可返回代理对象)            │
        └───────────────┬───────────────┘
                        │
                        ▼
        ┌───────────────────────────────┐
        │ ⑤ InitializingBean            │
        │    afterPropertiesSet()       │
        └───────────────┬───────────────┘
                        │
                        ▼
        ┌───────────────────────────────┐
        │ ⑥ BeanPostProcessor           │
        │    postProcessAfterInit()     │
        │    ★ AOP 代理在此创建！        │
        └───────────────┬───────────────┘
                        │
                        ▼
                ┌───────────────┐
                │ 最终 Bean     │
                │ (可能为代理)  │
                └───────────────┘
```

---

## AOP 切面

### 切面注册流程

```
    AspectBeanPostProcessor
    .postProcessAfterInitialization(bean, beanName)
                          │
         ┌────────────────▼────────────────┐
         │ bean 上有 @Aspect 注解？          │
         ├──── 是 ──────────────┬── 否 ────┤
         │                      │          │
         ▼                      │          ▼
  ┌──────────────────┐          │  ┌──────────────────┐
  │ parseAspect(bean) │          │  │ 通知列表为空？    │
  │                  │          │  ├── 是 → 返回原Bean │
  │ 遍历方法:         │          │  └── 否 ↓          │
  │  @Before → list  │          │        │            │
  │  @After  → list  │          │        ▼            │
  │  @Around → list  │          │  ┌──────────────────┐
  └──────────────────┘          │  │ wrapIfNecessary() │
                                │  │                  │
   parsedAspects 防重复         │  │ 有接口？ 切点匹配？│
   返回切面本身(不代理)         │  │ ↓                │
                                │  │ Proxy.newProxy.. │
                                │  │ (AopProxy处理器) │
                                │  └──────────────────┘
```

### 通知执行链

```
    AopProxy.invoke(proxy, method, args)
                    │
                    ▼
          ┌────────────────────┐
          │ 创建 JoinPoint(jp) │
          │ 封装 target/method │
          │ /args              │
          └─────────┬──────────┘
                    │
                    ▼
          ┌────────────────────┐
          │ filter() 筛选通知  │
          │ matchedBefore      │
          │ matchedAround      │
          │ matchedAfter       │
          └─────────┬──────────┘
                    │
        ┌───────────▼───────────────────────────┐
        │  try {                                 │
        │    ① @Before 链执行                    │
        │       for each → invokeAdvice(a, jp)   │
        │                                        │
        │    ② @Around 链 + 目标方法              │
        │       chain(jp, list, 0)               │
        │       ┌──────────────────────┐         │
        │       │ Around[0](nextJp1)    │         │
        │       │   nextJp1.proceed() →│         │
        │       │   Around[1](nextJp2) │         │
        │       │     nextJp2.proceed()│         │
        │       │     → 原始方法       │         │
        │       └──────────────────────┘         │
        │                                        │
        │  } finally {                            │
        │    ③ @After 链执行(异常也保证调用)      │
        │       for each → invokeAdvice(a, jp)   │
        │  }                                      │
        └──────────────────────────────────────────┘
```

---

## Spring MVC

### MVC 启动流程

```
        DispatcherServlet.init()
                  │
                  ▼
    ┌──────────────────────────────┐
    │ ① 读取 web.xml               │
    │    contextConfigLocation     │
    │    = com.cssmini.MvcAppConfig│
    └──────────────┬───────────────┘
                   │
                   ▼
    ┌──────────────────────────────┐
    │ ② new WebApplicationContext  │
    │    内部创建 ApplicationContext│ ← 复用 core IoC!
    │                              │
    │    → 扫描 @ComponentScan 包  │
    │    → 实例化所有 Bean         │
    │    → @Autowired 依赖注入     │
    └──────────────┬───────────────┘
                   │
                   ▼
    ┌──────────────────────────────┐
    │ ③ initHandlerMapping()      │
    │    遍历注册的所有 Bean        │
    │    查找 @Controller          │
    │    收集 @RequestMapping 方法 │
    │    建立 URL → Handler 映射   │
    └──────────────────────────────┘
```

### 请求处理流程

```
    GET /user/hello
          │
          ▼
  ┌───────────────────┐
  │ DispatcherServlet │
  │ doGet / doPost    │
  └─────────┬─────────┘
            │
            ▼
  ┌───────────────────┐
  │ doDispatcher()    │
  │ getHandler(uri)   │
  │ 匹配 URL→Handler  │
  └─────────┬─────────┘
            │
    ┌───────▼───────┐
    │ Handler 存在？ │
    ├── 否 → 404    │
    └── 是 ↓       │
            │       │
            ▼       │
  ┌───────────────────┐
  │ method.invoke()   │
  │ 调用 Controller   │
  └─────────┬─────────┘
            │
    ┌───────▼──────────────┐
    │ 返回值类型？           │
    │                      │
    ├── @ResponseBody      │
    │   → Jackson JSON     │
    │                      │
    ├── "forward:/xxx"     │
    │   → req.getRequest-  │
    │     Dispatcher()     │
    │                      │
    ├── "redirect:/xxx"    │
    │   → resp.sendRedirect│
    │                      │
    └── 默认 → forward     │
        req.getRequest-    │
        Dispatcher()       │
```

---

## 核心注解速查

### IoC (com.spring.core.annotation)

| 注解 | 位置 | 说明 |
|------|------|------|
| `@Component("name")` | 类 | Bean 注册 |
| `@ComponentScan({"p1","p2"})` | 类 | 包扫描 (String[]) |
| `@Autowired` | 字段 | 按字段名注入 |
| `@Scope("singleton"\|"prototype")` | 类 | 作用域 |
| `@Value("str")` | 字段 | 字符串注入 |

### AOP (com.spring.aop)

| 注解 | 位置 | 说明 |
|------|------|------|
| `@Aspect` | 类 | 切面标记 (元标注 @Component) |
| `@Before("execution(...)")` | 方法 | 前置通知 |
| `@After("execution(...)")` | 方法 | 后置通知 (finally) |
| `@Around("execution(...)")` | 方法 | 环绕通知 |

### MVC (com.spring.mvc.annotation)

| 注解 | 位置 | 说明 |
|------|------|------|
| `@Controller` | 类 | 控制器 (元标注 @Component) |
| `@RequestMapping("/url")` | 方法 | URL 映射 |
| `@ResponseBody` | 方法 | 返回 JSON |

### 生命周期接口 (com.spring.core.service)

| 接口 | 方法 | 时机 |
|------|------|------|
| `BeanPostProcessor` | `postProcessBefore/After` | 初始化前后 |
| `BeanNameAware` | `setBeanName(name)` | 注入后 |
| `InitializingBean` | `afterPropertiesSet()` | 初始化 |

---

## 快速开始

```bash
# 编译全量
mvn clean compile

# 运行 IoC 测试
# IDE: 运行 zero-test-ioc → com.cssmini.test.main()

# 运行 MVC 测试 (Jetty)
cd zero-test-mvc
mvn jetty:run
# 访问 http://localhost:8088/user/hello

# 运行 AOP 测试
# IDE: 运行 zero-test-aop → com.cssmini.aop.AopTest.main()
```

### 输出示例 (AOP)

```
[AOP] @Before : LogAspect.beforeLog → execution(* com.cssmini.aop..*.*(..))
[AOP] @After  : LogAspect.afterLog → ...
[AOP] @Around : LogAspect.aroundAdd → ...
[AOP] 创建代理: CalculatorImpl
=== add(3, 5) ===
  [@Before] >> add
  [@Around] >>> add
  [@Around] <<< add result=8
  [@After]  << add
add result: 8
```

---

## 设计要点

| 要点 | 说明 |
|------|------|
| **零外部依赖** | IoC/DI 部分纯 JDK 反射，不依赖任何第三方库 |
| **JDK 动态代理** | AOP 基于 `Proxy.newProxyInstance`，目标必须实现接口 |
| **递归包扫描** | `doScan()` 递归遍历目录，支持多级子包 |
| **后处理器优先** | `BeanPostProcessor` 在扫描阶段提前实例化注册 |
| **finally 语义** | `@After` 在 `try-finally` 中执行，异常也保证调用 |
| **环绕链递归** | 多个 `@Around` 通过匿名 `JoinPoint` 子类覆盖 `proceed()` 实现链式调用 |
| **元注解复用** | `@Controller` / `@Aspect` 元标注 `@Component`，core 容器统一识别 |
| **MVC 复用 IoC** | `WebApplicationContext` 委托 `ApplicationContext`，不重复扫描 |

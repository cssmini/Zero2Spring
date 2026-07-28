# zero-test-aop

> Spring AOP 切面编程测试项目 —— 演示 @Before / @After / @Around 三种通知

[🇨🇳 中文](README.md) | [🇯🇵 日本語](README.ja.md)

---

## 依赖关系

```
zero-test-aop → zero-spring-core (jar)
```

---

## 源码结构

```
zero-test-aop/src/main/java/com/cssmini/aop/
│
├── AopAppConfig.java              ← 配置类 @ComponentScan
├── AopTest.java                   ← main() 启动入口
│
├── aspect/
│   └── LogAspect.java             ← @Aspect 切面 (三种通知)
│
└── service/
    ├── Calculator.java            ← 接口
    └── impl/
        └── CalculatorImpl.java    ← @Component (AOP 代理目标)
```

---

## 运行方式

IDEA 中右键 `com.cssmini.aop.AopTest.java` → **Run 'AopTest.main()'**

```java
public class AopTest {
    public static void main(String[] args) {
        ApplicationContext ctx = new ApplicationContext(AopAppConfig.class);
        Calculator calc = (Calculator) ctx.getBean("calculator");

        calc.add(3, 5);    // 触发 @Before + @Around + @After
        calc.div(10, 2);   // 触发 @Before + @After（无 @Around 匹配）
    }
}
```

---

## 预期输出

```
[AOP] @Before : LogAspect.beforeLog → execution(* com.cssmini.aop..*.*(..))
[AOP] @After  : LogAspect.afterLog → execution(* com.cssmini.aop..*.*(..))
[AOP] @Around : LogAspect.aroundAdd → execution(* com.cssmini.aop..*.add(..))
[AOP] 创建代理: CalculatorImpl

=== add(3, 5) ===
  [@Before] >> add
  [@Around] >>> add
  [@Around] <<< add result=8
  [@After]  << add
add result: 8

=== div(10, 2) ===
  [@Before] >> div
  [@After]  << div
div result: 5
```

观察输出可知：
- `add()` 触发了全部 3 种通知（@Before → @Around → @After）
- `div()` 只触发 @Before + @After（@Around 的切点只匹配 `add` 方法）

---

## 各文件职责

### 1. AopAppConfig.java — 配置类

```java
@ComponentScan("com.cssmini.aop")
public class AopAppConfig {
}
```

### 2. Calculator.java — 接口（必须，JDK 代理前提）

```java
public interface Calculator {
    int add(int a, int b);
    int div(int a, int b);
}
```

### 3. CalculatorImpl.java — 目标 Bean

```java
@Component("calculator")
public class CalculatorImpl implements Calculator {
    @Override public int add(int a, int b) { return a + b; }
    @Override public int div(int a, int b) { return a / b; }
}
```

Proxy 对象类型为 `$Proxy0 implements Calculator`（JDK 动态代理）。

### 4. LogAspect.java — 切面定义（核心）

```java
@Aspect                          // 元标注 @Component → 自动注册为 Bean
public class LogAspect {

    @Before("execution(* com.cssmini.aop..*.*(..))")
    public void beforeLog(JoinPoint jp) {
        System.out.println("  [@Before] >> " + jp.getMethodName());
    }

    @After("execution(* com.cssmini.aop..*.*(..))")
    public void afterLog(JoinPoint jp) {
        System.out.println("  [@After]  << " + jp.getMethodName());
    }

    @Around("execution(* com.cssmini.aop..*.add(..))")
    public Object aroundAdd(JoinPoint jp) throws Throwable {
        System.out.println("  [@Around] >>> " + jp.getMethodName());
        Object result = jp.proceed();       // ★ 驱动执行链，调用下一个通知或目标方法
        System.out.println("  [@Around] <<< " + jp.getMethodName() + " result=" + result);
        return result;
    }
}
```

---

## AOP 执行时序

以 `calc.add(3,5)` 为例：

```
调用 calc.add(3,5)          ← calc 是 $Proxy0 代理对象
    │
    ▼
AopProxy.invoke()           ← InvocationHandler 拦截
    │
    ├── filter(beforeAdvices, method)
    ├── filter(aroundAdvices, method)     ← 切点表达式匹配
    ├── filter(afterAdvices, method)
    │
    ├── try {
    │       ① @Before: beforeLog(jp)     ──────── 输出 [@Before] >> add
    │
    │       ② @Around 链:
    │          aroundAdd(jp)             ──────── 输出 [@Around] >>> add
    │            └── jp.proceed()
    │                  │
    │                  ▼
    │            CalculatorImpl.add(3,5)  ──────── 原始方法 3+5=8
    │                  │
    │            ←── 返回 8
    │          ──────── 输出 [@Around] <<< add result=8
    │          ←── 返回 8
    │
    │   } finally {
    │       ③ @After: afterLog(jp)       ──────── 输出 [@After] << add
    │   }
    │
    ▼
返回 8
```

---

## 切点表达式语法

```
execution(* pkg..Class.method(params))

例:
  execution(* com.cssmini.aop..*.*(..))
  │          │                    │  │
  │          包路径(..表示递归子包)   │  参数(..任意)
  返回值(*任意)              方法名(*任意)

  → 匹配 com.cssmini.aop 包下所有类的所有方法

  execution(* com.cssmini.aop..*.add(..))
  → 匹配 com.cssmini.aop 包下所有类的 add 方法
```

| 表达式 | 匹配 |
|--------|------|
| `..*.add(..)` | 所有包的 add 方法 |
| `..*.*(..)` | 所有包的所有方法 |
| `com.service.*(..)` | com.service 包的任意方法 |

---

## AOP 核心组件回顾

```
AspectBeanPostProcessor (implements BeanPostProcessor)
    │
    │  postProcessAfterInitialization()
    │
    ├── 遇到 @Aspect 类
    │   └── parseAspect() → 提取 @Before/@After/@Around → 存入 Advice 列表
    │
    └── 遇到普通 Bean
        └── wrapIfNecessary()
            ├── 检查是否有接口？（无接口 → JDK代理不可用 → 跳过）
            ├── 切点表达式匹配类名？（不匹配 → 跳过）
            └── Proxy.newProxyInstance(loader, interfaces, new AopProxy(target, processor))
```

**AopProxy** 是 `InvocationHandler` 的实现，拦截每个方法调用，按 `@Before → @Around链 → 目标方法 → @After` 顺序执行。

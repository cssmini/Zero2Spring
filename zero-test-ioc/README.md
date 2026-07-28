# zero-test-ioc

> Spring IoC/DI 容器测试项目 —— 演示控制反转与依赖注入

---

## 依赖关系

```
zero-test-ioc → zero-spring-core (jar)
```

---

## 源码结构

```
zero-test-ioc/src/main/java/com/cssmini/
│
├── AppConfig.java                ← 配置类 @ComponentScan
├── test.java                     ← main() 启动入口
│
└── service/
    ├── UserService.java          ← 接口
    ├── OrderService.java         ← 接口
    └── Impl/
        ├── UserServiceImpl.java          ← @Component + @Autowired + @Value
        ├── OrderServiceImpl.java         ← 简单 Bean
        ├── TestBeanPostProcessor.java    ← 自定义 BeanPostProcessor (JDK 代理)
        └── ValueBeanPostProcessor.java   ← 自定义 BeanPostProcessor (@Value 注入)
```

---

## 运行方式

IDEA 中右键 `com.cssmini.test.java` → **Run 'test.main()'**

```java
public class test {
    public static void main(String[] args) {
        ApplicationContext ctx = new ApplicationContext(AppConfig.class);
        UserService userService = (UserService) ctx.getBean("userService");
        userService.test();
    }
}
```

---

## 预期输出

```
Autowired:      ← @Autowired 注入 OrderService
BeanNameAware:  ← setBeanName 回调
set value:Value xxx    ← @Value 注入
Post Process After Initialization   ← TestBeanPostProcessor 代理
UserServiceImpl setBeanName:userService
UserServiceImpl afterPropertiesSet
UserServiceImpl test
Value:Value xxx
OrderServiceImpl test
```

---

## 各文件职责

### 1. AppConfig.java — 配置类

```java
@ComponentScan("com.cssmini.service.Impl")
public class AppConfig {
}
```

唯一的作用是告诉容器扫描 `com.cssmini.service.Impl` 包。

### 2. test.java — 启动入口

```java
ApplicationContext ctx = new ApplicationContext(AppConfig.class);
UserService userService = (UserService) ctx.getBean("userService");
userService.test();
```

三步：创建容器 → 获取 Bean → 调用方法。`getBean("userService")` 实际返回的是 `TestBeanPostProcessor` 创建的 JDK 代理对象。

### 3. UserServiceImpl.java — 最完整的 Bean 示例

```java
@Component("userService")
public class UserServiceImpl implements UserService, BeanNameAware, InitializingBean {

    @Autowired                              // 按字段名自动注入
    private OrderService orderService;

    @Value("Value xxx")                     // 字符串注入
    private String test;

    @Override
    public void setBeanName(String name) {} // 感知自己的 beanName

    @Override
    public void afterPropertiesSet() {}     // 初始化回调

    @Override
    public void test() {}                   // 业务方法
}
```

该 Bean 经历完整的 **6 阶段生命周期**：
```
实例化 → @Autowired注入 → BeanNameAware → BP前置 → InitializingBean → BP后置(代理)
```

### 4. TestBeanPostProcessor.java — JDK 代理示例

```java
@Component
public class TestBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if ("userService".equals(beanName)) {
            return Proxy.newProxyInstance(/* ... */, (proxy, method, args) -> {
                System.out.println("代理切面日志");
                return method.invoke(bean, args);
            });
        }
        return bean;
    }
}
```

`BeanPostProcessor` 是容器最核心的扩展点。这里为 `userService` 创建 JDK 代理，在方法调用前后打印日志。所有 `BeanPostProcessor` 实现类会在扫描阶段**提前注册**。

### 5. ValueBeanPostProcessor.java — @Value 注入

```java
@Component
public class ValueBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Value.class)) {
                field.set(bean, field.getAnnotation(Value.class).value());
            }
        }
        return bean;
    }
}
```

在 `postProcessBeforeInitialization` 阶段扫描 `@Value` 字段并注入字符串值。选择前置阶段是因为属性注入应该发生在 `afterPropertiesSet()` 之前。

### 6. OrderServiceImpl.java — 简单 Bean

```java
@Component("orderService")
public class OrderServiceImpl implements OrderService {
    @Override
    public void test() {
        System.out.println("OrderServiceImpl test");
    }
}
```

最简 Bean 示例：仅 `@Component` + 实现接口，容器自动注册为 `"orderService"`。

---

## IoC 核心流程回顾

```
ApplicationContext 构造
    │
    ├── scan(configClass)
    │   ├── 读取 @ComponentScan("com.cssmini.service.Impl")
    │   ├── 递归扫描目录 → 加载 .class → 检查 @Component
    │   ├── BeanPostProcessor 实现类 → 立即实例化注册
    │   └── 其他 → 注册 BeanDefinition(type, scope)
    │
    └── 实例化单例
        └── for each singleton → createBean()
            ├── ① 反射 newInstance()
            ├── ② @Autowired 字段注入 (递归 getBean)
            ├── ③ BeanNameAware.setBeanName()
            ├── ④ postProcessBeforeInitialization()  ← ValueBeanPostProcessor
            ├── ⑤ InitializingBean.afterPropertiesSet()
            └── ⑥ postProcessAfterInitialization()   ← TestBeanPostProcessor (代理!)
```

---

## BeanPostProcessor 注册时序

| 阶段 | ValueBeanPostProcessor | TestBeanPostProcessor |
|------|----------------------|----------------------|
| scan() | 提前实例化加入 BP 列表 | 提前实例化加入 BP 列表 |
| ④ before | **@Value 注入** | 不处理 |
| ⑥ after | 不处理 | **创建 JDK 代理** |

两个后处理器按注册顺序串行执行，互不干扰。

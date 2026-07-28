# zero-test-ioc

> Spring IoC/DI コンテナ(container)テスト(test) —— 制御の反転と依存性注入のデモ(demo)

[🇨🇳 中文](README.md) | [🇯🇵 日本語](README.ja.md)

---

## 依存関係

```
zero-test-ioc → zero-spring-core (jar)
```

---

## ソース構成

```
zero-test-ioc/src/main/java/com/cssmini/
├── AppConfig.java              ← @ComponentScan 設定
├── test.java                   ← main() エントリポイント(entry point)
└── service/
    ├── UserService.java        ← インターフェース(interface)
    ├── OrderService.java       ← インターフェース(interface)
    └── Impl/
        ├── UserServiceImpl.java        ← @Component + @Autowired + @Value
        ├── OrderServiceImpl.java       ← シンプル(simple) Bean
        ├── TestBeanPostProcessor.java  ← JDK プロキシ(proxy)例
        └── ValueBeanPostProcessor.java ← @Value 注入処理
```

---

## 実行方法

IDE で `com.cssmini.test.java` → **Run**

```java
ApplicationContext ctx = new ApplicationContext(AppConfig.class);
UserService userService = (UserService) ctx.getBean("userService");
userService.test();
```

---

## 想定出力

```
Autowired:              ← OrderService 注入
BeanNameAware:          ← setBeanName コールバック(callback)
set value:Value xxx     ← @Value 注入
Post Process After Initialization   ← プロキシ(proxy)ログ(log)
UserServiceImpl setBeanName:userService
UserServiceImpl afterPropertiesSet
UserServiceImpl test
Value:Value xxx
OrderServiceImpl test
```

---

## 各ファイルの役割

### AppConfig.java

```java
@ComponentScan("com.cssmini.service.Impl")
public class AppConfig { }
```

### UserServiceImpl — 完全な Bean

```java
@Component("userService")
public class UserServiceImpl implements UserService,
        BeanNameAware, InitializingBean {

    @Autowired private OrderService orderService;  // フィールド(field)名で注入
    @Value("Value xxx") private String test;       // 文字列注入

    @Override public void setBeanName(String n) { }
    @Override public void afterPropertiesSet() { }
    @Override public void test() { }
}
```

**6 段階ライフサイクル(lifecycle)**: インスタンス(instance)化 → @Autowired → BeanNameAware → BP前置 → InitializingBean → BP後置

### TestBeanPostProcessor — JDK プロキシ(proxy)

```java
@Component
public class TestBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String name) {
        if ("userService".equals(name)) {
            return Proxy.newProxyInstance(/*...*/,
                (p, m, a) -> {
                    System.out.println("プロキシ(proxy)ログ(log)");
                    return m.invoke(bean, a);
                });
        }
        return bean;
    }
}
```

### ValueBeanPostProcessor — @Value 注入

```java
@Component
public class ValueBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String name) {
        for (Field f : bean.getClass().getDeclaredFields())
            if (f.isAnnotationPresent(Value.class))
                f.set(bean, f.getAnnotation(Value.class).value());
        return bean;
    }
}
```

### OrderServiceImpl — シンプル(simple) Bean

```java
@Component("orderService")
public class OrderServiceImpl implements OrderService {
    @Override public void test() { System.out.println("OrderServiceImpl test"); }
}
```

---

## IoC コアフロー(core flow)

```
ApplicationContext コンストラクタ(constructor)
    ├── scan(configClass)
    │   ├── @ComponentScan のパッケージ(package)を走査
    │   ├── .class 再帰ロード(load) → @Component チェック(check)
    │   ├── BeanPostProcessor → 事前インスタンス(instance)化 & リスト(list)登録
    │   └── その他 → BeanDefinition 登録
    └── シングルトン(singleton)インスタンス(instance)化
        └── createBean()
            ├── ① newInstance()
            ├── ② @Autowired 注入
            ├── ③ setBeanName()
            ├── ④ BP.before()  ← ValueBeanPostProcessor
            ├── ⑤ afterPropertiesSet()
            └── ⑥ BP.after()   ← TestBeanPostProcessor (プロキシ(proxy)!)
```

---

## BeanPostProcessor 登録タイミング(timing)

| 段階 | ValueBeanPostProcessor | TestBeanPostProcessor |
|------|----------------------|----------------------|
| scan() | 事前登録 | 事前登録 |
| ④ before | **@Value 注入** | — |
| ⑥ after | — | **JDK プロキシ(proxy)作成** |

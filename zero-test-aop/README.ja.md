# zero-test-aop

> Spring AOP アスペクト(aspect)テスト(test) —— @Before / @After / @Around の 3 種の通知をデモ(demo)

[🇨🇳 中文](README.md) | [🇯🇵 日本語](README.ja.md)

---

## 依存関係

```
zero-test-aop → zero-spring-core (jar)
```

---

## ソース構成

```
zero-test-aop/src/main/java/com/cssmini/aop/
├── AopAppConfig.java            ← @ComponentScan 設定
├── AopTest.java                 ← main() エントリポイント(entry point)
├── aspect/
│   └── LogAspect.java           ← @Aspect（3 種通知）
└── service/
    ├── Calculator.java          ← インターフェース(interface)
    └── impl/
        └── CalculatorImpl.java  ← @Component（プロキシ(proxy)対象）
```

---

## 実行方法

IDE で `AopTest.main()` → **Run**

```java
Calculator calc = (Calculator) ctx.getBean("calculator");
calc.add(3, 5);   // @Before + @Around + @After
calc.div(10, 2);  // @Before + @After のみ
```

---

## 想定出力

```
[AOP] @Before : LogAspect.beforeLog → ...
[AOP] @After  : LogAspect.afterLog → ...
[AOP] @Around : LogAspect.aroundAdd → ...
[AOP] プロキシ(proxy)作成: CalculatorImpl

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

---

## 各ファイルの役割

### Calculator.java — インターフェース(interface)

```java
public interface Calculator {
    int add(int a, int b);
    int div(int a, int b);
}
```

### CalculatorImpl.java — ターゲット(target)

```java
@Component("calculator")
public class CalculatorImpl implements Calculator { /* ... */ }
```

### LogAspect.java — アスペクト(aspect)

```java
@Aspect  // @Component メタ(meta)付与 → 自動 Bean 登録
public class LogAspect {

    @Before("execution(* com.cssmini.aop..*.*(..))")
    public void beforeLog(JoinPoint jp) { }

    @After("execution(* com.cssmini.aop..*.*(..))")
    public void afterLog(JoinPoint jp) { }

    @Around("execution(* com.cssmini.aop..*.add(..))")
    public Object aroundAdd(JoinPoint jp) throws Throwable {
        Object result = jp.proceed();  // ★ チェーン(chain)駆動
        return result;
    }
}
```

---

## AOP 実行タイミング(timing)

```
calc.add(3,5) 呼出し  ← calc は $Proxy0 プロキシ(proxy)
    │
    ▼
AopProxy.invoke()     ← InvocationHandler インターセプト(intercept)
    │
    ├── try {
    │       ① @Before: beforeLog(jp)        → [@Before] >> add
    │       ② @Around: aroundAdd(jp)        → [@Around] >>> add
    │            └── jp.proceed()
    │                  → CalculatorImpl.add(3,5) → 3+5=8
    │          return 8                      → [@Around] <<< add result=8
    │   } finally {
    │       ③ @After: afterLog(jp)           → [@After] << add
    │   }
    ▼
return 8
```

---

## ポイントカット(pointcut)式構文

```
execution(* pkg..Class.method(params))

例: execution(* com.cssmini.aop..*.*(..))
    → com.cssmini.aop 配下の全クラス(class)全メソッド(method)にマッチ(match)

    execution(* com.cssmini.aop..*.add(..))
    → com.cssmini.aop 配下の add メソッド(method)にのみマッチ(match)
```

| 式 | マッチ(match) |
|----|------|
| `..*.add(..)` | 全パッケージ(package)の add |
| `..*.*(..)` | 全パッケージ(package)の全メソッド(method) |
| `com.service.*(..)` | com.service 直下の任意 |

---

## AOP コアコンポーネント(core component)

```
AspectBeanPostProcessor (BeanPostProcessor 実装)
    │  postProcessAfterInitialization()
    ├── @Aspect 検出 → parseAspect() → Advice リスト(list)
    └── 通常 Bean → wrapIfNecessary()
        ├── インターフェース(interface)あり？切点マッチ(match)？
        └── Proxy.newProxyInstance(loader, interfaces, new AopProxy(...))
```

**AopProxy** = `InvocationHandler` 実装。全メソッド(method)呼出しをインターセプト(intercept)し `@Before → @Aroundチェーン(chain) → 対象メソッド(method) → @After` の順で実行。

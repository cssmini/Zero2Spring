# Zero2Spring

> Spring フレームワーク(framework)をゼロから手書き実装 —— 純粋な JDK で IoC/DI + AOP + MVC を再現

[🇨🇳 中文](README.md) | [🇯🇵 日本語](README.ja.md)

---

## プロジェクト(project)紹介

Zero2Spring は**サードパーティ依存ゼロ**で構築されたミニ Spring フレームワーク(framework)です。Java リフレクション(reflection)と動的プロキシ(proxy)のみを使用し、Spring の 3 つのコア機能を完全再現しています。

| 機能 | パッケージ(package) | 説明 |
|------|-----------|------|
| **IoC / DI** | `com.spring.core` | 制御の反転コンテナ(container) + 依存性注入 |
| **AOP** | `com.spring.aop` | @Before / @After / @Around アスペクト(aspect) |
| **MVC** | `com.spring.mvc` | DispatcherServlet + アノテーション(annotation)駆動 |

---

## モジュール(module)構成

```
Zero2Spring/ (親 POM)
├── zero-spring-core/     ← フレームワークコア(framework core) (jar)
├── zero-test-ioc/        ← IoC テスト(test) (jar)
├── zero-test-mvc/        ← MVC テスト(test) (war)
└── zero-test-aop/        ← AOP テスト(test) (jar)
```

```
zero-spring-core ─────────────────────────────┐
  ↑              ↑              ↑             │
  │              │              │             │
zero-test-ioc  zero-test-mvc  zero-test-aop    │
                                              │
javax.servlet-api  jackson-databind  commons-lang3
(provided)         (JSONシリアル化(serialization))  (ユーティリティ(utility))
```

---

## 技術スタック(stack)

- **JDK** 1.8+
- **ビルド(build)** Maven マルチモジュール(multi-module)
- **コア API** `java.lang.reflect.Proxy` / `InvocationHandler` / リフレクション(reflection)

---

## ソース構成

```
zero-spring-core/src/main/java/com/spring/
├── core/                       ← IoC/DI コンテナ(container)
│   ├── ApplicationContext.java
│   ├── annotation/             (@Component,@ComponentScan,@Autowired,@Scope)
│   ├── service/                (BeanPostProcessor,BeanNameAware,InitializingBean)
│   ├── entity/                 (BeanDefinition)
│   └── BeanPostProcessor/      (@Value)
├── aop/                        ← AOP アスペクト(aspect)
│   ├── Aspect.java             (@Aspect,@Before,@After,@Around,JoinPoint)
│   └── framework/              (Advice,AspectBeanPostProcessor)
└── mvc/                        ← MVC フロントコントローラ(front controller)
    ├── annotation/             (@Controller,@RequestMapping,@ResponseBody)
    ├── context/                (WebApplicationContext)
    ├── handler/                (MyHandler)
    └── servlet/                (DispatcherServlet)
```

---

## アーキテクチャ(architecture)概要

```
┌──────────────────────────────────────────────────────────┐
│  ApplicationContext (IoC / core)                          │
│  scan() → doScan() → registerClass()                      │
│  ライフサイクル(lifecycle): インスタンス(instance)化 → @Autowired → Aware      │
│                 → BP → init → BP (AOP プロキシ(proxy)生成)        │
└──────────────────────┬───────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────┐
│  AspectBeanPostProcessor (AOP / aop)                      │
│  @Aspect → parseAspect() → before/after/around リスト(list)     │
│  Bean マッチ(match) → JDK 動的プロキシ(proxy) → AopProxy                │
└──────────────────────┬───────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────┐
│  WebApplicationContext + DispatcherServlet (MVC / mvc)    │
│  core ApplicationContext を再利用して IoC を実現           │
│  @Controller → initHandlerMapping() → URL マッピング(mapping)      │
│  リクエスト(request): URL → Handler → invoke() → ビュー(view)/JSON       │
└──────────────────────────────────────────────────────────┘
```

---

## IoC コンテナ(container)

### 起動フロー(flow)

```
new ApplicationContext(AppConfig.class)
    │
    ├── scan(configClass)
    │   ├── @ComponentScan の String[] を走査
    │   ├── ディレクトリ(directory)再帰 → .class ロード(load)
    │   └── registerClass()
    │       ├── @Component/@Controller/@Aspect チェック(check)
    │       ├── BeanPostProcessor → 事前登録
    │       └── その他 → BeanDefinition 登録
    │
    └── シングルトン(singleton)インスタンス(instance)化
        └── createBean() (6 段階ライフサイクル(lifecycle))
```

### Bean ライフサイクル(lifecycle)

```
① リフレクション(reflection) newInstance()
② @Autowired フィールド(field)注入 (再帰 getBean)
③ BeanNameAware.setBeanName()
④ BeanPostProcessor.postProcessBeforeInitialization()
⑤ InitializingBean.afterPropertiesSet()
⑥ BeanPostProcessor.postProcessAfterInitialization()  ← AOP プロキシ(proxy)！
```

---

## AOP アスペクト(aspect)

### 通知実行チェーン(chain)

```
AopProxy.invoke() インターセプト(intercept)
    │
    ├── try {
    │       ① @Before チェーン(chain)
    │       ② @Around チェーン(chain)再帰 → jp.proceed() → 対象メソッド(method)
    │   } finally {
    │       ③ @After チェーン(chain) (例外時も保証)
    │   }
```

---

## Spring MVC

### 起動フロー(flow)

```
DispatcherServlet.init()
    ├── web.xml → contextConfigLocation 読込
    ├── new WebApplicationContext(configClass)
    │   └── 内部で ApplicationContext を作成（core IoC 再利用！）
    └── initHandlerMapping()
        └── @Controller → @RequestMapping → URL マッピング(mapping)
```

### リクエスト(request)処理

```
GET /user/hello
    ├── DispatcherServlet.doGet/doPost
    ├── doDispatcher()
    │   ├── getHandler(uri) → URL マッチ(match)
    │   ├── method.invoke(controller)
    │   └── 戻り値処理:
    │       ├── @ResponseBody → Jackson JSON
    │       ├── "forward:/xxx" → forward
    │       ├── "redirect:/xxx" → sendRedirect
    │       └── デフォルト(default) → forward
    └── 404
```

---

## アノテーション(annotation)一覧

### IoC

| アノテーション(annotation) | 対象 | 説明 |
|------|------|------|
| `@Component("name")` | クラス(class) | Bean 登録 |
| `@ComponentScan({"p1","p2"})` | クラス(class) | パッケージスキャン(scan) |
| `@Autowired` | フィールド(field) | フィールド(field)名で注入 |
| `@Scope` | クラス(class) | スコープ(scope) |

### AOP

| アノテーション(annotation) | 対象 | 説明 |
|------|------|------|
| `@Aspect` | クラス(class) | アスペクト(aspect) (@Component メタ(meta)付与) |
| `@Before` | メソッド(method) | 前置通知 |
| `@After` | メソッド(method) | 後置通知 (finally) |
| `@Around` | メソッド(method) | 环绕通知 |

### MVC

| アノテーション(annotation) | 対象 | 説明 |
|------|------|------|
| `@Controller` | クラス(class) | コントローラ(controller) |
| `@RequestMapping("/url")` | メソッド(method) | URL マッピング(mapping) |
| `@ResponseBody` | メソッド(method) | JSON 返却 |

---

## クイックスタート(quick start)

```bash
mvn clean compile

# IoC テスト(test): IDE → zero-test-ioc → test.main()
# MVC テスト(test): cd zero-test-mvc && mvn jetty:run → http://localhost:8088
# AOP テスト(test): IDE → zero-test-aop → AopTest.main()
```

### 出力例 (AOP)

```
[AOP] プロキシ(proxy)作成: CalculatorImpl
=== add(3, 5) ===
  [@Before] >> add
  [@Around] >>> add
  [@Around] <<< add result=8
  [@After]  << add
add result: 8
```

---

## 設計のポイント(point)

| ポイント(point) | 説明 |
|------|------|
| **ゼロ外部依存** | IoC/DI は純粋 JDK リフレクション(reflection)のみ |
| **JDK 動的プロキシ(proxy)** | AOP は Proxy.newProxyInstance ベース(base) |
| **再帰パッケージスキャン(scan)** | doScan() でサブパッケージ(sub-package)を再帰走査 |
| **後処理優先登録** | BeanPostProcessor はスキャン(scan)段階で事前登録 |
| **finally セマンティクス(semantics)** | @After は try-finally で例外時も保証 |
| **Around チェーン(chain)再帰** | 匿名 JoinPoint で proceed() をオーバーライド(override) |
| **メタアノテーション(meta-annotation)** | @Controller/@Aspect に @Component をメタ(meta)付与 |
| **MVC が IoC を再利用** | WebApplicationContext は ApplicationContext に委譲 |

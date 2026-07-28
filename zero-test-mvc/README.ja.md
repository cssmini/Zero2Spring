# zero-test-mvc

> Spring MVC テスト(test) —— DispatcherServlet + @Controller + JSON 応答 + ビュー(view)転送のデモ(demo)

[🇨🇳 中文](README.md) | [🇯🇵 日本語](README.ja.md)

---

## 依存関係

```
zero-test-mvc → zero-spring-core (jar)
```

---

## ソース構成

```
zero-test-mvc/
├── pom.xml                          ← Jetty プラグイン(plugin) + war
└── src/main/
    ├── java/com/cssmini/
    │   ├── MvcAppConfig.java        ← @ComponentScan 設定
    │   ├── controller/
    │   │   └── UserController.java  ← @Controller
    │   └── service/
    │       ├── UserService.java     ← インターフェース(interface)
    │       └── impl/
    │           └── UserServiceImpl  ← @Component
    └── webapp/
        ├── index.jsp / user.jsp
        └── WEB-INF/web.xml
```

---

## 実行方法

```bash
cd zero-test-mvc
mvn jetty:run
```

| URL | 説明 | 戻り値 |
|-----|------|--------|
| `http://localhost:8088/` | トップ(top) | JSP |
| `http://localhost:8088/user/hello` | テキスト(text) | JSON |
| `http://localhost:8088/user/find` | ユーザー(user) | JSON |
| `http://localhost:8088/user/page` | forward | user.jsp |

---

## 各ファイルの役割

### web.xml

```xml
<servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>com.spring.mvc.servlet.DispatcherServlet</servlet-class>
    <init-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>com.cssmini.MvcAppConfig</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping>
    <url-pattern>/</url-pattern>
</servlet-mapping>
```

### UserController.java

```java
@Controller("userController")
public class UserController {

    @Autowired                          // core の @Autowired
    private UserService userService;

    @RequestMapping("/user/find")
    @ResponseBody
    public User findUser() {            // → JSON
        return userService.findById(1L);
    }

    @RequestMapping("/user/page")
    public String userPage() {          // → forward:/user.jsp
        return "forward:/user.jsp";
    }

    @RequestMapping("/user/hello")
    @ResponseBody
    public String hello() {             // → "Hello, Zero2Spring!"
        return userService.sayHello("Zero2Spring");
    }
}
```

### UserServiceImpl.java

```java
@Component("userService")               // core の @Component
public class UserServiceImpl implements UserService { /* ... */ }
```

---

## リクエスト(request)完全チェーン(chain)

```
GET /user/hello → Jetty → DispatcherServlet.service()
    │
    ├── doDispatcher(req, resp)
    ├── getHandler("/user/hello")
    │   └── handList を走査 → MyHandler を取得
    ├── method.invoke(controller)
    │   → UserController.hello()
    │     → UserServiceImpl.sayHello()
    │     → return "Hello, Zero2Spring!"
    └── @ResponseBody → Jackson JSON シリアル化(serialization)
        → Content-Type: application/json
```

---

## 戻り値処理ロジック(logic)

```
Controller メソッド(method)戻り値
    ├── null → 200 OK
    ├── @ResponseBody → Jackson JSON
    ├── "forward:/xxx" → forward
    ├── "redirect:/xxx" → sendRedirect
    └── その他 String → デフォルト(default) forward
```

---

## MVC と IoC の統合

```
WebApplicationContext (MVC コンテナ(container))
    │  内部で作成:
    ├── new ApplicationContext(MvcAppConfig.class)  ← core IoC 再利用！
    │   ├── @ComponentScan 走査
    │   ├── @Component + @Controller Bean 登録
    │   ├── @Autowired 注入
    │   └── BeanPostProcessor 後処理
    │
    └── initHandlerMapping()
        ├── 登録済 Bean を走査
        ├── @Controller 検出
        └── @RequestMapping → handList に格納
```

**重要な設計**: MVC は IoC を再実装しません。`WebApplicationContext` は `ApplicationContext` に委譲し、Bean 管理（スキャン(scan)、インスタンス(instance)化、注入）は core が担当。MVC は URL マッピング(mapping)のみ。

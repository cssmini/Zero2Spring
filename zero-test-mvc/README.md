# zero-test-mvc

> Spring MVC 测试项目 —— 演示 DispatcherServlet + @Controller + JSON 响应 + 视图转发

[🇨🇳 中文](README.md) | [🇯🇵 日本語](README.ja.md)

---

## 依赖关系

```
zero-test-mvc → zero-spring-core (jar)
```

---

## 源码结构

```
zero-test-mvc/
├── pom.xml                              ← Jetty 插件 + war
│
└── src/main/
    ├── java/com/cssmini/
    │   ├── MvcAppConfig.java            ← 配置类 @ComponentScan
    │   ├── controller/
    │   │   └── UserController.java      ← @Controller 演示
    │   └── service/
    │       ├── UserService.java         ← 接口
    │       └── impl/
    │           └── UserServiceImpl.java ← @Component 业务层
    │
    └── webapp/
        ├── index.jsp                    ← 首页导航
        ├── user.jsp                     ← forward 目标页
        └── WEB-INF/
            └── web.xml                  ← DispatcherServlet 配置
```

---

## 运行方式

```bash
# 编译
mvn clean compile

# 启动 Jetty
cd zero-test-mvc
mvn jetty:run
```

启动后访问：

| 地址 | 说明 | 返回类型 |
|------|------|---------|
| `http://localhost:8088/` | 首页 | JSP |
| `http://localhost:8088/user/hello` | 纯文本 | JSON |
| `http://localhost:8088/user/find` | 用户对象 | JSON |
| `http://localhost:8088/user/page` | forward 跳转 | user.jsp |

---

## 各文件职责

### 1. web.xml — Servlet 配置

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
    <servlet-name>dispatcher</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
```

- `load-on-startup=1` → 容器启动时立即初始化 Servlet
- `url-pattern=/` → 拦截所有请求，但不拦截 `.jsp`（容器默认处理）

### 2. MvcAppConfig.java — 配置类

```java
@ComponentScan("com.cssmini")
public class MvcAppConfig {
}
```

`@Controller` 元标注 `@Component`，core 容器扫描时自动识别并注册。

### 3. UserController.java — 控制器

```java
@Controller("userController")
public class UserController {

    @Autowired                              // ← core 的 @Autowired
    private UserService userService;

    @RequestMapping("/user/find")
    @ResponseBody
    public User findUser() {               // → JSON: {"id":1,"name":"Zero2Spring User","age":25}
        return userService.findById(1L);
    }

    @RequestMapping("/user/page")
    public String userPage() {              // → 跳转 forward:/user.jsp
        return "forward:/user.jsp";
    }

    @RequestMapping("/user/hello")
    @ResponseBody
    public String hello() {                // → 纯文本 JSON
        return userService.sayHello("Zero2Spring");
    }
}
```

### 4. UserServiceImpl.java — 业务层

```java
@Component("userService")               // ← core 的 @Component（非 MVC 的 @Service）
public class UserServiceImpl implements UserService {
    @Override
    public User findById(Long id) {
        return new User(id, "Zero2Spring User", 25);
    }
    @Override
    public String sayHello(String name) {
        return "Hello, " + name + "!";
    }
}
```

---

## 请求完整链路

以 `GET /user/hello` 为例：

```
浏览器访问 http://localhost:8088/user/hello
        │
        ▼
┌──────────────────────────────────────────────────────────┐
│ Jetty Server (port 8088)                                  │
│   │                                                       │
│   └── web.xml servlet-mapping → /user/hello 匹配 /       │
│       └── DispatcherServlet.service()                     │
│               │                                           │
│               ├── doDispatcher(request, response)         │
│               │                                           │
│               ├── getHandler("/user/hello")               │
│               │   └── 遍历 handList                       │
│               │       匹配 url="/user/hello"               │
│               │       → MyHandler(controller, method)     │
│               │                                           │
│               ├── handler.getMethod().invoke(controller)   │
│               │   → UserController.hello()                │
│               │     → UserServiceImpl.sayHello()          │
│               │     → return "Hello, Zero2Spring!"        │
│               │                                           │
│               └── @ResponseBody? → Jackson 序列化         │
│                   写入 response                           │
│                   Content-Type: application/json           │
│                   Body: "Hello, Zero2Spring!"             │
└──────────────────────────────────────────────────────────┘
```

---

## DispatcherServlet 返回值处理逻辑

```
Controller 方法返回
    │
    ├── null ──────────────────→ 200 OK 空响应
    │
    ├── @ResponseBody 标注 ────→ Jackson → JSON 输出
    │
    ├── "forward:/path" ───────→ RequestDispatcher.forward()
    │
    ├── "redirect:/path" ──────→ HttpServletResponse.sendRedirect()
    │
    └── 其他 String ───────────→ 默认 forward 视图
```

---

## MVC 与 IoC 的整合

```
WebApplicationContext (MVC 容器)
    │
    │  构造时内部创建:
    │
    ├── new ApplicationContext(MvcAppConfig.class)
    │   │                                     ← 复用 core 的 IoC!
    │   ├── 扫描 @ComponentScan 包
    │   ├── 注册 @Component + @Controller Bean
    │   ├── @Autowired 依赖注入
    │   └── BeanPostProcessor 后处理
    │
    │  initHandlerMapping():
    │
    ├── 遍历所有已注册 Bean
    │   ├── 找到 @Controller 类的实例
    │   └── 收集 @RequestMapping → 存入 handList
    │
    └── 响应请求时从 handList 匹配 URL
```

**关键设计**：MVC 不重复实现 IoC。`WebApplicationContext` 内部持有一个 `ApplicationContext`，所有 Bean 管理（扫描、实例化、注入）由 core 完成，MVC 只做 URL 映射。

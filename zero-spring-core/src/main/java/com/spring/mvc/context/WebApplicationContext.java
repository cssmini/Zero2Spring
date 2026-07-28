package com.spring.mvc.context;

import com.spring.core.ApplicationContext;
import com.spring.mvc.annotation.Controller;
import com.spring.mvc.annotation.RequestMapping;
import com.spring.mvc.handler.MyHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Spring MVC 容器 —— 复用 core 的 ApplicationContext 进行 IoC，
 * 仅在此构建 URL → Handler 映射。
 *
 * ★ 对比旧版改动：
 *   - 删除重复的包扫描逻辑 → 复用 core scan()
 *   - 删除重复的实例化逻辑 → 复用 core createBean()
 *   - 删除 @AutoWired → 使用 core @Autowired
 *   - 删除 @Service → 使用 core @Component
 *   - 删除 @ComponentScan 重复定义 → 使用 core
 *
 * @author Ka KinRai
 */
public class WebApplicationContext {

    /** 复用 core IoC 容器 */
    private final ApplicationContext applicationContext;

    /** URL → Handler 映射列表 */
    private final List<MyHandler> handlerList = new ArrayList<>();

    /**
     * @param configClass 配置类（需标注 @ComponentScan）
     */
    public WebApplicationContext(Class<?> configClass) {
        // 直接使用 core 的 ApplicationContext 完成扫描 + 实例化 + 注入
        this.applicationContext = new ApplicationContext(configClass);
    }

    /**
     * 初始化：从 IoC 容器中找出 @Controller Bean，构建 URL 映射
     */
    public void initHandlerMapping() {
        Set<String> beanNames = applicationContext.getBeanDefinitionNames();
        System.out.println("[MVC] 容器中共注册 " + beanNames.size() + " 个 Bean: " + beanNames);

        for (String beanName : beanNames) {
            Class<?> clazz = applicationContext.getBeanType(beanName);

            System.out.println("[MVC] 检查 Bean: " + beanName + " → " + (clazz != null ? clazz.getName() : "null"));

            if (clazz != null && clazz.isAnnotationPresent(Controller.class)) {
                Object controller = applicationContext.getBean(beanName);
                System.out.println("[MVC] 发现 Controller: " + clazz.getSimpleName());

                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(RequestMapping.class)) {
                        String url = method.getAnnotation(RequestMapping.class).value();
                        handlerList.add(new MyHandler(url, controller, method));
                        System.out.println("[MVC]   ↳ 映射: " + url + " → " + method.getName());
                    }
                }
            }
        }
        System.out.println("[MVC] 共建立 " + handlerList.size() + " 条 Handler 映射");
    }

    /** 获取 IoC 容器（供外部按需获取 Bean） */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /** 获取所有 Handler 映射 */
    public List<MyHandler> getHandlerList() {
        return handlerList;
    }
}

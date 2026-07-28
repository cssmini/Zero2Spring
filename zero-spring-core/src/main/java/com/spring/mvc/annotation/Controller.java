package com.spring.mvc.annotation;

import com.spring.core.annotation.Component;

import java.lang.annotation.*;

/**
 * 控制器注解 —— 元标注 @Component，复用 core IoC 容器管理
 *
 * @author Ka KinRai
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Controller {
    /** Bean 名称，默认空则取类名首字母小写 */
    String value() default "";
}

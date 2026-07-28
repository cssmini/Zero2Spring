package com.spring.mvc.annotation;

import java.lang.annotation.*;

/**
 * URL 请求映射
 *
 * @author Ka KinRai
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {
    String value() default "";
}

package com.spring.mvc.annotation;

import java.lang.annotation.*;

/**
 * 标记方法返回 JSON 格式数据
 *
 * @author Ka KinRai
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseBody {
}

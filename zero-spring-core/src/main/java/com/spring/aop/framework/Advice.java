package com.spring.aop.framework;

import java.lang.reflect.Method;

/**
 * 封装一条通知：切面实例 + 方法 + 切点表达式
 */
public class Advice {
    final Object aspect;
    final Method method;
    final String pointcut;

    public Advice(Object aspect, Method method, String pointcut) {
        this.aspect = aspect;
        this.method = method;
        this.pointcut = pointcut;
    }
}

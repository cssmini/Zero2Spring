package com.spring.aop;

import java.lang.reflect.Method;

/**
 * 连接点 —— 封装被拦截方法的信息
 */
public class JoinPoint {

    private final Object target;
    private final Method method;
    private final Object[] args;

    public JoinPoint(Object target, Method method, Object[] args) {
        this.target = target;
        this.method = method;
        this.args = args;
    }

    /** 执行原始目标方法 */
    public Object proceed() throws Throwable {
        return method.invoke(target, args);
    }

    public Object getTarget()     { return target; }
    public Method getMethod()     { return method; }
    public Object[] getArgs()     { return args; }
    public String getMethodName() { return method.getName(); }
}

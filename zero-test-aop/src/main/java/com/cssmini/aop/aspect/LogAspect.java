package com.cssmini.aop.aspect;

import com.spring.aop.*;

/**
 * 日志切面 —— 演示 @Before / @After / @Around
 */
@Aspect
public class LogAspect {

    @Before("execution(* com.cssmini.aop..*.*(..))")
    public void beforeLog(JoinPoint jp) {
        System.out.println("  [@Before] >> " + jp.getMethodName());
    }

    @After("execution(* com.cssmini.aop..*.*(..))")
    public void afterLog(JoinPoint jp) {
        System.out.println("  [@After]  << " + jp.getMethodName());
    }

    @Around("execution(* com.cssmini.aop..*.add(..))")
    public Object aroundAdd(JoinPoint jp) throws Throwable {
        System.out.println("  [@Around] >>> " + jp.getMethodName());
        Object result = jp.proceed();
        System.out.println("  [@Around] <<< " + jp.getMethodName() + " result=" + result);
        return result;
    }
}

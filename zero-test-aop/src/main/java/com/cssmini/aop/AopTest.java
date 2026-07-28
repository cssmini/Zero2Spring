package com.cssmini.aop;

import com.cssmini.aop.service.Calculator;
import com.spring.core.ApplicationContext;

/**
 * AOP 测试入口
 */
public class AopTest {
    public static void main(String[] args) {
        ApplicationContext ctx = new ApplicationContext(AopAppConfig.class);

        Calculator calc = (Calculator) ctx.getBean("calculator");

        System.out.println("=== add(3, 5) ===");
        int result = calc.add(3, 5);
        System.out.println("add result: " + result);

        System.out.println("\n=== div(10, 2) ===");
        int result2 = calc.div(10, 2);
        System.out.println("div result: " + result2);
    }
}

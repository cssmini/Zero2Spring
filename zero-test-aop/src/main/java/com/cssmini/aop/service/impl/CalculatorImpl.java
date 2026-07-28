package com.cssmini.aop.service.impl;

import com.cssmini.aop.service.Calculator;
import com.spring.core.annotation.Component;

/**
 * 计算器实现（将被 AOP 代理增强）
 */
@Component("calculator")
public class CalculatorImpl implements Calculator {

    @Override
    public int add(int a, int b) {
        return a + b;
    }

    @Override
    public int div(int a, int b) {
        return a / b;
    }
}

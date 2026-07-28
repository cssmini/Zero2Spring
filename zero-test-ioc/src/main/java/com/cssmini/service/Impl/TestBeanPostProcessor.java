package com.cssmini.service.Impl;

import com.spring.core.annotation.Component;
import com.spring.core.service.BeanPostProcessor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author Ka KinRai
 * @date 2026/07/20 01:58
 * @description
 */
@Component
public class TestBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (beanName.equals("userService")) {
            Object proxyInstance = Proxy.newProxyInstance(TestBeanPostProcessor.class.getClassLoader(), bean.getClass().getInterfaces(), new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    System.out.println("Post Process After Initialization");
                    return method.invoke(bean, args);
                }
            });

            return proxyInstance;
        }
        return bean;
    }
}
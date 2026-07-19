package com.cssmini.service.Impl;

import com.spring.BeanPostProcessor.Value;
import com.spring.annotation.Component;
import com.spring.service.BeanPostProcessor;

import java.lang.reflect.Field;

/**
 * @author Ka KinRai
 * @date 2026/07/20 02:14
 * @description
 */
@Component
public class ValueBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {

        for (Field field : bean.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Value.class)) {
                field.setAccessible(true);
                try {
                    System.out.println("set value"+field.getAnnotation(Value.class).value());
                    field.set(bean, field.getAnnotation(Value.class).value());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        // bean
        return bean;
    }
}

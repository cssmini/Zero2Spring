package com.spring.service;


/**
 * @author Ka KinRai
 * @date 2026/4/4 01:07
 * @description
 */
public interface BeanPostProcessor {

    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}

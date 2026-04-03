package com.spring;


import com.spring.service.BeanPostProcessor;
import com.spring.entity.BeanDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ka KinRai
 * @date 2026/4/4 01:04
 * @description
 */
public class ApplicationContext {
    private Class configClass;
    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
    private Map<String, Object> singletonObjects = new HashMap<>();
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    /**
     * init spring
     * @param configClass
     */
    public ApplicationContext(Class configClass) {
        // 1. set config class.
        this.configClass = configClass;
        // 2. scan all class and put into bean map.
        scan(configClass);
        // 3. check. if singleton object need to be set up.
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if (beanDefinition.getScope().equals("singletion")){
                Object bean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, bean);
            }
        }
    }

    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        return null;
    }



    public Object getBean(String beanName) {
        return null;
    }

    private void scan(Class configClass) {

    }
}


package com.spring.core;

import com.spring.core.annotation.Autowired;
import com.spring.core.annotation.Component;
import com.spring.core.annotation.ComponentScan;
import com.spring.core.annotation.Scope;
import com.spring.core.service.BeanNameAware;
import com.spring.core.service.BeanPostProcessor;
import com.spring.core.entity.BeanDefinition;
import com.spring.core.service.InitializingBean;
import com.spring.aop.Aspect;
import com.spring.mvc.annotation.Controller;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

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
            if (beanDefinition.getScope().equals("singleton")){
                Object bean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, bean);
            }
        }
    }


    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class clazz = beanDefinition.getType();

        Object instance = null;
        try {

            instance = clazz.getConstructor().newInstance();

            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {

                    field.setAccessible(true);
                    System.out.println("Autowired:"+ instance);
                    System.out.println("Autowired:"+ field.getName());
                    field.set(instance, getBean(field.getName()));
                }
            }

            if (instance instanceof BeanNameAware) {
                System.out.println("BeanNameAware:"+ instance);
                System.out.println("BeanNameAware:"+ beanName);
                ((BeanNameAware)instance).setBeanName(beanName);
            }

            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(instance, beanName);
            }

            if (instance instanceof InitializingBean) {
                ((InitializingBean)instance).afterPropertiesSet();
            }

            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(instance, beanName);
            }


        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return instance;
    }





    public Object getBean(String beanName) {

        if (!beanDefinitionMap.containsKey(beanName)) {
            throw new NullPointerException();
        }

        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);

        if (beanDefinition.getScope().equals("singleton")) {
            Object singletonBean = singletonObjects.get(beanName);
            if (singletonBean == null) {
                singletonBean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, singletonBean);
            }
            return singletonBean;
        } else {
            Object prototypeBean = createBean(beanName, beanDefinition);
            return prototypeBean;
        }

    }

    // ==================== 供 MVC 子���块使用的公开方法 ====================

    /** 获取所有已注册的 Bean 名称 */
    public Set<String> getBeanDefinitionNames() {
        return beanDefinitionMap.keySet();
    }

    /** 根据 beanName 获取 Bean 的 Class 类型 */
    public Class<?> getBeanType(String beanName) {
        BeanDefinition bd = beanDefinitionMap.get(beanName);
        return bd != null ? bd.getType() : null;
    }

    /** 获取配置类 */
    public Class getConfigClass() {
        return configClass;
    }

    private void scan(Class configClass) {

        if (!configClass.isAnnotationPresent(ComponentScan.class)) {
            return;
        }

        ComponentScan componentScan = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
        String[] basePackages = componentScan.value();   // String[] 支持多包
        ClassLoader classLoader = ApplicationContext.class.getClassLoader();

        for (String basePackage : basePackages) {
            String basePath = basePackage.replace(".", "/");
            URL resource = classLoader.getResource(basePath);
            System.out.println("[IoC] 扫描包: " + basePackage + " → " + (resource != null ? resource.getFile() : "NOT FOUND"));
            if (resource == null) continue;

            File rootDir = new File(resource.getFile());
            if (!rootDir.isDirectory()) {
                System.out.println("[IoC] 不是目录，跳过: " + rootDir);
                continue;
            }

            doScan(rootDir, basePackage, classLoader);
        }
        System.out.println("[IoC] 扫描完成，注册 " + beanDefinitionMap.size() + " 个 Bean: " + beanDefinitionMap.keySet());
    }

    /** 递归扫描目录，加载 @Component 类 */
    private void doScan(File dir, String basePackage, ClassLoader classLoader) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                // 递归子目录
                String subPackage = basePackage + "." + f.getName();
                doScan(f, subPackage, classLoader);
            } else if (f.getName().endsWith(".class")) {
                // 去掉 .class 后缀得到类名
                String className = basePackage + "." + f.getName().replace(".class", "");
                registerClass(className, classLoader);
            }
        }
    }

    /** 加载并注册 Bean */
    private void registerClass(String className, ClassLoader classLoader) {
        try {
            Class<?> clazz = classLoader.loadClass(className);

            // ★ 同时检查 @Component/@Controller/@Aspect（元注解解析不可靠，显式检查）
            boolean isComponent = clazz.isAnnotationPresent(Component.class)
                               || clazz.isAnnotationPresent(Controller.class)
                               || clazz.isAnnotationPresent(Aspect.class);
            System.out.println("[IoC] 发现类: " + className + " isComponent=" + isComponent);

            if (!isComponent) return;

            // BeanPostProcessor 提前实例化
            if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                BeanPostProcessor instance = (BeanPostProcessor) clazz.getConstructor().newInstance();
                beanPostProcessorList.add(instance);
            }

            // 确定 beanName
            String annValue;
            if (clazz.isAnnotationPresent(Controller.class)) {
                annValue = clazz.getAnnotation(Controller.class).value();
            } else if (clazz.isAnnotationPresent(Aspect.class)) {
                annValue = "";  // @Aspect 的 value 来自 @Component，用类名兜底
            } else {
                annValue = clazz.getAnnotation(Component.class).value();
            }
            String beanName = annValue;
            if ("".equals(beanName)) {
                // set init class name as bean name. e.g. UserServiceImpl -> userServiceImpl
                beanName = Introspector.decapitalize(clazz.getSimpleName());
            }

            // 注册 BeanDefinition
            BeanDefinition bd = new BeanDefinition();
            bd.setType(clazz);
            if (clazz.isAnnotationPresent(Scope.class)) {
                bd.setScope(clazz.getAnnotation(Scope.class).value());
            } else {
                bd.setScope("singleton");
            }
            beanDefinitionMap.put(beanName, bd);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException | InstantiationException |
                 IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}

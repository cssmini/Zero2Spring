package com.cssmini.service.Impl;


import com.cssmini.service.OrderService;
import com.cssmini.service.UserService;
import com.spring.BeanPostProcessor.Value;
import com.spring.annotation.Autowired;
import com.spring.annotation.Component;
import com.spring.service.BeanNameAware;
import com.spring.service.InitializingBean;

/**
 * @author Ka KinRai
 * @date 2026/4/4 01:40
 * @description
 */
@Component("userService")
public class UserServiceImpl implements UserService, BeanNameAware, InitializingBean {
    @Autowired
    private OrderService orderService;

    @Value("Value xxx")
    private String test;

    private String beanName;

    @Override
    public void test() {
        System.out.println("UserServiceImpl test");
        System.out.println("Value:" + test);
    }

    @Override
    public void setBeanName(String name) {
        System.out.println("UserServiceImpl setBeanName:"+ name);
        this.beanName = name;
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("UserServiceImpl afterPropertiesSet");
    }
}

package com.cssmini;


import com.cssmini.service.UserService;
import com.spring.ApplicationContext;

/**
 * @author Ka KinRai
 * @date 2026/4/4 00:55
 * @description
 */
public class test {
    public static void main(String[] args) {
        // spring init
        ApplicationContext applicationContext = new ApplicationContext(AppConfig.class);
        // get spring bean
        UserService userService = (UserService) applicationContext.getBean("userService");
        // userService function running
        userService.test();
    }
}

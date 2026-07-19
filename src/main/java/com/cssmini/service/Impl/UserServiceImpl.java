package com.cssmini.service.Impl;


import com.cssmini.service.UserService;
import com.spring.annotation.Component;

/**
 * @author Ka KinRai
 * @date 2026/4/4 01:40
 * @description
 */
@Component("UserService")
public class UserServiceImpl implements UserService {
    @Override
    public void test() {
        System.out.println("UserServiceImpl test");
    }
}

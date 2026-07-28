package com.cssmini.service.impl;

import com.spring.core.annotation.Component;
import com.cssmini.controller.UserController.User;
import com.cssmini.service.UserService;

/**
 * 用户服务实现 —— ★ 使用 core @Component（替代旧的 MVC @Service）
 *
 * @author Ka KinRai
 */
@Component("userService")
public class UserServiceImpl implements UserService {

    @Override
    public User findById(Long id) {
        return new User(id, "Zero2Spring User", 25);
    }

    @Override
    public String sayHello(String name) {
        return "Hello, " + name + "! (from core IoC)";
    }
}

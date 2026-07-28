package com.cssmini.service;

import com.cssmini.controller.UserController.User;

/**
 * 用户服务接口
 *
 * @author Ka KinRai
 */
public interface UserService {
    User findById(Long id);
    String sayHello(String name);
}

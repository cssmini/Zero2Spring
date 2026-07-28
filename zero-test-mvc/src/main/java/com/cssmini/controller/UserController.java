package com.cssmini.controller;

import com.spring.core.annotation.Autowired;
import com.spring.mvc.annotation.Controller;
import com.spring.mvc.annotation.RequestMapping;
import com.spring.mvc.annotation.ResponseBody;
import com.cssmini.service.UserService;

/**
 * 用户控制器 —— 演示 @Controller 复用 core IoC
 *
 * @author Ka KinRai
 */
@Controller("userController")
public class UserController {

    /** ★ 直接使用 core 的 @Autowired（替代旧的 MVC @AutoWired） */
    @Autowired
    private UserService userService;

    /**
     * 返回 JSON
     */
    @RequestMapping("/user/find")
    @ResponseBody
    public User findUser() {
        return userService.findById(1L);
    }

    /**
     * 视图跳转（forward）
     */
    @RequestMapping("/user/page")
    public String userPage() {
        return "forward:/user.jsp";
    }

    /**
     * 纯文本
     */
    @RequestMapping("/user/hello")
    @ResponseBody
    public String hello() {
        return userService.sayHello("Zero2Spring");
    }

    // 简单 VO
    public static class User {
        private Long id;
        private String name;
        private int age;

        public User() {}
        public User(Long id, String name, int age) {
            this.id = id; this.name = name; this.age = age;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }
}

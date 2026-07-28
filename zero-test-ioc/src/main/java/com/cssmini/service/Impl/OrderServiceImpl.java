package com.cssmini.service.Impl;

import com.cssmini.service.OrderService;
import com.spring.core.annotation.Component;

/**
 * @author Ka KinRai
 * @date 2026/07/20 01:34
 * @description
 */
@Component("orderService")
public class OrderServiceImpl implements OrderService {
    @Override
    public void test() {
        System.out.println("OrderServiceImpl test");
    }
}
package com.spring.mvc.handler;

import java.lang.reflect.Method;

/**
 * URL → Controller.Method 映射处理器
 *
 * @author Ka KinRai
 */
public class MyHandler {
    private String url;
    private Object controller;
    private Method method;

    public MyHandler() {}

    public MyHandler(String url, Object controller, Method method) {
        this.url = url;
        this.controller = controller;
        this.method = method;
    }

    public String getUrl()              { return url; }
    public void setUrl(String url)      { this.url = url; }
    public Object getController()       { return controller; }
    public void setController(Object c) { this.controller = c; }
    public Method getMethod()           { return method; }
    public void setMethod(Method m)     { this.method = m; }
}

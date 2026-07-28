package com.spring.mvc.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.mvc.annotation.ResponseBody;
import com.spring.mvc.context.WebApplicationContext;
import com.spring.mvc.handler.MyHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;

/**
 * 前端控制器 —— MVC 核心入口
 *
 * @author Ka KinRai
 */
public class DispatcherServlet extends HttpServlet {

    private WebApplicationContext webApplicationContext;

    @Override
    public void init() throws ServletException {
        // 1. 读取配置类全限定名
        String contextConfigLocation = this.getServletConfig()
                .getInitParameter("contextConfigLocation");

        // 2. 加载配置类并创建 MVC 容器（内部复用 core ApplicationContext）
        try {
            Class<?> configClass = Class.forName(contextConfigLocation);
            webApplicationContext = new WebApplicationContext(configClass);
        } catch (ClassNotFoundException e) {
            throw new ServletException("找不到配置类: " + contextConfigLocation, e);
        }

        // 3. 构建 URL → Handler 映射
        webApplicationContext.initHandlerMapping();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doDispatcher(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doPost(req, resp);
    }

    /** 请求分发 */
    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) {
        MyHandler handler = getHandler(req);

        try {
            if (handler == null) {
                resp.setStatus(404);
                resp.setContentType("text/html;charset=utf-8");
                resp.getWriter().print("<h1>404 NOT FOUND</h1>");
                return;
            }

            // 调用 Controller 方法
            Object result = handler.getMethod().invoke(handler.getController());
            Method method = handler.getMethod();

            // 返回 null → 空响应
            if (result == null) {
                resp.setStatus(200);
                return;
            }

            // 有 @ResponseBody → JSON 序列化
            if (method.isAnnotationPresent(ResponseBody.class)) {
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(result);
                resp.setContentType("application/json;charset=utf-8");
                PrintWriter writer = resp.getWriter();
                writer.print(json);
                writer.flush();
                return;
            }

            // String 类型 → 视图跳转
            if (result instanceof String) {
                String viewName = (String) result;
                if (viewName.startsWith("forward:")) {
                    // forward:/page.jsp
                    req.getRequestDispatcher(viewName.substring(8)).forward(req, resp);
                } else if (viewName.startsWith("redirect:")) {
                    // redirect:/page.jsp  或  redirect:http://...
                    resp.sendRedirect(viewName.substring(9));
                } else {
                    // 纯路径 → 默认 forward
                    req.getRequestDispatcher(viewName).forward(req, resp);
                }
                return;
            }

            // 兜底：非 String 且无 @ResponseBody → 输出 toString
            resp.setContentType("text/plain;charset=utf-8");
            resp.getWriter().print(result.toString());

        } catch (Exception e) {
            e.printStackTrace();
            try {
                resp.setStatus(500);
                resp.setContentType("text/html;charset=utf-8");
                resp.getWriter().print("<h1>500 Internal Server Error</h1>");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /** 根据请求 URL 匹配 Handler */
    private MyHandler getHandler(HttpServletRequest req) {
        String uri = req.getRequestURI();
        for (MyHandler h : webApplicationContext.getHandlerList()) {
            if (h.getUrl().equals(uri)) {
                return h;
            }
        }
        return null;
    }
}

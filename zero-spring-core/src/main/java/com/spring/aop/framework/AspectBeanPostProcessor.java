package com.spring.aop.framework;

import com.spring.core.annotation.Component;
import com.spring.aop.*;
import com.spring.core.service.BeanPostProcessor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AOP 核心后处理器 —— 解析 @Aspect，为匹配 Bean 创建 JDK 代理
 *
 * 执行链: @Before → @Around → 目标方法 → @After
 */
@Component
public class AspectBeanPostProcessor implements BeanPostProcessor {

    private final Set<Class<?>> parsedAspects = ConcurrentHashMap.newKeySet();
    private final List<Advice> beforeAdvices = new ArrayList<>();
    private final List<Advice> afterAdvices  = new ArrayList<>();
    private final List<Advice> aroundAdvices = new ArrayList<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> clazz = bean.getClass();

        if (clazz.isAnnotationPresent(Aspect.class)) {
            if (parsedAspects.add(clazz)) parseAspect(bean);
            return bean;
        }

        if (beforeAdvices.isEmpty() && afterAdvices.isEmpty() && aroundAdvices.isEmpty())
            return bean;

        return wrapIfNecessary(bean);
    }

    // ==================== 切面解析 ====================

    private void parseAspect(Object aspectBean) {
        Class<?> clazz = aspectBean.getClass();
        for (Method m : clazz.getDeclaredMethods()) {
            m.setAccessible(true);
            String expr;
            if (m.isAnnotationPresent(Before.class)) {
                expr = m.getAnnotation(Before.class).value();
                beforeAdvices.add(new Advice(aspectBean, m, expr));
                System.out.println("[AOP] @Before : " + clazz.getSimpleName() + "." + m.getName() + " → " + expr);
            } else if (m.isAnnotationPresent(After.class)) {
                expr = m.getAnnotation(After.class).value();
                afterAdvices.add(new Advice(aspectBean, m, expr));
                System.out.println("[AOP] @After  : " + clazz.getSimpleName() + "." + m.getName() + " → " + expr);
            } else if (m.isAnnotationPresent(Around.class)) {
                expr = m.getAnnotation(Around.class).value();
                aroundAdvices.add(new Advice(aspectBean, m, expr));
                System.out.println("[AOP] @Around : " + clazz.getSimpleName() + "." + m.getName() + " → " + expr);
            }
        }
    }

    // ==================== 代理创建 ====================

    private Object wrapIfNecessary(Object target) {
        Class<?> clazz = target.getClass();
        Class<?>[] interfaces = clazz.getInterfaces();
        if (interfaces.length == 0) return target;
        if (!anyAdviceMatches(clazz)) return target;

        System.out.println("[AOP] 创建代理: " + clazz.getSimpleName());
        return Proxy.newProxyInstance(clazz.getClassLoader(), interfaces,
                new AopProxy(target, this));
    }

    private boolean anyAdviceMatches(Class<?> targetClass) {
        String name = targetClass.getName();
        for (Advice a : beforeAdvices)  if (matchClass(a.pointcut, name)) return true;
        for (Advice a : afterAdvices)   if (matchClass(a.pointcut, name)) return true;
        for (Advice a : aroundAdvices)  if (matchClass(a.pointcut, name)) return true;
        return false;
    }

    // ==================== 代理调用处理 (静态内部类) ====================

    private static class AopProxy implements InvocationHandler {
        private final Object target;
        private final AspectBeanPostProcessor processor;

        AopProxy(Object target, AspectBeanPostProcessor processor) {
            this.target = target;
            this.processor = processor;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            JoinPoint jp = new JoinPoint(target, method, args);

            List<Advice> mb = processor.filter(processor.beforeAdvices, method);
            List<Advice> ma = processor.filter(processor.afterAdvices,  method);
            List<Advice> mr = processor.filter(processor.aroundAdvices, method);

            if (mb.isEmpty() && ma.isEmpty() && mr.isEmpty())
                return method.invoke(target, args);

            try {
                for (Advice a : mb) processor.invokeAdvice(a, jp);     // ① @Before
                if (!mr.isEmpty()) return processor.chain(jp, mr, 0);  // ② @Around
                return jp.proceed();                                   // ② 目标方法
            } finally {
                for (Advice a : ma) processor.invokeAdvice(a, jp);     // ③ @After
            }
        }
    }

    // ==================== 通知调用 & 匹配方法 ====================

    List<Advice> filter(List<Advice> list, Method m) {
        List<Advice> r = new ArrayList<>();
        for (Advice a : list) if (matchMethod(a, m)) r.add(a);
        return r;
    }

    void invokeAdvice(Advice a, JoinPoint jp) throws Throwable {
        Class<?>[] types = a.method.getParameterTypes();
        if (types.length == 0) a.method.invoke(a.aspect);
        else a.method.invoke(a.aspect, jp);
    }

    Object chain(JoinPoint jp, List<Advice> list, int i) throws Throwable {
        if (i >= list.size()) return jp.proceed();
        Advice cur = list.get(i);
        JoinPoint next = new JoinPoint(jp.getTarget(), jp.getMethod(), jp.getArgs()) {
            @Override public Object proceed() throws Throwable { return chain(jp, list, i + 1); }
        };
        return cur.method.invoke(cur.aspect, next);
    }

    // ==================== 切点匹配 ====================

    private boolean matchClass(String expr, String className) {
        String p = extractClass(expr);
        if ("*".equals(p)) return true;
        if (p.contains("..")) return className.startsWith(p.substring(0, p.indexOf("..")));
        return className.equals(p);
    }

    private boolean matchMethod(Advice a, Method m) {
        String p = extractMethod(a.pointcut);
        return "*".equals(p) || p.equals(m.getName());
    }

    private String extractClass(String expr) {
        String s = expr.replace("execution(", "").replace(")", "");
        int sp = s.indexOf(' ');  if (sp >= 0) s = s.substring(sp + 1);
        int lp = s.lastIndexOf('('); if (lp >= 0) s = s.substring(0, lp);
        int dot = s.lastIndexOf('.');
        return dot >= 0 ? s.substring(0, dot) : s;
    }

    private String extractMethod(String expr) {
        String s = expr.replace("execution(", "").replace(")", "");
        int sp = s.indexOf(' ');  if (sp >= 0) s = s.substring(sp + 1);
        int lp = s.lastIndexOf('('); if (lp >= 0) s = s.substring(0, lp);
        int dot = s.lastIndexOf('.');
        return dot >= 0 ? s.substring(dot + 1) : s;
    }
}

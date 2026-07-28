package com.cssmini;

import com.spring.core.annotation.ComponentScan;

/**
 * MVC 配置类
 * —— @Controller 因元标注 @Component，会被 core 容器自动扫描注册
 *
 * @author Ka KinRai
 */
@ComponentScan("com.cssmini")
public class MvcAppConfig {
}

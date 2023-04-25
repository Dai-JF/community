package com.dai.community.config;

import com.dai.community.interceptor.DataInterceptor;
import com.dai.community.interceptor.LoginTicketInterceptor;
import com.dai.community.interceptor.MessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Description: 拦截静态资源以外的所有请求
 *
 * @author: DaiJF
 * @date: 2022/7/26 - 13:08
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Autowired
  private LoginTicketInterceptor loginTicketInterceptor;
  @Autowired
  private MessageInterceptor messageInterceptor;
  @Autowired
  private DataInterceptor dataInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loginTicketInterceptor)
        .excludePathPatterns("/* */*.css", "/**/ *.js", "/* */*.png", "/ **/ *.jpg", "/* */*.jpeg");

    registry.addInterceptor(messageInterceptor)
        .excludePathPatterns("/* */*.css", "/**/ *.js", "/* */*.png", "/ **/ *.jpg", "/* */*.jpeg");

    registry.addInterceptor(dataInterceptor)
        .excludePathPatterns("/* */*.css", "/ **/ *.js", "/* */*.png", "/**/ *.jpg", "/* */*.jpeg");
  }
}

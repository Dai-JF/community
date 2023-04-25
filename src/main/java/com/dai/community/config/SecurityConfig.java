package com.dai.community.config;

import com.dai.community.Util.CommunityUtil;
import com.dai.community.consts.CommunityConst;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import java.io.PrintWriter;

/**
 * Description:之所以没有configure(AuthenticationManagerBuilder auth)，是因为要绕过security自带的方案，使用之前自己写的
 *
 * @author DaiJF
 * @date 2022/8/12 - 22:50
 */
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConst {


  @Override
  public void configure(WebSecurity web) throws Exception {
    // 忽略静态资源拦截
    web.ignoring().antMatchers("/resources/**");
  }


  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // 授权
    http.authorizeRequests()
        .antMatchers(
            //"/user/setting",
            //"/user/pwd",
            //"/follow",
            //"/unfollow",
            //"/discuss/add",
            //"/like",
            //"/letter/**",
            //"/notice/**",
            //"/comment/add/**"
            // 这三种权限能访问以上路径
        ).hasAnyAuthority(AUTHORITY_USER, AUTHORITY_ADMIN, AUTHORITY_MODERATOR)
        // 版主授予加精、置顶权限
        .antMatchers("/discuss/top", "/discuss/wonderful").hasAnyAuthority(AUTHORITY_MODERATOR)
        // 管理员授予删除帖子权限和查看数据统计权限
        // .antMatchers("/discuss/delete", "/data/**").hasAnyAuthority(AUTHORITY_ADMIN)
        // 放行其他请求
        .anyRequest().permitAll()
        // 禁用csrf
        .and().csrf().disable();

    // 权限不够时的处理
    http.exceptionHandling()
        // 未登录
        .authenticationEntryPoint((request, response, e) -> {
          // 异步请求
          String xRequestedWith = request.getHeader("x-requested-with");
          if ("XMLHttpRequest".equals(xRequestedWith)) {
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(403, "你还没有登录哦!"));
          } else {
            // 同步请求
            response.sendRedirect(request.getContextPath() + "/login");
          }
        })
        // 权限不足
        .accessDeniedHandler((request, response, e) -> {
          String xRequestedWith = request.getHeader("x-requested-with");
          if ("XMLHttpRequest".equals(xRequestedWith)) {
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限!"));
          } else {
            response.sendRedirect(request.getContextPath() + "/denied");
          }
        });

    // Security底层默认会拦截/logout请求,进行退出处理.
    // 覆盖它默认的逻辑,才能执行我们自己的退出代码.
    http.logout().logoutUrl("/security-logout");
  }


}




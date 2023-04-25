package com.dai.community.interceptor;

import com.dai.community.Util.HostHolder;
import com.dai.community.annotation.LoginRequired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * Description:登陆状态拦截器
 *
 * @author: DaiJF
 * @date: 2022/7/26 - 15:50
 */
@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

  @Autowired
  private HostHolder hostHolder;

  /**
   * 在请求路径前执行该方法*
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    //判断拦截的目标是不是一个方法
    if (handler instanceof HandlerMethod) {
      //如果是一个方法，将handler转化我HandlerMethod类型
      HandlerMethod handlerMethod = (HandlerMethod) handler;
      Method method = handlerMethod.getMethod();
      //获取方法上的自定义注解
      LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
      /*
       * 如果没有登录并且有自定义注解（需要登录才能访问的方法注解）
       * 通过response来重定向，这里不可以通过return 重定向
       */
      if (hostHolder.getUser() == null && loginRequired != null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
      }
    }
    return true;
  }
}

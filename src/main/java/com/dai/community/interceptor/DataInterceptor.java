package com.dai.community.interceptor;

import com.dai.community.Util.HostHolder;
import com.dai.community.entity.User;
import com.dai.community.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Description:
 *
 * @author DaiJF
 * @date 2022/8/13 - 13:47
 */
@Component
public class DataInterceptor implements HandlerInterceptor {

  @Autowired
  private DataService dataService;
  @Autowired
  private HostHolder hostHolder;

  /**
   * 在所有请求之前存用户访问数和日活跃人数
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    // 获取请求用户的ip地址，统计UV
    String ip = request.getRemoteHost();
    dataService.recordUV(ip);

    // 统计DAU
    User user = hostHolder.getUser();
    if (user != null) {
      dataService.recordDAU(user.getId());
    }
    return true;
  }
}

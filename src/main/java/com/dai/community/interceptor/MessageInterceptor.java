package com.dai.community.interceptor;

import com.dai.community.Util.HostHolder;
import com.dai.community.entity.User;
import com.dai.community.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * description：
 *
 * @author DaiJF
 * @date 2022/8/10 - 12:03
 */
@Component
public class MessageInterceptor implements HandlerInterceptor {
  @Autowired
  private HostHolder hostHolder;
  @Autowired
  private MessageService messageService;

  /**
   * 查询未读消息总数(AOP),controller之后，渲染模板之前
   */
  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    User user = hostHolder.getUser();
    if (user != null && modelAndView != null) {
      int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
      int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);

      modelAndView.addObject("allUnreadCount", letterUnreadCount + noticeUnreadCount);
    }
  }
}
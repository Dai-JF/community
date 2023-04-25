package com.dai.community.Util;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * description: 统一异常处理
 *
 * @author DaiJF
 * @date 2022/7/31 - 13:47
 */
@ControllerAdvice(annotations = Controller.class)
public class ExceptionUtil {

  private static final Logger logger = LoggerFactory.getLogger(ExceptionUtil.class);

  @ExceptionHandler({Exception.class})
  public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    // 错误概括
    logger.error("服务器发生异常: " + e.getMessage());
    // 错误详情
    for (StackTraceElement element : e.getStackTrace()) {
      logger.error(element.toString());
    }
    // 获得请求方式
    String xRequestedWith = request.getHeader("x-requested-with");
    if ("XMLHttpRequest".equals(xRequestedWith)) {
      response.setContentType("application/plain;charset=utf-8");
      PrintWriter writer = response.getWriter();
      writer.write(CommunityUtil.getJSONString(1, "服务器异常!"));
    } else {
      response.sendRedirect(request.getContextPath() + "/error");
    }
  }

}

package com.dai.community.controller;

import com.dai.community.Util.CommunityUtil;
import com.dai.community.Util.RedisKeyUtil;
import com.dai.community.consts.CommunityConst;
import com.dai.community.entity.User;
import com.dai.community.service.UserService;
import com.google.code.kaptcha.Producer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 *
 * @author: DaiJF
 * @date: 2022/7/25 - 18:17
 */
@Controller
public class LoginController implements CommunityConst {

  private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

  @Autowired
  UserService userService;

  @Autowired
  Producer producer;

  @Value("${server.servlet.context-path}")
  private String contextPath;

  @Autowired
  private RedisTemplate redisTemplate;

  /**
   * 转向注册页面
   */
  @RequestMapping(path = "/register", method = RequestMethod.GET)
  public String getRegisterPage() {
    return "/site/register";
  }

  /**
   * 转向登录页面
   */
  @RequestMapping(path = "/login", method = RequestMethod.GET)
  public String getLoginPage() {
    return "/site/login";
  }

  /**
   * 注册 user对象：接受页面传过来的值
   */
  @RequestMapping(path = "/register", method = RequestMethod.POST)
  public String register(User user, Model model) {
    Map<String, Object> map = userService.register(user);
    // map为空，表示 userService 的 register方法的 map中 没有异常消息，则注册成功
    if (map == null || map.isEmpty()) {
      model.addAttribute("msg", "注册成功,我们已经向您的邮件发送了一封激活邮件,请尽快激活！");
      // 倒数回到首页
      model.addAttribute("target", "/index");
      return "/site/operate-result";
    } else {
      model.addAttribute("usernameMsg", map.get("usernameMsg"));
      model.addAttribute("passwordMsg", map.get("passwordMsg"));
      model.addAttribute("emailMsg", map.get("emailMsg"));
      return "/site/register";
    }
  }

  /**
   * 激活邮箱 规定url为：//http://localhost:8080/community/activation/{userId}/激活码
   */
  @RequestMapping(value = "/activation/{userId}/{code}", method = RequestMethod.GET)
  public String activation(
      Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
    int result = userService.activation(userId, code);
    if (result == ACTIVATION_SUCCESS) {
      model.addAttribute("msg", "激活成功,你的账号已经可以正常使用了！");
      model.addAttribute("target", "/login");
    } else if (result == ACTIVATION_REPEAT) {
      model.addAttribute("msg", "无效操作,该账号已经激活过了！");
      model.addAttribute("target", "/index");
    } else {
      model.addAttribute("msg", "激活失败,你提供的激活码不正确！");
      model.addAttribute("target", "/index");
    }
    return "/site/operate-result";
  }

  /**
   *
   */
  @RequestMapping(value = "/kaptcha", method = RequestMethod.GET)
  public void getKaptcha(HttpServletResponse response) {
    // 生成验证码
    String text = producer.createText();
    BufferedImage image = producer.createImage(text);
    // 将验证码存入session
    // session.setAttribute("kaptcha", text);

    // 生成验证码的归属传给浏览器Cookie
    String kaptchaOwner = CommunityUtil.generateUUID();
    Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
    cookie.setMaxAge(60);
    cookie.setPath(contextPath);
    response.addCookie(cookie);
    // 将验证码存入Redis
    String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
    redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);

    // 将图片输出给浏览器
    response.setContentType("image/png");
    try {
      ServletOutputStream os = response.getOutputStream();
      ImageIO.write(image, "png", os);
    } catch (IOException e) {
      logger.error("响应验证码失败:" + e.getMessage());
    }
  }

  /**
   * 登录功能 username,password这些没有封装进model,自定义对象类型才会自动封装普通类型
   *
   * @param code       用于校验验证码
   * @param rememberme 记住我
   * @param model      用于将数据传递给前端页面
   * @param response   用于浏览器接受cookie
   */
  @RequestMapping(value = "/login", method = RequestMethod.POST)

  public String login(String username, String password, String code, boolean rememberme,
                      @CookieValue("kaptchaOwner") String kaptchaOwner, Model model, HttpServletResponse response) {
    // 首先检验验证码
    // String kaptcha = (String) session.getAttribute("kaptcha");

    String kaptcha = null;
    if (StringUtils.isNotBlank(kaptchaOwner)) {
      String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
      kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
    }

    if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(
        code)) {
      model.addAttribute("codeMsg", "验证码不正确！");
      return "/site/login";
    }
    int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
    Map<String, Object> map = userService.login(username, password, expiredSeconds);
    // 成功
    if (map.containsKey("ticket")) {
      Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
      cookie.setPath(contextPath);
      cookie.setMaxAge(expiredSeconds);
      response.addCookie(cookie);
      return "redirect:/index";
    } else {
      model.addAttribute("usernameMsg", map.get("usernameMsg"));
      model.addAttribute("passwordMsg", map.get("passwordMsg"));
      return "/site/login";
    }
  }

  /**
   * 退出登录
   */
  @RequestMapping(value = "/logout", method = RequestMethod.GET)
  public String logout(@CookieValue("ticket") String ticket) {
    userService.logout(ticket);
    // 释放SecurityContext资源
    SecurityContextHolder.clearContext();
    return "redirect:/login";
  }
}

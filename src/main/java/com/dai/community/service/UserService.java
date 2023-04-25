package com.dai.community.service;

import com.dai.community.Util.CommunityUtil;
import com.dai.community.Util.MailClient;
import com.dai.community.Util.RedisKeyUtil;
import com.dai.community.consts.CommunityConst;
import com.dai.community.dao.UserMapper;
import com.dai.community.entity.LoginTicket;
import com.dai.community.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 *
 * @author: DaiJF
 * @date: 2022/7/25 - 13:40
 */
@Service
public class UserService implements CommunityConst {

  @Autowired
  private UserMapper userMapper;

  @Autowired
  private MailClient mailClient;

  @Resource
  private TemplateEngine templateEngine;


  @Autowired
  private RedisTemplate redisTemplate;

  /**
   * 注入域名和项目名【上传邮件的激活码中需要包含其二】
   */
  @Value("${community.path.domain}")
  private String domain;

  @Value("${server.servlet.context-path}")
  private String contextPath;


  public User findUserById(int id) {
    // return userMapper.selectById(id);
    User user = getCache(id);
    if (user == null) {
      user = initCache(id);
    }
    return user;
  }


  public User findUserByName(String username) {
    return userMapper.selectByName(username);
  }


  /**
   * 注册功能 为什么返回的是Map类型，因为用Map来存各种情况下的信息，返回给前端页面
   */

  public Map<String, Object> register(User user) {
    Map<String, Object> map = new HashMap<>();
    // 空值处理
    if (user == null) {
      throw new IllegalArgumentException("参数不能为空！");
    }
    if (StringUtils.isBlank(user.getUsername())) {
      map.put("usernameMsg", "账户不能为空");
      return map;
    }
    if (StringUtils.isBlank(user.getPassword())) {
      map.put("passwordMsg", "密码不能为空");
      return map;
    }
    if (StringUtils.isBlank(user.getEmail())) {
      map.put("emailMsg", "邮箱不能为空");
      return map;
    }

    // 验证账号
    User u = userMapper.selectByName(user.getUsername());
    if (u != null) {
      map.put("usernameMsg", "该账号已存在！");
      return map;
    }
    // 验证邮箱
    u = userMapper.selectByEmail(user.getEmail());
    if (u != null) {
      map.put("emailMsg", "该邮箱已被注册！");
      return map;
    }

        /*
         注册账户
         1.设置salt加密(随机5位数追加至密码)
         2.设置最终密码为：md5加密后的密码+salt
         3.设置随机数激活码
         4.设置status,type=0,时间
         5.设置头像(动态)
         6.设置注册时间
         */
    user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
    user.setPassword(CommunityUtil.md5(user.getUsername() + user.getSalt()));
    user.setActivationCode(CommunityUtil.generateUUID());
    user.setType(0);
    user.setStatus(0);
    //%d:占位符，表示一个数字
    user.setHeaderUrl(
        String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
    user.setCreateTime(new Date());
    userMapper.insertUser(user);

    /*
     *激活邮箱
     * 1.创建Context对象-->context.setVariable(name,value)将name传入前端为thymeleaf提供变量
     * 2.设置email和url
     * 3.templateEngine.process执行相应HTML
     * 4.发送邮件
     */
    Context context = new Context();
    context.setVariable("email", user.getEmail());
    //规定url为：//http://localhost:8080/community/activation/{userId}/激活码
    String url =
        domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
    context.setVariable("url", url);
    String content = templateEngine.process("/mail/activation", context);
    mailClient.sendMail(user.getEmail(), "激活账号", content);
    return map;
  }

  /**
   * 激活邮件功能
   */
  public int activation(int userId, String code) {
    User user = userMapper.selectById(userId);
    if (user.getStatus() == 1) {
      return ACTIVATION_REPEAT;
    } else if (user.getActivationCode().equals(code)) {
      userMapper.updateStatus(userId, 1);
      clearCache(userId);
      return ACTIVATION_SUCCESS;
    } else {
      return ACTIVATION_FAILURE;
    }
  }

  /**
   * 登录功能
   **/
  public Map<String, Object> login(String username, String password, int expiredSeconds) {
    HashMap<String, Object> map = new HashMap<>();
    //空值处理
    if (StringUtils.isBlank(username)) {
      map.put("usernameMsg", "号码不能为空！");
      return map;
    }
    if (StringUtils.isBlank(password)) {
      map.put("passwordMsg", "密码不能为空！");
      return map;
    }
    //验证账号
    User user = userMapper.selectByName(username);
    if (user == null) {
      map.put("usernameMsg", "该账号不存在！");
      return map;
    }
    //验证激活状态
    if (user.getStatus() == 0) {
      map.put("usernameMsg", "该账号未激活！");
      return map;
    }
    //验证密码(先加密再对比)
    password = CommunityUtil.md5(password + user.getSalt());
    if (!user.getPassword().equals(password)) {
      map.put("passwordMsg", "密码输入错误！");
      return map;
    }
    //生成登录凭证
    LoginTicket ticket = new LoginTicket();
    ticket.setUserId(user.getId());
    ticket.setTicket(CommunityUtil.generateUUID());
    ticket.setStatus(0);
    //当前时间的毫秒数+过期时间毫秒数
    ticket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000L));
    //loginTicketMapper.insertLoginTicket(ticket);
    String redisKey = RedisKeyUtil.getTicketKey(ticket.getTicket());
    redisTemplate.opsForValue().set(redisKey, ticket);

    map.put("ticket", ticket.getTicket());
    return map;
  }

  /**
   * 登出功能
   */
  public void logout(String ticket) {
    //loginTicketMapper.updateStatus(ticket, 1);
    String redisKey = RedisKeyUtil.getTicketKey(ticket);
    //去除ticket
    LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    loginTicket.setStatus(1);
    //再存入ticket
    redisTemplate.opsForValue().set(redisKey, loginTicket);
  }

  /**
   * 查询凭证
   */
  public LoginTicket findLoginTicket(String ticket) {
    //return loginTicketMapper.selectByTicket(ticket);
    String redisKey = RedisKeyUtil.getTicketKey(ticket);
    return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
  }

  /**
   * 上传头像
   */
  public int updateHeader(int userId, String headerUrl) {
    // return userMapper.updateHeader(userId, headerUrl);
    int rows = userMapper.updateHeader(userId, headerUrl);
    clearCache(userId);
    return rows;
  }

  /**
   * 修改密码
   */
  public Map<String, Object> updatePassword(int userId, String oldPwd, String newPwd,
                                            String checkPwd) {
    HashMap<String, Object> map = new HashMap<>();
    //空值处理
    if (StringUtils.isBlank(oldPwd)) {
      map.put("oldPwdMsg", "原密码不能为空");
      return map;
    }
    if (StringUtils.isBlank(newPwd)) {
      map.put("newPwdMsg", "新密码不能为空");
      return map;
    }
    if (StringUtils.isBlank(checkPwd)) {
      map.put("checkPwdMsg", "请确认密码");
      return map;
    }
    User user = userMapper.selectById(userId);

    if (!checkPwd.equals(newPwd)) {
      map.put("checkPwdMsg", "两次密码输入不一致！");
      return map;
    }
    //检验原密码
    oldPwd = CommunityUtil.md5(oldPwd + user.getSalt());
    if (!user.getPassword().equals(oldPwd)) {
      map.put("oldPwdMsg", "原密码输入错误！");
      return map;
    }

    //更新密码
    newPwd = CommunityUtil.md5(newPwd + user.getSalt());
    userMapper.updatePassword(userId, newPwd);
    return map;
  }

  /**
   * 1.优先从缓存中取值
   */
  private User getCache(int userId) {
    String redisKey = RedisKeyUtil.getUserKey(userId);
    return (User) redisTemplate.opsForValue().get(redisKey);
  }

  /**
   * 2.取不到时初始化缓存数据
   */
  private User initCache(int userId) {
    //找不到先查数据库
    User user = userMapper.selectById(userId);
    String redisKey = RedisKeyUtil.getUserKey(userId);
    redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
    return user;
  }

  /**
   * 3.数据变更时清除缓存数据
   */
  private void clearCache(int userId) {
    String redisKey = RedisKeyUtil.getUserKey(userId);
    redisTemplate.delete(redisKey);
  }

  /**
   * 增加自定义登录认证方法绕过security自带认证流程，采用原来的认证方案,封装认证结果
   **/
  /**
   * 增加自定义登录认证方法绕过security自带认证流程，采用原来的认证方案,封装认证结果
   **/
  public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
    User user = this.findUserById(userId);

    List<GrantedAuthority> list = new ArrayList<>();

    list.add((GrantedAuthority) () -> {
      switch (user.getType()) {
        case 1:
          return AUTHORITY_ADMIN;
        case 2:
          return AUTHORITY_MODERATOR;
        default:
          return AUTHORITY_USER;
      }
    });
    return list;
  }
}


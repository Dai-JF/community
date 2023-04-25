package com.dai.community.Util;

import com.dai.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * Description: 线程对象用于存储用户信息【代替session】
 *
 * @author: DaiJF
 * @date: 2022/7/26 - 13:04
 */
@Component
public class HostHolder {

  //key就是线程对象，值为线程的变量副本
  private ThreadLocal<User> users = new ThreadLocal<>();

  /**
   * 以线程为key存入User
   */
  public void setUser(User user) {
    users.set(user);
  }

  /**
   * 从ThreadLocal中取出User
   */
  public User getUser() {
    return users.get();
  }

  /**
   * 释放线程 清理用户
   */
  public void clear() {
    users.remove();
  }
}

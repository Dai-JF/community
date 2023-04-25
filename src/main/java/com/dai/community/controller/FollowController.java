package com.dai.community.controller;

import com.dai.community.Util.CommunityUtil;
import com.dai.community.Util.HostHolder;
import com.dai.community.annotation.LoginRequired;
import com.dai.community.consts.CommunityConst;
import com.dai.community.entity.Event;
import com.dai.community.entity.Page;
import com.dai.community.entity.User;
import com.dai.community.event.EventProducer;
import com.dai.community.service.FollowService;
import com.dai.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * description:
 *
 * @author DaiJF
 */
@Controller
public class FollowController implements CommunityConst {

  @Autowired
  private HostHolder hostHolder;

  @Autowired
  private FollowService followService;

  @Autowired
  private UserService userService;
  @Autowired
  private EventProducer eventProducer;


  /**
   * 关注
   */
  @RequestMapping(path = "/follow", method = RequestMethod.POST)
  @ResponseBody
  @LoginRequired
  public String follow(int entityType, int entityId) {
    User user = hostHolder.getUser();

    followService.follow(user.getId(), entityType, entityId);

    /*
     * 触发关注事件
     * 关注完后，调用Kafka生产者，发送系统通知
     */
    Event event = new Event()
        .setTopic(TOPIC_FOLLOW)
        .setUserId(hostHolder.getUser().getId())
        .setEntityType(entityType)
        .setEntityId(entityId)
        .setEntityUserId(entityId);
    // 用户关注实体的id就是被关注的用户id->EntityId=EntityUserId
    eventProducer.fireEvent(event);

    return CommunityUtil.getJSONString(0, "关注成功!");
  }

  /**
   * 取关
   */
  @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
  @ResponseBody
  @LoginRequired
  public String unfollow(int entityType, int entityId) {
    User user = hostHolder.getUser();

    followService.unfollow(user.getId(), entityType, entityId);

    return CommunityUtil.getJSONString(0, "取关成功!");
  }


  /**
   * 查询某用户关注的人
   */
  @RequestMapping(path = "/followees/{userId}", method = RequestMethod.GET)
  public String getFollowees(@PathVariable("userId") int userId, Page page, Model model) {
    User user = userService.findUserById(userId);
    if (user == null) {
      throw new RuntimeException("该用户不存在!");
    }
    model.addAttribute("user", user);

    page.setLimit(5);
    page.setPath("/followees/" + userId);
    page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_USER));

    List<Map<String, Object>> userList = followService.findFollowees(userId, page.getOffset(),
        page.getLimit());
    if (userList != null) {
      for (Map<String, Object> map : userList) {
        User u = (User) map.get("user");
        map.put("hasFollowed", hasFollowed(u.getId()));
      }
    }
    model.addAttribute("users", userList);

    return "/site/followee";
  }

  /**
   * 查询某用户的粉丝
   */
  @RequestMapping(path = "/followers/{userId}", method = RequestMethod.GET)
  public String getFollowers(@PathVariable("userId") int userId, Page page, Model model) {
    User user = userService.findUserById(userId);
    if (user == null) {
      throw new RuntimeException("该用户不存在!");
    }
    model.addAttribute("user", user);

    page.setLimit(5);
    page.setPath("/followers/" + userId);
    page.setRows((int) followService.findFollowerCount(ENTITY_TYPE_USER, userId));

    List<Map<String, Object>> userList = followService.findFollowers(userId, page.getOffset(),
        page.getLimit());
    if (userList != null) {
      for (Map<String, Object> map : userList) {
        User u = (User) map.get("user");
        map.put("hasFollowed", hasFollowed(u.getId()));
      }
    }
    model.addAttribute("users", userList);

    return "/site/follower";
  }

  /**
   * 判断当前登录用户与关注、粉丝列表的关注关系
   **/
  private boolean hasFollowed(int userId) {
    if (hostHolder.getUser() == null) {
      return false;
    }

    return followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
  }


}

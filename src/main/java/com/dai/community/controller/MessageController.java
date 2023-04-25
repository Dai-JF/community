package com.dai.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.dai.community.Util.CommunityUtil;
import com.dai.community.Util.HostHolder;
import com.dai.community.annotation.LoginRequired;
import com.dai.community.consts.CommunityConst;
import com.dai.community.entity.Message;
import com.dai.community.entity.Page;
import com.dai.community.entity.User;
import com.dai.community.service.MessageService;
import com.dai.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

/**
 * description:
 *
 * @author DaiJF
 */
@Controller
public class MessageController implements CommunityConst {

  @Autowired
  HostHolder hostHolder;

  @Autowired
  MessageService messageService;

  @Autowired
  UserService userService;


  /**
   * 私信列表
   */
  @LoginRequired
  @RequestMapping(value = "/letter/list", method = RequestMethod.GET)
  public String getLetterList(Model model, Page page) {
    // 获取当前登录用户
    User user = hostHolder.getUser();
    // 分页信息
    page.setLimit(5);
    page.setPath("/letter/list");
    page.setRows(messageService.findConversationCount(user.getId()));
    // 查询所有会话
    List<Message> conversationList = messageService
        .findConversations(user.getId(), page.getOffset(), page.getLimit());
    //存放所有私信
    List<Map<String, Object>> conversations = new ArrayList<>();
    if (conversationList != null) {
      for (Message message : conversationList) {
        //存放与某用户的私信数据
        HashMap<String, Object> map = new HashMap<>();
        map.put("conversation", message);
        map.put("letterCount", messageService.findLetterCount(message.getConversationId()));
        map.put("unreadCount",
            messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));
        // 当前登录用户若与当前会话信息中fromId相同，则目标id为ToId;
        int targetId =
            user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
        User target = userService.findUserById(targetId);
        map.put("target", target);

        conversations.add(map);
      }
    }
    model.addAttribute("conversations", conversations);
    // 当前登录用户总未读条数
    int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
    model.addAttribute("letterUnreadCount", letterUnreadCount);

    int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
    model.addAttribute("noticeUnreadCount", noticeUnreadCount);

    return "/site/letter";
  }

  /**
   * 私信详情
   */
  @LoginRequired
  @RequestMapping(value = "/letter/detail/{conversationId}", method = RequestMethod.GET)
  public String getLetterDetail(@PathVariable("conversationId") String conversationId, Model model, Page page) {

    //分页信息
    page.setLimit(5);
    page.setPath("/letter/detail/" + conversationId);
    page.setRows(messageService.findLetterCount(conversationId));

    List<Message> letterList = messageService
        .findLetters(conversationId, page.getOffset(), page.getLimit());

    List<Map<String, Object>> letters = new ArrayList<>();

    if (letterList != null) {
      for (Message message : letterList) {
        HashMap<String, Object> map = new HashMap<>();
        //map封装每条私信
        map.put("letter", message);
        map.put("fromUser", userService.findUserById(message.getFromId()));

        letters.add(map);
      }
    }
    model.addAttribute("letters", letters);
    //私信目标用户
    model.addAttribute("target", getLetterTarget(conversationId));

    /*设置已读*/
    List<Integer> ids = getLetterIds(letterList);
    if (!ids.isEmpty()) {
      messageService.readMessage(ids);
    }

    return "site/letter-detail";
  }

  /**
   * 获取私信目标用户
   */
  private User getLetterTarget(String conversationId) {
    //拆分conversationId[101_107]
    String[] ids = conversationId.split("_");
    int id0 = Integer.parseInt(ids[0]);
    int id1 = Integer.parseInt(ids[1]);

    if (hostHolder.getUser().getId() == id0) {
      return userService.findUserById(id1);
    } else {
      return userService.findUserById(id0);
    }
  }

  /**
   * 获得批量私信的未读数id
   */
  private List<Integer> getLetterIds(List<Message> letterList) {
    List<Integer> ids = new ArrayList<>();

    if (letterList != null) {
      for (Message message : letterList) {
        //当前用户为目标用户 并且 status = 0 时才是未读数，加入未读私信集合
        if (hostHolder.getUser().getId() == message.getToId() && message.getStatus() == 0) {
          ids.add(message.getId());
        }
      }
    }
    return ids;
  }

  /**
   * 发送私信
   *
   * @param toName  对方
   * @param content 内容
   */
  @RequestMapping(value = "/letter/send", method = RequestMethod.POST)
  @ResponseBody
  @LoginRequired
  public String sendMsg(String toName, String content) {
    //根据目标发送人姓名获取其id
    User targetUser = userService.findUserByName(toName);

    if (targetUser == null) {
      return CommunityUtil.getJSONString(1, "目标用户不存在!");
    }

    Message message = new Message();
    message.setFromId(hostHolder.getUser().getId());
    message.setToId(targetUser.getId());
    // conversationId (如101_102: 小_大)
    if (message.getFromId() < message.getToId()) {
      message.setConversationId(message.getFromId() + "_" + message.getToId());
    } else {
      message.setConversationId(message.getToId() + "_" + message.getFromId());
    }
    message.setContent(content);
    message.setStatus(0);
    message.setCreateTime(new Date());

    messageService.addMessage(message);

    return CommunityUtil.getJSONString(0);
  }

  /**
   * 查询系统通知
   */
  @LoginRequired
  @RequestMapping(value = "/notice/list", method = RequestMethod.GET)
  public String getNoticeList(Model model) {

    User user = hostHolder.getUser();

    /*
     * 查询评论类通知
     */
    Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);
    if (message != null) {
      HashMap<String, Object> messageVO = new HashMap<>();
      messageVO.put("message", message);
      // 转化message表中content为HashMap类型
      //去掉转义字符
      String content = HtmlUtils.htmlUnescape(message.getContent());
      Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

      // 将content数据中的每一个字段都存入map
      // 用于显示->用户[user] (评论、点赞、关注[entityType])...了你的(帖子、回复、用户[entityId]) 查看详情连接[postId]
      messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
      messageVO.put("entityType", data.get("entityType"));
      messageVO.put("entityId", data.get("entityId"));
      messageVO.put("postId", data.get("postId"));

      // 共几条会话
      int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);
      messageVO.put("count", count);
      // 评论类未读数
      int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
      messageVO.put("unreadCount", unreadCount);

      model.addAttribute("commentNotice", messageVO);
    }

    /*
     * 查询点赞类通知
     */
    message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);

    if (message != null) {
      HashMap<String, Object> messageVO = new HashMap<>();
      messageVO.put("message", message);
      // 转化message表中content为HashMap<k,v>类型
      String content = HtmlUtils.htmlUnescape(message.getContent());
      Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
      // 将content数据中的每一个字段都存入map
      // 用于显示->用户[user] (评论、点赞、关注[entityType])...了你的(帖子、回复、用户[entityId]) 查看详情连接[postId]
      messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
      messageVO.put("entityType", data.get("entityType"));
      messageVO.put("entityId", data.get("entityId"));
      messageVO.put("postId", data.get("postId"));

      // 共几条会话
      int count = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
      messageVO.put("count", count);
      // 点赞类未读数
      int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
      messageVO.put("unreadCount", unreadCount);
      model.addAttribute("likeNotice", messageVO);
    }

    /*
     * 查询关注类通知
     */
    message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);

    if (message != null) {
      HashMap<String, Object> messageVO = new HashMap<>();
      messageVO.put("message", message);
      // 转化message表中content为HashMap<k,v>类型
      String content = HtmlUtils.htmlUnescape(message.getContent());
      Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
      // 将content数据中的每一个字段都存入map
      // 用于显示->用户[user] (评论、点赞、关注)...了你的(帖子、回复、用户[entityType]) 查看详情连接[postId]
      messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
      messageVO.put("entityType", data.get("entityType"));
      messageVO.put("entityId", data.get("entityId"));
      messageVO.put("postId", data.get("postId"));

      // 共几条会话
      int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
      messageVO.put("count", count);
      // 关注类未读数
      int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
      messageVO.put("unreadCount", unreadCount);
      model.addAttribute("followNotice", messageVO);
    }

    /*
     * 查询未读私信数量
     */
    int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
    model.addAttribute("letterUnreadCount", letterUnreadCount);
    /*
     * 查询所有未读系统通知数量
     */
    int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
    model.addAttribute("noticeUnreadCount", noticeUnreadCount);

    return "/site/notice";
  }

  /**
   * 查询系统通知详情页（分页）
   */
  @LoginRequired
  @RequestMapping(value = "/notice/detail/{topic}", method = RequestMethod.GET)
  public String getNoticeDetail(@PathVariable("topic") String topic, Page page, Model model) {
    User user = hostHolder.getUser();

    page.setLimit(5);
    page.setPath("/notice/detail/" + topic);
    page.setRows(messageService.findNoticeCount(user.getId(), topic));

    List<Message> noticeList = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
    // 聚合拼接User
    List<Map<String, Object>> noticeVoList = new ArrayList<>();
    if (noticeList != null) {
      for (Message notice : noticeList) {
        HashMap<String, Object> map = new HashMap<>();
        // 将查询出来的每一个通知封装Map
        map.put("notice", notice);
        // 发起事件的user
        map.put("user", userService.findUserById(user.getId()));

        // 把message中的content内容转化Object
        String content = HtmlUtils.htmlUnescape(notice.getContent());
        Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
        map.put("entityType", data.get("entityType"));
        map.put("entityId", data.get("entityId"));
        map.put("postId", data.get("postId"));
        // 系统通知->id=1的系统用户
        map.put("fromUser", userService.findUserById(notice.getFromId()));

        noticeVoList.add(map);
      }
    }
    model.addAttribute("notices", noticeVoList);

    /*
     * 设置已读(当打开这个页面是就更改status =1)
     */
    List<Integer> ids = getLetterIds(noticeList);
    if (!ids.isEmpty()) {
      messageService.readMessage(ids);
    }

    return "/site/notice-detail";
  }


}

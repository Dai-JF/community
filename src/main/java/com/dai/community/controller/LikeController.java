package com.dai.community.controller;

import com.dai.community.Util.CommunityUtil;
import com.dai.community.Util.HostHolder;
import com.dai.community.Util.RedisKeyUtil;
import com.dai.community.annotation.LoginRequired;
import com.dai.community.consts.CommunityConst;
import com.dai.community.entity.Event;
import com.dai.community.entity.User;
import com.dai.community.event.EventProducer;
import com.dai.community.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;

/**
 * description:
 *
 * @author DaiJF
 */
@Controller
public class LikeController implements CommunityConst {

  @Autowired
  LikeService likeService;

  @Autowired
  HostHolder hostHolder;

  @Autowired
  private EventProducer eventProducer;

  @Autowired
  RedisTemplate redisTemplate;


  @RequestMapping(value = "like", method = RequestMethod.POST)
  @ResponseBody
  @LoginRequired
  public String like(int entityType, int entityId, int entityUserId, int postId) {
    User user = hostHolder.getUser();

    //点赞
    likeService.like(user.getId(), entityType, entityId, entityUserId);
    // 数量
    long likeCount = likeService.findEntityLikeCount(entityType, entityId);
    // 状态
    int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);

    HashMap<String, Object> map = new HashMap<>();
    map.put("likeCount", likeCount);
    map.put("likeStatus", likeStatus);

    /*
     * 触发点赞事件
     * 只有点赞完后，才会调用Kafka生产者，发送系统通知，取消点赞不会调用事件
     */
    if (likeStatus == 1) {
      Event event = new Event()
          .setTopic(TOPIC_LIKE)
          .setEntityId(entityId)
          .setEntityType(entityType)
          .setUserId(user.getId())
          .setEntityUserId(entityUserId)
          .setData("postId", postId);
      // 注意：data里面存postId是因为点击查看后链接到具体帖子的页面
      eventProducer.fireEvent(event);
    }
    /*
     * 计算帖子分数
     * 将点赞过的帖子id存入set去重的redis集合------like()
     */
    if (entityType == ENTITY_TYPE_POST) {
      String redisKey = RedisKeyUtil.getPostScore();
      redisTemplate.opsForSet().add(redisKey, postId);
    }

    return CommunityUtil.getJSONString(0, null, map);
  }
}

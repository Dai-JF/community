package com.dai.community.controller;

import com.dai.community.Util.HostHolder;
import com.dai.community.Util.RedisKeyUtil;
import com.dai.community.consts.CommunityConst;
import com.dai.community.entity.Comment;
import com.dai.community.entity.DiscussPost;
import com.dai.community.entity.Event;
import com.dai.community.event.EventProducer;
import com.dai.community.service.CommentService;
import com.dai.community.service.DiscussPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

/**
 * Description:
 *
 * @author DaiJF
 */
@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConst {

  @Autowired
  HostHolder hostHolder;
  @Autowired
  CommentService commentService;
  @Autowired
  DiscussPostService discussPostService;
  @Autowired
  EventProducer eventProducer;
  @Autowired
  RedisTemplate redisTemplate;

  /**
   * description: 添加评论
   *
   * @param discussPostId 用于重定向回当前帖子页
   * @return java.lang.String
   */
  @RequestMapping(value = "/add/{discussPostId}", method = RequestMethod.POST)
  public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
    comment.setUserId(hostHolder.getUser().getId());
    comment.setStatus(0);
    comment.setCreateTime(new Date());
    commentService.addComment(comment);

    /*
     * 触发评论事件
     * 评论完后，调用Kafka生产者，发送系统通知
     */
    Event event = new Event()
        .setTopic(TOPIC_COMMENT)
        .setEntityId(comment.getEntityId())
        .setEntityType(comment.getEntityType())
        .setUserId(hostHolder.getUser().getId())
        .setData("postId", discussPostId);

    /*
     * event.setEntityUserId要分情况设置被发起事件的用户id
     * 1.评论的是帖子，被发起事件（评论）的用户->该帖子发布人id
     * 2.评论的是用户的评论，被发起事件（评论）的用户->该评论发布人id
     */
    if (comment.getEntityType() == ENTITY_TYPE_POST) {
      // 评论帖子
      DiscussPost target = discussPostService.findPost(comment.getEntityId());
      event.setEntityUserId(target.getUserId());
    } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
      // 评论回复
      Comment target = commentService.findCommentById(comment.getEntityId());
      event.setEntityUserId(target.getUserId());
    }
    eventProducer.fireEvent(event);


    /*
     * 增加评论时，将帖子异步提交到Elasticsearch服务器
     * 通过Kafka消息队列去提交，修改Elasticsearch中帖子的评论数
     */
    //若评论为帖子类型时，才需要加入消息队列处理
    if (comment.getEntityType() == ENTITY_TYPE_POST) {
      event = new Event()
          .setTopic(TOPIC_PUBLISH)
          .setUserId(comment.getUserId())
          .setEntityType(ENTITY_TYPE_POST)
          .setEntityId(discussPostId);
      eventProducer.fireEvent(event);
      /*
       * 计算帖子分数
       * 将评论过的帖子id存入set去重的redis集合------addComment()
       */
      String redisKey = RedisKeyUtil.getPostScore();
      redisTemplate.opsForSet().add(redisKey, discussPostId);


    }


    return "redirect:/discuss/detail/" + discussPostId;
  }


}

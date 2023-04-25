package com.dai.community.controller;

import com.dai.community.Util.CommunityUtil;
import com.dai.community.Util.HostHolder;
import com.dai.community.Util.RedisKeyUtil;
import com.dai.community.annotation.LoginRequired;
import com.dai.community.consts.CommunityConst;
import com.dai.community.entity.*;
import com.dai.community.event.EventProducer;
import com.dai.community.service.CommentService;
import com.dai.community.service.DiscussPostService;
import com.dai.community.service.LikeService;
import com.dai.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * Description:
 *
 * @author: DaiJF
 * @date: 2022/7/28 - 13:36
 */
@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConst {

  @Autowired
  private DiscussPostService discussPostService;
  @Autowired
  private HostHolder hostHolder;
  @Autowired
  UserService userService;
  @Autowired
  CommentService commentService;
  @Autowired
  private LikeService likeService;
  @Autowired
  private EventProducer eventProducer;
  @Autowired
  RedisTemplate redisTemplate;


  @RequestMapping(path = "/add", method = RequestMethod.POST)
  @ResponseBody
  @LoginRequired
  public String addDiscussPost(String title, String content) {
    User user = hostHolder.getUser();
    if (user == null) {
      //403:权限不够
      return CommunityUtil.getJSONString(403, "你还没有登录哦！");
    }
    DiscussPost post = new DiscussPost();
    post.setUserId(user.getId());
    post.setTitle(title);
    post.setContent(content);
    post.setCreateTime(new Date());

    discussPostService.addPost(post);

    /*
     * 发布帖子时，将帖子异步提交到Elasticsearch服务器
     * 通过Kafka消息队列去提交，将新发布的帖子存入Elasticsearch
     */
    Event event = new Event()
        .setTopic(TOPIC_PUBLISH)
        .setUserId(user.getId())
        .setEntityType(ENTITY_TYPE_POST)
        .setEntityId(post.getId());
    eventProducer.fireEvent(event);

    /*
     * 计算帖子分数
     * 将新发布的帖子id存入set去重的redis集合------addDiscussPost()
     */
    String redisKey = RedisKeyUtil.getPostScore();
    redisTemplate.opsForSet().add(redisKey, post.getId());

    //返回Json格式字符串给前端JS,报错的情况将来统一处理
    return CommunityUtil.getJSONString(0, "发布成功！");
  }

  @RequestMapping(value = "/detail/{postId}", method = RequestMethod.GET)
  public String getDetailPage(@PathVariable("postId") int postId, Model model, Page page) {
    //帖子
    DiscussPost post = discussPostService.findPost(postId);
    model.addAttribute("post", post);

    //用以显示发帖人的头像及用户名
    User user = userService.findUserById(post.getUserId());
    model.addAttribute("user", user);

    //点赞信息
    long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);
    model.addAttribute("likeCount", likeCount);

    int likeStatus = hostHolder.getUser() == null ? '0' :
        likeService.findEntityLikeStatus(hostHolder.getUser().getId(),
            ENTITY_TYPE_POST, postId);
    model.addAttribute("likeStatus", likeStatus);

    //评论分页信息
    page.setLimit(3);
    page.setPath("/discuss/detail/" + postId);
    page.setRows(post.getCommentCount());

    // 评论: 给帖子的评论
    // 回复: 给评论的评论
    // 评论列表集合
    List<Comment> commentList = commentService
        .findComments(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());

    // 评论VO(viewObject[显示对象])列表 (将comment,user信息封装到一个Map，Map再封装到List中)
    List<Map<String, Object>> commentVoList = new ArrayList<>();

    if (commentList != null) {
      // 每一条评论及该评论的用户封装进map集合
      for (Comment comment : commentList) {
        // 评论Map-->commentVo
        HashMap<String, Object> commentVo = new HashMap<>();
        // 添加评论
        commentVo.put("comment", comment);
        // 添加评论作者(由comment表中 entity = 1 查user表)
        commentVo.put("user", userService.findUserById(comment.getUserId()));

        //点赞信息
        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
        commentVo.put("likeCount", likeCount);

        likeStatus = hostHolder.getUser() == null ? '0' :
            likeService.findEntityLikeStatus(hostHolder.getUser().getId(),
                ENTITY_TYPE_COMMENT, comment.getId());
        commentVo.put("likeStatus", likeStatus);

        //==========================================================

        //回复列表（每一条评论的所有回复,不分页）
        List<Comment> replyList = commentService
            .findComments(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
        // 回复VO
        List<Map<String, Object>> replyVoList = new ArrayList<>();
        if (replyList != null) {
          for (Comment reply : replyList) {
            HashMap<String, Object> replyVo = new HashMap<>();
            // 回复
            replyVo.put("reply", reply);
            // 作者 (由comment表中 entity = 2 查user表)
            replyVo.put("user", userService.findUserById(reply.getUserId()));
            // 回复目标 (有2种：1.直接回复【targetId没值】 2.追加回复【targetId有值】)
            User target =
                reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
            replyVo.put("target", target);

            //点赞信息
            likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
            replyVo.put("likeCount", likeCount);

            likeStatus = hostHolder.getUser() == null ? '0' :
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(),
                    ENTITY_TYPE_COMMENT, reply.getId());
            replyVo.put("likeStatus", likeStatus);

            // 将每一个回复Map放在回复List中
            replyVoList.add(replyVo);
          }
        }
        // 将每一个回复List放在评论Map中
        commentVo.put("replies", replyVoList);
        // 回复数量统计
        int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
        commentVo.put("replyCount", replyCount);

        // 再将每一个评论Map放在评论List中
        commentVoList.add(commentVo);
      }
    }
    model.addAttribute("comments", commentVoList);

    return "site/discuss-detail";
  }

  /**
   * 置顶
   */
  @RequestMapping(value = "/top", method = RequestMethod.POST)
  @ResponseBody
  public String setTop(int id) {
    discussPostService.updateType(id, 1);

    // 触发发帖事件
    Event event = new Event()
        .setTopic(TOPIC_PUBLISH)
        .setUserId(hostHolder.getUser().getId())
        .setEntityType(ENTITY_TYPE_POST)
        .setEntityId(id);
    eventProducer.fireEvent(event);

    return CommunityUtil.getJSONString(0);
  }

  /**
   * 加精、取消加精
   */
  @RequestMapping(value = "/wonderful", method = RequestMethod.POST)
  @ResponseBody
  public String setWonderful(int id) {
    discussPostService.updateStatus(id, 1);

    // 触发发帖事件
    Event event = new Event()
        .setTopic(TOPIC_PUBLISH)
        .setUserId(hostHolder.getUser().getId())
        .setEntityType(ENTITY_TYPE_POST)
        .setEntityId(id);
    eventProducer.fireEvent(event);

    /*
     * 计算帖子分数
     * 将加精的帖子id存入set去重的redis集合-------setWonderful()
     */
    String redisKey = RedisKeyUtil.getPostScore();
    redisTemplate.opsForSet().add(redisKey, id);

    return CommunityUtil.getJSONString(0);
  }

  /**
   * 删除
   */
  @RequestMapping(value = "/delete", method = RequestMethod.POST)
  @ResponseBody
  public String setDelete(int id) {
    discussPostService.updateStatus(id, 2);

    // 触发删帖事件,将帖子从Elasticsearch中删除
    Event event = new Event()
        .setTopic(TOPIC_DELETE)
        .setUserId(hostHolder.getUser().getId())
        .setEntityType(ENTITY_TYPE_POST)
        .setEntityId(id);
    eventProducer.fireEvent(event);

    return CommunityUtil.getJSONString(0);
  }


}
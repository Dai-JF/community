package com.dai.community.controller;

import com.dai.community.consts.CommunityConst;
import com.dai.community.entity.DiscussPost;
import com.dai.community.entity.Page;
import com.dai.community.entity.User;
import com.dai.community.service.DiscussPostService;
import com.dai.community.service.LikeService;
import com.dai.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description:
 *
 * @author: DaiJF
 * @date: 2022/7/25 - 13:26
 */
@Controller
public class HomeController implements CommunityConst {

  @Autowired
  private DiscussPostService discussPostService;

  @Autowired
  private UserService userService;

  @Autowired
  LikeService likeService;

  //@RequestMapping(path = "/index", method = RequestMethod.GET)
  //public String getIndexPage(Model model, Page page) {
  //      /*方法调用前，springMVC自动实例化Model和Page,并将Page注入Model
  //        在thymeleaf中可以直接访问Page对象中的数据 */
  //
  //  //总行数
  //  page.setRows(discussPostService.findDiscussPostRows(0));
  //  page.setPath("/index");
  //
  //  //分页查询所有帖子
  //  List<DiscussPost> list = discussPostService
  //      .findDiscussPosts(0, page.getOffset(), page.getLimit());
  //
  //      /*将查询的post帖子和user用户名拼接后放入map中,最后把全部map放入新的List中,
  //        因为UserId是外键，需要显示的是对应用户名字 */
  //  List<Map<String, Object>> discussPost = new ArrayList<>();
  //
  //  if (list != null) {
  //    for (DiscussPost post : list) {
  //      Map<String, Object> map = new HashMap<>();
  //      // 将查询到的帖子放入map
  //      map.put("post", post);
  //      // 将发布帖子对应的用户id作为参数，查询完整的用户信息
  //      User user = userService.findUserById(post.getUserId());
  //      // 将发帖子的所有用户放入map
  //      map.put("user", user);
  //
  //      //查询帖子点赞信息
  //      long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
  //      map.put("likeCount", likeCount);
  //
  //      //将组合的map放入List<>
  //      discussPost.add(map);
  //
  //    }
  //  }
  //  model.addAttribute("discussPosts", discussPost);
  //  return "/index";
  //}

  /**
   * 统一异常处理,，获取错误页面
   */
  @RequestMapping(path = "/error", method = RequestMethod.GET)
  public String getErrorPage() {
    return "/error/500";
  }

  /**
   * 权限不足
   */
  @RequestMapping(value = "/denied", method = RequestMethod.GET)
  public String getDeniedPage() {
    return "/error/404";
  }

  /**
   * @param model
   * @param page
   * @param orderMode @RequestParam这是从前端传参数方法是：/index?xx与Controller绑定
   * @return java.lang.String
   * @date 2022/8/21 21:02
   */
  @RequestMapping(value = "/index", method = RequestMethod.GET)
  public String getIndexPage(Model model, Page page, @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {

    page.setRows(discussPostService.findDiscussPostRows(0));
    page.setPath("/index?orderMode=" + orderMode);

    List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit(), orderMode);
    List<Map<String, Object>> discussPost = new ArrayList<>();

    if (list != null) {
      for (DiscussPost post : list) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("post", post);
        User user = userService.findUserById(post.getUserId());
        map.put("user", user);
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
        map.put("likeCount", likeCount);
        discussPost.add(map);
      }
    }
    model.addAttribute("discussPosts", discussPost);
    model.addAttribute("orderMode", orderMode);
    return "/index";
  }
}

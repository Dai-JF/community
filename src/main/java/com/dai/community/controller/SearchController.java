package com.dai.community.controller;


import com.dai.community.consts.CommunityConst;
import com.dai.community.entity.DiscussPost;
import com.dai.community.entity.Page;
import com.dai.community.service.ElasticsearchService;
import com.dai.community.service.LikeService;
import com.dai.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description:
 *
 * @author DaiJF
 * @date 2022/8/11 - 20:36
 */
@Controller
public class SearchController implements CommunityConst {
  @Autowired
  private UserService userService;
  @Autowired
  private LikeService likeService;
  @Autowired
  private ElasticsearchService elasticsearchService;

  /**
   * search?keyword=xxx
   */
  @RequestMapping(value = "/search", method = RequestMethod.GET)
  public String search(String keyword, Page page, Model model) {
    // 搜索帖子
    // 在调用elasticsearchService完成搜索的时候，查询条件设置的是从第几页开始，所以要填getCurrent，填getOffset会导致翻页的时候查询错误
    org.springframework.data.domain.Page<DiscussPost> searchResult =
        elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());
    // 聚合数据
    List<Map<String, Object>> discussPosts = new ArrayList<>();

    if (searchResult != null) {
      for (DiscussPost post : searchResult) {
        Map<String, Object> map = new HashMap<>();
        // 帖子
        map.put("post", post);
        // 作者
        map.put("user", userService.findUserById(post.getUserId()));
        // 点赞数量
        map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));

        discussPosts.add(map);
      }
    }
    model.addAttribute("discussPosts", discussPosts);
    // 为了页面上取的默认值方便
    model.addAttribute("keyword", keyword);

    page.setPath("/search?keyword=" + keyword);
    page.setRows(searchResult == null ? 0 : (int) searchResult.getTotalElements());
    return "/site/search";
  }
}


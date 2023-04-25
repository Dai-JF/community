package com.dai.community.service;

import com.dai.community.dao.DiscussPostMapper;
import com.dai.community.entity.DiscussPost;
import com.dai.community.filter.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * Description:
 *
 * @author: DaiJF
 * @date: 2022/7/25 - 13:25
 */
@Service
public class DiscussPostService {

  @Autowired
  private DiscussPostMapper discussPostMapper;

  @Autowired
  private SensitiveFilter filter;

  public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
    return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
  }

  public int findDiscussPostRows(int userId) {
    return discussPostMapper.selectDiscussPostRows(userId);
  }

  public int addPost(DiscussPost post) {
    if (post == null) {
      throw new IllegalArgumentException("参数不能为空");
    }

    // 转义html标记
    post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
    post.setContent(HtmlUtils.htmlEscape(post.getContent()));
    // 过滤敏感词
    post.setTitle(filter.filter(post.getTitle()));
    post.setContent(filter.filter(post.getContent()));

    return discussPostMapper.insertDiscussPost(post);

  }

  public DiscussPost findPost(int discussPostId) {
    return discussPostMapper.selectDiscussPostById(discussPostId);
  }

  public int updateCommentCount(int id, int commentCount) {
    return discussPostMapper.updateCommentCount(id, commentCount);
  }

  public int updateType(int id, int type) {
    return discussPostMapper.updateType(id, type);
  }

  public int updateStatus(int id, int status) {
    return discussPostMapper.updateStatus(id, status);
  }

  public int updateScore(int id, double score) {
    return discussPostMapper.updateScore(id, score);
  }

}

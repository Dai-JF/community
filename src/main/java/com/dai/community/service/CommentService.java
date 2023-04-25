package com.dai.community.service;

import com.dai.community.consts.CommunityConst;
import com.dai.community.dao.CommentMapper;
import com.dai.community.entity.Comment;
import com.dai.community.filter.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * Description:
 * @author dai
 */
@Service
public class CommentService implements CommunityConst {

  @Autowired
  CommentMapper commentMapper;

  @Autowired
  SensitiveFilter sensitiveFilter;
  @Autowired
  DiscussPostService discussPostService;


  public List<Comment> findComments(int type, int id, int offset, int limit) {
    return commentMapper.selectCommentByEntity(type, id, offset, limit);
  }

  public int findCommentCount(int type, int id) {
    return commentMapper.selectCountByEntity(type, id);
  }

  /**
   * 添加评论(涉及事务) 先添加评论，后修改discuss_post中的评论数（作为一个整体事务，出错需要整体回滚！）
   */
  @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
  public int addComment(Comment comment) {
    if (comment == null) {
      throw new IllegalArgumentException("参数不能为空！");
    }

    /*
     * comment表 添加评论
     */

    //过滤标签
    comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
    //过滤敏感词
    comment.setContent(sensitiveFilter.filter(comment.getContent()));

    int rows = commentMapper.insertComment(comment);

    /*
     * discuss_post表 更新【帖子】评论数量
     * 如果是帖子类型才跟新，回复类型则不
     */
    if (comment.getEntityType() == ENTITY_TYPE_POST) {
      int count = commentMapper.selectCountByEntity(comment.getEntityType(), comment.getEntityId());
      discussPostService.updateCommentCount(comment.getEntityId(), count);
    }
    return rows;
  }

  public Comment findCommentById(int id) {
    return commentMapper.selectCommentById(id);
  }
}

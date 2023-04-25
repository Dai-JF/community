package com.dai.community.dao;

import com.dai.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Description:
 *
 * @author: DaiJF
 * @date: 2022/7/28 - 22:56
 */
@Mapper
public interface CommentMapper {

  List<Comment> selectCommentByEntity(int entityType, int entityId, int offset, int limit);

  int selectCountByEntity(int entityType, int entityId);

  int insertComment(Comment comment);

  Comment selectCommentById(int id);
}

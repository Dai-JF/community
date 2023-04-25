package com.dai.community.dao;

import com.dai.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Description:
 *
 * @author: DaiJF
 * @date: 2022/7/25 - 12:57
 */
@Mapper
public interface DiscussPostMapper {

  /**
   * 分页查询所有帖子，userId=0为所有帖子，!=0为个人帖子
   */
  List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int orderMode);

  /**
   * 查询某用户帖子行数，@Param注解用于给参数取别名,如果只有一个参数,并且在<if>里使用,则必须加别名
   */
  int selectDiscussPostRows(@Param("userId") int userId);

  int insertDiscussPost(DiscussPost discussPost);

  DiscussPost selectDiscussPostById(int id);

  int updateCommentCount(int id, int commentCount);

  int updateType(int id, int type);

  int updateStatus(int id, int status);

  int updateScore(int id, double score);



}

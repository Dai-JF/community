package com.dai.community.dao;

import com.dai.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageMapper {

  /**
   * 查询当前用户全部会话列表,针对每个会话只返回一条最新的私信
   */
  List<Message> selectConversations(int userId, int offset, int limit);

  /**
   * 查询当前用户的所有会话数量
   */
  int selectConversationCount(int userId);

  /**
   * 查询某个会话所包含的所有私信
   */
  List<Message> selectLetters(String conversationId, int offset, int limit);

  /**
   * 查询某个会话所包含的私信数量
   */
  int selectLetterCount(String conversationId);

  /**
   * 查询未读私信的数量 1.带参数conversationId ：私信未读数量 2.不带参数conversationId ：所有会话未读数量
   */
  int selectLetterUnreadCount(int userId, String conversationId);

  /**
   * 插入会话
   */
  int insertMessage(Message message);

  /**
   * 批量更改每个会话的所有未读消息为已读
   */
  int updateStatus(List<Integer> ids, int status);


  /**
   * 查询某个主题最新通知
   */
  Message selectLatestNotice(int userId, String topic);

  /**
   * 查询某个主题通知个数
   */
  int selectNoticeCount(int userId, String topic);

  /**
   * 查询某个主题未读个数(topic可为null,若为null:查询所有类系统未读通知个数)
   */
  int selectNoticeUnreadCount(int userId, String topic);

  /**
   * 分页查询某个主题的详情
   */
  List<Message> selectNotices(int userId, String topic, int offset, int limit);

}

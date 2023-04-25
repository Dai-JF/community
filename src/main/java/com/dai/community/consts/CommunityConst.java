package com.dai.community.consts;

/**
 * Description:
 *
 * @author DaiJF
 * @date 2022/7/25 - 22:37
 */
public interface CommunityConst {
  /* 以下用于注册功能 */
  /**
   * 激活成功
   */
  int ACTIVATION_SUCCESS = 0;
  /**
   * 重复激活
   */
  int ACTIVATION_REPEAT = 1;
  /**
   * 激活失败
   */
  int ACTIVATION_FAILURE = 2;

  /* 以下用于登录功能 */

  /**
   * 默认状态凭证的超时时间
   */
  int DEFAULT_EXPIRED_SECONDS = 3600 * 12;

  /**
   * 记住状态凭证的超时时间
   */
  int REMEMBER_EXPIRED_SECONDS = 3600 * 24 * 7;

  /* 以下用于评论功能 */
  /**
   * 回复类型：帖子
   */
  int ENTITY_TYPE_POST = 1;

  /**
   * 回复类型：评论
   */
  int ENTITY_TYPE_COMMENT = 2;

  /* 以下用于关注功能*/
  /**
   * 实体类型用户
   */
  int ENTITY_TYPE_USER = 3;


  /* 以下用于通知功能*/
  /**
   * 主题: 评论
   */
  String TOPIC_COMMENT = "comment";
  /**
   * 主题: 点赞
   */
  String TOPIC_LIKE = "like";
  /**
   * 主题: 关注
   */
  String TOPIC_FOLLOW = "follow";
  /**
   * 主题: 删除
   */
  String TOPIC_DELETE = "delete";
  /**
   * 系统ID
   */
  int SYSTEM_USER_ID = 1;

  /*以下用于搜索功能*/
  /**
   * Kafka主题: 发布帖子(常量接口)
   */
  String TOPIC_PUBLISH = "publish";

  /* 以下用于权限控制功能 */
  /**
   * 权限: 普通用户
   */
  String AUTHORITY_USER = "user";
  /**
   * 权限: 管理员
   */
  String AUTHORITY_ADMIN = "admin";
  /**
   * 权限: 版主
   */
  String AUTHORITY_MODERATOR = "moderator";

}

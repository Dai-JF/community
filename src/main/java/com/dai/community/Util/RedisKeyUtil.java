package com.dai.community.Util;

/**
 * description: 生成redis的key
 *
 * @author DaiJF
 * @date 2022/7/31 - 17:32
 */
public class RedisKeyUtil {

  private static final String SPLIT = ":";
  /** 实体key前缀 */
  private static final String PREFIX_ENTITY_LIKE = "like:entity";
  /** 用户key前缀 */
  private static final String PREFIX_USER_LIKE = "like:user";
  /** 关注key前缀 */
  private static final String PREFIX_FOLLOWEE = "followee";
  /** 粉丝key前缀 */
  private static final String PREFIX_FOLLOWER = "follower";
  /** 验证码key前缀 */
  private static final String PREFIX_KAPTCHA = "kapthca";
  /** 登录凭证key前缀 */
  private static final String PREFIX_TICKET = "ticket";
  /** 用户缓存 */
  private static final String PREFIX_USER = "user";
  /** UV (网站访问用户数量---根据Ip地址统计(包括没有登录的用户)) */
  private static final String PREFIX_UV = "uv";
  /** DAU (活跃用户数量---根据userId) */
  private static final String PREFIX_DAU = "dau";
  /** 热帖分数 (把需要更新的帖子id存入Redis当作缓存) */
  private static final String PREFIX_POST = "post";

  /**
   * 某实体的赞的【key】 ====>【 like:entity:entityType:entityId 】==key所对应的值==>【 set(userId) 】
   */
  public static String getEntityLikeKey(int entityType, int entityId) {
    return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
  }

  /**
   * 某用户收到的赞的【key】====>【 like:user:userId 】====>【int】
   */
  public static String getUserLikeKey(int userId) {
    return PREFIX_USER_LIKE + SPLIT + userId;
  }

  /**
   * 某用户关注的实体的【key】 ====> followee:userId:entityType ====> zset(entityId, date)
   */
  public static String getFolloweeKey(int userId, int entityType) {
    return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
  }

  /**
   * 某实体拥有的粉丝的【key】 ====> follower:entityType:entityId ====> zset(userId, date)
   */
  public static String getFollowerKey(int entityType, int entityId) {
    return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
  }

  /**
   * 某用户登录验证码的【key】 ====> kapthca：owner(用户临时凭证)
   */
  public static String getKaptchaKey(String owner) {
    return PREFIX_KAPTCHA + SPLIT + owner;
  }

  /**
   * 登录凭证
   */
  public static String getTicketKey(String ticket) {
    return PREFIX_TICKET + SPLIT + ticket;
  }

  /**
   * 用户缓存
   **/
  public static String getUserKey(int userId) {
    return PREFIX_USER + SPLIT + userId;
  }

  /**
   * 存储单日ip访问数量（uv）--HyperLogLog ---k:时间 v:ip  (HyperLogLog)
   */
  public static String getUVKey(String date) {
    return PREFIX_UV + SPLIT + date;
  }

  /**
   * 获取区间ip访问数量（uv） uv合并
   */
  public static String getUVKey(String startDate, String endDate) {
    return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
  }

  /**
   * 存储单日活跃用户（dau）--BitMap ---k:date v:userId索引下为true  (BitMap)
   * 示例：dau:20220526 = userId1索引--(true),userId2索引--(true),....
   */
  public static String getDAUKey(String date) {
    return PREFIX_DAU + SPLIT + date;
  }

  /**
   * 获取区间活跃用户
   * 示例：dau:20220526:20220526
   */
  public static String getDAUKey(String startDate, String endDate) {
    return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
  }


  /**
   * 帖子分数 (发布、点赞、加精、评论时放入)
   */
  public static String getPostScore() {
    return PREFIX_POST + SPLIT + "score";
  }
}

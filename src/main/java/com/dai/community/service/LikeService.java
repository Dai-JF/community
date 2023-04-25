package com.dai.community.service;

import static com.dai.community.Util.RedisKeyUtil.getUserLikeKey;

import com.dai.community.Util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

/**
 * description:
 *
 * @author DaiJF
 * @date 2022/7/31 - 19:10
 */
@Service
public class LikeService {

  @Autowired
  private RedisTemplate redisTemplate;

  /**
   * 点赞 set集合存储谁给某个实体点的赞
   *
   * @param userId       点赞者
   * @param entityType   被点赞的实体类型【帖子、评论】
   * @param entityId     实体id
   * @param entityUserId 被点赞者
   */
  public void like(int userId, int entityType, int entityId, int entityUserId) {
    // 连续两次执行更新操作，因此加上redis事务
    redisTemplate.execute(new SessionCallback() {
      @Override
      public Object execute(RedisOperations operations) throws DataAccessException {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        String userLikeKey = getUserLikeKey(entityUserId);

        // 判断用户是否点赞 【查询放事务之外】
        Boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);

        // 开启事务
        operations.multi();


        if (isMember) {
          operations.opsForSet().remove(entityLikeKey, userId);
          operations.opsForValue().decrement(userLikeKey);
        } else {
          operations.opsForSet().add(entityLikeKey, userId);
          operations.opsForValue().increment(userLikeKey);
        }

        //执行事务
        return operations.exec();
      }
    });
  }

  /**
   * 查询某用户获赞数量
   */
  public int findUserLikeCount(int userId) {
    String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
    Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
    return count == null ? 0 : count;
  }

  /**
   * 查询某实体(帖子、留言)点赞的数量
   */
  public long findEntityLikeCount(int entityType, int entityId) {
    String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
    return redisTemplate.opsForSet().size(entityLikeKey);
  }

  /**
   * 查询某人对某实体的点赞状态
   */
  public int findEntityLikeStatus(int userId, int entityType, int entityId) {
    String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
    // 1已点赞 0未点赞
    return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
  }
}

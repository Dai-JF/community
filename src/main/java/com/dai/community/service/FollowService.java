package com.dai.community.service;

import com.dai.community.Util.RedisKeyUtil;
import com.dai.community.consts.CommunityConst;
import com.dai.community.entity.User;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * @date 2022/8/1 - 11:45
 */
@Service
public class FollowService implements CommunityConst {

  @Autowired
  private RedisTemplate redisTemplate;

  @Autowired
  private UserService userService;

  /**
   * 关注 一项业务两次存储，事务
   */
  public void follow(int userId, int entityType, int entityId) {
    redisTemplate.execute(new SessionCallback() {
      @Override
      public Object execute(RedisOperations operations) throws DataAccessException {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

        operations.multi();

        // System.currentTimeMillis():用于统计排序
        operations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
        operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());

        return operations.exec();
      }
    });
  }

  /**
   * 取关 一项业务两次存储，事务
   */
  public void unfollow(int userId, int entityType, int entityId) {
    redisTemplate.execute(new SessionCallback() {
      @Override
      public Object execute(RedisOperations operations) throws DataAccessException {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

        operations.multi();

        operations.opsForZSet().remove(followeeKey, entityId);
        operations.opsForZSet().remove(followerKey, userId);

        return operations.exec();
      }
    });
  }

  /**
   * 查询关注数
   */
  public long findFolloweeCount(int userId, int entityType) {
    String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
    //zCard 统计数量
    return redisTemplate.opsForZSet().zCard(followeeKey);
  }

  /**
   * 查询粉丝数
   */
  public long findFollowerCount(int entityType, int entityId) {
    String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
    return redisTemplate.opsForZSet().zCard(followerKey);
  }

  /**
   * 查询当前用户是否已关注该实体
   */
  public boolean hasFollowed(int userId, int entityType, int entityId) {
    String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
    return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
  }

  /**
   * 查询某用户关注的人
   */
  public List<Map<String, Object>> findFollowees(int userId, int offset, int limit) {
    String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
    // reverseRange：倒序
    Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);

    if (targetIds == null) {
      return null;
    }

    List<Map<String, Object>> list = new ArrayList<>();
    for (Integer targetId : targetIds) {
      Map<String, Object> map = new HashMap<>();
      User user = userService.findUserById(targetId);
      map.put("user", user);
      Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
      map.put("followTime", new Date(score.longValue()));
      list.add(map);
    }

    return list;
  }

  /**
   * 查询某用户的粉丝
   */
  public List<Map<String, Object>> findFollowers(int userId, int offset, int limit) {
    String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
    Set<Integer> targetIds = redisTemplate.opsForZSet()
        .reverseRange(followerKey, offset, offset + limit - 1);

    if (targetIds == null) {
      return null;
    }

    List<Map<String, Object>> list = new ArrayList<>();
    for (Integer targetId : targetIds) {
      Map<String, Object> map = new HashMap<>();
      User user = userService.findUserById(targetId);
      map.put("user", user);
      Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
      map.put("followTime", new Date(score.longValue()));
      list.add(map);
    }

    return list;
  }

}
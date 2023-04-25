package com.dai.community.event;

import com.alibaba.fastjson2.JSONObject;
import com.dai.community.consts.CommunityConst;
import com.dai.community.entity.DiscussPost;
import com.dai.community.entity.Event;
import com.dai.community.entity.Message;
import com.dai.community.service.DiscussPostService;
import com.dai.community.service.ElasticsearchService;
import com.dai.community.service.MessageService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;

/**
 * description: Kafka事件消费者 (被动调用)
 * 对Message表扩充：1：系统通知，当生产者调用时，存入消息队列，消费者自动调用将event事件相关信息存入Message表
 *
 * @author DaiJF
 */
@Component
public class EventConsumer implements CommunityConst {

  private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

  @Autowired
  private MessageService messageService;
  @Autowired
  private DiscussPostService discussPostService;
  @Autowired
  ElasticsearchService elasticsearchService;

  /**
   * 处理事件
   */
  @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
  public void handleMessages(ConsumerRecord record) {

    if (record == null || record.value() == null) {
      logger.error("消息的内容为空!");
      return;
    }
    // 将消息内容字符串 转化为 Event对象
    Event event = JSONObject.parseObject(record.value().toString(), Event.class);
    // 注意：event中若data = null,是fastjson依赖版本的问题(不能太高1.0.xx)
    if (event == null) {
      logger.error("消息格式错误!");
      return;
    }
    // 发送系统通知
    Message message = new Message();
    message.setFromId(SYSTEM_USER_ID);
    // Message表中ToId设置为被发起事件的用户id
    message.setToId(event.getEntityUserId());
    // ConversationId设置为事件的主题（点赞、评论、关注）
    message.setConversationId(event.getTopic());
    message.setCreateTime(new Date());

    // 消息具体内容
    HashMap<String, Object> content = new HashMap<>();
    content.put("userId", event.getUserId());
    content.put("entityId", event.getEntityId());
    content.put("entityType", event.getEntityType());


    if (!event.getData().isEmpty()) {
      content.putAll(event.getData());
    }
    // 将content(map类型)转化成字符串类型封装进message
    message.setContent(JSONObject.toJSONString(content));
    messageService.addMessage(message);

  }

  /**
   * 消费帖子发布事件，将新增的帖子和添加评论后帖子评论数通过消息队列的方式save进Elasticsearch服务器中
   */
  @KafkaListener(topics = {TOPIC_PUBLISH})
  public void handleDiscussPostMessage(ConsumerRecord record) {
    if (record == null || record.value() == null) {
      logger.error("消息的内容为空!");
      return;
    }
    // 将record.value字符串格式转化为Event对象
    Event event = JSONObject.parseObject(record.value().toString(), Event.class);
    // 注意：event若data=null,是fastjson依赖版本的问题
    if (event == null) {
      logger.error("消息格式错误!");
      return;
    }
    DiscussPost post = discussPostService.findPost(event.getEntityId());
    elasticsearchService.saveDiscussPost(post);
  }

  /**
   * 帖子删除事件
   */
  @KafkaListener(topics = {TOPIC_DELETE})
  public void handleDeleteMessage(ConsumerRecord record) {
    if (record == null || record.value() == null) {
      logger.error("消息的内容为空!");
      return;
    }
    // 将record.value字符串格式转化为Event对象
    Event event = JSONObject.parseObject(record.value().toString(), Event.class);
    // 注意：event若data=null,是fastjson依赖版本的问题
    if (event == null) {
      logger.error("消息格式错误!");
      return;
    }
    elasticsearchService.deleteDiscussPost(event.getEntityId());
  }


}

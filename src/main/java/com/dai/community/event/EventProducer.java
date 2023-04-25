package com.dai.community.event;

import com.alibaba.fastjson2.JSONObject;
import com.dai.community.entity.Event;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * description: 生产者 触发事件时调用
 *
 * @author DaiJF
 * @date 2022/8/7 - 17:24
 */
@Component
public class EventProducer {

  @Resource
  private KafkaTemplate kafkaTemplate;

  /**
   * 处理事件
   */
  public void fireEvent(Event event) {
    // 将事件发布到指定的主题,内容为event对象转化的json字符串
    kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
  }
}

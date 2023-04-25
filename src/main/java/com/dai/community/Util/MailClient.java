package com.dai.community.Util;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Description:
 *
 * @author: DaiJF
 * @date: 2022/7/25 - 16:17
 */
@Component
public class MailClient {

  private static final Logger logger = LoggerFactory.getLogger(MailClient.class);

  @Resource
  private JavaMailSender javaMailSender;

  //将properties的属性注入到from
  @Value("${spring.mail.username}")
  private String from;


  public void sendMail(String to, String subject, String content) {
    try {
      //MimeMessage用于封装邮件相关信息
      MimeMessage message = javaMailSender.createMimeMessage();
      //需要一个邮件帮助器，负责构建MimeMessage对象
      MimeMessageHelper helper = new MimeMessageHelper(message);
      helper.setFrom(from);
      helper.setTo(to);
      helper.setSubject(subject);
      //支持HTML文本
      helper.setText(content, true);
      //发送邮件都有JavaMailSender来做
      javaMailSender.send(helper.getMimeMessage());
    } catch (MessagingException e) {
      logger.error("发送邮件失败：" + e.getMessage());
    }
  }
}


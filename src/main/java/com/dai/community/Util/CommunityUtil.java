package com.dai.community.Util;

import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.Map;
import java.util.UUID;

/**
 * Description:
 *
 * @author: DaiJF
 * @date: 2022/7/25 - 18:45
 */
public class CommunityUtil {
  /**
   * 生成随机字符串 salt：5位随机数加密
   */
  public static String generateUUID() {
    return UUID.randomUUID().toString().replaceAll("-", "");
  }

  /**
   * MD5加密
   * 密码+salt再去MD5加密
   * key：密码 + salt
   */
  public static String md5(String key) {
    if (StringUtils.isBlank(key)) {
      return null;
    }
    //MD5加密成十六进制字符串返回
    return DigestUtils.md5DigestAsHex(key.getBytes());
  }

  public static String getJSONString(int code, String msg, Map<String, Object> map) {
    JSONObject json = new JSONObject();
    json.put("code", code);
    json.put("msg", msg);
    if (map != null) {
      //从map里的key集合中取出每一个key
      for (String key : map.keySet()) {
        json.put(key, map.get(key));

      }
    }
    return json.toJSONString();
  }

  public static String getJSONString(int code, String msg) {
    return getJSONString(code, msg, null);
  }

  public static String getJSONString(int code) {
    return getJSONString(code, null, null);
  }


}
package com.dai.community.dao;

import com.dai.community.entity.LoginTicket;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Description: @Deprecated:不推荐使用
 *
 * @author: DaiJF
 * @date: 2022/7/26 - 9:05
 */
@Mapper
@Deprecated
public interface LoginTicketMapper {

  /**
   * 添加登录凭证ticket
   */
  @Insert({
      "insert into login_ticket(user_id,ticket,status,expired) values(#{userId},#{ticket},#{status},#{expired}) "})
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertLoginTicket(LoginTicket loginTicket);

  /**
   * 检查登录状态
   */
  @Select({"select id,user_id,ticket,status,expired from login_ticket where ticket=#{ticket}"})
  LoginTicket selectByTicket(String ticket);

  /**
   * 退出功能 修改status状态
   */
  @Update({"update login_ticket set status=#{status} where ticket=#{ticket} "})
  int updateStatus(@Param("ticket") String ticket, @Param("status") int status);

}

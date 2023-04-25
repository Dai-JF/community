package com.dai.community.controller;

import com.dai.community.Util.CommunityUtil;
import com.dai.community.Util.HostHolder;
import com.dai.community.annotation.LoginRequired;
import com.dai.community.consts.CommunityConst;
import com.dai.community.entity.User;
import com.dai.community.service.FollowService;
import com.dai.community.service.LikeService;
import com.dai.community.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Description:
 */
@Controller
@RequestMapping("/user")
public class UserController implements CommunityConst {

  private static final Logger logger = LoggerFactory.getLogger(UserController.class);

  @Value("${community.path.upload}")
  private String uploadPath;

  @Value("${community.path.domain}")
  private String domain;

  @Value("${server.servlet.context-path}")
  private String contextPath;

  @Autowired
  private UserService userService;

  @Autowired
  private LikeService likeService;

  @Autowired
  private FollowService followService;

  /**
   * 获得当前登录用户的信息
   */
  @Autowired
  private HostHolder hostHolder;

  @LoginRequired
  @RequestMapping(path = "/setting", method = RequestMethod.GET)
  public String getSettingPage() {
    return "site/setting";
  }

  /**
   * 上传头像
   */
  @LoginRequired
  @RequestMapping(value = "/upload", method = RequestMethod.POST)
  public String uploadHeader(MultipartFile headerImage, Model model) {
    // StringUtils.isBlank(headerImage)
    if (headerImage == null) {
      model.addAttribute("errorMsg", "您还没有选择图片！");
      return "/site/setting";
    }
    /*
     * 获得原始文件名字
     * 目的是：生成随机不重复文件名，防止同名文件覆盖
     * 方法：获取.后面的图片类型 加上 随机数
     */
    String filename = headerImage.getOriginalFilename();
    String suffix = filename.substring(filename.lastIndexOf("."));

    // 任何文件都可以上传,根据业务在此加限制
    if (StringUtils.isBlank(suffix)) {
      model.addAttribute("errorMsg", "文件格式不正确！");
      return "/site/setting";
    }

    // 生成随机文件名
    filename = CommunityUtil.generateUUID() + suffix;
    // 确定文件存放路劲
    File dest = new File(uploadPath + "/" + filename);
    try {
      // 将文件存入指定位置
      headerImage.transferTo(dest);
    } catch (IOException e) {
      logger.error("上传文件失败： " + e.getMessage());
      throw new RuntimeException("上传文件失败，服务器发生异常！", e);
    }
    // 更新当前用户的头像路径（web访问路径）
    // http://localhost:8080/community/user/header/xxx.png
    User user = hostHolder.getUser();
    String headerUrl = domain + contextPath + "/user/header/" + filename;
    userService.updateHeader(user.getId(), headerUrl);
    return "redirect:/index";
  }

  /**
   * 得到服务器图片
   */
  @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
  public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
    // 服务器存放路径(本地路径)
    fileName = uploadPath + "/" + fileName;
    // 文件后缀
    String suffix = fileName.substring(fileName.lastIndexOf("."));
    // 浏览器响应图片
    response.setContentType("image/" + suffix);
    try (
        // 图片是二进制用字节流
        FileInputStream fis = new FileInputStream(fileName);
        OutputStream os = response.getOutputStream();) {
      // 设置缓冲区
      byte[] buffer = new byte[1024];
      // 设置游标
      int b = 0;
      while ((b = fis.read(buffer)) != -1) {
        os.write(buffer, 0, b);
      }
    } catch (IOException e) {
      logger.error("读取头像失败: " + e.getMessage());
    }
  }

  /**
   * 修改密码
   */
  @LoginRequired
  @RequestMapping(path = "/pwd", method = RequestMethod.POST)
  public String updatePassword(String oldPwd, String newPwd, String checkPwd, Model model) {
    User user = hostHolder.getUser();
    Map<String, Object> map = userService.updatePassword(user.getId(), oldPwd, newPwd, checkPwd);
    if (map == null || map.isEmpty()) {
      return "redirect:/logout";
    }
    model.addAttribute("oldPwdMsg", map.get("oldPwdMsg"));
    model.addAttribute("newPwdMsg", map.get("newPwdMsg"));
    model.addAttribute("checkPwdMsg", map.get("checkPwdMsg"));
    return "/site/setting";
  }

  /**
   * 用户主页
   */
  @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
  public String getProfilePage(@PathVariable("userId") int userId, Model model) {
    User user = userService.findUserById(userId);
    if (user == null) {
      throw new RuntimeException("该用户不存在!");
      //return "site/login";
    }

    // 用户
    model.addAttribute("user", user);

    // 点赞数量
    int likeCount = likeService.findUserLikeCount(userId);
    model.addAttribute("likeCount", likeCount);

    // 关注数量
    long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
    model.addAttribute("followeeCount", followeeCount);

    // 粉丝数量
    long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
    model.addAttribute("followerCount", followerCount);

    // 是否已关注
    boolean hasFollowed = false;
    if (hostHolder.getUser() != null) {
      hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER,
          userId);
    }
    model.addAttribute("hasFollowed", hasFollowed);

    return "/site/profile";
  }
}

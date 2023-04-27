package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
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
import java.io.*;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static  final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domin}")
    private String domin;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(){
        return "/site/setting";
    }

    @LoginRequired
    @RequestMapping(path = "/upload" , method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model){
        if (headerImage == null){
            model.addAttribute("error", "上传错误");
            return "/site/setting";
        }
        String fileName = headerImage.getOriginalFilename();
        //获取最后一个.以及之后的子串
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error", "文件格式不正确");
            return "/site/setting";
        }
        //生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;
        //确定文件存放的位置
        File dest = new File(uploadPath + "/" + fileName);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败" + e.getMessage());
            throw  new RuntimeException("上传文件失败", e);
        }
        //更新当前用户头像的路径（web访问路径)
        //localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domin + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);
        return "redirect:/index";

    }
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response){
        //服务器存放路径
        fileName = uploadPath + "/" + fileName;
        //文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        //响应图片
        response.setContentType("image/" + suffix);
        try (FileInputStream fis = new FileInputStream(fileName);
             OutputStream os = response.getOutputStream();)
        {
            byte [] buffer = new byte[1024];
            int b = 0;
            while((b = fis.read(buffer)) != -1){
                os.write(buffer, 0 , b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败" + e.getMessage());
            throw  new RuntimeException("上传失败", e);
        }
    }

    @RequestMapping(path = "/updatePassword" , method = RequestMethod.POST)
    public String updatePassword(String oldPassword, String newPassword, Model model){
        User user = hostHolder.getUser();
        Map<String, Object> map = userService.updatePassword(user.getId(), oldPassword, newPassword);
        if(map == null || map.isEmpty()){
            return "redirect:/logout";
        }
        else{
            model.addAttribute("oldPasswordMsg", map.get(" oldPasswordMsg"));
            model.addAttribute("newPasswordMsg", map.get(" newPasswordMsg"));
            return "/site/setting";
        }
    }

    @RequestMapping(path = "/profile/{userId}" , method = RequestMethod.GET)
    public String getProfilePage(@PathVariable int userId , Model model){
        User user = userService.findUserById(userId);
        if(user == null){
            throw new RuntimeException("该用户不存在");
        }
        //用户信息传达给页面
        model.addAttribute("user" , user);
        //用户获赞的数量
       int likeCount =  likeService.findUserLikeCount(user.getId());
       model.addAttribute("likeCount" , likeCount);
        //关注数量
        long followeeCount = followService.findFolloweeCount(userId , ENTITY_TYPE_USER );
        model.addAttribute("followeeCount" , followeeCount);
        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER,userId);
        model.addAttribute("followerCount" , followerCount);
        //是否关注
        boolean hasFollowed = false;
        if(hostHolder.getUser() != null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER ,userId);
        }
        model.addAttribute("hasFollowed" , hasFollowed);
       return "/site/profile";
    }




}

package com.forum.community.controller;

import com.forum.community.annotation.LoginRequired;
import com.forum.community.entity.Comment;
import com.forum.community.entity.DiscussPost;
import com.forum.community.entity.Page;
import com.forum.community.entity.User;
import com.forum.community.service.*;
import com.forum.community.util.CommunityConstant;
import com.forum.community.util.CommunityUtil;
import com.forum.community.util.HostHolder;
import com.forum.community.util.S3Util;
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
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private DiscussService discussPostService;

    @Autowired
    private S3Util s3Util;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(){
        return "/site/setting";
    }

    @LoginRequired
    @RequestMapping(path = "/upload",method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model){
        if(headerImage == null){
            model.addAttribute("error","No picture is chosen.");
            return "/site/setting";
        }

        String fileName = headerImage.getOriginalFilename();
        String surffix = fileName.substring(fileName.lastIndexOf("."));
        if (StringUtils.isBlank(surffix)){
            model.addAttribute("error","File format incorrect.");
            return "/site/setting";
        }

        //生成随机文件名
        fileName = CommunityUtil.generateUUID() + surffix;
        //确定文件存放的路径
//        File dest = new File(uploadPath + "/" + fileName);
        try {
            // upload to S3
            s3Util.uploadFile(fileName,headerImage,"1024forum-avatar");
            //存储文件
//            headerImage.transferTo(dest);
        } catch (Exception e) {
            logger.error("上传文件失败：" + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常", e);
        }

        //更新当前用户的头像的路径（web访问路径）
        //http://localhost:8081/community/user/header/xxx.png
//        User user = hostHolder.getUser();
//        String headerUrl = domain + contextPath + "/user/header/" + fileName;

        //S3 Url
        //https://1024forum-avatar.s3.amazonaws.com/912367b66d56442e8d2bcd9dcdedcf82.jpg
        User user = hostHolder.getUser();
        String headerUrl = "https://1024forum-avatar.s3.amazonaws.com/" + fileName;

        userService.updateHeader(user.getId(),headerUrl);

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
        try (
                FileInputStream fis = new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
        ){
            byte[] buffer = new byte[1024];
            int b = 0;
            while((b = fis.read(buffer)) != -1){
                os.write(buffer,0,b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败：" + e.getMessage());
        }
    }

    @RequestMapping(path = "/updatePassword",method = RequestMethod.POST)
    public String updatePassword(String oldPassword, String newPassword, Model model) {
        User user = hostHolder.getUser();
        Map<String, Object> map = userService.updatePassword(user.getId(), oldPassword, newPassword);
        if (map == null || map.isEmpty()) {
            return "redirect:/logout";
        } else {
            model.addAttribute("oldPasswordMsg", map.get("oldPasswordMsg"));
            model.addAttribute("newPasswordMsg", map.get("newPasswordMsg"));
            return "/site/setting";
        }
    }

    // 个人主页
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model){
        User user = userService.findUserById(userId);
        if(user == null){
            throw new RuntimeException("该用户不存在！");
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
        if(hostHolder.getUser() != null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
            model.addAttribute("hasFollowed", hasFollowed);
        }

        return "/site/profile";
    }

    // 我的帖子
    @RequestMapping(path = "/mypost/{userId}", method = RequestMethod.GET)
    public String getMyPost(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user", user);

        // 分页信息
        page.setPath("/user/mypost/" + userId);
        page.setRows(discussPostService.findDiscussPostRows(userId));

        // 帖子列表
        List<DiscussPost> discussList = discussPostService
                .findDiscussPosts(userId, page.getOffset(), page.getLimit(),0);
        List<Map<String, Object>> discussVOList = new ArrayList<>();
        if (discussList != null) {
            for (DiscussPost post : discussList) {
                Map<String, Object> map = new HashMap<>();
                map.put("discussPost", post);
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                discussVOList.add(map);
            }
        }
        model.addAttribute("discussPosts", discussVOList);

        return "/site/my-post";
    }

    // 我的回复
    @RequestMapping(path = "/myreply/{userId}", method = RequestMethod.GET)
    public String getMyReply(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user", user);

        // 分页信息
        page.setPath("/user/myreply/" + userId);
        page.setRows(commentService.findUserCount(userId));

        // 回复列表
        List<Comment> commentList = commentService.findUserComments(userId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> commentVOList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                Map<String, Object> map = new HashMap<>();
                map.put("comment", comment);
                DiscussPost post = discussPostService.findDiscussPostById(comment.getEntityId());
                map.put("discussPost", post);
                commentVOList.add(map);
            }
        }
        model.addAttribute("comments", commentVOList);

        return "/site/my-reply";
    }


}

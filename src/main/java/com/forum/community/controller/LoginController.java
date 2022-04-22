package com.forum.community.controller;

import com.forum.community.entity.User;
import com.forum.community.service.UserService;
import com.forum.community.util.CommunityConstant;
import com.forum.community.util.CommunityUtil;
import com.forum.community.util.RedisKeyUtil;
import com.google.code.kaptcha.Producer;
import org.apache.commons.lang3.StringUtils;
import org.omg.CORBA.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaproduer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @RequestMapping(value = "/register",method = RequestMethod.GET)
    public String getRegisterPage(){
        return "/site/register";
    }

    @RequestMapping(value = "/login",method = RequestMethod.GET)
    public String getLoginPage(){
        return "/site/login";
    }

    @RequestMapping(path = "/register",method = RequestMethod.POST)
    public String register(Model model, User user){
        Map<String, Object> map = userService.register(user);
        if(map == null || map.isEmpty()){
            model.addAttribute("msg","Registration is successful. We have sent an activation email to you. Please activate quickly.");
            model.addAttribute("target","/index");
            return "/site/operate-result";
        }else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("emailMsg",map.get("emailMsg"));

            return "/site/register";
        }
    }

    //http://localhost:8080/community/activation/userId/code
    @RequestMapping(path = "/activation/{userId}/{code}",method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId,@PathVariable("code") String code){
        int result = userService.activation(userId,code);
        if(result == ACTIVATION_SUCCESS){
            model.addAttribute("msg","Activation is successful!");
            model.addAttribute("target","/login");
        }else if(result == ACTIVATION_REPEAT){
            model.addAttribute("msg","Invalid process. This account is active!");
            model.addAttribute("target","/index");
        }else{
            model.addAttribute("msg","Activation failure.");
            model.addAttribute("target","/index");
        }

        return "/site/operate-result";
    }

    @RequestMapping(path = "/kaptcha",method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session){
        //生成验证码
        String text = kaptchaproduer.createText();
        BufferedImage image = kaptchaproduer.createImage(text);

        //将验证码存入session
        // session.setAttribute("kaptcha",text);

        // 验证码的归属
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        //将验证码存入Redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);

        //将图片输出给浏览器
        response.setContentType("image/png");

        try {
            OutputStream outputStream = response.getOutputStream();
            ImageIO.write(image,"png",outputStream);
        } catch (IOException e) {
            logger.error("响应验证码失败：" + e.getMessage());
        }
    }

    @RequestMapping(path = "/login",method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme, Model model,
                        HttpSession session, HttpServletResponse response,
                        @CookieValue("kaptchaOwner") String kaptchaOwner){

        //检查验证码
//        String kaptcha = (String) session.getAttribute("kaptcha");

        String kaptcha = null;
        if(StringUtils.isNoneBlank(kaptchaOwner)){
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }

        if(StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)){
            model.addAttribute("codeMsg","Verification code incorrect.");
            return "/site/login";
        }

        //检查账号，密码
        int expiredSeconds = rememberme? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String,Object> map = userService.login(username,password,expiredSeconds);
        if(map.containsKey("ticket")){
            Cookie cookie = new Cookie("ticket",map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "/site/login";
        }
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }

    // 忘记密码
    @RequestMapping(path = "/forget",method = RequestMethod.GET)
    public String getForgetPage(){
        return "site/forget";
    }

    @RequestMapping(path = "/getCode", method = RequestMethod.GET)
    public String getCode(String email, Model model) {
        Map<String, Object> map = userService.getCode(email);
        if (map.containsKey("emailMsg")) {//有错误的情况下
            model.addAttribute("emailMsg", map.get("emailMsg"));
        } else {//正确的情况下，向邮箱发送了验证码
            model.addAttribute("msg", "Verification sent successfully.");

//            //将验证码存放在 session 中，后序和用户输入的信息进行比较
//            session.setAttribute("code",map.get("code"));
//            //后序判断用户输入验证码的时候验证码是否已经过期
//            session.setAttribute("expirationTime",map.get("expirationTime"));

        }
        return "site/forget";
    }

    @RequestMapping(path = "/forget", method = RequestMethod.POST)
    public String forget(Model model, String email, String verifycode, String password) {
        Map<String, Object> map = userService.forget(email, verifycode, password);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "Password changed successfully. Please use new password to login");
            model.addAttribute("target", "/login");
            return "site/operate-result";
        } else {
            model.addAttribute("emailMsg", map.get("emailMsg"));
            model.addAttribute("codeMsg", map.get("codeMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/forget";
        }
    }

}

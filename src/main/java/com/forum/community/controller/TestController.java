package com.forum.community.controller;

import com.forum.community.util.CommunityUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Controller
public class TestController {

    @RequestMapping(path = "/cookie/set", method = RequestMethod.GET)
    @ResponseBody
    public String setCookie(HttpServletResponse response){
        //创建Cookie,一个Cookie对象只能存放一组数据
        Cookie cookie = new Cookie("code", CommunityUtil.generateUUID());
        //设置cookie生效的范围
        cookie.setPath("/community/");
        //设置cookie的生存时间,如果不设置则默认存储在内存中,关闭浏览器cookie也会删除
        //设置生存时间以后cookie存在硬盘中,可长期有效
        cookie.setMaxAge(60 * 10);
        //发送cookie
        response.addCookie(cookie);

        return "Set Cookie...";
    }

    @RequestMapping(path = "/cookie/get", method = RequestMethod.GET)
    @ResponseBody
    public String getCookie(@CookieValue("code") String code){
        System.out.println(code);
        return code;
    }

    @RequestMapping(path = "/session/set",method = RequestMethod.GET)
    @ResponseBody
    public String setSession(HttpSession session){
        session.setAttribute("id",1);
        session.setAttribute("name","Test");
        return "set session...";
    }

    @RequestMapping(path = "/session/get",method = RequestMethod.GET)
    @ResponseBody
    public String getSession(HttpSession session){
        System.out.println(session.getAttribute("id"));
        System.out.println(session.getAttribute("name"));

        return "get session...";
    }

    //Ajax test
    @RequestMapping(path = "/test/ajax", method = RequestMethod.POST)
    @ResponseBody
    public String testAjax(String name, int age){
        System.out.println(name);
        System.out.println(age);
        return CommunityUtil.getJSONString(0,"操作成功！");
    }
}

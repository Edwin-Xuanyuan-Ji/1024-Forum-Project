package com.forum.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import com.forum.community.util.MailClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MailTests {

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void testTextMail(){
        mailClient.sendMail("edjige99@gmail.com","大家好","欢迎来到1024论坛");
    }

    @Test
    public void testHtmlMail(){
        Context context = new Context();
        context.setVariable("username","Edward");

        String content = templateEngine.process("/mail/demo",context);
        System.out.println(content);

        mailClient.sendMail("edjige99@gmail.com","HTML",content);
    }
}

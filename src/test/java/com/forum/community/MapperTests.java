package com.forum.community;

import com.forum.community.dao.DiscussPostMapper;
import com.forum.community.dao.LoginTicketMapper;
import com.forum.community.dao.MessageMapper;
import com.forum.community.dao.UserMapper;
import com.forum.community.entity.DiscussPost;
import com.forum.community.entity.LoginTicket;
import com.forum.community.entity.Message;
import com.forum.community.entity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTests {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Test
    public void testSelectPosts() {
        List<DiscussPost> list = discussPostMapper.selectDiscussPosts(0, 0, 10,0);
        for(DiscussPost post : list) {
            System.out.println(post);
        }

        int rows = discussPostMapper.selectDiscussPostRows(0);
        System.out.println(rows);
    }

    @Test
    public void testInsertLoginTicket(){
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(123);
        loginTicket.setStatus(0);
        loginTicket.setTicket("1234567ABCDEFG");
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 600 * 1000)); //10分钟有600 * 100ms

        loginTicketMapper.insertLoginTicket(loginTicket);
    }

    @Test
    public void testSelectLoginTicket(){
        LoginTicket loginTicket = loginTicketMapper.selectByTicket("1234567ABCDEFG");
        System.out.println(loginTicket);

        loginTicketMapper.updateStatus("1234567ABCDEFG",1);
        loginTicket = loginTicketMapper.selectByTicket("1234567ABCDEFG");
        System.out.println(loginTicket);
    }

    @Test
    public void testSelectLetters(){
        List<Message> list = messageMapper.selectConversations(8,0,20);
        for (Message message : list){
            System.out.println(message);
        }

        int count = messageMapper.selectConversationCount(7);
        System.out.println(count);

        list = messageMapper.selectLetters("7_8",0,10);
        for (Message message : list){
            System.out.println(message);
        }

        count = messageMapper.selectLetterCount("7_8");
        System.out.println(count);

        count = messageMapper.selectLetterUnreadCount(7,"7_8");
        System.out.println(count);
    }

}

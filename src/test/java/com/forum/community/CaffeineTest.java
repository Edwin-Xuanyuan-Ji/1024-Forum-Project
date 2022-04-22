package com.forum.community;

import com.forum.community.service.DiscussService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class CaffeineTest {
    @Autowired
    private DiscussService discussService;

    @Test
    public void testCache(){
        System.out.println(discussService.findDiscussPosts(0,0,10,1));
        System.out.println(discussService.findDiscussPosts(0,0,10,1));
        System.out.println(discussService.findDiscussPosts(0,0,10,1));
        System.out.println(discussService.findDiscussPosts(0,0,10,0));
    }

}

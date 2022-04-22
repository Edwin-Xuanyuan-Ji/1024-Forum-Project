package com.forum.community;

import net.minidev.json.JSONUtil;
import org.junit.Test;
import org.junit.jupiter.params.shadow.com.univocity.parsers.conversions.ObjectConversion;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class RedisTests {

    @Autowired
    public RedisTemplate<String, Object> redisTemplate;

    @Test
    public void redisTest(){
        redisTemplate.opsForValue().set("test:count", 1);

        System.out.println(redisTemplate.opsForValue().get("test:count"));
        System.out.println(redisTemplate.opsForValue().increment("test:count"));
        System.out.println(redisTemplate.opsForValue().decrement("test:count"));
    }

    // 多次访问同一个key, 可以bound
    @Test
    public void testBoundOperations(){
        String redisKey = "test:count";
        BoundValueOperations operations = redisTemplate.boundValueOps(redisKey);
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        System.out.println(operations.get());
    }

    // 编程式事务
    @Test
    public void testTransaction(){
        Object obj = redisTemplate.execute(new SessionCallback() {

            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String redisKey = "test:tx";

                // 开启事务
                operations.multi();

                operations.opsForSet().add(redisKey,"zhangsan");
                operations.opsForSet().add(redisKey,"lisi");
                operations.opsForSet().add(redisKey,"wangwu");

                // 无效
                System.out.println(operations.opsForSet().members(redisKey));

                // 提交事务
                return operations.exec();
            }
        });
        System.out.println(obj);
    }

    @Test
    public void testHyperLogLogUnion(){
        String redisKey = "test:0728";
        String ip = "0.0.0.0";
        redisTemplate.opsForHyperLogLog().add(redisKey,2);
    }
}

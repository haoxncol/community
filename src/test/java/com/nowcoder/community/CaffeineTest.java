package com.nowcoder.community;


import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Date;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class CaffeineTest {
    @Autowired
    private DiscussPostService discussPostService;

    @Test
    public void initDataForTest(){
        for (int i = 0; i < 300000; i++) {
            DiscussPost post = new DiscussPost();
            post.setUserId(111);
            post.setTitle("牛客社区负载测试");
            post.setContent("负载测试 负载测试 负载测试 负载测试 负载测试 负载测试 负载测试");
            post.setCreateTime(new Date());
            post.setScore(Math.random() * 2000);
            discussPostService.addDiscussPost(post);
        }
    }

    @Test
    public void testCache(){
        System.out.println(discussPostService.finDiscussPosts(0,0,10,1));
        System.out.println(discussPostService.finDiscussPosts(0,0,10,1));
        System.out.println(discussPostService.finDiscussPosts(0,0,10,1));
        System.out.println(discussPostService.finDiscussPosts(0,0,10,0));
    }


}

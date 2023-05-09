package com.nowcoder.community;


import com.nowcoder.community.config.RedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.TimeUnit;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class RedisTest {
    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void test(){
        String redisKey = "test:count";
        redisTemplate.opsForValue().set(redisKey, 1);
        System.out.println(redisTemplate.opsForValue().get(redisKey));
        System.out.println(redisTemplate.opsForValue().increment(redisKey));
        System.out.println(redisTemplate.opsForValue().decrement(redisKey));
    }

    @Test
    public void testHash(){
        String redisKey = "test:user";

        redisTemplate.opsForHash().put(redisKey, "id", 1);
        redisTemplate.opsForHash().put(redisKey, "username", "王选");

        System.out.println(redisTemplate.opsForHash().get(redisKey, "id"));
        System.out.println(redisTemplate.opsForHash().get(redisKey, "username"));
    }

    @Test
    public void testList(){
        String redisKey = "test:ids";
        redisTemplate.opsForList().leftPush(redisKey, 101);
        redisTemplate.opsForList().leftPush(redisKey, 102);
        redisTemplate.opsForList().leftPush(redisKey, 103);

        System.out.println(redisTemplate.opsForList().size(redisKey));

    }

    @Test
    public void testSet(){
        String redisKey = "test:teachers";

        redisTemplate.opsForSet().add(redisKey, "张三", "李四", "王五", "张三", "李四");
        System.out.println(redisTemplate.opsForSet().size(redisKey));  //结果为3，因为set中不允许重复元素
        System.out.println(redisTemplate.opsForSet().pop(redisKey));
        System.out.println(redisTemplate.opsForSet().size(redisKey));

    }

    @Test
    public void testSortedSet(){
        String redisKey = "test:students";

        redisTemplate.opsForZSet().add(redisKey, "唐僧", 80);
        redisTemplate.opsForZSet().add(redisKey, "悟空", 90);
        redisTemplate.opsForZSet().add(redisKey, "八戒", 50);
        redisTemplate.opsForZSet().add(redisKey, "沙僧", 70);
        redisTemplate.opsForZSet().add(redisKey, "白龙马", 60);

        System.out.println(redisTemplate.opsForZSet().zCard(redisKey));  //结果为5 统计数据
        System.out.println(redisTemplate.opsForZSet().score(redisKey, "悟空"));  //结果为90 查询分数
        System.out.println(redisTemplate.opsForZSet().reverseRank(redisKey, "八戒"));  //查询排名 结果为4(返回的索引）
        System.out.println(redisTemplate.opsForZSet().reverseRange(redisKey,0,2)); //[悟空, 唐僧, 沙僧]

    }

    @Test
    public void testKeys(){
        redisTemplate.delete("test:user");

        System.out.println(redisTemplate.hasKey("test:user"));

        redisTemplate.expire("test:students", 10, TimeUnit.SECONDS);  //设置过期时间")
    }

    //多次访问同一个key
    @Test
    public void testBoundOperations(){
        String redisKey = "test:count";
        BoundValueOperations operations = redisTemplate.boundValueOps(redisKey);
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        System.out.println(operations.get());
    }

    //编程式事务
    @Test
    public void testTransactional(){
        Object obj = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String redisKey = "test:tx";

                operations.multi();  //开启事务

                operations.opsForSet().add(redisKey, "张三");
                operations.opsForSet().add(redisKey, "李四");
                operations.opsForSet().add(redisKey, "王五");

                System.out.println(operations.opsForSet().members(redisKey));

                return operations.exec();  //提交事务
            }
        });
        System.out.println(obj);
    }

    //统计20w个重复数据的独立总数
    @Test
    public void testHyperLogLog(){
        String rediskey = "test:hll:01";
        for (int i = 1; i <=100000 ; i++) {
            redisTemplate.opsForHyperLogLog().add(rediskey,i);
        }
        for (int i = 1; i <=100000 ; i++) {
            int r = (int) (Math.random() * 100000 + 1);  //[1,1000001)
            redisTemplate.opsForHyperLogLog().add(rediskey,r);
        }
        long size = redisTemplate.opsForHyperLogLog().size(rediskey);
        System.out.println(size);
    }

    //将三组数据合并，再统计合并后的重复数据独立总数
    @Test
    public void testHyperLogLogUnion(){
        String redisKey2 = "test:hll:02";
        for (int i = 1; i <= 10000 ; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey2 , i);
        }
        String redisKey3 = "test:hll:03";
        for (int i = 5001; i <= 15000 ; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey3 , i);
        }

        String redisKey4 = "test:hll:04";
        for (int i = 10001; i <= 20000 ; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey4 , i);
        }

        String unionKey = "test:hll:05";
        redisTemplate.opsForHyperLogLog().union(unionKey, redisKey2, redisKey3, redisKey4);
        long size = redisTemplate.opsForHyperLogLog().size(unionKey);
        System.out.println(size);
    }

    //统计一组数据布尔值
    @Test
    public void testBitMap(){
        String redisKey = "test:bm:01";
        //记录
        redisTemplate.opsForValue().setBit(redisKey, 1 , true);
        redisTemplate.opsForValue().setBit(redisKey, 4 , true);
        redisTemplate.opsForValue().setBit(redisKey, 7 , true);

        //查询
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,4));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,5));

        //统计
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.bitCount(redisKey.getBytes());
            }
        });
        System.out.println(obj);
    }
}





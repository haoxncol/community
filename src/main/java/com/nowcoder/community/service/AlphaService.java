package com.nowcoder.community.service;

import com.nowcoder.community.dao.AlphaDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
//@Scope("prototype")
public class AlphaService {


    @Autowired
    private AlphaDao alphaDao;

    private static final Logger logger = LoggerFactory.getLogger(AlphaService.class);


    public AlphaService(){
        System.out.println("实例化AlphaService");
    }

    @PostConstruct
    public void init(){
        System.out.println("初始化AlphaService");
    }
    @PreDestroy
    public void destory(){
        System.out.println("销毁AlphaService");
    }
     public String find(){
        return alphaDao.select();
    }

    //@Async注解让该方法在多线程环境下被异步的调用
    @Async
    public void execute1(){
        logger.debug("execute1");
    }
    //@Scheduled注解延时执行
//    @Scheduled(initialDelay = 10000, fixedRate = 1000)
//    public void execute2(){
//        logger.debug("execute2");
//    }

}

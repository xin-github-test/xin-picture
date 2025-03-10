package com.xin.xinpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.xin.xinpicturebackend.mapper")
//将aop代理暴露出来，从而能够在开发中获取（AopContext.currentProxy()）
@EnableAspectJAutoProxy(exposeProxy = true)
public class XinPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(XinPictureBackendApplication.class, args);
    }

}

package com.class242014group06;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement //开启注解方式的事务管理
@EnableAsync //开启异步方法支持
@Slf4j
@MapperScan("com.class242014group06.mapper")
public class code1stApplication {
    public static void main(String[] args) {
        SpringApplication.run(code1stApplication.class, args);
        log.info("server started");
    }
}

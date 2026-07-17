package com.class242014group06.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Instant;

/**
 * 记录服务启动时间，用于判断 JWT 是否在本次运行中签发。
 * 每次重启服务后，之前签发的所有 token 都会因 iat < startupTime 而失效，
 * 所有角色必须重新登录。
 */
@Component
@Slf4j
public class ServerStartupTracker {

    /** 服务本次启动的时间戳（毫秒），作为校验 JWT iat 的阈值 */
    @Getter
    private long startupTimeMillis;

    @PostConstruct
    public void init() {
        startupTimeMillis = System.currentTimeMillis();
        log.info("服务启动时间记录：{}", Instant.ofEpochMilli(startupTimeMillis));
    }
}

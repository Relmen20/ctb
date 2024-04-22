package com.copy.telegram.config;

import com.copy.common.entity.AuthEntity;
import com.copy.common.entity.FollowEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class AppConfig {

    @Value("${bot.session.maxSessions}")
    private Integer maxSessions;

    @Bean(name = "authExecutor")
    public ThreadPoolTaskExecutor getAuthExecutor() {
        final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(maxSessions);
        threadPoolTaskExecutor.setMaxPoolSize(maxSessions);
        //threadPoolTaskExecutor.setQueueCapacity(numberOfClientSessions);
        threadPoolTaskExecutor.setThreadNamePrefix("auth-session-");
        return threadPoolTaskExecutor;
    }

    @Bean(name = "followExecutor")
    public ThreadPoolTaskExecutor getFollowExecutor() {
        final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(maxSessions);
        threadPoolTaskExecutor.setMaxPoolSize(maxSessions);
        //threadPoolTaskExecutor.setQueueCapacity(numberOfClientSessions);
        threadPoolTaskExecutor.setThreadNamePrefix("follow-sess-");
        return threadPoolTaskExecutor;
    }

    @Bean
    public ConcurrentHashMap<Long, AuthEntity> pendingRegistrations() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public ConcurrentHashMap<Long, FollowEntity> pendingAddingFollow() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public ConcurrentHashMap<Long, Integer> chatIdToLastMessage() {
        return new ConcurrentHashMap<>();
    }
}

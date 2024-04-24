package com.copy.telegram.config;

import com.copy.common.entity.AuthEntity;
import com.copy.common.entity.FollowEntity;
import com.copy.common.entity.UserWalletsEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class AppConfig {

    @Value("${bot.session.maxAuthSessions}")
    private Integer maxAuthSessions;
    @Value("${bot.session.maxFollowSessions}")
    private Integer maxFollowSessions;
    @Value("${bot.session.maxSubscriptionSessions}")
    private Integer maxSubscriptionSessions;

    @Bean(name = "authExecutor")
    public ThreadPoolTaskExecutor getAuthExecutor() {
        final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(maxAuthSessions);
        threadPoolTaskExecutor.setMaxPoolSize(maxAuthSessions);
        //threadPoolTaskExecutor.setQueueCapacity(numberOfClientSessions);
        threadPoolTaskExecutor.setThreadNamePrefix("auth-session-");
        return threadPoolTaskExecutor;
    }

    @Bean(name = "followExecutor")
    public ThreadPoolTaskExecutor getFollowExecutor() {
        final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(maxFollowSessions);
        threadPoolTaskExecutor.setMaxPoolSize(maxFollowSessions);
        threadPoolTaskExecutor.setThreadNamePrefix("foll-session-");
        return threadPoolTaskExecutor;
    }

    @Bean(name = "subscriptionExecutor")
    public ThreadPoolTaskExecutor getSubscriptionExecutor() {
        final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(maxSubscriptionSessions);
        threadPoolTaskExecutor.setMaxPoolSize(maxSubscriptionSessions);
        threadPoolTaskExecutor.setThreadNamePrefix("sub-session-");
        return threadPoolTaskExecutor;
    }

    @Bean
    public ConcurrentHashMap<Long, AuthEntity> pendingRegistrations() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public ConcurrentHashMap<Long, UserWalletsEntity> pendingUserWallet() {
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

    @Bean
    public ConcurrentHashMap<Long, Integer> pendingSubscriptions() {
        return new ConcurrentHashMap<>();
    }
}

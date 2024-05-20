package com.copy.trader.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class BeanConfig {

    @Value("${server.followExecutor.corePoolSize}")
    private Integer corePoolSize;
    @Value("${server.followExecutor.maxPoolSize}")
    private Integer maxPoolSize;

    @Value("${solana.web.address}")
    private String SOLANA_WEB_ADDRESS;

    @Bean(name = "solanaWebAddress")
    public String getSolanaWebAddress() {
        return SOLANA_WEB_ADDRESS;
    }

    @Bean(name = "followTasksExecutor")
    public ThreadPoolTaskExecutor followTasksExecutor() {
        final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
        threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
        threadPoolTaskExecutor.setThreadNamePrefix("follow-proc-");
        return threadPoolTaskExecutor;
    }

    @Bean(name = "followTrackerExecutor")
    public ThreadPoolTaskExecutor followTrackerExecutor() {
        final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
        threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
        threadPoolTaskExecutor.setThreadNamePrefix("tracker-");
        return threadPoolTaskExecutor;
    }
}

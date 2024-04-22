package com.copy.telegram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {"com/copy/common/repository"})
@EntityScan(basePackages = {"com/copy/common/entity"})
public class TelegramBotStartApplication {
    public static void main(String[] args) {
        SpringApplication.run(TelegramBotStartApplication.class, args);
    }
}
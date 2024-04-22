package com.copy.telegram.config;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(final CachingConnectionFactory connectionFactory) {
        final var rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(getJsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean("jsonMessageConverter")
    public Jackson2JsonMessageConverter getJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

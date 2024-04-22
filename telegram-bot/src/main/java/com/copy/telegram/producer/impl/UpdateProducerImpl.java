package com.copy.telegram.producer.impl;

import com.copy.telegram.producer.UpdateProducer;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
@Setter
@RequiredArgsConstructor
@Slf4j
public class UpdateProducerImpl implements UpdateProducer {

    @Value("${spring.rabbitmq.exchanges.trader}")
    private String traderExchange;
    @Value("${spring.rabbitmq.routing-keys.default}")
    private String routingKey;

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void produce(Update update) {
        log.debug("Produce message: {}, to exchange: {}", update, traderExchange);
        rabbitTemplate.convertAndSend(traderExchange, routingKey, update);
    }
}
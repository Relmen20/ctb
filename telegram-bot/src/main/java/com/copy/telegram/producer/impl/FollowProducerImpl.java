package com.copy.telegram.producer.impl;

import com.copy.common.dto.FollowTaskDto;
import com.copy.telegram.producer.UpdateProducer;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Setter
@RequiredArgsConstructor
@Slf4j
public class FollowProducerImpl implements UpdateProducer {

    @Value("${spring.rabbitmq.exchanges.trader}")
    private String traderExchange;

    @Value("${spring.rabbitmq.routing-keys.start}")
    private String startRoutingKey;

    @Value("${spring.rabbitmq.routing-keys.stop}")
    private String stopRoutingKey;

    private final RabbitTemplate rabbitTemplate;

    public void produceToFollowExchange(FollowTaskDto dto){
        if(dto.isStart()){
            produce(startRoutingKey, dto);
        } else {
            produce(stopRoutingKey, dto);
        }
    }

    @Override
    public void produce(String routingKey, FollowTaskDto dto) {
        log.debug("Produce message: {}, to exchange: {}", dto, traderExchange);
        rabbitTemplate.convertAndSend(traderExchange, routingKey, dto);
    }
}
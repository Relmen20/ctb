package com.copy.trader.producer;

import com.copy.common.dto.FollowReceiptTaskDto;
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
public class ReceiptProducer {

    @Value("${spring.rabbitmq.exchanges.traderReceipt}")
    private String traderReceiptExchange;

    @Value("${spring.rabbitmq.routing-keys.start}")
    private String startRoutingKey;

    @Value("${spring.rabbitmq.routing-keys.stop}")
    private String stopRoutingKey;

    private final RabbitTemplate rabbitTemplate;

    public void produceReceipt(FollowReceiptTaskDto dto){
        if(dto.isStart()){
            produce(startRoutingKey, dto);
        } else {
            produce(stopRoutingKey, dto);
        }
    }

    private void produce(String routingKey, FollowReceiptTaskDto dto) {
        log.debug("Produce message: {}, to exchange: {}", dto, traderReceiptExchange);
        rabbitTemplate.convertAndSend(traderReceiptExchange, routingKey, dto);
    }

}

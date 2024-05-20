package com.copy.trader.producer;

import com.copy.common.dto.TransactionDto;
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
public class CollsProducer {
    @Value("${spring.rabbitmq.exchanges.collExchange}")
    private String traderReceiptExchange;

    @Value("${spring.rabbitmq.routing-keys.default}")
    private String collRoutingKey;

    private final RabbitTemplate rabbitTemplate;

    public void produceReceipt(TransactionDto dto){
       produce(collRoutingKey, dto);
    }

    private void produce(String routingKey, TransactionDto dto) {
        log.debug("Produce message: {}, to exchange: {}", dto, traderReceiptExchange);
        rabbitTemplate.convertAndSend(traderReceiptExchange, routingKey, dto);
    }
}

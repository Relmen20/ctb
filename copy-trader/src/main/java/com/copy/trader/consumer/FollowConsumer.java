package com.copy.trader.consumer;

import com.copy.common.dto.FollowTaskDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FollowConsumer {

    @RabbitListener(queues = "${spring.rabbitmq.queues.startQueue}",
                    concurrency = "${spring.rabbitmq.concurrency}",
                    messageConverter = "jsonMessageConverter")
    public void consumeStartEvent(FollowTaskDto dto){
        log.info("Income START_FOLLOW dto {}", dto.toString());
    }

    @RabbitListener(queues = "${spring.rabbitmq.queues.stopQueue}",
                    concurrency = "${spring.rabbitmq.concurrency}",
                    messageConverter = "jsonMessageConverter")
    public void consumeStopEvent(FollowTaskDto dto){
        log.info("Income STOP_FOLLOW dto {}", dto.toString());
    }
}

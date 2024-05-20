package com.copy.telegram.consumer;

import com.copy.common.dto.FollowReceiptTaskDto;
import com.copy.telegram.service.FollowReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReceiptConsumer {

    @Qualifier(value = "followExecutor")
    private final ThreadPoolTaskExecutor followExecutor;
    private final FollowReceiptService followReceiptService;

    @RabbitListener(queues = "${spring.rabbitmq.queues.startReceiptQueue}",
            concurrency = "${spring.rabbitmq.concurrency}",
            messageConverter = "jsonMessageConverter")
    public void consumeStartEvent(FollowReceiptTaskDto dto){
        log.info("Income receipt START_FOLLOW dto from authId: {}, followId: {}", dto.getFollow().getAuthEntity().getAuthId(),
                dto.getFollow().getFollowId());
        CompletableFuture.runAsync(() -> followReceiptService.sendReceiptOnFollowFunction(dto), followExecutor);
    }

    @RabbitListener(queues = "${spring.rabbitmq.queues.stopReceiptQueue}",
            concurrency = "${spring.rabbitmq.concurrency}",
            messageConverter = "jsonMessageConverter")
    public void consumeStopEvent(FollowReceiptTaskDto dto){
        log.info("Income receipt STOP_FOLLOW dto from authId: {}, followId: {}", dto.getFollow().getAuthEntity().getAuthId(),
                dto.getFollow().getFollowId());
        CompletableFuture.runAsync(() -> followReceiptService.sendReceiptOnFollowFunction(dto), followExecutor);
    }
}

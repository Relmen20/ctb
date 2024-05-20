package com.copy.telegram.consumer;

import com.copy.common.dto.TransactionDto;
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
public class CollConsumer {

    @Qualifier(value = "followExecutor")
    private final ThreadPoolTaskExecutor followExecutor;
    private final FollowReceiptService followReceiptService;

    @RabbitListener(queues = "${spring.rabbitmq.queues.collQueue}",
            concurrency = "${spring.rabbitmq.concurrency}",
            messageConverter = "jsonMessageConverter")
    public void consumeStartEvent(TransactionDto dto){
        log.info("Income receipt to coll dto from auth: {}, follow: {}", dto.getFollowEntity().getAuthEntity().getAuthId(),
                                                                        dto.getFollowEntity().getFollowId());
        CompletableFuture.runAsync(() -> followReceiptService.sendCollReceipt(dto), followExecutor);
    }
}

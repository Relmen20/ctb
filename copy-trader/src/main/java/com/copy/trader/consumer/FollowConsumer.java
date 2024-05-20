package com.copy.trader.consumer;

import com.copy.common.dto.FollowTaskDto;
import com.copy.trader.service.FollowTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class FollowConsumer {

    private final ThreadPoolTaskExecutor followTasksExecutor;
    private final FollowTrackingService followTrackingService;

    @RabbitListener(queues = "${spring.rabbitmq.queues.startQueue}",
                    concurrency = "${spring.rabbitmq.concurrency}",
                    messageConverter = "jsonMessageConverter")
    public void consumeStartEvent(FollowTaskDto dto){
        log.info("Income START_FOLLOW dto from authId: {}, followId: {}", dto.getFollow().getAuthEntity().getAuthId(),
                                                                          dto.getFollow().getFollowId());
        CompletableFuture.runAsync(() -> followTrackingService.startFollow(dto), followTasksExecutor);
    }

    @RabbitListener(queues = "${spring.rabbitmq.queues.stopQueue}",
                    concurrency = "${spring.rabbitmq.concurrency}",
                    messageConverter = "jsonMessageConverter")
    public void consumeStopEvent(FollowTaskDto dto){
        log.info("Income STOP_FOLLOW dto from authId: {}, followId: {}", dto.getFollow().getAuthEntity().getAuthId(),
                                                                         dto.getFollow().getFollowId());
        CompletableFuture.runAsync(() -> followTrackingService.stopFollow(dto), followTasksExecutor);
    }
}

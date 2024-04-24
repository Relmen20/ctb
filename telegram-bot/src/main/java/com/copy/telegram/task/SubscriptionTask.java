package com.copy.telegram.task;

import com.copy.common.repository.AuthRepository;
import com.copy.common.repository.SubscriptionRepository;
import com.copy.telegram.controller.TelegramBot;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Scope("prototype")
@Slf4j
@Setter
public class SubscriptionTask implements Runnable{

    private final Long curChatId;
    private final String textMessage;
    private final Integer messageId;
    private final SubscriptionRepository subscriptionRepository;
    private final AuthRepository authRepository;

    private final ConcurrentHashMap<Long, Integer> chatIdToLastMessage;
    private final TelegramBot telegramBot;

    public SubscriptionTask(Long curChatId, String textMessage, Integer messageId,
                            SubscriptionRepository subscriptionRepository, AuthRepository authRepository,
                            ConcurrentHashMap<Long, Integer> chatIdToLastMessage, TelegramBot telegramBot) {
        this.curChatId = curChatId;
        this.textMessage = textMessage;
        this.messageId = messageId;
        this.subscriptionRepository = subscriptionRepository;
        this.authRepository = authRepository;

        this.chatIdToLastMessage = chatIdToLastMessage;
        this.telegramBot = telegramBot;
    }

    @Override
    public void run() {

    }
}

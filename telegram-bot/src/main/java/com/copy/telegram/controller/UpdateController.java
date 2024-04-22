package com.copy.telegram.controller;

import com.copy.common.entity.AuthEntity;
import com.copy.common.entity.FollowEntity;
import com.copy.common.repository.AuthRepository;
import com.copy.common.repository.FollowRepository;
import com.copy.telegram.producer.impl.UpdateProducerImpl;
import com.copy.telegram.task.AuthTask;
import com.copy.telegram.task.FollowTask;
import com.copy.telegram.utils.Commands;
import com.copy.telegram.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.copy.telegram.utils.Commands.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class UpdateController {

    @Autowired
    @Qualifier("authExecutor")
    private final ThreadPoolTaskExecutor authExecutor;

    @Autowired
    @Qualifier("followExecutor")
    private final ThreadPoolTaskExecutor followExecutor;

    private TelegramBot telegramBot;

    @Autowired
    private AuthRepository authRepository;
    @Autowired
    private FollowRepository followRepository;
    @Autowired
    private ConcurrentHashMap<Long, AuthEntity> pendingRegistrations;
    @Autowired
    private ConcurrentHashMap<Long, FollowEntity> pendingFollow;
    @Autowired
    private ConcurrentHashMap<Long, Integer> chatIdToLastMessage;

    private final UpdateProducerImpl updateProducer;
    private final MessageUtils messageUtils;

    public void registerBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    @Bean
    @Scope("prototype")
    public AuthTask getAuthTask(Long chatId, String message, Integer messageId) {
        return new AuthTask(chatId, message, messageId, authRepository, followRepository, pendingRegistrations, chatIdToLastMessage, telegramBot);
    }

    @Bean
    @Scope("prototype")
    public FollowTask getFollowTask(Long chatId, String message, Integer messageId) {
        return new FollowTask(chatId, message, messageId, followRepository, authRepository, pendingFollow, chatIdToLastMessage, telegramBot);
    }

    public void processUpdate(Update update) {
        if (update == null) {
            log.error("Update is null");
            return;
        }

        if (update.getMessage() != null) {
            String message = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            User sender = update.getMessage().getFrom();
            Integer messageId = sender.getIsBot() ? update.getMessage().getMessageId() : null;
            processTextMessage(message, chatId, messageId);
        } else if (update.getCallbackQuery() != null) {
            String callbackQuery = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            User sender = update.getCallbackQuery().getMessage().getFrom();
            Integer messageId = sender.getIsBot() ? update.getCallbackQuery().getMessage().getMessageId() : null;
            processTextMessage(callbackQuery, chatId, messageId);
        } else {
            log.error("Received unsupported message type: {}", update);
        }
    }

    private void processTextMessage(String textMessage, Long chatId, Integer messageId) {

        try {
            Commands command = null;
            if (textContainsCommand(textMessage)) {
                if (textMessage.startsWith(SHOW_.getShC())) {
                    command = SHOW_;
                } else if (textMessage.startsWith(DELETE_FOLLOW.getShC())) {
                    command = DELETE_FOLLOW;
                } else if (textMessage.startsWith(START_FOLLOW.getShC())) {
                    command = START_FOLLOW;
                } else if (textMessage.startsWith(STOP_FOLLOW.getShC())) {
                    command = STOP_FOLLOW;
                } else {
                    command = getCommandByText(textMessage);
                }
            } else if (pendingRegistrations.containsKey(chatId)) {
                command = REGISTRATION;
            } else if (pendingFollow.containsKey(chatId)) {
                command = FOLLOW;
            }

            switch (Objects.requireNonNull(command)) {
                case MENU, BACK_MENU:
                    computeAndDelete(chatId, messageId);
                    pendingFollow.remove(chatId);
                    pendingRegistrations.remove(chatId);
                    telegramBot.sendMenuKeyBoard(chatId);
                    break;
                case REGISTRATION, UPDATE, AUTH_CANCEL, SHOW_MY_DATA:
                    authExecutor.execute(getAuthTask(chatId, textMessage, messageId));
                    break;
                case FOLLOW, ADD_FOLLOW, DELETE_FOLLOW, SHOW_,
                     FOLLOW_CANCEL, BACK_ALL_FOLLOW, START_FOLLOW, STOP_FOLLOW:
                    followExecutor.execute(getFollowTask(chatId, textMessage, messageId));
                    break;
                default:
                    break;
            }
        } catch (Throwable e) {
            log.error("{}", e.getMessage());
        }
//        updateProducer.produce(update);
    }

    private boolean textContainsCommand(String textMessage) {
        for (Commands command : Commands.values()) {
            if (textMessage.contains(command.getShC())) {
                return true;
            }
        }
        return false;
    }

    private void computeAndDelete(Long curChatId, Integer messageId) {
        if (messageId != null) {
            chatIdToLastMessage.compute(curChatId, (k, existingValue) -> messageId);

            DeleteMessage deleteMessage = DeleteMessage.builder()
                    .chatId(curChatId)
                    .messageId(chatIdToLastMessage.get(curChatId))
                    .build();
            telegramBot.sendDeleteMessage(deleteMessage);
        }
    }

}

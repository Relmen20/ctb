package com.copy.telegram.controller;

import com.copy.common.entity.AuthEntity;
import com.copy.common.entity.FollowEntity;
import com.copy.common.entity.UserWalletsEntity;
import com.copy.common.repository.AuthRepository;
import com.copy.common.repository.FollowRepository;
import com.copy.common.repository.SubscriptionRepository;
import com.copy.common.repository.UserWalletsRepository;
import com.copy.telegram.producer.impl.FollowProducerImpl;
import com.copy.telegram.task.AuthTask;
import com.copy.telegram.task.FollowTask;
import com.copy.telegram.task.SubscriptionTask;
import com.copy.telegram.utils.Commands;
import com.copy.telegram.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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

    private static final char[] notAllowedChars = new char[]{'_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!'};
    public static final String NOT_USE_IT = "Character \\%s not allowed, please not use it";

    @Autowired
    @Qualifier(value = "authExecutor")
    private final ThreadPoolTaskExecutor authExecutor;

    @Autowired
    @Qualifier(value = "followExecutor")
    private final ThreadPoolTaskExecutor followExecutor;

    @Autowired
    @Qualifier(value = "subscriptionExecutor")
    private final ThreadPoolTaskExecutor subscriptionExecutor;

    private TelegramBot telegramBot;

    private final FollowProducerImpl followProducer;
    private final AuthRepository authRepository;
    private final FollowRepository followRepository;
    private final UserWalletsRepository userWalletsRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ConcurrentHashMap<Long, AuthEntity> pendingRegistrations;
    private final ConcurrentHashMap<Long, FollowEntity> pendingFollow;
    private final ConcurrentHashMap<Long, UserWalletsEntity> pendingUserWallet;
    private final ConcurrentHashMap<Long, Integer> chatIdToLastMessage;
    private final MessageUtils messageUtils;

    public void registerBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
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

    @Bean
    @Scope("prototype")
    private AuthTask getAuthTask(Long chatId, String message, Integer messageId) {
        return new AuthTask(chatId, message, messageId, authRepository, followRepository, userWalletsRepository,
                subscriptionRepository, pendingRegistrations, pendingUserWallet, chatIdToLastMessage, telegramBot);
    }

    @Bean
    @Scope("prototype")
    private FollowTask getFollowTask(Long chatId, String message, Integer messageId) {
        return new FollowTask(chatId, message, messageId, followRepository, authRepository,
                pendingFollow, chatIdToLastMessage, followProducer, telegramBot);
    }

    @Bean
    @Scope("prototype")
    private SubscriptionTask getSubscriptionTask(Long chatId, String message, Integer messageId) {
        return new SubscriptionTask(chatId, message, messageId, subscriptionRepository,
                authRepository, chatIdToLastMessage, telegramBot);
    }

    private void processTextMessage(String textMessage, Long chatId, Integer messageId) {
        char notAllowed = textContainsNotAllowedChars(textMessage);
        try {
            Commands command = null;
            if (textContainsCommand(textMessage)) {
                command = getCommands(textMessage);
            } else if (notAllowed != 'n'){
                String message = String.format(NOT_USE_IT, String.valueOf(notAllowedChars));
                telegramBot.sendResponseMessage(SendMessage.builder().chatId(chatId).text(message).build());
            }  else if (pendingRegistrations.containsKey(chatId)) {
                command = REGISTRATION;
            } else if (pendingFollow.containsKey(chatId)) {
                command = FOLLOW;
            }


            switch (Objects.requireNonNull(command)) {
                case MENU, BACK_MENU:
                    prepareForSendTgMessage(chatId, messageId);
                    telegramBot.sendMenuKeyBoard(chatId);
                    break;
                case REGISTRATION, UPDATE, AUTH_CANCEL, SHOW_MY_DATA:
                    authExecutor.execute(getAuthTask(chatId, textMessage, messageId));
                    break;
                case FOLLOW, ADD_FOLLOW, DELETE_FOLLOW, SHOW_,
                     FOLLOW_CANCEL, BACK_ALL_FOLLOW, START_FOLLOW,
                     STOP_FOLLOW, CHANGE_FOLLOW_NAME:
                    followExecutor.execute(getFollowTask(chatId, textMessage, messageId));
                    break;
                case SUBSCRIBE, SUB_SHOW:
                    prepareForSendTgMessage(chatId, messageId);
                    subscriptionExecutor.execute(getSubscriptionTask(chatId, textMessage, messageId));
                    break;
                default:
                    break;
            }
        } catch (Throwable e) {
            log.error("{}", e.getMessage());
        }
    }

    private char textContainsNotAllowedChars(String textMessage) {
        for (char notAllowedChar : notAllowedChars) {
            if (textMessage.indexOf(notAllowedChar) != -1){
                return notAllowedChar;
            }
        }
        return 'n';
    }

    private void prepareForSendTgMessage(Long chatId, Integer messageId) {
        computeAndDelete(chatId, messageId);
        pendingFollow.remove(chatId);
        pendingRegistrations.remove(chatId);
    }

    private static @Nullable Commands getCommands(String textMessage) {
        Commands command;
        if (textMessage.startsWith(SHOW_.getShC())) {
            command = SHOW_;
        } else if (textMessage.startsWith(DELETE_FOLLOW.getShC())) {
            command = DELETE_FOLLOW;
        } else if (textMessage.startsWith(CHANGE_FOLLOW_NAME.getShC())) {
            command = CHANGE_FOLLOW_NAME;
        } else if (textMessage.startsWith(START_FOLLOW.getShC())) {
            command = START_FOLLOW;
        } else if (textMessage.startsWith(STOP_FOLLOW.getShC())) {
            command = STOP_FOLLOW;
        } else if (textMessage.startsWith(SUB_SHOW.getShC())) {
            command = SUB_SHOW;
        } else {
            command = getCommandByText(textMessage);
        }
        return command;
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

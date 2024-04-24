package com.copy.telegram.task;

import com.copy.common.entity.AuthEntity;
import com.copy.common.entity.FollowEntity;
import com.copy.common.entity.SubscriptionEntity;
import com.copy.common.entity.UserWalletsEntity;
import com.copy.common.repository.AuthRepository;
import com.copy.common.repository.FollowRepository;
import com.copy.common.repository.SubscriptionRepository;
import com.copy.common.repository.UserWalletsRepository;
import com.copy.telegram.controller.TelegramBot;
import com.copy.telegram.utils.Commands;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.copy.telegram.utils.Commands.*;

@Component
@Scope("prototype")
@Slf4j
@Setter
public class AuthTask implements Runnable {

    public static final String ALREADY_REGISTERED = "You already registered";
    public static final String YOUR_NAME = "Please enter your Name";
    public static final String WALLET_OR_SEND_SKIP = "Please enter your Solana private key Wallet";
    public static final String ANOTHER_NAME = "Invalid name %s, please enter another name";
    public static final String REGISTERED = "Successfully registered!";
    public static final String NOT_AVAILABLE_YET = "Not available yet!";
    public static final String ANOTHER_WALLET = "Invalid wallet %s, please enter another wallet";
    public static final String PLEASE_USE_REG = "You need to be registered, please use /reg";

    private final ConcurrentHashMap<Long, UserWalletsEntity> pendingUserWallet;
    private final ConcurrentHashMap<Long, Integer> chatIdToLastMessage;
    private final TelegramBot telegramBot;
    private final FollowRepository followRepository;
    private final UserWalletsRepository userWalletsRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ConcurrentHashMap<Long, AuthEntity> pendingRegistrations;
    private final Integer messageId;
    private final AuthRepository authRepository;
    private final Long curChatId;
    private final String textMessage;

    public AuthTask(Long chatId, String textMessage, Integer messageId, AuthRepository authRepository,
                    FollowRepository followRepository, UserWalletsRepository userWalletsRepository,
                    SubscriptionRepository subscriptionRepository, ConcurrentHashMap<Long, AuthEntity> pendingRegistrations,
                    ConcurrentHashMap<Long, UserWalletsEntity> pendingUserWallet,
                    ConcurrentHashMap<Long, Integer> chatIdToLastMessage, TelegramBot telegramBot) {
        this.curChatId = chatId;
        this.textMessage = textMessage;
        this.messageId = messageId;
        this.authRepository = authRepository;
        this.followRepository = followRepository;
        this.userWalletsRepository = userWalletsRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.pendingRegistrations = pendingRegistrations;
        this.pendingUserWallet = pendingUserWallet;
        this.chatIdToLastMessage = chatIdToLastMessage;
        this.telegramBot = telegramBot;
    }

    @Override
    public void run() {
        try {
            Optional<AuthEntity> optionalAut = authRepository.findByChatId(curChatId);
            AuthEntity auth = optionalAut.orElse(null);

            if (textMessage.equals(REGISTRATION.getShC()) && auth == null) {
                log.info("Start registration new user with chatId: {}", curChatId);
                registrationNewUser(curChatId);
            } else if (textMessage.equals(AUTH_CANCEL.getShC())) {
                log.info("User cancel registration in chat {}", curChatId);
                pendingRegistrations.remove(curChatId);
                computeAndDelete();
                telegramBot.sendMenuKeyBoard(curChatId);
            } else if (textMessage.equals(SHOW_MY_DATA.getShC()) && auth != null) {
                showMyData(auth);
            } else if (pendingRegistrations.containsKey(curChatId)) {
                log.info("Continue registration new user with chatId: {}", curChatId);
                continueRegistration(curChatId);
            } else if (textMessage.equals(UPDATE.getShC()) && auth != null) {
                log.info("Update user name: {}", auth.getAuthId());
                registrationNewUser(curChatId);
            } else if (auth != null) {
                log.info("Attempt to registration already contains user, chatId: {}, userId: {}", curChatId, auth.getAuthId());
                computeAndDelete();
                telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, ALREADY_REGISTERED, BACK_MENU));
            } else {
                computeAndDelete();
                telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, PLEASE_USE_REG, BACK_MENU));
            }
        } catch (Throwable e) {
            log.error("Unknown error, error message: {}", e.getMessage());
        }
    }

    private void showMyData(AuthEntity authEntity) {

        List<FollowEntity> followEntities = followRepository.findByAuthEntity(authEntity);
        List<UserWalletsEntity> userWalletEntities = userWalletsRepository.findByAuthEntity(authEntity);

        StringBuilder messageBuilder = new StringBuilder();

        messageBuilder.append("*Информация об аккаунте*\n\n");

        messageBuilder.append("Имя пользователя: *").append(authEntity.getPersonName()).append("*\n");
        messageBuilder.append("Тип подписки: *").append(authEntity.getSubscriptionEntity().getSubName()).append("*\n");
        messageBuilder.append(authEntity.getSubscriptionEntity().getSubDescription()).append("\n\n");

        int totalFollowedWallets = followEntities.size();
        int totalCollectionsDone = followEntities.stream().mapToInt(FollowEntity::getCountCollDone).sum();
        int totalAutotradesDone = followEntities.stream().mapToInt(FollowEntity::getCountAutotradeDone).sum();

        messageBuilder.append("*Статистика*\n");
        messageBuilder.append("Количество добавленных кошельков: `").append(userWalletEntities.size()).append("`\n\n");
        messageBuilder.append("Количество отслеживаемых кошельков: ").append(totalFollowedWallets).append("\n");
        messageBuilder.append("Количество выполненных коллов: ").append(totalCollectionsDone).append("\n");
        messageBuilder.append("Количество выполненных автотрейдов: ").append(totalAutotradesDone).append("\n\n");

        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(List.of(InlineKeyboardButton.builder().text("Menu").callbackData(MENU.getShC()).build()));
        buttons.add(List.of(InlineKeyboardButton.builder().text("All Follows").callbackData(FOLLOW.getShC()).build(),
                            InlineKeyboardButton.builder().text("All Wallets").callbackData(WALLETS.getShC()).build()));
        buttons.add(List.of(InlineKeyboardButton.builder().text("Subscription").callbackData(SUBSCRIBE.getShC()).build()));


        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(authEntity.getChatId().toString());
        sendMessage.setText(messageBuilder.toString().replace(".", "\\.")
                                                     .replace("!", "\\!")
        );
        sendMessage.setParseMode("MarkdownV2");
        sendMessage.setReplyMarkup(InlineKeyboardMarkup.builder().keyboard(buttons).build());

        computeAndDelete();
        telegramBot.sendResponseMessage(sendMessage);
    }

    private void registrationNewUser(Long curChatId) {
        pendingRegistrations.remove(curChatId);
        SubscriptionEntity subEntity = subscriptionRepository.getBySubName("Default");
        AuthEntity auth = new AuthEntity();
        auth.setChatId(curChatId);
        auth.setSubscriptionEntity(subEntity);
        auth.setSubStartDate(LocalDate.now());
        try {
            pendingRegistrations.put(curChatId, auth);
            computeAndDelete();
            telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, YOUR_NAME, BACK_MENU));
        } catch (Exception e) {
            throw new RuntimeException("Error while registration: " + e.getMessage());
        }
    }

    private void continueRegistration(Long curChatId) {
        try {
            AuthEntity auth = null;
            if (textMessage.equals(UPDATE.getShC())) {
                pendingRegistrations.remove(curChatId);
                computeAndDelete();
                telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, YOUR_NAME, BACK_MENU));
            } else {
                auth = pendingRegistrations.get(curChatId);
                if (auth.getPersonName() == null) {
                    if (isValidUsername(textMessage)) {
                        auth.setPersonName(textMessage);
                        computeAndDelete();
                        authRepository.save(auth);
                        telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, REGISTERED, null));
                    } else {
                        String wrongNameMessage = String.format(ANOTHER_NAME, textMessage);
                        computeAndDelete();
                        telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, wrongNameMessage, BACK_MENU));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while continue registration: " + e.getMessage());
        }
    }

    private void computeAndDelete() {
        if (messageId != null) {
            chatIdToLastMessage.compute(curChatId, (k, existingValue) -> messageId);

            DeleteMessage deleteMessage = DeleteMessage.builder()
                    .chatId(curChatId)
                    .messageId(chatIdToLastMessage.get(curChatId))
                    .build();
            telegramBot.sendDeleteMessage(deleteMessage);
        }
    }

    private SendMessage composeDefaultMessage(Long chatId, String message, Commands back) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        if (back != null) {
            sendMessage.setReplyMarkup(getInlineKeyboardButtons(Commands.AUTH_CANCEL.getShC(), back.getShC()));
        }

        return sendMessage;
    }

    private static @NotNull InlineKeyboardMarkup getInlineKeyboardButtons(String cancel, String back) {
        List<InlineKeyboardButton> backAndCancelRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("Назад");
        backButton.setCallbackData(back);
        backAndCancelRow.add(backButton);

        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("Отменить");
        cancelButton.setCallbackData(cancel);
        backAndCancelRow.add(cancelButton);

        return InlineKeyboardMarkup.builder()
                .keyboardRow(backAndCancelRow)
                .build();
    }

    private boolean isValidUsername(String username) {
        String pattern = "^[\\p{L}\\d_]+$";
        if (username.length() < 3 || username.length() > 40) {
            return false;
        }
        return username.matches(pattern);
    }
}

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
import com.copy.telegram.utils.MessageUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.copy.telegram.utils.Commands.*;
import static com.copy.telegram.utils.MessageUtils.getAndDelete;

@Component
@Scope("prototype")
@Slf4j
@Setter
public class AuthTask implements Runnable {

    public static final String ALREADY_REGISTERED = "You already registered";
    public static final String YOUR_NAME = "Please enter your Name";
    public static final String ANOTHER_NAME = "Invalid name %s, please enter another name";
    public static final String REGISTERED = "Successfully registered";
//    public static final String WALLET_OR_SEND_SKIP = "Please enter your Solana private key Wallet";
//    public static final String NOT_AVAILABLE_YET = "Not available yet!";
//    public static final String ANOTHER_WALLET = "Invalid wallet %s, please enter another wallet";
    public static final String PLEASE_USE_REG = "You need to be registered, please use /reg";

    private final ConcurrentHashMap<Long, UserWalletsEntity> pendingUserWallet;
    private final TelegramBot telegramBot;
    private final FollowRepository followRepository;
    private final UserWalletsRepository userWalletsRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ConcurrentHashMap<Long, AuthEntity> pendingRegistrations;
    private final AuthRepository authRepository;
    private final Long curChatId;
    private final String textMessage;

    public AuthTask(Long chatId, String textMessage, AuthRepository authRepository, FollowRepository followRepository,
                    UserWalletsRepository userWalletsRepository, SubscriptionRepository subscriptionRepository,
                    ConcurrentHashMap<Long, AuthEntity> pendingRegistrations,
                    ConcurrentHashMap<Long, UserWalletsEntity> pendingUserWallet, TelegramBot telegramBot) {
        this.curChatId = chatId;
        this.textMessage = textMessage;
        this.authRepository = authRepository;
        this.followRepository = followRepository;
        this.userWalletsRepository = userWalletsRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.pendingRegistrations = pendingRegistrations;
        this.pendingUserWallet = pendingUserWallet;
        this.telegramBot = telegramBot;
    }

    @Override
    public void run() {
        try {
            Optional<AuthEntity> optionalAut = authRepository.findByChatId(curChatId);
            AuthEntity auth = optionalAut.orElse(null);

            if (textMessage.equals(REGISTRATION.getShC()) && auth == null) {
                log.info("Start registration new user with chatId: {}", curChatId);
                registrationNewUser();
            } else if (textMessage.equals(AUTH_CANCEL.getShC())) {
                log.info("User cancel registration in chat {}", curChatId);
                pendingRegistrations.remove(curChatId);
                telegramBot.sendDeleteMessage(getAndDelete(curChatId));
                telegramBot.sendMenuKeyBoard(curChatId);
            } else if (textMessage.equals(SHOW_MY_DATA.getShC()) && auth != null) {
                showMyData(auth);
            } else if (pendingRegistrations.containsKey(curChatId)) {
                log.info("Continue registration new user with chatId: {}", curChatId);
                continueRegistration(curChatId);
            } else if (textMessage.equals(UPDATE.getShC()) && auth != null) {
                log.info("Update user name: {}", auth.getAuthId());
                updateUserName(auth);
            } else if (auth != null) {
                log.info("Attempt to registration already contains user, chatId: {}, userId: {}", curChatId, auth.getAuthId());
                telegramBot.sendDeleteMessage(getAndDelete(curChatId));
                telegramBot.sendResponseMessage(MessageUtils.composeMessageOneButtonRow(curChatId,
                                                                                        ALREADY_REGISTERED,
                                                                                        BACK_MENU,
                                                                                        BACK_MENU.getDesc()));
            } else {
                telegramBot.sendDeleteMessage(getAndDelete(curChatId));
                telegramBot.sendResponseMessage(MessageUtils.composeMessageOneButtonRow(curChatId,
                                                                                        PLEASE_USE_REG,
                                                                                        BACK_MENU,
                                                                                        BACK_MENU.getDesc()));
            }
        } catch (Throwable e) {
            log.error("Unknown error, error message: {}", e.getMessage());
        }
    }

    private void updateUserName(AuthEntity auth) {
        pendingRegistrations.remove(curChatId);

        auth.setPersonName(null);
        pendingRegistrations.put(curChatId, auth);

        telegramBot.sendDeleteMessage(getAndDelete(curChatId));
        telegramBot.sendResponseMessage(MessageUtils.composeMessageTwoButtonsRow(curChatId, YOUR_NAME,
                BACK_MENU, BACK_MENU.getDesc(),
                AUTH_CANCEL, AUTH_CANCEL.getDesc()));
    }

    private void showMyData(AuthEntity authEntity) {

        List<FollowEntity> followEntities = followRepository.findByAuthEntity(authEntity);
        List<UserWalletsEntity> userWalletEntities = userWalletsRepository.findByAuthEntity(authEntity);
        SubscriptionEntity sub = authEntity.getSubscriptionEntity();
        int totalCollectionsDone = followEntities.stream().mapToInt(FollowEntity::getCountCollDone).sum();
        int totalAutotradesDone = followEntities.stream().mapToInt(FollowEntity::getCountAutotradeDone).sum();


        String messageBuilder = "*Информация об аккаунте*\n\n" +
                "Имя пользователя: *" + authEntity.getPersonName() + "*\n" +
                "Тип подписки: *" + authEntity.getSubscriptionEntity().getSubName() + "*\n" +
                "_" + authEntity.getSubscriptionEntity().getSubDescription() + "_\n\n" +
                "*Статистика:*\n" +
                "Количество добавленных кошельков: _" + userWalletEntities.size() + "_\n" +
                "Количество отслеживаемых кошельков: _" + followEntities.size() + "/" + sub.getFollowKeyAvailable() + "_\n" +
                "Количество выполненных коллов: _" + totalCollectionsDone + "/" + sub.getCountCollAvailable() + "_\n" +
                "Количество выполненных автотрейдов: _" + totalAutotradesDone + "/" + sub.getCountAutotradeAvailable() + "_\n";

        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(List.of(InlineKeyboardButton.builder().text(MENU.getDesc()).callbackData(MENU.getShC()).build()));
        buttons.add(List.of(InlineKeyboardButton.builder().text(FOLLOW.getDesc()).callbackData(FOLLOW.getShC()).build(),
                            InlineKeyboardButton.builder().text(WALLETS.getDesc()).callbackData(WALLETS.getShC()).build()));
        buttons.add(List.of(InlineKeyboardButton.builder().text(SUBSCRIBE.getDesc()).callbackData(SUBSCRIBE.getShC()).build()));


        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(authEntity.getChatId().toString());
        sendMessage.setText(messageBuilder.replace("?n", "\n"));
        sendMessage.setReplyMarkup(InlineKeyboardMarkup.builder().keyboard(buttons).build());

        telegramBot.sendDeleteMessage(getAndDelete(curChatId));
        telegramBot.sendResponseMessage(sendMessage);
    }

    private void registrationNewUser() {
        pendingRegistrations.remove(curChatId);
        SubscriptionEntity subEntity = subscriptionRepository.getBySubName("Default");
        AuthEntity auth = new AuthEntity();
        auth.setChatId(curChatId);
        auth.setSubscriptionEntity(subEntity);
        auth.setSubStartDate(LocalDate.now());
        try {
            pendingRegistrations.put(curChatId, auth);
            telegramBot.sendDeleteMessage(getAndDelete(curChatId));
            telegramBot.sendResponseMessage(MessageUtils.composeMessageTwoButtonsRow(curChatId, YOUR_NAME,
                                                                                     BACK_MENU, BACK_MENU.getDesc(),
                                                                                     AUTH_CANCEL, AUTH_CANCEL.getDesc()));
        } catch (Exception e) {
            throw new RuntimeException("Error while registration: " + e.getMessage());
        }
    }

    private void continueRegistration(Long curChatId) {
        try {
            AuthEntity auth = pendingRegistrations.get(curChatId);
            if (auth.getPersonName() == null) {
                if (isValidUsername(textMessage)) {
                    auth.setPersonName(textMessage);
//                    telegramBot.sendDeleteMessage(getAndDelete(curChatId));
                    authRepository.save(auth);
                    telegramBot.sendResponseMessage(MessageUtils.composeMessageOneButtonRow(curChatId, REGISTERED,
                                                                                            BACK_MENU, BACK_MENU.getDesc()));
                } else {
                    String wrongNameMessage = String.format(ANOTHER_NAME, textMessage);
//                    telegramBot.sendDeleteMessage(getAndDelete(curChatId));
                    telegramBot.sendResponseMessage(MessageUtils.composeMessageTwoButtonsRow(curChatId, wrongNameMessage,
                                                                                            BACK_MENU, BACK_MENU.getDesc(),
                                                                                            AUTH_CANCEL, AUTH_CANCEL.getDesc()));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while continue registration: " + e.getMessage());
        }
    }

    private boolean isValidUsername(String username) {
        String pattern = "^[\\p{L}\\d_]+$";
        if (username.length() < 3 || username.length() > 40) {
            return false;
        }
        return username.matches(pattern);
    }
}

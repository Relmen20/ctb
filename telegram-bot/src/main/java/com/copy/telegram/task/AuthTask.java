package com.copy.telegram.task;

import com.copy.common.entity.AuthEntity;
import com.copy.common.entity.FollowEntity;
import com.copy.common.repository.AuthRepository;
import com.copy.common.repository.FollowRepository;
import com.copy.telegram.controller.TelegramBot;
import com.copy.telegram.utils.Commands;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.rpc.RpcApi;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.types.AccountInfo;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

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
    private String client = "https://mainnet.helius-rpc.com/?api-key=aa61fddb-a509-48d4-998b-7ae0b0ae5319";

    private final ConcurrentHashMap<Long, Integer> chatIdToLastMessage;
    private final TelegramBot telegramBot;
    private final FollowRepository followRepository;
    private final ConcurrentHashMap<Long, AuthEntity> pendingRegistrations;
    private final Integer messageId;
    private final AuthRepository authRepository;
    private final Long curChatId;
    private final String textMessage;

    public AuthTask(Long chatId, String textMessage, Integer messageId, AuthRepository authRepository,
                    FollowRepository followRepository, ConcurrentHashMap<Long, AuthEntity> pendingRegistrations,
                    ConcurrentHashMap<Long, Integer> chatIdToLastMessage, TelegramBot telegramBot) {
        this.curChatId = chatId;
        this.textMessage = textMessage;
        this.messageId = messageId;
        this.authRepository = authRepository;
        this.followRepository = followRepository;
        this.pendingRegistrations = pendingRegistrations;
        this.chatIdToLastMessage = chatIdToLastMessage;
        this.telegramBot = telegramBot;
    }

    @Override
    public void run() {
        try {
            Optional<AuthEntity> auth = authRepository.findByChatId(curChatId);

            AuthEntity authFromOptional = auth.orElse(null);

            if (authFromOptional != null && !pendingRegistrations.containsKey(curChatId) && textMessage.equals(UPDATE.getShC())) {
                AuthEntity updateAuth = new AuthEntity();
                updateAuth.setAuthId(authFromOptional.getAuthId());
                updateAuth.setChatId(curChatId);
                pendingRegistrations.put(curChatId, updateAuth);
            }

            if (textMessage.equals(SHOW_MY_DATA.getShC()) && authFromOptional != null){
                showMyData(authFromOptional);
            } else if (textMessage.equals(AUTH_CANCEL.getShC())) {
                pendingRegistrations.remove(curChatId);
                computeAndDelete();
                telegramBot.sendMenuKeyBoard(curChatId);
            } else if (pendingRegistrations.containsKey(curChatId)) {
                log.info("Continue registration new user with chatId: {}", curChatId);
                continueRegistration(curChatId);
            } else if (authFromOptional == null) {
                log.info("Start registration new user with chatId: {}", curChatId);
                registrationNewUser(curChatId);
            } else {
                log.info("error");
                computeAndDelete();
                telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, ALREADY_REGISTERED, BACK_MENU));
            }
        } catch (Throwable e) {
            log.error("{}", e.getMessage());
        }
    }

    private void showMyData(AuthEntity authEntity) {

        List<FollowEntity> followEntities = followRepository.findByUserChatId(authEntity.getChatId());

        StringBuilder messageBuilder = new StringBuilder();

        messageBuilder.append("*Информация об аккаунте*\n\n");

        messageBuilder.append("Имя пользователя: *").append(authEntity.getPersonName()).append("*\n");
        messageBuilder.append("Адрес кошелька: `").append(authEntity.getWalletAddress()).append("`\n\n");

        int totalFollowedWallets = followEntities.size();
        int totalCollectionsDone = followEntities.stream().mapToInt(FollowEntity::getCountCollDone).sum();
        int totalAutotradesDone = followEntities.stream().mapToInt(FollowEntity::getCountAutotradeDone).sum();

        messageBuilder.append("*Статистика*\n");
        messageBuilder.append("Количество отслеживаемых кошельков: ").append(totalFollowedWallets).append("\n");
        messageBuilder.append("Количество выполненных коллов: ").append(totalCollectionsDone).append("\n");
        messageBuilder.append("Количество выполненных автотрейдов: ").append(totalAutotradesDone).append("\n\n");

        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(List.of(InlineKeyboardButton.builder().text("Menu").callbackData(MENU.getShC()).build()));
        buttons.add(List.of(InlineKeyboardButton.builder().text("All Follows").callbackData(FOLLOW.getShC()).build()));

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(authEntity.getChatId().toString());
        sendMessage.setText(messageBuilder.toString());
        sendMessage.setParseMode("MarkdownV2");
        sendMessage.setReplyMarkup(InlineKeyboardMarkup.builder().keyboard(buttons).build());

        computeAndDelete();
        telegramBot.sendResponseMessage(sendMessage);
    }

    private void registrationNewUser(Long curChatId) {
        AuthEntity auth = new AuthEntity();
        auth.setChatId(curChatId);
        try {
            pendingRegistrations.put(curChatId, auth);
            computeAndDelete();
            telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, YOUR_NAME, BACK_MENU));
        } catch (Exception e) {
            throw new RuntimeException("Error while registration");
        }
    }

    private void continueRegistration(Long curChatId) {
        try {
            AuthEntity auth = null;
            if (textMessage.equals(UPDATE.getShC())) {
                computeAndDelete();
                telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, YOUR_NAME, BACK_MENU));
            } else {
                auth = pendingRegistrations.get(curChatId);
                if (auth.getPersonName() == null) {
                    if (isValidUsername(textMessage)) {
                        auth.setPersonName(textMessage);
                        computeAndDelete();
                        telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, WALLET_OR_SEND_SKIP, BACK_MENU));
                    } else {
                        String wrongNameMessage = String.format(ANOTHER_NAME, textMessage);
                        computeAndDelete();
                        telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, wrongNameMessage, BACK_MENU));
                    }
                } else if (auth.getWalletAddress() == null) {
//                    if (textMessage.equals(SKIP.getShC())) {
//                        authRepository.save(auth);
//                        pendingRegistrations.remove(curChatId);
//                        computeAndDelete();
//                        telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, REGISTERED, null));
//                    } else
                    if (isValidAddress(textMessage)) {
                        auth.setWalletAddress(textMessage);
                        authRepository.save(auth);
                        pendingRegistrations.remove(curChatId);
                        computeAndDelete();
                        telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, REGISTERED, null));
                    } else {
                        String wrongNameMessage = String.format(ANOTHER_WALLET, textMessage);
                        computeAndDelete();
                        telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, wrongNameMessage, BACK_MENU));
                    }
                } else {
                    authRepository.save(auth);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while continue registration");
        }
    }

    private boolean isValidSolanaWalletId(String textMessage) {
        return true;
    }

    private boolean isValidAddress(String textMessage) {
        try {
            RpcClient rpcClient = new RpcClient(client);
            RpcApi rpcApi = rpcClient.getApi();
            AccountInfo info = rpcApi.getAccountInfo(PublicKey.valueOf(textMessage));
            return info != null;
        } catch (Exception e) {
            log.error("{}", e.getMessage());
            return false;
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

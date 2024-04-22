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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.copy.telegram.utils.Commands.*;

@Component
@Scope("prototype")
@Slf4j
@Setter
public class FollowTask implements Runnable {

    public static final String NOT_VALID_FOLLOW_KEY = "Not valid follow key";
    public static final String ALREADY_HAS_THIS_FOLLOW_KEY = "Your already has this follow key!";
    public static final String KEY_SUCCESSFULLY_ADDED = "Your follow key successfully added!";
    public static final String TO_FOLLOW = "Send key you want to follow";
    public static final String TO_ADD = "You dont have any follows key yet, want to add?";
    public static final String NO_SUCH_FOLLOW_KEY = "There is no such follow key";
    private final String YOU_NEED_REG = "You need to be registered to follow, use /reg";

    private String client = "https://mainnet.helius-rpc.com/?api-key=aa61fddb-a509-48d4-998b-7ae0b0ae5319";

    private final Long curChatId;
    private final String textMessage;
    private final Integer messageId;
    private final FollowRepository followRepository;
    private final AuthRepository authRepository;
    private final ConcurrentHashMap<Long, FollowEntity> pendingFollow;
    private final ConcurrentHashMap<Long, Integer> chatIdToLastMessage;
    private final TelegramBot telegramBot;

    public FollowTask(Long curChatId, String textMessage, Integer messageId, FollowRepository followRepository,
                      AuthRepository authRepository, ConcurrentHashMap<Long, FollowEntity> pendingFollow,
                      ConcurrentHashMap<Long, Integer> chatIdToLastMessage, TelegramBot telegramBot) {
        this.curChatId = curChatId;
        this.textMessage = textMessage;
        this.messageId = messageId;
        this.followRepository = followRepository;
        this.authRepository = authRepository;
        this.pendingFollow = pendingFollow;
        this.chatIdToLastMessage = chatIdToLastMessage;
        this.telegramBot = telegramBot;
    }

    @Override
    public void run() {
        try {
            List<FollowEntity> followList = followRepository.findByUserChatId(curChatId);

            if (textMessage.equals(FOLLOW.getShC()) ||
                    textMessage.equals(BACK_ALL_FOLLOW.getShC())) {
                processAllFollow(followList);
            }
            else if (textMessage.startsWith(SHOW_.getShC()) ||
                    textMessage.equals(FOLLOW_CANCEL.getShC()) ||
                    textMessage.equals(STOP_FOLLOW.getShC()) ||
                    textMessage.equals(START_FOLLOW.getShC())) {
                handleButtonClick();
            }
            else if (textMessage.startsWith(DELETE_FOLLOW.getShC())) {
                processDeleteFollow();
            }
            else if (textMessage.equals(ADD_FOLLOW.getShC()) || pendingFollow.containsKey(curChatId)) {
                processAddFollow(followList);
            }
        } catch (Throwable e) {
            log.error("{}", e.getMessage());
        }
    }

    private void processAllFollow(List<FollowEntity> followList) {
        if (!followList.isEmpty()) {
            pendingFollow.remove(curChatId);
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            for (FollowEntity followEntity : followList) {
                String followKeyWallet = followEntity.getFollowKeyWallet();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(followKeyWallet);
                button.setCallbackData(SHOW_.getShC() + followKeyWallet);
                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);
                rows.add(row);
            }

            rows.add(getInlineKeyboardButtonsListWithNameAdd(MENU.getShC(), ADD_FOLLOW.getShC()));

            inlineKeyboardMarkup.setKeyboard(rows);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(curChatId);
            sendMessage.setText("Выберите ключ:");
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);

            computeAndDelete();

            telegramBot.sendResponseMessage(sendMessage);
        } else {

            computeAndDelete();

            telegramBot.sendResponseMessage(composeDefaultMessageWithAddFollowButton(curChatId));
        }
    }

    public void handleButtonClick() {
        if (textMessage.startsWith(SHOW_.getShC())) {
            String followKeyWallet = textMessage.substring(5);
            FollowEntity followEntity = followRepository.findByFollowKeyWalletAndUserChatId(followKeyWallet, curChatId);

            if (followEntity != null) {
                StringBuilder messageBuilder = new StringBuilder();

                messageBuilder.append("*Информация об адресе слежения*\n\n");
                messageBuilder.append("Кошелек следования: `").append(followKeyWallet).append("`\n");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault());
                String formattedDate = followEntity.getDateStartFollow().format(formatter);
                messageBuilder.append("Дата начала следования: ").append(formattedDate).append("\n");
                messageBuilder.append("Выполненных коллов: ").append(followEntity.getCountCollDone()).append("\n");
                messageBuilder.append("Выполненных автотрейдов: ").append(followEntity.getCountAutotradeDone());
                String message = messageBuilder.toString();

                List<InlineKeyboardButton> keyboardStartStop = getKeyboardStartStop(followKeyWallet);

                String deleteCallbackFollow = DELETE_FOLLOW.getShC() + followEntity.getFollowKeyWallet();
                List<InlineKeyboardButton> keyboardBackDelete = getKeyboardBackDelete(deleteCallbackFollow);

                InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                        .keyboardRow(keyboardBackDelete)
                        .keyboardRow(keyboardStartStop)
                        .build();

                SendMessage sendMessage = SendMessage.builder()
                        .chatId(curChatId)
                        .text(message)
                        .parseMode("MarkdownV2")
                        .replyMarkup(keyboard)
                        .build();

                computeAndDelete();
                telegramBot.sendResponseMessage(sendMessage);
            }
        }
        else if (textMessage.equals(FOLLOW_CANCEL.getShC())) {
            pendingFollow.remove(curChatId);
            computeAndDelete();
            telegramBot.sendMenuKeyBoard(curChatId);
        }
//        else if (textMessage.equals(START_FOLLOW.getShC())) {
//            pendingFollow.remove(curChatId);
//
//
//
//            computeAndDelete();
//            telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, message, FOLLOW));
//        }
//        else if (textMessage.equals(STOP_FOLLOW.getShC())) {
//            pendingFollow.remove(curChatId);
//            computeAndDelete();
//            telegramBot.sendMenuKeyBoard(curChatId);
//        }
        else {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(curChatId);
            sendMessage.setText("Произошла ошибка при получении информации о слежении.");

            telegramBot.sendResponseMessage(sendMessage);
        }
    }

    private void processDeleteFollow() {

        String followKeyWallet = textMessage.substring(15);
        List<FollowEntity> followList = followRepository.findByUserChatId(curChatId);
        try {
            Optional<FollowEntity> followEntityOptional = followList.stream()
                    .filter(fe -> fe.getFollowKeyWallet().equals(followKeyWallet))
                    .findFirst();

            if (followEntityOptional.isPresent()) {
                FollowEntity deleteFollowEntity = followEntityOptional.get();
                followRepository.delete(deleteFollowEntity);
                followList.remove(deleteFollowEntity);
                processAllFollow(followList);
            } else {
                telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, NO_SUCH_FOLLOW_KEY, BACK_ALL_FOLLOW));
            }
        } catch (Exception e) {
            log.error("{}", e.getMessage());
        }
    }

    private void processAddFollow(List<FollowEntity> followList) {
        Optional<AuthEntity> auth = authRepository.findByChatId(curChatId);

        if (auth.isEmpty()) {
            computeAndDelete();
            telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, YOU_NEED_REG, BACK_MENU));
        } else {
            if (pendingFollow.containsKey(curChatId)) {
                FollowEntity containsEntity = pendingFollow.get(curChatId);
                if (!isValidFollowKey(textMessage)) {
                    computeAndDelete();
                    telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, NOT_VALID_FOLLOW_KEY, BACK_MENU));
                } else if (isAlreadyContains(textMessage, followList)) {
                    computeAndDelete();
                    telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, ALREADY_HAS_THIS_FOLLOW_KEY, BACK_MENU));
                } else {
                    containsEntity.setFollowKeyWallet(textMessage);
                    containsEntity.setDateStartFollow(LocalDate.now());
                    pendingFollow.remove(curChatId);
                    followRepository.save(containsEntity);
                    computeAndDelete();
                    telegramBot.sendResponseMessage(composeDefaultMessageWithKeysAddBack(curChatId, KEY_SUCCESSFULLY_ADDED, BACK_MENU, ADD_FOLLOW));
                }
            } else {
                FollowEntity newFollowEntity = new FollowEntity();
                newFollowEntity.setUser(auth.get());
                newFollowEntity.setCountCollDone(0);
                newFollowEntity.setCountAutotradeDone(0);
                pendingFollow.put(curChatId, newFollowEntity);

                computeAndDelete();
                telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, TO_FOLLOW, BACK_MENU));
            }
        }
    }

    private boolean isAlreadyContains(String textMessage, List<FollowEntity> followList) {
        return followList.stream().anyMatch(thisFollow -> Objects.equals(thisFollow.getFollowKeyWallet(), textMessage));
    }

    private boolean isValidFollowKey(String textMessage) {
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
            sendMessage.setReplyMarkup(getInlineKeyboardButtons(Commands.FOLLOW_CANCEL.getShC(), back.getShC()));
        }

        return sendMessage;
    }

    private SendMessage composeDefaultMessageWithKeysAddBack(Long chatId, String message, Commands back, Commands add) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(getKeyboardBackAdd(add.getShC(), back.getShC()));

        return sendMessage;
    }

    private SendMessage composeDefaultMessageWithAddFollowButton(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(TO_ADD);
        sendMessage.setReplyMarkup(getKeyboardBackAdd(ADD_FOLLOW.getShC(), Commands.BACK_MENU.getShC()));

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

    private static @NotNull InlineKeyboardMarkup getKeyboardBackAdd(String add, String back) {
        List<InlineKeyboardButton> backAndAddRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("Назад");
        backButton.setCallbackData(back);
        backAndAddRow.add(backButton);

        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText("Добавить");
        addButton.setCallbackData(add);
        backAndAddRow.add(addButton);

        return InlineKeyboardMarkup.builder()
                .keyboardRow(backAndAddRow)
                .build();
    }

    private static @NotNull List<InlineKeyboardButton> getKeyboardBackDelete(String delete) {
        List<InlineKeyboardButton> backAndAddRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("Назад");
        backButton.setCallbackData(BACK_ALL_FOLLOW.getShC());
        backAndAddRow.add(backButton);

        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText("Удалить");
        addButton.setCallbackData(delete);
        backAndAddRow.add(addButton);

        return backAndAddRow;
    }

    private static @NotNull List<InlineKeyboardButton> getKeyboardStartStop(String addressFollow) {
        List<InlineKeyboardButton> startAndStopRow = new ArrayList<>();
        InlineKeyboardButton startButton = new InlineKeyboardButton();
        startButton.setText("Start follow");
        startButton.setCallbackData(START_FOLLOW.getShC() + addressFollow);
        startAndStopRow.add(startButton);

        InlineKeyboardButton stopButton = new InlineKeyboardButton();
        stopButton.setText("Stop follow");
        stopButton.setCallbackData(STOP_FOLLOW.getShC() + addressFollow);
        startAndStopRow.add(stopButton);

        return startAndStopRow;
    }

    private static @NotNull List<InlineKeyboardButton> getInlineKeyboardButtonsListWithNameAdd(String add, String menu) {
        List<InlineKeyboardButton> backAndAddRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("Назад");
        backButton.setCallbackData(add);
        backAndAddRow.add(backButton);

        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText("Добавить");
        addButton.setCallbackData(menu);
        backAndAddRow.add(addButton);

        return backAndAddRow;
    }
}

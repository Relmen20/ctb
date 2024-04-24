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
    public static final String KEY_SUCCESSFULLY_ADDED = "Your follow key successfully added!\nPlease enter name of this follow";
    public static final String TO_FOLLOW = "Send key you want to follow";
    public static final String TO_ADD = "You dont have any follows key yet, want to add?";
    public static final String NO_SUCH_FOLLOW_KEY = "There is no such follow key";
    public static final String NAME_FOR_YOUR_FOLLOW_KEY_S = "Please enter new name for your follow key\n`%s`";
    public static final String ADDRESS_S_WITH_NAME_S = "You successfully add follow address: `%s`\nWith name: %s";
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
            AuthEntity auth = authRepository.findByChatId(curChatId).orElse(null);

            if (auth != null) {
                List<FollowEntity> followList = followRepository.findByAuthEntity(auth);

                if (textMessage.equals(FOLLOW.getShC()) ||
                        textMessage.equals(BACK_ALL_FOLLOW.getShC())) {
                    processAllFollow(followList);
                } else if (textMessage.startsWith(SHOW_.getShC()) ||
                        (textMessage.startsWith(CHANGE_FOLLOW_NAME.getShC()) || isListFollowInPending(followList)) ||
                        textMessage.equals(FOLLOW_CANCEL.getShC()) ||
                        textMessage.equals(STOP_FOLLOW.getShC()) ||
                        textMessage.equals(START_FOLLOW.getShC())) {
                    handleButtonClick(followList);
                } else if (textMessage.startsWith(DELETE_FOLLOW.getShC())) {
                    processDeleteFollow(followList);
                } else if (textMessage.equals(ADD_FOLLOW.getShC()) || pendingFollow.containsKey(curChatId)) {
                    processAddFollow(followList);
                }
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
                String followKeyName = followEntity.getNameOfWallet();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(followKeyName);
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

    public void handleButtonClick(List<FollowEntity> followList) {

        String followKeyWallet;

        if (textMessage.startsWith(SHOW_.getShC())) {
            followKeyWallet = textMessage.substring(6);
            FollowEntity followEntity = followList.stream()
                    .filter(follEnt -> Objects.equals(follEnt.getFollowKeyWallet(), followKeyWallet))
                    .findFirst()
                    .orElse(null);

            showFollowAddressInfo(followEntity);
        } else  if (textMessage.startsWith(CHANGE_FOLLOW_NAME.getShC()) || isListFollowInPending(followList)) {
            if (textMessage.startsWith(CHANGE_FOLLOW_NAME.getShC())) {
                pendingFollow.remove(curChatId);
                followKeyWallet = textMessage.substring(20);
                followList.stream()
                        .filter(follEnt -> Objects.equals(follEnt.getFollowKeyWallet(), followKeyWallet))
                        .findFirst().ifPresent(followEntity -> {
                            followEntity.setNameOfWallet(null);
                            pendingFollow.compute(curChatId, (k, existingValue) -> followEntity);
                            computeAndDelete();
                            String newFollowKey = String.format(NAME_FOR_YOUR_FOLLOW_KEY_S, followKeyWallet);
                            telegramBot.sendResponseMessage(composeDefaultMessage(curChatId,
                                                                                  newFollowKey,
                                                                                  BACK_ALL_FOLLOW));
                        });
            } else {
                FollowEntity followEntity = pendingFollow.get(curChatId);
                followEntity.setNameOfWallet(textMessage);
                followRepository.save(followEntity);
                pendingFollow.remove(curChatId);
                showFollowAddressInfo(followEntity);
            }
        } else if (textMessage.equals(FOLLOW_CANCEL.getShC())) {
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

    private void showFollowAddressInfo(FollowEntity followEntity) {
        if (followEntity != null) {
            String followKeyWallet = followEntity.getFollowKeyWallet();
            StringBuilder messageBuilder = new StringBuilder();

            messageBuilder.append("*Информация об адресе слежения*\n\n");
            messageBuilder.append("Имя кошелька: *").append(followEntity.getNameOfWallet()).append("*\n");
            messageBuilder.append("Кошелек следования: `").append(followKeyWallet).append("`\n");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault());
            String formattedDate = followEntity.getDateStartFollow().format(formatter);
            messageBuilder.append("Дата начала следования: ").append(formattedDate).append("\n");
            messageBuilder.append("Выполненных коллов: ").append(followEntity.getCountCollDone()).append("\n");
            messageBuilder.append("Выполненных автотрейдов: ").append(followEntity.getCountAutotradeDone()).append("\n");
            messageBuilder.append("Статус слежения кошелька: ");
            if (followEntity.getTrackingStatus()) {
                messageBuilder.append("\uD83D\uDFE2 _active_");
            } else {
                messageBuilder.append("\uD83D\uDD34 _inactive_");
            }
            String message = messageBuilder.toString();

            List<InlineKeyboardButton> keyboardStartStop = getKeyboardStartStop(followKeyWallet);

            List<InlineKeyboardButton> keyboardBackDelete = getKeyboardBackDelete(followKeyWallet);

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

    private void processDeleteFollow(List<FollowEntity> followList) {

        String followKeyWallet = textMessage.substring(15);
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
                if (containsEntity.getFollowKeyWallet() == null) {
                    if (!isValidFollowKey(textMessage)) {
                        computeAndDelete();
                        log.info("Not valid wallet address: {}  for userId: {}", textMessage, containsEntity.getAuthEntity().getAuthId());
                        telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, NOT_VALID_FOLLOW_KEY, BACK_MENU));
                    } else if (isAlreadyContains(textMessage, followList)) {
                        computeAndDelete();
                        telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, ALREADY_HAS_THIS_FOLLOW_KEY, BACK_MENU));
                    } else {
                        containsEntity.setFollowKeyWallet(textMessage);
                        computeAndDelete();
                        telegramBot.sendResponseMessage(composeDefaultMessageWithKeysAddBack(curChatId, KEY_SUCCESSFULLY_ADDED,
                                                                                             BACK_MENU, ADD_FOLLOW));
                    }
                } else if (containsEntity.getNameOfWallet() == null) {
                    containsEntity.setNameOfWallet(textMessage);
                    containsEntity.setDateStartFollow(LocalDate.now());
                    containsEntity.setTrackingStatus(false);
                    pendingFollow.remove(curChatId);
                    followRepository.save(containsEntity);

                    log.info("Add wallet: {}  for userId: {}", containsEntity.getFollowKeyWallet(), containsEntity.getAuthEntity().getAuthId());
                    computeAndDelete();
                    String completeAdd = String.format(ADDRESS_S_WITH_NAME_S, containsEntity.getFollowKeyWallet(), textMessage);
                    SendMessage sendMessage = SendMessage.builder()
                            .text(completeAdd)
                            .chatId(curChatId)
                            .parseMode("MarkdownV2")
                            .replyMarkup(getKeyboardBackAdd(ADD_FOLLOW.getShC(), BACK_ALL_FOLLOW.getShC()))
                            .build();

                    telegramBot.sendResponseMessage(sendMessage);
                }

            } else {
                FollowEntity newFollowEntity = new FollowEntity();
                newFollowEntity.setAuthEntity(auth.get());
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

    private boolean isListFollowInPending(List<FollowEntity> followList) {
        if (pendingFollow.containsKey(curChatId)) {
            for (FollowEntity followEntity : followList) {
                if (pendingFollow.get(curChatId).getNameOfWallet() != null) {
                    if (followEntity.getFollowKeyWallet().equals(pendingFollow.get(curChatId).getFollowKeyWallet())) {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }
        return false;
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

    private static @NotNull List<InlineKeyboardButton> getKeyboardBackDelete(String followWallet) {
        List<InlineKeyboardButton> backAddRenameRow = new ArrayList<>();

        InlineKeyboardButton renameButton = new InlineKeyboardButton();
        renameButton.setText("Rename");
        renameButton.setCallbackData(CHANGE_FOLLOW_NAME.getShC() + followWallet);
        backAddRenameRow.add(renameButton);

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("Back");
        backButton.setCallbackData(BACK_ALL_FOLLOW.getShC());
        backAddRenameRow.add(backButton);

        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText("Delete");
        addButton.setCallbackData(DELETE_FOLLOW.getShC() + followWallet);
        backAddRenameRow.add(addButton);

        return backAddRenameRow;
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

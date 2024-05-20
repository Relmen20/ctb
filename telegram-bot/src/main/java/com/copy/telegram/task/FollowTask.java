package com.copy.telegram.task;

import com.copy.common.dto.FollowTaskDto;
import com.copy.common.entity.AuthEntity;
import com.copy.common.entity.FollowEntity;
import com.copy.common.repository.AuthRepository;
import com.copy.common.repository.FollowRepository;
import com.copy.telegram.controller.TelegramBot;
import com.copy.telegram.producer.impl.FollowProducer;
import com.copy.telegram.utils.MessageUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.rpc.RpcApi;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.types.AccountInfo;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.copy.telegram.utils.Commands.*;
import static com.copy.telegram.utils.MessageUtils.computeAndDelete;

@Component
@Scope("prototype")
@Slf4j
@Setter
public class FollowTask implements Runnable {

    public static final String NOT_VALID_FOLLOW_KEY = "Not valid follow key";
    public static final String ALREADY_HAS_THIS_FOLLOW_KEY = "Your already has this follow key";
    public static final String KEY_SUCCESSFULLY_ADDED = "Please enter name of this follow";
    public static final String TO_FOLLOW = "Send key you want to follow";
    public static final String TO_ADD = "You don't have any follows key yet, want to add?";
    public static final String NO_SUCH_FOLLOW_KEY = "There is no such follow key";
    public static final String NAME_FOR_YOUR_FOLLOW_KEY_S = "Please enter new name for your follow key\n`%s`";
    public static final String ADDRESS_S_WITH_NAME_S = "You successfully add follow address: `%s`\nWith name: %s";
    public static final String MAX_FOLLOW_KEY_RESEARCHED = "You researched your follow keys limit.\nTry to upgrade your subscription or delete some keys";
    public static final String PLEASE_WAIT_START = "Sending info on server to start follow your address %s\nPlease wait";
    public static final String PLEASE_WAIT_STOP = "Sending info on server to stop follow your address %s\nPlease wait";
    public static final String WALLET_NOT_FOUND = "Wallet not found";
    private final String YOU_NEED_REG = "You need to be registered to follow, use /reg";

    private final String client = "https://mainnet.helius-rpc.com/?api-key=aa61fddb-a509-48d4-998b-7ae0b0ae5319";

    private final Long curChatId;
    private final String textMessage;
    private final FollowRepository followRepository;
    private final AuthRepository authRepository;
    private final ConcurrentHashMap<Long, FollowEntity> pendingFollow;
    private final FollowProducer followProducer;
    private final TelegramBot telegramBot;

    public FollowTask(Long curChatId, String textMessage, FollowRepository followRepository,
                      AuthRepository authRepository, ConcurrentHashMap<Long, FollowEntity> pendingFollow,
                      FollowProducer followProducer, TelegramBot telegramBot) {
        this.curChatId = curChatId;
        this.textMessage = textMessage;
        this.followRepository = followRepository;
        this.authRepository = authRepository;
        this.pendingFollow = pendingFollow;
        this.followProducer = followProducer;
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
                        textMessage.startsWith(STOP_FOLLOW.getShC()) ||
                        textMessage.startsWith(START_FOLLOW.getShC())) {
                    handleButtonClick(followList);
                } else if (textMessage.startsWith(DELETE_FOLLOW.getShC())) {
                    processDeleteFollow(followList);
                } else if (textMessage.equals(ADD_FOLLOW.getShC()) || pendingFollow.containsKey(curChatId)) {
                    processAddFollow(followList, auth);
                }
            } else {
                telegramBot.sendDeleteMessage(computeAndDelete(curChatId));
                telegramBot.sendResponseMessage(MessageUtils.composeMessageOneButtonRow(curChatId, YOU_NEED_REG, MENU, MENU.getDesc()));
            }
        } catch (Throwable e) {
            log.error("Error at star: {}", e.getMessage());
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

            rows.add(MessageUtils.getInlineTwoButtons(MENU.getDesc(), MENU, ADD_FOLLOW.getDesc(), ADD_FOLLOW));

            inlineKeyboardMarkup.setKeyboard(rows);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(curChatId);
            sendMessage.setText("Выберите ключ:");
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);

            telegramBot.sendDeleteMessage(computeAndDelete(curChatId));

            telegramBot.sendResponseMessage(sendMessage);
        } else {
            telegramBot.sendDeleteMessage(computeAndDelete(curChatId));

            telegramBot.sendResponseMessage(MessageUtils.composeMessageTwoButtonsRow(curChatId, TO_ADD,
                    ADD_FOLLOW, ADD_FOLLOW.getDesc(),
                    BACK_MENU, BACK_MENU.getDesc()));
        }
    }

    public void handleButtonClick(List<FollowEntity> followList) {

        String followKeyWallet;

        if (textMessage.equals(FOLLOW_CANCEL.getShC())) {
            pendingFollow.remove(curChatId);
            telegramBot.sendDeleteMessage(computeAndDelete(curChatId));
            telegramBot.sendMenuKeyBoard(curChatId);
        } else if (textMessage.startsWith(CHANGE_FOLLOW_NAME.getShC()) || isListFollowInPending(followList)) {
            if (textMessage.startsWith(CHANGE_FOLLOW_NAME.getShC())) {
                pendingFollow.remove(curChatId);
                followKeyWallet = textMessage.substring(20);
                followList.stream()
                        .filter(follEnt -> Objects.equals(follEnt.getFollowKeyWallet(), followKeyWallet))
                        .findFirst().ifPresent(followEntity -> {
                            followEntity.setNameOfWallet(null);
                            pendingFollow.put(curChatId, followEntity);
                            String newFollowKey = String.format(NAME_FOR_YOUR_FOLLOW_KEY_S, followKeyWallet);
                            telegramBot.sendDeleteMessage(computeAndDelete(curChatId));
                            telegramBot.sendResponseMessage(MessageUtils.composeMessageTwoButtonsRow(curChatId, newFollowKey,
                                    BACK_ALL_FOLLOW, BACK_ALL_FOLLOW.getDesc(),
                                    FOLLOW_CANCEL, FOLLOW_CANCEL.getDesc()));
                        });
            } else {
                FollowEntity followEntity = pendingFollow.get(curChatId);
                followEntity.setNameOfWallet(textMessage);
                followRepository.save(followEntity);
                pendingFollow.remove(curChatId);
                showFollowAddressInfo(followEntity);
            }
        } else if (textMessage.startsWith(SHOW_.getShC())) {
            followKeyWallet = textMessage.substring(16);
            FollowEntity followEntity = followList.stream()
                    .filter(follEnt -> Objects.equals(follEnt.getFollowKeyWallet(), followKeyWallet))
                    .findFirst()
                    .orElse(null);

            showFollowAddressInfo(followEntity);
        } else if (textMessage.startsWith(START_FOLLOW.getShC())) {
            pendingFollow.remove(curChatId);
            followKeyWallet = textMessage.substring(14);

            Optional<FollowEntity> optionalFollowEntity = followList.stream()
                    .filter(followEntity -> followEntity.getFollowKeyWallet().equals(followKeyWallet))
                    .findFirst();

            telegramBot.sendDeleteMessage(computeAndDelete(curChatId));
            if (optionalFollowEntity.isEmpty()) {
                telegramBot.sendResponseMessage(MessageUtils.composeMessageOneButtonRow(curChatId, NO_SUCH_FOLLOW_KEY, MENU, MENU.getDesc()));
            } else {
                FollowEntity followEntity = optionalFollowEntity.get();

                String pleaseWait = String.format(PLEASE_WAIT_START, followEntity.getNameOfWallet());
                telegramBot.sendResponseMessage(SendMessage.builder().chatId(curChatId).text(pleaseWait).build());

                followProducer.produceToFollowExchange(FollowTaskDto.builder()
                        .follow(followEntity)
                        .isStart(true)
                        .build());
            }
        } else if (textMessage.startsWith(STOP_FOLLOW.getShC())) {
            pendingFollow.remove(curChatId);
            followKeyWallet = textMessage.substring(13);

            Optional<FollowEntity> optionalFollowEntity = followList.stream()
                    .filter(followEntity -> followEntity.getFollowKeyWallet().equals(followKeyWallet))
                    .findFirst();

            telegramBot.sendDeleteMessage(computeAndDelete(curChatId));
            if (optionalFollowEntity.isEmpty()) {
                telegramBot.sendResponseMessage(MessageUtils.composeMessageOneButtonRow(curChatId, NO_SUCH_FOLLOW_KEY, MENU, MENU.getDesc()));
            } else {
                FollowEntity followEntity = optionalFollowEntity.get();

                String pleaseWait = String.format(PLEASE_WAIT_STOP, followEntity.getNameOfWallet());
                telegramBot.sendResponseMessage(SendMessage.builder().chatId(curChatId).text(pleaseWait).build());
                followProducer.produceToFollowExchange(FollowTaskDto.builder()
                        .follow(followEntity)
                        .isStart(false)
                        .build());
            }
        }
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

            List<InlineKeyboardButton> keyboardStartStop = MessageUtils.getInlineTwoButtons(START_FOLLOW.getDesc(), START_FOLLOW.getShC() + followKeyWallet,
                    STOP_FOLLOW.getDesc(), STOP_FOLLOW.getShC() + followKeyWallet);

            List<InlineKeyboardButton> keyboardBackDelete = MessageUtils.getInlineThreeButtons(CHANGE_FOLLOW_NAME.getDesc(), CHANGE_FOLLOW_NAME.getShC() + followKeyWallet,
                    BACK_ALL_FOLLOW.getDesc(), BACK_ALL_FOLLOW.getShC(), DELETE_FOLLOW.getDesc(), DELETE_FOLLOW.getShC() + followKeyWallet);

            InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                    .keyboardRow(keyboardBackDelete)
                    .keyboardRow(keyboardStartStop)
                    .build();

            SendMessage sendMessage = SendMessage.builder()
                    .chatId(curChatId)
                    .text(message)
                    .replyMarkup(keyboard)
                    .build();

            telegramBot.sendDeleteMessage(computeAndDelete(curChatId));
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
                telegramBot.sendResponseMessage(MessageUtils.composeMessageOneButtonRow(curChatId, NO_SUCH_FOLLOW_KEY,
                        BACK_ALL_FOLLOW, BACK_ALL_FOLLOW.getDesc()));
            }
        } catch (Exception e) {
            log.error("Error while delete follow: {}", e.getMessage());
        }
    }

    private void processAddFollow(List<FollowEntity> followList, AuthEntity auth) {
        if (isMaxAddFollowNotResearched(followList, auth)) {
            if (pendingFollow.containsKey(curChatId)) {
                FollowEntity containsEntity = pendingFollow.get(curChatId);
                if (containsEntity.getFollowKeyWallet() == null) {
                    if (!isValidFollowKey(textMessage)) {
                        log.info("Not valid wallet address: {}  for userId: {}", textMessage, containsEntity.getAuthEntity().getAuthId());
                        telegramBot.sendResponseMessage(MessageUtils.composeMessageOneButtonRow(curChatId, NOT_VALID_FOLLOW_KEY,
                                BACK_MENU, BACK_MENU.getDesc()));
                    } else if (isAlreadyContains(textMessage, followList)) {
                        telegramBot.sendResponseMessage(MessageUtils.composeMessageOneButtonRow(curChatId, ALREADY_HAS_THIS_FOLLOW_KEY,
                                BACK_MENU, BACK_MENU.getDesc()));
                    } else {
                        containsEntity.setFollowKeyWallet(textMessage);
                        telegramBot.sendResponseMessage(MessageUtils.composeMessageTwoButtonsRow(curChatId, KEY_SUCCESSFULLY_ADDED,
                                BACK_MENU, BACK_MENU.getDesc(),
                                ADD_FOLLOW, ADD_FOLLOW.getDesc()));
                    }
                } else if (containsEntity.getNameOfWallet() == null) {
                    containsEntity.setNameOfWallet(textMessage);
                    containsEntity.setDateStartFollow(LocalDate.now());
                    containsEntity.setTrackingStatus(false);
                    pendingFollow.remove(curChatId);
                    followRepository.save(containsEntity);

                    log.info("Add wallet: {}  for userId: {}", containsEntity.getFollowKeyWallet(), containsEntity.getAuthEntity().getAuthId());
                    String completeAdd = String.format(ADDRESS_S_WITH_NAME_S, containsEntity.getFollowKeyWallet(), textMessage);
                    SendMessage sendMessage = SendMessage.builder()
                            .text(completeAdd)
                            .chatId(curChatId)
                            .replyMarkup(MessageUtils.getInlineKeyboardTwoButtons(FOLLOW_CANCEL.getDesc(), FOLLOW_CANCEL,
                                    BACK_ALL_FOLLOW.getDesc(), BACK_ALL_FOLLOW))
                            .build();

                    telegramBot.sendResponseMessage(sendMessage);
                }

            } else {
                FollowEntity newFollowEntity = new FollowEntity();
                newFollowEntity.setAuthEntity(auth);
                pendingFollow.put(curChatId, newFollowEntity);

                telegramBot.sendDeleteMessage(computeAndDelete(curChatId));
                telegramBot.sendResponseMessage(MessageUtils.composeMessageTwoButtonsRow(curChatId, TO_FOLLOW,
                        FOLLOW_CANCEL, FOLLOW_CANCEL.getDesc(),
                        BACK_MENU, BACK_MENU.getDesc()));
            }
        } else {
            telegramBot.sendDeleteMessage(computeAndDelete(curChatId));
            telegramBot.sendResponseMessage(MessageUtils.composeMessageTwoButtonsRow(curChatId, MAX_FOLLOW_KEY_RESEARCHED,
                    BACK_ALL_FOLLOW, BACK_ALL_FOLLOW.getDesc(), SUBSCRIBE, SUBSCRIBE.getDesc()));
        }
    }

    private boolean isMaxAddFollowNotResearched(List<FollowEntity> followList, AuthEntity auth) {
        return followList.size() < auth.getSubscriptionEntity().getFollowKeyAvailable() ||
                isListFollowInPending(followList);
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
            return followList.stream().anyMatch(folEnt ->
                    folEnt.getFollowKeyWallet().equals(pendingFollow.get(curChatId).getFollowKeyWallet()) &&
                            pendingFollow.get(curChatId).getNameOfWallet() == null
            );
        }
        return false;
    }
}

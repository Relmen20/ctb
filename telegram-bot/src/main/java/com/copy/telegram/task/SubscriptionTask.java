package com.copy.telegram.task;

import com.copy.common.entity.SubscriptionEntity;
import com.copy.common.repository.AuthRepository;
import com.copy.common.repository.SubscriptionRepository;
import com.copy.telegram.controller.TelegramBot;
import com.copy.telegram.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.copy.telegram.utils.Commands.*;

@Component
@Scope("prototype")
@Slf4j
public class SubscriptionTask implements Runnable {

    public static final String ONE_OF_SUBSCRIPTIONS = "Choose one of Subscriptions: ";

    private final Long curChatId;
    private final String textMessage;
    private final SubscriptionRepository subscriptionRepository;
    private final AuthRepository authRepository;
    private final TelegramBot telegramBot;

    public SubscriptionTask(Long curChatId, String textMessage, SubscriptionRepository subscriptionRepository,
                            AuthRepository authRepository, TelegramBot telegramBot) {
        this.curChatId = curChatId;
        this.textMessage = textMessage;
        this.subscriptionRepository = subscriptionRepository;
        this.authRepository = authRepository;
        this.telegramBot = telegramBot;
    }

    @Override
    public void run() {
        if (textMessage.equals(SUBSCRIBE.getShC())) {
            showAllSubscriptions();
        } else if (textMessage.startsWith(SUB_SHOW.getShC())) {
            showChosenSub();
        }
    }

    private void showChosenSub() {
        try{
            String subName = textMessage.substring(10);
            SubscriptionEntity subscriptionEntity = subscriptionRepository.getBySubName(subName);

            String stringBuilder = "*                           Подписка  " + subName + ":*\n\n" +
                    "_" + subscriptionEntity.getSubDescription() + "_\n\n" +
                    "*С этой подпиской вам будет доступно:*\n" +
                    "Количество ключей для слежения: `" + subscriptionEntity.getFollowKeyAvailable() + "`\n" +
                    "Количество коллов: `" + subscriptionEntity.getCountCollAvailable() + "`\n" +
                    "Количество автосделок: `" + subscriptionEntity.getCountAutotradeAvailable() + "`\n\n" +
                    "Срок подписки: _" + subscriptionEntity.getSubDatePeriod() + "_\n\n" +
                    "Стоимость подписки: _" + subscriptionEntity.getSubPrice() + "$_\n\n";

            String message = stringBuilder.replace("?n", "\n");

            telegramBot.sendResponseMessage(MessageUtils.composeMessageTwoButtonsRow(curChatId, message,
                                        MENU.getShC(), MENU.getDesc(),
                    BUY_SUBSCRIBE_NOW.getShC() + subName, BUY_SUBSCRIBE_NOW.getDesc()));
        } catch (Exception e) {
            log.error("Fail while process subscription to user: {}", e.getMessage());
        }
    }

    private void showAllSubscriptions() {
        try {
            List<SubscriptionEntity> subscriptionEntities = subscriptionRepository.findAll();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            for (SubscriptionEntity subscriptionEntity : subscriptionEntities) {
                rows.add(Stream.of(InlineKeyboardButton.builder()
                                .text(subscriptionEntity.getSubName())
                                .callbackData(SUB_SHOW.getShC() + subscriptionEntity.getSubName())
                                .build())
                        .toList());
            }

            rows.add(List.of(InlineKeyboardButton.builder().text(MENU.getDesc()).callbackData(MENU.getShC()).build()));
            telegramBot.sendResponseMessage(SendMessage.builder()
                    .chatId(curChatId)
                    .text(ONE_OF_SUBSCRIPTIONS)
                    .replyMarkup(InlineKeyboardMarkup.builder()
                            .keyboard(rows)
                            .build())
                    .build());
        } catch (Exception e) {
            log.error("Error while formating all subs: {}", e.getMessage());
        }
    }
}

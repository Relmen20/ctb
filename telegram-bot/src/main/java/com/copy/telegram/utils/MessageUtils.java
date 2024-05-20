package com.copy.telegram.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class MessageUtils {

    private static final ConcurrentHashMap<Long, Integer> chatIdToLastMessage =  new ConcurrentHashMap<>();

    public static DeleteMessage computeAndDelete(Long curChatId) {
        return DeleteMessage.builder()
                .chatId(curChatId)
                .messageId(chatIdToLastMessage.get(curChatId))
                .build();
    }

    public static void computeMessage(Long curChatId, Integer messageId) {
        if (messageId != null) {
            chatIdToLastMessage.compute(curChatId, (k, existingValue) -> messageId);
        }
    }

    public SendMessage generateSendMessageWithText(Update update, String text) {
        var message = update.getMessage();
        var sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText(text);
        return sendMessage;
    }


    public static SendMessage composeMessageOneButtonRow(Long chatId, String message, Commands buttonCommand, String buttonText) {
        return SendMessage.builder()
                .text(message)
                .chatId(chatId)
                .replyMarkup(getInlineKeyboardOneButtonRow(buttonText, buttonCommand))
                .build();
    }

    public static InlineKeyboardMarkup getInlineKeyboardOneButtonRow(String buttonText, Commands buttonCommand) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(
                        Stream.of(InlineKeyboardButton.builder()
                                        .text(buttonText)
                                        .callbackData(buttonCommand.getShC())
                                        .build())
                                .toList()
                )
                .build();
    }


    public static SendMessage composeMessageTwoButtonsRow(Long chatId, String message,
                                                          Commands firstButtonCommand, String firstButtonText,
                                                          Commands secondButtonCommand, String secondButtonText) {
        return SendMessage.builder()
                .text(message)
                .chatId(chatId)
                .replyMarkup(MessageUtils.getInlineKeyboardTwoButtons(firstButtonText, firstButtonCommand,
                        secondButtonText, secondButtonCommand))
                .build();
    }

    public static InlineKeyboardMarkup getInlineKeyboardTwoButtons(String firstButtonText, Commands firstButtonCommand,
                                                                   String secondButtonText, Commands secondButtonCommand) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(
                        getInlineTwoButtons(firstButtonText, firstButtonCommand,
                                            secondButtonText, secondButtonCommand)
                )
                .build();
    }

    public static List<InlineKeyboardButton> getInlineTwoButtons(String firstButtonText, Commands firstButtonCommand,
                                                                 String secondButtonText, Commands secondButtonCommand){
        return Stream.of(
                InlineKeyboardButton.builder()
                        .text(firstButtonText)
                        .callbackData(firstButtonCommand.getShC())
                        .build(),
                InlineKeyboardButton.builder()
                        .text(secondButtonText)
                        .callbackData(secondButtonCommand.getShC())
                        .build()
        ).toList();
    }


    public static SendMessage composeMessageTwoButtonsRow(Long chatId, String message,
                                                          String firstButtonCommand, String firstButtonText,
                                                          String secondButtonCommand, String secondButtonText) {
        return SendMessage.builder()
                .text(message)
                .chatId(chatId)
                .replyMarkup(MessageUtils.getInlineKeyboardTwoButtons(firstButtonText, firstButtonCommand,
                        secondButtonText, secondButtonCommand))
                .build();
    }

    public static InlineKeyboardMarkup getInlineKeyboardTwoButtons(String firstButtonText, String firstButtonCommand,
                                                                   String secondButtonText, String secondButtonCommand) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(
                        getInlineTwoButtons(firstButtonText, firstButtonCommand,
                                secondButtonText, secondButtonCommand)
                )
                .build();
    }

    public static List<InlineKeyboardButton> getInlineTwoButtons(String firstButtonText, String firstButtonCommand,
                                                                 String secondButtonText, String secondButtonCommand){
        return Stream.of(
                InlineKeyboardButton.builder()
                        .text(firstButtonText)
                        .callbackData(firstButtonCommand)
                        .build(),
                InlineKeyboardButton.builder()
                        .text(secondButtonText)
                        .callbackData(secondButtonCommand)
                        .build()
        ).toList();
    }



    public static SendMessage composeMessageThreeButtonsRow(Long chatId, String message,
                                                          String firstButtonCommand, String firstButtonText,
                                                          String secondButtonCommand, String secondButtonText,
                                                            String thirdButtonCommand, String thirdButtonText) {
        return SendMessage.builder()
                .text(message)
                .chatId(chatId)
                .replyMarkup(MessageUtils.getInlineKeyboardThreeButtons(firstButtonText, firstButtonCommand,
                        secondButtonText, secondButtonCommand,
                        thirdButtonText, thirdButtonCommand))
                .build();
    }

    public static InlineKeyboardMarkup getInlineKeyboardThreeButtons(String firstButtonText, String firstButtonCommand,
                                                                   String secondButtonText, String secondButtonCommand,
                                                                     String thirdButtonText,  String thirdButtonCommand) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(
                        getInlineThreeButtons(firstButtonText, firstButtonCommand,
                                secondButtonText, secondButtonCommand,
                                thirdButtonText, thirdButtonCommand)
                )
                .build();
    }

    public static List<InlineKeyboardButton> getInlineThreeButtons(String firstButtonText, String firstButtonCommand,
                                                                 String secondButtonText, String secondButtonCommand,
                                                                   String thirdButtonText, String thirdButtonCommand){
        return Stream.of(
                InlineKeyboardButton.builder()
                        .text(firstButtonText)
                        .callbackData(firstButtonCommand)
                        .build(),
                InlineKeyboardButton.builder()
                        .text(secondButtonText)
                        .callbackData(secondButtonCommand)
                        .build(),
                InlineKeyboardButton.builder()
                        .text(thirdButtonText)
                        .callbackData(thirdButtonCommand)
                        .build()
        ).toList();
    }



    public static SendMessage composeMessageThreeButtonsRow(Long chatId, String message,
                                                            Commands firstButtonCommand, String firstButtonText,
                                                            Commands secondButtonCommand, String secondButtonText,
                                                            Commands thirdButtonCommand, String thirdButtonText) {
        return SendMessage.builder()
                .text(message)
                .chatId(chatId)
                .replyMarkup(getInlineKeyboardThreeButtons(firstButtonText, firstButtonCommand,
                                                           secondButtonText, secondButtonCommand,
                                                           thirdButtonText, thirdButtonCommand))
                .build();
    }

    public static InlineKeyboardMarkup getInlineKeyboardThreeButtons(String firstButtonText, Commands firstButtonCommand,
                                                                     String secondButtonText, Commands secondButtonCommand,
                                                                     String thirdButtonText, Commands thirdButtonCommand) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(
                        getInlineThreeButtons(firstButtonText, firstButtonCommand,
                                secondButtonText, secondButtonCommand,
                                thirdButtonText, thirdButtonCommand)
                )
                .build();
    }

    public static List<InlineKeyboardButton> getInlineThreeButtons(String firstButtonText, Commands firstButtonCommand,
                                                                   String secondButtonText, Commands secondButtonCommand,
                                                                   String thirdButtonText, Commands thirdButtonCommand){
        return Stream.of(
                InlineKeyboardButton.builder()
                        .text(firstButtonText)
                        .callbackData(firstButtonCommand.getShC())
                        .build(),
                InlineKeyboardButton.builder()
                        .text(secondButtonText)
                        .callbackData(secondButtonCommand.getShC())
                        .build(),
                InlineKeyboardButton.builder()
                        .text(thirdButtonText)
                        .callbackData(thirdButtonCommand.getShC())
                        .build()
        ).toList();
    }
}

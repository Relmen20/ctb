package com.copy.telegram.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.util.stream.Stream;

import static com.copy.telegram.utils.Commands.*;

@Service
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${bot.token}")
    private String botToken;
    @Value("${bot.username}")
    private String botUsername;

    private final UpdateController updateController;

    public TelegramBot(UpdateController updateController){
        this.updateController = updateController;
    }

    @PostConstruct
    public void init(){
        updateController.registerBot(this);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        updateController.processUpdate(update);
    }

    public void sendResponseMessage(SendMessage sendMessage){
        if(sendMessage != null){
            try{
                execute(sendMessage);
            } catch (TelegramApiException e){
                log.error("Error while sending message in chat: {}", e.getMessage());
            }
        }
    }

    public void sendDeleteMessage(DeleteMessage deleteMessage) {
        if(deleteMessage != null){
            try{
                execute(deleteMessage);
            } catch (TelegramApiException e){
                log.error("Error while sending message in chat: {}", e.getMessage());
            }
        }
    }

    public void sendMenuKeyBoard(Long chatId) {
        InlineKeyboardButton buttonRegistration = InlineKeyboardButton.builder()
                .text("Registration")
                .callbackData(REGISTRATION.getShC())
                .build();
        InlineKeyboardButton buttonUpdateData = InlineKeyboardButton.builder()
                .text("Update your data")
                .callbackData(UPDATE.getShC())
                .build();
        InlineKeyboardButton buttonFollow = InlineKeyboardButton.builder()
                .text("Your follows")
                .callbackData(FOLLOW.getShC())
                .build();
        InlineKeyboardButton buttonAddFollow = InlineKeyboardButton.builder()
                .text("Add follow key")
                .callbackData(ADD_FOLLOW.getShC())
                .build();

        InlineKeyboardButton buttonShowMyData = InlineKeyboardButton.builder()
                .text("Show my data")
                .callbackData(SHOW_MY_DATA.getShC())
                .build();

        InlineKeyboardButton buttonSubscribe = InlineKeyboardButton.builder()
                .text("Subscribe")
                .callbackData(SUBSCRIBE.getShC())
                .build();

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(Stream.of(buttonShowMyData).toList())
                .keyboardRow(Stream.of(buttonRegistration, buttonUpdateData).toList())
                .keyboardRow(Stream.of(buttonFollow, buttonAddFollow).toList())
                .keyboardRow(Stream.of(buttonSubscribe).toList())
                .build();

        SendMessage keyboardMessage = SendMessage.builder()
                .text("Choose one of the option: ")
                .replyMarkup(keyboard)
                .chatId(chatId)
                .build();

        sendResponseMessage(keyboardMessage);
    }
}

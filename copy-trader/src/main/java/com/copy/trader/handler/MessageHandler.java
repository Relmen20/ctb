package com.copy.trader.handler;

public interface MessageHandler {

    void handleMessage(String message, String specialKey);
}

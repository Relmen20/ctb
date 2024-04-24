package com.copy.telegram.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Commands {
    MENU("/menu"),
    // help for menu
    BACK_MENU("/back-menu"),

    START("/start"),
    SHOW_MY_DATA("/show-my-data"),

    REGISTRATION("/reg"),
    UPDATE("/update-name"),
    // help for auth
    AUTH_CANCEL("/auth-cancel"),

    WALLETS("/wallets"),
    WALLET_CANCEL("/wallet-cancel"),
    WALLET_ADD("/wallet-add"),
    WALLET_DELETE("/wallet-delete_"),

    FOLLOW("/follow"),
    ADD_FOLLOW("/add-follow"),
    DELETE_FOLLOW("/delete-follow_"),
    // help commands for follow
    FOLLOW_CANCEL("/follow-cancel"),
    SHOW_("/show_"),
    BACK_ALL_FOLLOW("/back-all-follow"),
    CHANGE_FOLLOW_NAME("/change_follow_name_"),

    START_FOLLOW("/start-follow_"),
    STOP_FOLLOW("/stop-follow_"),

    SUBSCRIBE("/sub");

    private final String shC;

    public static Commands getCommandByText(String text) {
        for (Commands command : Commands.values()) {
            if (text.equals(command.shC)) {
                return command;
            }
        }
        return null;
    }
}
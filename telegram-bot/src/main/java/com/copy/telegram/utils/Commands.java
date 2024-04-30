package com.copy.telegram.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Commands {
    MENU("/menu", "Menu"),
    // help for menu
    BACK_MENU("/back-menu", "Back to menu"),

    START("/start", "Start"),
    SHOW_MY_DATA("/show-my-data", "Show my data"),

    REGISTRATION("/reg", "Registration"),
    UPDATE("/update-name", "Change name"),
    // help for auth
    AUTH_CANCEL("/auth-cancel", "Cancel"),

    WALLETS("/wallets" ,"Wallets"),
    WALLET_CANCEL("/wallet-cancel", "Cancel"),
    WALLET_ADD("/wallet-add", "Add Wallet"),
    WALLET_DELETE("/wallet-delete_", "Delete wallet"),

    FOLLOW("/follow", "My follows"),
    ADD_FOLLOW("/add-follow", "Add follow"),
    DELETE_FOLLOW("/delete-follow_", "Delete follow"),
    // help commands for follow
    FOLLOW_CANCEL("/follow-cancel", "Cancel"),
    SHOW_("/my-show-follow_", "Show "),
    BACK_ALL_FOLLOW("/back-all-follow", "Back all follows"),
    CHANGE_FOLLOW_NAME("/change_follow_name_", "Rename follow"),

    START_FOLLOW("/start-follow_", "Start follow"),
    STOP_FOLLOW("/stop-follow_", "Stop follow"),

    SUBSCRIBE("/sub", "Subscriptions"),
    SUB_SHOW("/sub-show_", "Show "),
    BUY_SUBSCRIBE_NOW("/buy-subscribe-now_", "Buy subscribe");

    private final String shC;
    private final String desc;

    public static Commands getCommandByText(String text) {
        for (Commands command : Commands.values()) {
            if (text.equals(command.shC)) {
                return command;
            }
        }
        return null;
    }
}
package com.copy.telegram.task;

import lombok.extern.slf4j.Slf4j;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.rpc.RpcApi;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.types.AccountInfo;

@Slf4j
public class WalletsTask {


    private final String client = "https://mainnet.helius-rpc.com/?api-key=aa61fddb-a509-48d4-998b-7ae0b0ae5319";

    //TODO: Вынести в walletsTask
//                else if (auth.getWalletAddress() == null) {
//                    if (isValidAddress(textMessage)) {
//                        auth.setWalletAddress(textMessage);
//                        authRepository.save(auth);
//                        pendingRegistrations.remove(curChatId);
//                        computeAndDelete();
//                        telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, REGISTERED, null));
//                    } else {
//                        String wrongNameMessage = String.format(ANOTHER_WALLET, textMessage);
//                        computeAndDelete();
//                        telegramBot.sendResponseMessage(composeDefaultMessage(curChatId, wrongNameMessage, BACK_MENU));
//                    }
//                }


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
}

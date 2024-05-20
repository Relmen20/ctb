package com.copy.trader.task;

import com.copy.common.dto.FollowTaskDto;
import com.copy.common.entity.AuthEntity;
import com.copy.common.entity.FollowEntity;
import com.copy.trader.handler.MessageHandler;
import com.copy.trader.service.FollowTrackingService;
import com.copy.trader.service.TrackingSessionService;
import com.copy.trader.web.WebsocketClientEndpoint;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.websocket.Session;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Data
@RequiredArgsConstructor
public class SolanaTransactionTracker{

    private volatile boolean running = true;

    private FollowTrackingService followTrackingService;
    private final FollowEntity followEntity;
    private final AuthEntity authEntity;
    private final String SOLANA_WEB_ADDRESS;
    private final MessageHandler messageHandler;

    private Session webSocketSession;

    public void start(){
        subscribe();
    }

    public void subscribe() {
        while(running){
            try {
                JSONObject requestData = composeRequestData();
                String specialKey = authEntity.getAuthId() + TrackingSessionService.DELIMITER + followEntity.getFollowKeyWallet();
                final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI(SOLANA_WEB_ADDRESS), specialKey);
                clientEndPoint.addMessageHandler(messageHandler);
                webSocketSession = clientEndPoint.getUserSession();
                clientEndPoint.sendMessage(String.valueOf(requestData));
                while(webSocketSession.isOpen()){
                    if(followEntity.getCountCollDone() < authEntity.getSubscriptionEntity().getCountCollAvailable()){
                        continue;
                    } else {
                        stop();
                        CompletableFuture.runAsync(() -> followTrackingService.stopFollow(FollowTaskDto.builder()
                                .follow(followEntity)
                                .isStart(false)
                                .build()));
                    }
                }
            } catch (Throwable e) {
                log.error("Error while connecting Session, authId: {}, followId: {}, error: {}",
                            authEntity.getAuthId(), followEntity.getFollowId(), e.getMessage());
            }
        }
    }

    private JSONObject composeRequestData(){
        var list = new ArrayList<String>();
        list.add(followEntity.getFollowKeyWallet());
        JSONObject nestedObject1 = new JSONObject()
                .put("mentions", list);
        JSONObject nestedObject2 = new JSONObject()
                .put("commitment", "confirmed");
        JSONObject requestData = new JSONObject();
        requestData.put("jsonrpc", "2.0");
        requestData.put("id", "0");
        requestData.put("method", "logsSubscribe");
        var list2 = new JSONArray();
        list2.put(nestedObject1);
        list2.put(nestedObject2);
        requestData.put("params", list2);
        return requestData;

    }

    public void stop() {
        try {
            running = false;
            if (webSocketSession != null && webSocketSession.isOpen()) {
                webSocketSession.close();
            }
        } catch (IOException e) {
            log.error("Error while closing socket for authId: {}, followId: {}, message: {}",
                    authEntity.getAuthId(), followEntity.getFollowId(), e.getMessage());
        }
    }

}

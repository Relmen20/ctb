package com.copy.trader.web;

import com.copy.trader.handler.MessageHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Date;

@Slf4j
@Data
@ClientEndpoint
public class WebsocketClientEndpoint {

    private long timeStartConnection, timeStopConnection, timeConnectionWasAvailable;

    private Session userSession = null;
    private com.copy.trader.handler.MessageHandler messageHandler;

    public WebsocketClientEndpoint(URI endpointURI) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, endpointURI);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @OnOpen
    public void onOpen(Session userSession) {
        timeStartConnection = new Date().getTime();
        log.info("Opening websocket, connection starts at: {}", new Date(timeStartConnection));
        this.userSession = userSession;
    }


    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        timeStopConnection = new Date().getTime();
        connectionAliveTime();
        log.info("Closing websocket at: {}", new Date(timeStopConnection));
        this.userSession = null;
    }


    @OnMessage
    public void onMessage(String message) {
        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(message);
        }
    }

    @OnMessage
    public void onMessage(ByteBuffer bytes) {
        System.out.println("Handle byte buffer");
    }

    public void addMessageHandler(MessageHandler msgHandler) {
        this.messageHandler = msgHandler;
    }

    public void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }

    private void connectionAliveTime(){
        timeConnectionWasAvailable = timeStopConnection - timeStartConnection;
        log.info("Connection was opened: {}", new Date(timeConnectionWasAvailable).getTime());
    }
}
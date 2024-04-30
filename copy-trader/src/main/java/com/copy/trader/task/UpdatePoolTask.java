package com.copy.trader.task;

import com.copy.trader.handler.MessageHandler;
import com.copy.trader.web.WebsocketClientEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;

@Service
@Slf4j
public class UpdatePoolTask {

    @Value("${solana.web.address}")
    private String SOLANA_WEB_ADDRESS;

    @Value("${raydium.address}")
    private String person;

    @Autowired
    private MessageHandler messageHandler;

    public void start(){
//        subscribe(person);
    }

    public void subscribe(String person) {
        while(true){
            try {
                JSONObject requestData = composeRequestData(person);

                final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI(SOLANA_WEB_ADDRESS));

                clientEndPoint.addMessageHandler(messageHandler);

                while(clientEndPoint.getUserSession().isOpen()){
                    clientEndPoint.sendMessage(String.valueOf(requestData));

                    Thread.sleep(5000);
                }

            } catch (Throwable e) {
                log.error(e.toString());
            }
        }
    }

    private JSONObject composeRequestData(String person){
        var list = new ArrayList<String>();
        list.add(person);
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

}

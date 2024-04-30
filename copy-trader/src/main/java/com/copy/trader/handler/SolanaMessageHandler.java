package com.copy.trader.handler;

import com.copy.trader.service.TradeService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class SolanaMessageHandler implements MessageHandler{

    @Value("${raydium.lpv4}")
    private String RAYDIUM_LPV4;
    @Value("${solana.web.timeout.seconds}")
    public int TIMEOUT_IN_SECONDS;

    @Autowired
    private TradeService tradeService;

    private static final int MAX_RETRIES = 3;

    @Override
    public void handleMessage(String message) {
        try{
            JSONObject jsonMessage = new JSONObject(message);
            if(!handleErrorMessage(jsonMessage)){
                handleTransaction(jsonMessage);
            }
        }catch(Throwable e){
            log.error(e.getMessage());
        }
    }

    private void handleTransaction(JSONObject jsonMessage) {
        JSONObject resultMessage = jsonMessage.getJSONObject("params").getJSONObject("result");

        if(resultMessage.getJSONObject("value").isNull("err")){
//            log.info("Found transaction!");
            String signature = resultMessage.getJSONObject("value").getString("signature");
            String subscription = String.valueOf(jsonMessage.getJSONObject("params").get("subscription"));
            String slot = String.valueOf(resultMessage.getJSONObject("context").get("slot"));
            log.info("Subscription: {}; slot: {}; signature: {}", subscription, slot, signature);
//            tradeService.startTradeProcedure(signature, Integer.parseInt(slot));
        }
    }

    private boolean handleErrorMessage(JSONObject jsonMessage) {
        boolean hasErrorMessage = false;
        if(jsonMessage.has("error") && !jsonMessage.isNull("error")){
            JSONObject errorMessage = jsonMessage.getJSONObject("error");
            log.info(String.valueOf(errorMessage));

            //TODO: Обрабатывать возможные виды ошибок
            // -32602: Ошибка в токене mentions; 0xffff8044 - ошибка парсинга json и т.д...
//            String errorCode = String.valueOf(errorMessage.getJSONObject("code"));
//            if(HexFormat.fromHexDigit(errorCode) != 0xffff8044){
//                log.error("Unknown error, code: {}; full message: {}", errorCode, errorMessage);
//            } else {
//                log.info("No transactions");
//            }
            hasErrorMessage = true;
        }

        return hasErrorMessage;
    }
}

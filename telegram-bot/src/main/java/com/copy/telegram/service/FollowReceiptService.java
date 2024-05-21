package com.copy.telegram.service;

import com.copy.common.dto.FollowReceiptTaskDto;
import com.copy.common.dto.TransactionDto;
import com.copy.common.entity.AuthEntity;
import com.copy.common.entity.FollowEntity;
import com.copy.common.entity.SubscriptionEntity;
import com.copy.common.repository.FollowRepository;
import com.copy.common.util.TokenMetaData;
import com.copy.telegram.controller.TelegramBot;
import com.copy.telegram.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.copy.common.dto.TransactionDto.TokenBalanceDto;
import static com.copy.telegram.utils.Commands.BACK_ALL_FOLLOW;
import static com.copy.telegram.utils.Commands.SHOW_;
import static com.copy.telegram.utils.MessageUtils.computeAndDelete;

@Service
@Slf4j
@RequiredArgsConstructor
public class FollowReceiptService {

    @Value("${rpc.web.client}")
    private String PRIVATE_RPC_CLIENT;

    private final TelegramBot telegramBot;
    private final FollowRepository followRepository;

    public void sendReceiptOnFollowFunction(FollowReceiptTaskDto dto) {
        SendMessage message = new SendMessage();
        try {
            telegramBot.sendDeleteMessage(computeAndDelete(dto.getFollow().getAuthEntity().getChatId()));
            message.setText(generateFollowMessage(dto));
            setSendMessageAttributes(dto.getFollow(), message);

            telegramBot.sendResponseMessage(message);
        } catch (Exception e) {
            log.error("Exception while send receipt to authId: {}, followId: {}, error: {}",
                dto.getFollow().getAuthEntity().getAuthId(), dto.getFollow().getFollowId(), e.getMessage());
        }
    }

    private String generateFollowMessage(FollowReceiptTaskDto dto) {
        FollowEntity follow = dto.getFollow();
        AuthEntity auth = follow.getAuthEntity();
        SubscriptionEntity subscriptionEntity = auth.getSubscriptionEntity();
        List<FollowEntity> followEntities = followRepository.findByAuthEntity(auth);
        int totalCollectionsDone = followEntities.stream().mapToInt(FollowEntity::getCountCollDone).sum();
        int totalAutotradesDone = followEntities.stream().mapToInt(FollowEntity::getCountAutotradeDone).sum();

        StringBuilder message = new StringBuilder();

        message.append("Статус следования за ключом ")
                .append(follow.getFollowKeyWallet())
                .append(":\n");

        if (dto.isStart()) {
            if(dto.getAnswer().containsKey(false)){
                message.append("❌ Следование не запущено.\n")
                        .append("Причина: ")
                        .append(dto.getAnswer().get(false))
                        .append("\n");;
            } else {
                message.append("✅ Следование успешно запущено.\n");
            }
        } else {
            message.append("❌ Следование остановлено.\n");
            if(dto.getAnswer().containsKey(false)){
                message.append("Причина: ")
                        .append(dto.getAnswer().get(false))
                        .append("\n");
            }
        }

        message.append("\nИнформация о следовании:\n");
        message.append("\uD83D\uDD11 Ключ следования: ")
                .append(follow.getNameOfWallet())
                .append("\n");

        int remainingCalls = subscriptionEntity.getCountCollAvailable() - totalCollectionsDone;
        message.append("❗️ Оставшиеся коллы: ")
                .append(remainingCalls)
                .append(" / ")
                .append(subscriptionEntity.getCountCollAvailable())
                .append("\n");

        int remainingAutotrades = subscriptionEntity.getCountAutotradeAvailable() - totalAutotradesDone;
        message.append("\uD83E\uDD16 Оставшиеся автотрейды: ")
                .append(remainingAutotrades)
                .append(" / ")
                .append(subscriptionEntity.getCountAutotradeAvailable())
                .append("\n\n");

        message.append("Спасибо за использование нашего сервиса!");

        return message.toString();
    }

    public void sendCollReceipt(TransactionDto dto){
        try {
            SendMessage message = generateCollResponseMessage(dto);
            setSendMessageAttributes(dto.getFollowEntity(), message);
            telegramBot.sendResponseMessage(message);
        } catch (Exception e) {
            log.error("Exception while send coll to authId: {}, followId: {}, error: {}",
                 dto.getFollowEntity().getAuthEntity().getAuthId(), dto.getFollowEntity().getFollowId(), e.getMessage());
        }
    }

    private static void setSendMessageAttributes(FollowEntity follow, SendMessage message) {
        Long chatId = follow.getAuthEntity().getChatId();
        message.setChatId(chatId);

        String buttonCommandShowFollow = SHOW_.getShC() + follow.getFollowKeyWallet();
        String buttonTextShowFollow = SHOW_.getDesc() + follow.getNameOfWallet();
        List<InlineKeyboardButton> row = MessageUtils.getInlineTwoButtons(BACK_ALL_FOLLOW.getDesc(), BACK_ALL_FOLLOW.getShC(),
                                                                          buttonTextShowFollow, buttonCommandShowFollow);
        message.setReplyMarkup(InlineKeyboardMarkup.builder().keyboardRow(row).build());
    }

    private SendMessage generateCollResponseMessage(TransactionDto dto) {
        SendMessage message = new SendMessage();
        message.enableMarkdown(true);

        Set<String> uniqueMints = dto.getPostTokenBalances().stream()
                .map(TokenBalanceDto::getMint)
                .collect(Collectors.toSet());

        Map<String, TokenMetaData> tokenNames = getTokenNames(uniqueMints);

        String messageText = """
            *Transaction Summary*\n
            📝 [View on Explorer](https://explorer.solana.com/tx/%s)\n
            📝 [View on Solscan](https://solscan.io/tx/%s)\n
            💸 Fee: `%s`\n
            *🪙 Tokens Involved:*\n
            """.formatted(dto.getSignature(), dto.getSignature(), dto.getFee());

        messageText += dto.getPostTokenBalances().stream()
                .map(TokenBalanceDto::getMint)
                .distinct()
                .map(mint -> {
                                TokenMetaData token = tokenNames.getOrDefault(mint, null);
                                String symbol = token == null ? mint : token.getSymbol();
                                return "[%s](https://solscan.io/token/%s): `%s` -> `%s`\n".formatted(
                                        symbol, mint,
                                        getTokenBalance(dto.getPreTokenBalances(), mint),
                                        getTokenBalance(dto.getPostTokenBalances(), mint));
                            }
                )
                .collect(Collectors.joining());

        message.setText(messageText);
        return message;
    }

    private Map<String, TokenMetaData> getTokenNames(Set<String> mints) {
        OkHttpClient client = new OkHttpClient();
        Map<String, TokenMetaData> tokenNames = new HashMap<>();

        for (String mint : mints) {
            String requestBody = String.format(
                    "{\"jsonrpc\":\"2.0\",\"id\":\"text\",\"method\":\"getAsset\",\"params\":{\"id\":\"%s\"}}",
                    mint
            );

            Request request = new Request.Builder()
                    .url(PRIVATE_RPC_CLIENT)
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    JSONObject jsonObject = new JSONObject(json).getJSONObject("result")
                                                                .getJSONObject("content")
                                                                .getJSONObject("metadata");
                    String name = jsonObject.get("name").toString();
                    String symbol = jsonObject.get("symbol").toString();
                    tokenNames.put(mint, TokenMetaData.builder().mint(mint).name(name).symbol(symbol).build());
                }
            } catch (IOException | JSONException e) {
                log.error("Error while get token names by mint< exception: {}", e.getMessage());
            }
        }

        return tokenNames;
    }

    private String getTokenBalance(List<TokenBalanceDto> balances, String mint) {
        return balances.stream()
                .filter(balance -> balance.getMint().equals(mint))
                .map(balance -> balance.getUiTokenAmount() == null ? "0" : String.valueOf(balance.getUiTokenAmount()))
                .findFirst()
                .orElse("0");
    }
}



package com.copy.trader.service;

import com.copy.common.dto.TransactionDto;
import com.copy.common.entity.FollowEntity;
import com.copy.common.repository.FollowRepository;
import com.copy.trader.producer.CollsProducer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.p2p.solanaj.rpc.RpcApi;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.RpcException;
import org.p2p.solanaj.rpc.types.ConfirmedTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

import static com.copy.common.dto.TransactionDto.TokenBalanceDto;

@Service
@Slf4j
public class TradeService {

    @Value("${rpc.web.client}")
    private String PRIVATE_RPC_CLIENT;
    @Value("${solana.value}")
    private String SOL_VALUE;
    @Value("${rpc.web.timeout.seconds}")
    private int TIMEOUT_IN_SECONDS;
    @Value("${rpc.web.retries}")
    private int MAX_RETRIES;

    @Autowired
    private TrackingSessionService trackingSessionService;
    @Autowired
    private CollsProducer collsProducer;
    @Autowired
    private FollowRepository followRepository;

    private RpcClient rpcClient;
    private RpcApi rpcApi;

    @PostConstruct
    public void setTradeService() {
        rpcClient = new RpcClient(PRIVATE_RPC_CLIENT);
        rpcApi = rpcClient.getApi();
    }


//    public void startTradeProcedure(String signature, String specialKey) {
//        ConfirmedTransaction transaction;
//        try {
//            transaction = rpcApi.getTransaction(signature);
//            if (transaction != null && String.valueOf(new JSONObject(transaction)).contains(SOL_VALUE)) {
//                log.info("Start handle transaction for user:follow {}, signature: {}", specialKey, signature);
//                handleTransaction(transaction, specialKey);
//            } else {
//                log.info("Null Transaction for signature: {}", signature);
//            }
//        } catch (RpcException e) {
//            log.error("RpcException: {}", e.getMessage());
//            handleRpcException(signature, 0, rpcApi, specialKey);
//        } catch (Throwable e) {
//            log.error("UnknownException: {}", e.getMessage());
//        }
//    }
//
//    private void handleRpcException(String signature, int retries, RpcApi rpcApi, String specialKey) {
//        if (retries >= MAX_RETRIES) {
//            log.error("Max count of retries ({}) was exceeded for signature: {}", retries, signature);
//            return;
//        }
//        ConfirmedTransaction transaction;
//        try {
//            transaction = rpcApi.getTransaction(signature, Commitment.CONFIRMED);
//            if (transaction != null && String.valueOf(new JSONObject(transaction)).contains(SOL_VALUE)) {
//                handleTransaction(transaction, specialKey);
//            } else {
//                log.info("Null Transaction for signature: {}", signature);
//            }
//        } catch (RpcException e) {
//            retries++;
//            log.error("Retry attempt {} failed: {}", retries, e.getMessage());
//            try {
//                Thread.sleep(250);
//            } catch (InterruptedException ex) {
//                log.error("Thread interrupted: {}", ex.getMessage());
//                Thread.currentThread().interrupt();
//            }
//            handleRpcException(signature, retries, rpcApi, specialKey);
//        }
//    }


    public void startTradeProcedure(String signature, String specialKey) {
        ConfirmedTransaction transaction;
        int rpcRetryCount = 0;
        int transactionRetryCount = 0;

        while (true) {
            try {
                transaction = rpcApi.getTransaction(signature);
                if (transaction != null && String.valueOf(new JSONObject(transaction)).contains(SOL_VALUE)) {
                    log.info("Start handle transaction for user:follow {}, signature: {}", specialKey, signature);
                    handleTransaction(transaction, specialKey);
                    break;
                } else {
                    log.info("Null transaction for user:follow {}; Signature: {}. Retrying... (Attempt: {})",
                                                            specialKey, signature, transactionRetryCount + 1);
                    transactionRetryCount++;
                    if (transactionRetryCount >= MAX_RETRIES) {
                        log.info("Max retries reached for user:follow {}. Signature: {}", specialKey, signature);
                        break;
                    }
                    Thread.sleep(1000);
                }
            } catch (RpcException e) {
                log.error("RpcException for user:follow {}, exception: {}. Retrying... (Attempt: {})",
                                                        specialKey, e.getMessage(), rpcRetryCount + 1);
                rpcRetryCount++;
                if (rpcRetryCount >= 3) {
                    log.error("Max retries reached for RpcException. Signature: {}", signature);
                    break;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    log.error("InterruptedException occurred during retry delay: {}", ex.getMessage());
                }
            } catch (Throwable e) {
                log.error("UnknownException: {}", e.getMessage());
                break;
            }
        }
    }

    private void handleTransaction(ConfirmedTransaction transaction, String specialKey) {
        FollowEntity follow = trackingSessionService.getTracker(specialKey).getFollowEntity();
        int accountIndex = transaction.getTransaction().getMessage().getAccountKeys().indexOf(follow.getFollowKeyWallet());
        if (accountIndex != -1) {
            upCollsCout(follow, specialKey);
            collsProducer.produceReceipt(createTransactionDto(transaction, follow));
        }
    }

    private void upCollsCout(FollowEntity follow, String specialKey) {
        follow.setCountCollDone(follow.getCountCollDone() + 1);
        followRepository.save(follow);

        trackingSessionService.getTracker(specialKey).getFollowEntity().setCountCollDone(follow.getCountCollDone());
    }

    private TransactionDto createTransactionDto(ConfirmedTransaction transaction, FollowEntity follow) {
        List<TokenBalanceDto> preTokenBalances = getBalanceDtos(transaction.getMeta().getPreTokenBalances());

        List<TokenBalanceDto> postTokenBalances = getBalanceDtos(transaction.getMeta().getPostTokenBalances());

        return TransactionDto.builder()
                .signature(transaction.getTransaction().getSignatures().get(0))
                .fee(transaction.getMeta().getFee())
                .slot(transaction.getSlot())
                .status(transaction.getMeta().getStatus().getOk() != null ? "Success" : "Failed")
                .preBalances(transaction.getMeta().getPreBalances())
                .postBalances(transaction.getMeta().getPostBalances())
                .preTokenBalances(preTokenBalances)
                .postTokenBalances(postTokenBalances)
                .followEntity(follow)
                .build();
    }

    private static @NotNull List<TokenBalanceDto> getBalanceDtos(List<ConfirmedTransaction.TokenBalance> transaction) {
        return transaction.stream()
                .map(balance -> TokenBalanceDto.builder()
                        .accountIndex(balance.getAccountIndex())
                        .mint(balance.getMint())
                        .uiTokenAmount(balance.getUiTokenAmount().getUiAmount())
                        .build())
                .collect(Collectors.toList());
    }

}

package com.copy.trader.service;

import lombok.extern.slf4j.Slf4j;
import org.p2p.solanaj.rpc.RpcApi;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.RpcException;
import org.p2p.solanaj.rpc.types.ConfirmedTransaction;
import org.p2p.solanaj.rpc.types.config.Commitment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TradeService {

    @Value("${rpc.web.client}")
    private String PRIVATE_RPC_CLIENT;
    @Value("${solana.value}")
    private String SOL_VALUE;
    @Value("${rpc.web.timeout.seconds}")
    private int TIMEOUT_IN_SECONDS;

//    @Value("${solana.person}")
//    private String person;

    private RpcClient rpcClient;
    private RpcApi rpcApi;

    public void setTradeService() {
        rpcClient = new RpcClient(PRIVATE_RPC_CLIENT);
        rpcApi = rpcClient.getApi();
    }

    private static final int MAX_RETRIES = 9;

    public void startTradeProcedure(String signature, int slot) {
        ConfirmedTransaction transaction;
        try {
            transaction = rpcApi.getTransaction(signature, Commitment.CONFIRMED);
            if(transaction != null && transaction.getTransaction().getMessage().getAccountKeys().contains(SOL_VALUE)) {
                handleTransaction(transaction);
            }else{
                log.info("Null Transaction for signature: {}", signature);
            }
        } catch (RpcException e) {
            log.error("RpcException: {}", e.getMessage());
            handleRpcException(signature, 0, rpcApi);
        } catch (Throwable e) {
            log.error("UnknownException: {}", e.getMessage());
        }
    }

    private void handleRpcException(String signature, int retries, RpcApi rpcApi) {
        if (retries >= MAX_RETRIES) {
            log.error("Max count of retries ({}) was exceeded for signature: {}", retries, signature);
            return;
        }
        ConfirmedTransaction transaction;
        try {
            transaction = rpcApi.getTransaction(signature, Commitment.CONFIRMED);
            if(transaction != null) {
                handleTransaction(transaction);
            }else{
                log.info("Null Transaction for signature: {}", signature);
            }
        } catch (RpcException e) {
            retries++;
            log.error("Retry attempt {} failed: {}", retries, e.getMessage());
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) {
                log.error("Thread interrupted: {}", ex.getMessage());
                Thread.currentThread().interrupt();
            }
            handleRpcException(signature, retries, rpcApi);
        }
    }

    private void handleTransaction(ConfirmedTransaction transaction) {

        int accountIndex = transaction.getTransaction().getMessage().getAccountKeys().indexOf("person");
        if(accountIndex != -1) {
            long solPreValue = transaction.getMeta().getPreBalances().get(accountIndex);
            long solPostValue = transaction.getMeta().getPostBalances().get(accountIndex);
            long fee = transaction.getMeta().getFee();
            long lamports = solPostValue - solPreValue + fee;
            log.info("Transaction lamports is: {}", lamports);
        }
    }

}

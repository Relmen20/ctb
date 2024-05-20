package com.copy.common.dto;

import com.copy.common.entity.FollowEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {
    private long slot;
    private long fee;
    private String status;
    private List<Long> preBalances;
    private List<Long> postBalances;
    private List<TokenBalanceDto> preTokenBalances;
    private List<TokenBalanceDto> postTokenBalances;
    private String signature;
    private FollowEntity followEntity;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TokenBalanceDto {
        private double accountIndex;
        private String mint;
        private Double uiTokenAmount;
    }

}

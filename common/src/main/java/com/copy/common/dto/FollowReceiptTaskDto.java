package com.copy.common.dto;

import com.copy.common.entity.FollowEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FollowReceiptTaskDto {
    private FollowEntity follow;
    private Map<Boolean, String> answer;
    private boolean isStart;
}
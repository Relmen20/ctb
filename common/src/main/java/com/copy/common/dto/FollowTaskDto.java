package com.copy.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FollowTaskDto {

    private Long authId;
    private String followAddress;
    private boolean isStart;
}

package com.copy.common.dto;

import com.copy.common.entity.FollowEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FollowTaskDto {

    private FollowEntity follow;
    private boolean isStart;
}

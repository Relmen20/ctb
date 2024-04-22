package com.copy.common.dto;

import lombok.Data;

@Data
public class FollowTaskDto {

    private int authId;
    private String followAddress;
    private boolean isFollow;

}

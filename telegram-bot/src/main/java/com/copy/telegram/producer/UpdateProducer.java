package com.copy.telegram.producer;

import com.copy.common.dto.FollowTaskDto;

public interface UpdateProducer {

    void produce(String routingKey, FollowTaskDto dto);
}

package com.copy.telegram.producer;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface UpdateProducer {

    void produce(Update update);
}

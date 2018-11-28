package com.propy.service.bctransaction.messaging.streams;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface TransactionStreams {

    String OUTPUT = "transaction-broadcast";

    @Output(OUTPUT)
    MessageChannel broadcastTransaction();

}

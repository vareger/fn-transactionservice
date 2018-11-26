package com.propy.service.bctransaction.streams;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface SendTransactionStreams {

    String INPUT = "send-transaction-in";
    String OUTPUT = "send-transaction-out";

    @Input(INPUT)
    SubscribableChannel inputSendTransaction();

    @Output(OUTPUT)
    MessageChannel outputTransactionHash();

}

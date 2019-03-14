package com.postnode.service.bctransaction.messaging.streams;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface SendTransactionStreams {

    String INPUT = "send-transaction-in";
    String OUTPUT = "send-transaction-out";
    String INPUT_HASH = "transaction-in-hash";

    @Input(INPUT)
    SubscribableChannel inputSendTransaction();

    @Input(INPUT_HASH)
    SubscribableChannel inputHashTransaction();

    @Output(OUTPUT)
    MessageChannel outputTransactionHash();

}

package com.postnode.service.bctransaction.messaging.streams;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface ContractCallStreams {

    String INPUT = "transaction-contract-read-in";
    String OUTPUT = "transaction-contract-read-out";

    @Input(INPUT)
    SubscribableChannel readContract();

    @Output(OUTPUT)
    MessageChannel sendContractResult();

}

package com.postnode.service.bctransaction.listeners;

import com.postnode.service.bctransaction.messaging.models.SendTransaction;
import com.postnode.service.bctransaction.messaging.streams.SendTransactionStreams;
import com.postnode.service.bctransaction.processors.TransactionSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@EnableBinding(SendTransactionStreams.class)
@RequiredArgsConstructor
public class TransactionMessageListener {

    private final TransactionSender sender;

    @StreamListener(SendTransactionStreams.INPUT)
    @SendTo(SendTransactionStreams.OUTPUT)
    public String sendTransactionMessage(
            @Payload SendTransaction transaction,
            @Header(required = false, value = SendTransaction.PRIVATE_KEY_HEADER) String privateKey
    ) {
        log.info("Received send transaction with sender: {}", transaction.getSender());
        return sender.sendTransaction(transaction, privateKey);
    }

    @StreamListener(SendTransactionStreams.INPUT_HASH)
    public void listenTransaction(@Payload String transactionHash) {
        sender.setWaitTransaction(transactionHash);
    }

}

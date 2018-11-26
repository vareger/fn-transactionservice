package com.propy.service.bctransaction.listeners;

import com.propy.service.bctransaction.entities.SendTransaction;
import com.propy.service.bctransaction.processors.TransactionSender;
import com.propy.service.bctransaction.streams.SendTransactionStreams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
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
    public String sendTransactionMessage(@Payload SendTransaction transaction) {
        log.info("Received send transaction with sender: {}", transaction.getSender());
        return sender.sendTransaction(transaction);
    }

}

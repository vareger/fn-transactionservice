package com.propy.service.bctransaction.listeners;

import com.propy.service.bctransaction.entities.SendTransaction;
import com.propy.service.bctransaction.processors.TransactionSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Service;

@RefreshScope
@Service
public class TransactionMessageListener {

    private final TransactionSender sender;

    @Autowired
    public TransactionMessageListener(TransactionSender sender) {
        this.sender = sender;
    }

    @KafkaListener(topics = "${kafka.topic.request-topic}")
    @SendTo
    public String sendTransactionMessage(@Payload SendTransaction transaction) {
        return sender.sendTransaction(transaction);
    }

}

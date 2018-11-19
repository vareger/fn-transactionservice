package com.propy.service.bctransaction.producers;

import com.propy.service.bctransaction.entities.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;

@Slf4j
@Component
public class KafkaMQ implements SuccessCallback<SendResult<String, Transaction>>, FailureCallback {

    private final KafkaTemplate<String, Transaction> kafka;

    @Autowired
    public KafkaMQ(KafkaTemplate<String, Transaction> kafka) {
        this.kafka = kafka;
    }

    public void publish(Transaction transaction) {
        log.info("Publish transaction {}", transaction.getTransactionHash());
        this.kafka.sendDefault(transaction.getTransactionHash(), transaction).addCallback(this, this);
    }

    @Override
    public void onFailure(Throwable throwable) {
        log.error("Error sending message", throwable);
    }

    @Override
    public void onSuccess(SendResult<String, Transaction> message) {
        Transaction transaction = message.getProducerRecord().value();
        log.info("Published {} transaction.", transaction.getTransactionHash());
    }
}

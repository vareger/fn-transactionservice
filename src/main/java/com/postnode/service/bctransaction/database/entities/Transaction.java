package com.postnode.service.bctransaction.database.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction implements Cloneable {

    @Id
    private String transactionHash;
    private Boolean mined = false;
    private TransactionStatus status = TransactionStatus.PENDING;
    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, String> keys;

    public Transaction(String txHash) {
        this.transactionHash = txHash;
    }

    public Transaction(String transactionHash, Map<String, String> keys) {
        this(transactionHash);
        this.keys = keys != null ? new HashMap<>(keys) : Collections.emptyMap();
    }

    @Override
    public Transaction clone() {
        return new Transaction(
                transactionHash,
                mined,
                status,
                keys
        );
    }

    public enum TransactionStatus {
        PENDING,
        FAIL,
        SUCCESS,
        DEPLOYED
    }

}

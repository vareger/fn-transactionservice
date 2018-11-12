package com.propy.service.bctransaction.entities;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import java.util.HashMap;
import java.util.Map;

@Entity
public class Transaction implements Cloneable {

    @Id
    private String transactionHash;
    private Boolean mined;
    private TransactionStatus status;
    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, String> keys;

    public Transaction() {
        this.keys = new HashMap<>();
    }

    public Transaction(String transactionHash, Map<String, String> keys) {
        this(transactionHash);
        this.keys = new HashMap<>(keys);
    }

    public Transaction(String transactionHash) {
        this();
        this.transactionHash = transactionHash;
        this.mined = false;
        this.status = TransactionStatus.PENDING;
    }

    public Transaction(String transactionHash, Boolean mined, TransactionStatus status, Map<String, String> keys) {
        this.transactionHash = transactionHash;
        this.mined = mined;
        this.status = status;
        this.keys = new HashMap<>(keys);
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public Boolean getMined() {
        return mined;
    }

    public void setMined(Boolean mined) {
        this.mined = mined;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Map<String, String> getKeys() {
        return keys;
    }

    public void setKey(Map<String, String> keys) {
        this.keys = new HashMap<>(keys);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionHash='" + transactionHash + '\'' +
                ", mined=" + mined +
                ", status=" + status +
                ", keys=" + keys +
                '}';
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
        SUCCESS
    }

}

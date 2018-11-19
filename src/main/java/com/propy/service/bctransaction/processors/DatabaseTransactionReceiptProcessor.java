package com.propy.service.bctransaction.processors;

import com.propy.service.bctransaction.coordinator.ZooKeeperCoordinator;
import com.propy.service.bctransaction.entities.Transaction;
import com.propy.service.bctransaction.entities.Transaction.TransactionStatus;
import com.propy.service.bctransaction.producers.KafkaMQ;
import com.propy.service.bctransaction.repositories.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.response.EmptyTransactionReceipt;
import org.web3j.tx.response.TransactionReceiptProcessor;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class DatabaseTransactionReceiptProcessor extends TransactionReceiptProcessor {

    private final TransactionRepository transactions;

    private final Web3j web3j;

    private final KafkaMQ kafkaMQ;

    private ZooKeeperCoordinator coordinator;

    @Autowired
    public DatabaseTransactionReceiptProcessor(
            TransactionRepository transactions,
            Web3j web3j,
            KafkaMQ kafkaMQ,
            ZooKeeperCoordinator coordinator
    ) {
        super(null);
        this.transactions = transactions;
        this.web3j = web3j;
        this.kafkaMQ = kafkaMQ;
        this.coordinator = coordinator;
    }

    /**
     * Add transaction to listener's database of Transaction receipt and send receipt to callback immediately
     *
     * @param receipt  Object of the mined transaction
     */
    synchronized public void addTransaction(TransactionReceipt receipt) {
        this.transactions.save(new Transaction(receipt.getTransactionHash()));
        this.updateTransactionInfo(receipt);
    }

    /**
     * Add transaction to listener's database of Transaction receipt
     *
     * @param txHash   Hash of the sent transaction
     * @param keys Keys for filter transaction
     */
    synchronized public void addTransaction(String txHash, Map<String, String> keys) {
        this.transactions.save(new Transaction(txHash, keys));
    }

    /**
     * Add transaction to listener's database of Transaction receipt and send receipt to callback immediately
     *
     * @param receipt  Object of the mined transaction
     * @param keys Keys for filter transaction
     */
    synchronized public void addTransaction(TransactionReceipt receipt, Map<String, String> keys) {
        this.transactions.save(new Transaction(receipt.getTransactionHash(), keys));
        this.updateTransactionInfo(receipt);
    }

    /**
     * Add transaction to listener's database of Transaction receipt
     *
     * @param txHash Hash of the sent transaction
     */
    synchronized public void addTransaction(String txHash) {
        this.transactions.save(new Transaction(txHash));
    }

    /**
     * Remove transaction from listener
     *
     * @param txHash Hash of the sent transaction
     * @return true if successful removed
     */
    synchronized public boolean removeTransaction(String txHash) {
        if (this.transactions.existsById(txHash)) {
            this.transactions.deleteById(txHash);
            return true;
        }
        return false;
    }

    /**
     * Change transaction hash to new one or add if doesn't exist (for example reject)
     *
     * @param oldHash
     * @param newHash
     */
    synchronized public void changeTransactionHash(String oldHash, String newHash) {
        if (this.transactions.existsById(oldHash) && !this.transactions.existsById(newHash)) {
            Transaction transaction = this.transactions.findById(oldHash).get();
            this.transactions.deleteById(oldHash);
            transaction.setTransactionHash(newHash);
            this.transactions.save(transaction);
        } else {
            this.addTransaction(newHash);
        }
    }

    @Override
    public TransactionReceipt waitForTransactionReceipt(String transactionHash)
            throws IOException, TransactionException {
        return new EmptyTransactionReceipt(transactionHash);
    }

    @Scheduled(fixedDelay = 1000)
    public void update() {
        ZooKeeperCoordinator.Lock lock = this.coordinator.obtainLock();
        log.debug("Checking for transactions");
        transactions.findAllByStatus(TransactionStatus.PENDING).stream()
                .map(this::getTransactionReceipt)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(this::isTransactionMined)
                .forEach(this::updateTransactionInfo);
        lock.release();
    }

    synchronized private void updateTransactionInfo(TransactionReceipt receipt) {
        Optional<Transaction> dbtransaction = this.transactions.findById(receipt.getTransactionHash());
        Transaction transaction = dbtransaction.orElseThrow(() -> new IllegalStateException("Transaction object does not exists!"));
        transaction.setMined(true);
        transaction.setStatus(receipt.isStatusOK() ? TransactionStatus.SUCCESS : TransactionStatus.FAIL);
        this.transactions.save(transaction);
        log.info("Transaction: {} was set to state: {}", transaction.getTransactionHash(), transaction.getStatus().name());
        this.kafkaMQ.publish(transaction);
    }

    private Optional<TransactionReceipt> sendTransactionReceiptRequest(
            String transactionHash) throws IOException, TransactionException {
        EthGetTransactionReceipt transactionReceipt =
                web3j.ethGetTransactionReceipt(transactionHash).send();
        if (transactionReceipt.hasError()) {
            throw new TransactionException("Error processing request: "
                    + transactionReceipt.getError().getMessage());
        }

        return transactionReceipt.getTransactionReceipt();
    }

    private Optional<TransactionReceipt> getTransactionReceipt(Transaction transaction) {
        try {
            return this.sendTransactionReceiptRequest(transaction.getTransactionHash());
        } catch (IOException | TransactionException e) {
            log.error("Transaction receipt exception: {}", e);
            return Optional.empty();
        }
    }

    private boolean isTransactionMined(TransactionReceipt transactionReceipt) {
        return transactionReceipt.getBlockNumberRaw() != null;
    }

}

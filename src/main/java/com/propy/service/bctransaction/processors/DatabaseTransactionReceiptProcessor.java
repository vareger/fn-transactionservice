package com.propy.service.bctransaction.processors;

import com.propy.service.bctransaction.database.entities.Transaction;
import com.propy.service.bctransaction.database.entities.Transaction.TransactionStatus;
import com.propy.service.bctransaction.database.repositories.TransactionRepository;
import com.propy.service.bctransaction.messaging.models.TransactionInfo;
import com.propy.service.bctransaction.messaging.streams.TransactionStreams;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.Locker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.response.EmptyTransactionReceipt;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@EnableBinding(TransactionStreams.class)
public class DatabaseTransactionReceiptProcessor extends TransactionReceiptProcessor {

    private static final String ZNODE_LOCK_ROOT = "/lock_root";

    private final TransactionRepository transactions;

    private final Web3j web3j;

    private final TransactionStreams transactionStreams;

    private CuratorFramework framework;

    @Autowired
    public DatabaseTransactionReceiptProcessor(
            TransactionRepository transactions,
            Web3j web3j,
            TransactionStreams streams,
            CuratorFramework framework
    ) {
        super(null);
        this.transactions = transactions;
        this.web3j = web3j;
        this.transactionStreams = streams;
        this.framework = framework;
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
        InterProcessMutex mutex = new InterProcessMutex(this.framework, ZNODE_LOCK_ROOT);
        try (Locker ignored = new Locker(mutex)) {
            log.debug("Checking for transactions");
            transactions.findAllByStatus(TransactionStatus.PENDING).stream()
                    .map(this::getTransactionReceipt)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(this::isTransactionMined)
                    .forEach(this::updateTransactionInfo);
        } catch (Exception e) {
            log.error("Locking error", e);
        }
    }

    synchronized private void updateTransactionInfo(TransactionReceipt receipt) {
        Optional<Transaction> dbtransaction = this.transactions.findById(receipt.getTransactionHash());
        Transaction transaction = dbtransaction.orElseThrow(() -> new IllegalStateException("Transaction object does not exists!"));
        transaction.setMined(true);
        transaction.setStatus(receipt.isStatusOK() ?
                receipt.getContractAddress() != null ? TransactionStatus.DEPLOYED : TransactionStatus.SUCCESS :
                TransactionStatus.FAIL);
        this.transactions.save(transaction);
        log.info("Transaction: {} was set to state: {}", transaction.getTransactionHash(), transaction.getStatus().name());
        this.sendTransactionMessage(receipt, transaction);
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

    private void sendTransactionMessage(TransactionReceipt receipt, Transaction transaction) {
        String address = transaction.getStatus() == TransactionStatus.DEPLOYED ? receipt.getContractAddress() : receipt.getTo();
        TransactionInfo transactionInfo = TransactionInfo.builder()
                .transactionHash(Numeric.hexStringToByteArray(receipt.getTransactionHash()))
                .from(Numeric.hexStringToByteArray(receipt.getFrom()))
                .status(transaction.getStatus())
                .to(Numeric.hexStringToByteArray(address))
                .events(receipt.getLogs().stream().map(TransactionInfo.Event::new).collect(Collectors.toList()))
                .tags(transaction.getKeys())
                .build();
        MessageChannel messageChannel = this.transactionStreams.broadcastTransaction();
        messageChannel.send(
                MessageBuilder
                        .withPayload(transactionInfo)
                        .build()
        );
    }

}

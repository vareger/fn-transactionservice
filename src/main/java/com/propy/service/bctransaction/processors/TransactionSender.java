package com.propy.service.bctransaction.processors;

import com.propy.service.bctransaction.beans.Web3jBeans;
import com.propy.service.bctransaction.configs.EthereumProperties;
import com.propy.service.bctransaction.entities.SendTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.ChainId;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@Slf4j
@RefreshScope
@Component
public class TransactionSender {

    private static final Map<String, BigInteger> Nonces = Collections.synchronizedMap(new HashMap<>());

    private final Web3j web3j;

    private final EthereumProperties properties;

    private final DatabaseTransactionReceiptProcessor processor;

    @Autowired
    public TransactionSender(Web3j web3j, EthereumProperties properties, DatabaseTransactionReceiptProcessor processor) {
        this.web3j = web3j;
        this.properties = properties;
        this.processor = processor;
    }

    public String sendTransaction(SendTransaction sendTransaction) {
        synchronized (Nonces) {
            int tries = 3;
            do {
                try {
                    Credentials credentials =
                            this.findCredentials(sendTransaction.getSender()).orElseThrow(() -> new IllegalArgumentException("Sender\'s private key not found"));
                    this.updateNonce(sendTransaction.getSender());
                    EthSendTransaction ethSendTransaction = this.sendTransaction(
                            credentials,
                            Convert.toWei(BigDecimal.TEN, Convert.Unit.GWEI).toBigInteger(),
                            BigInteger.valueOf(4_000_000L),
                            sendTransaction.getReceiver(),
                            Numeric.toHexString(sendTransaction.getData()),
                            sendTransaction.getValue(),
                            this.getNonce(sendTransaction.getSender())
                    );
                    log.info("Transaction is sent by {}. Hash: {}.",
                            sendTransaction.getSender(),
                            ethSendTransaction.getTransactionHash());
                    if (ethSendTransaction.hasError()) {
                        this.setPreviousNonce(
                                sendTransaction.getSender(),
                                this.previousNonce(sendTransaction.getSender()).subtract(BigInteger.ONE)
                        );
                    }
                    String transactionHash = ethSendTransaction.getTransactionHash();
                    processor.addTransaction(transactionHash, sendTransaction.getTags());
                    return transactionHash;
                } catch (Throwable e) {
                    log.error("Transaction sending error", e);
                    if (--tries > 0)
                        continue;
                    this.setPreviousNonce(
                            sendTransaction.getSender(),
                            this.previousNonce(sendTransaction.getSender()).subtract(BigInteger.ONE)
                    );
                }
            } while (true);
        }
    }

    private BigInteger getNonce(String address) {
        return this.previousNonce(address);
    }

    private void updateNonce(String address) throws IOException {
        BigInteger remoteNonce = this.getNonceNode(address);
        if (this.previousNonce(address) == null || this.previousNonce(address).compareTo(remoteNonce) < 0) {
            this.setPreviousNonce(address, remoteNonce);
        } else {
            this.setPreviousNonce(address, this.previousNonce(address).add(BigInteger.ONE));
        }
    }

    private BigInteger getNonceNode(String address) throws IOException {
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                address, DefaultBlockParameterName.PENDING).send();

        return ethGetTransactionCount.getTransactionCount();
    }

    private void setPreviousNonce(String address, BigInteger nonce) {
        Nonces.put(address, nonce);
    }

    private BigInteger previousNonce(String address) {
        return Nonces.get(address);
    }

    private Optional<Credentials> findCredentials(String address) {
        if (properties.getWallets().getSystem().getAddress().equalsIgnoreCase(address)) {
            return Optional.of(Web3jBeans.initCredentials(properties.getWallets().getSystem().getPrivateKey()));
        } else {
            List<EthereumProperties.Wallet> wallets = properties.getWallets().getMultiSig();
            return wallets.stream()
                    .filter(w -> w.getAddress().equalsIgnoreCase(address))
                    .map(EthereumProperties.Wallet::getPrivateKey)
                    .map(Web3jBeans::initCredentials)
                    .findAny();
        }
    }

    private EthSendTransaction sendTransaction(
            Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String to,
            String data, BigInteger value, BigInteger nonce) throws IOException {

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                gasLimit,
                to,
                value,
                data);
        return signAndSend(rawTransaction, credentials);
    }

    private EthSendTransaction signAndSend(RawTransaction rawTransaction, Credentials credentials)
            throws IOException {

        byte[] signedMessage;

        if (properties.getChainId() > ChainId.NONE) {
            signedMessage = TransactionEncoder.signMessage(rawTransaction, properties.getChainId(), credentials);
        } else {
            signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        }

        String hexValue = Numeric.toHexString(signedMessage);

        return web3j.ethSendRawTransaction(hexValue).send();
    }

}
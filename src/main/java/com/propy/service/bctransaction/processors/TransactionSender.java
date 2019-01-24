package com.propy.service.bctransaction.processors;

import com.propy.service.bctransaction.beans.Web3jBeans;
import com.propy.service.bctransaction.configs.EthereumProperties;
import com.propy.service.bctransaction.messaging.models.SendTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.ChainId;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Slf4j
@RefreshScope
@Component
public class TransactionSender {

    private final Web3j web3j;

    private final EthereumProperties properties;

    private final DatabaseTransactionReceiptProcessor processor;

    private ZookeeperNonce nonce;

    @Autowired
    public TransactionSender(
            Web3j web3j,
            EthereumProperties properties,
            DatabaseTransactionReceiptProcessor processor,
            ZookeeperNonce nonce
    ) {
        this.web3j = web3j;
        this.properties = properties;
        this.processor = processor;
        this.nonce = nonce;
    }

    public String sendTransaction(SendTransaction sendTransaction, String privateKey) {
        int tries = 3;
        do {
            try {
                Credentials credentials =
                        this.findCredentials(sendTransaction.getSender(), privateKey).orElseThrow(() -> new IllegalArgumentException("Sender\'s private key not found or incorrect"));
                this.nonce.lock(credentials.getAddress());
                BigInteger nonce_v = BigInteger.valueOf(this.nonce.loadNonce());
                BigInteger gasLimit = this.getEstimatedGasLimit(
                        new Transaction(credentials.getAddress(), BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, sendTransaction.getReceiver(), sendTransaction.getValue(), Numeric.toHexString(sendTransaction.getData()))
                );
                EthSendTransaction ethSendTransaction = this.sendTransaction(
                        credentials,
                        Convert.toWei(BigDecimal.TEN, Convert.Unit.GWEI).toBigInteger(),
                        gasLimit,
                        sendTransaction.getReceiver(),
                        Numeric.toHexString(sendTransaction.getData()),
                        sendTransaction.getValue(),
                        nonce_v
                );
                log.info("Transaction is sent by {}. Hash: {}, Nonce: {}.",
                        credentials.getAddress(),
                        ethSendTransaction.getTransactionHash(),
                        nonce_v.toString());
                if (ethSendTransaction.hasError()) {
                    log.error("Sending error: {}", ethSendTransaction.getError().getMessage());
                    this.nonce.unlock(false);
                    break;
                }
                String transactionHash = ethSendTransaction.getTransactionHash();
                processor.addTransaction(transactionHash, sendTransaction.getTags());
                this.nonce.unlock(true);
                return transactionHash;
            } catch (Throwable e) {
                log.error("Transaction sending error", e);
                this.nonce.unlock(false);
                if (--tries > 0)
                    continue;
                break;
            }
        } while (true);
        return "";
    }

    private Optional<Credentials> findCredentials(String address, String privateKey) {
        if (privateKey != null) {
            Credentials credentials = Web3jBeans.initCredentials(privateKey);
            return credentials.getAddress().equalsIgnoreCase(address) ?
                    Optional.of(credentials) : Optional.empty();
        }
        else if (address == null || address.isEmpty()) {
            return Optional.of(Web3jBeans.initCredentials(properties.getWallets().getSystem().getPrivateKey()));
        }
        else if (properties.getWallets().getSystem().getAddress().equalsIgnoreCase(address)) {
            return Optional.of(Web3jBeans.initCredentials(properties.getWallets().getSystem().getPrivateKey()));
        }
        else {
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

    private BigInteger getEstimatedGasLimit(Transaction transaction) {
        final BigInteger defaultGasLimit = BigInteger.valueOf(4_000_000L);
        try {
            EthEstimateGas estimateGas1 = web3j.ethEstimateGas(transaction).send();
            EthEstimateGas estimateGas2 = web3j.ethEstimateGas(transaction).send();
            EthEstimateGas estimateGas3 = web3j.ethEstimateGas(transaction).send();
            log.debug("Using estimated gas limit value.");
            if (estimateGas1.hasError() && estimateGas2.hasError() && estimateGas3.hasError()) {
                log.error("Failed transaction: from:{}, to:{}, nonce:{}, data:{}, value: {}",
                        transaction.getFrom(),
                        transaction.getTo(),
                        transaction.getNonce(),
                        transaction.getData(),
                        transaction.getValue()
                );
                return defaultGasLimit;
            }
            BigInteger estimated = estimateGas1.getAmountUsed().max(estimateGas2.getAmountUsed());
            estimated = estimated.max(estimateGas3.getAmountUsed());
            return estimated.add(BigInteger.valueOf(21000));
        } catch (Exception e) {
            log.error("Gas limit not allowed, set to default value {}, {}", defaultGasLimit, e);
            return defaultGasLimit;
        }
    }

}

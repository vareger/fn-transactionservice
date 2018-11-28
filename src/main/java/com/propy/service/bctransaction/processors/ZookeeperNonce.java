package com.propy.service.bctransaction.processors;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicInteger;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;

import java.io.IOException;

@Slf4j
@Component
@RefreshScope
public class ZookeeperNonce {

    private static final String NONCE_ROOT = "/nonce";

    private Web3j web3j;
    private CuratorFramework curatorFramework;
    private InterProcessMutex mutex;
    private DistributedAtomicInteger nonceAtomic;

    private final String zNonceRoot;

    private String address;

    @Autowired
    public ZookeeperNonce(
            Web3j web3j,
            CuratorFramework curatorFramework
    ) {
        this.web3j = web3j;
        this.zNonceRoot = NONCE_ROOT;
        this.curatorFramework = curatorFramework;
    }

    void lock(String address) throws Exception {
        if (this.mutex != null && this.mutex.isAcquiredInThisProcess()) {
            throw new IllegalAccessException("Already locked by this process");
        }
        this.mutex = new InterProcessMutex(
                this.curatorFramework,
                this.zNoncePath(address)
        );
        this.mutex.acquire();
        this.nonceAtomic = new DistributedAtomicInteger(
                this.curatorFramework,
                this.zNoncePath(address),
                new ExponentialBackoffRetry(1000, 5)
        );
        this.address = address;
        this.nonceAtomic.initialize(0);
    }

    void unlock(boolean increment) {
        try {
            if (increment) {
                if (!this.nonceAtomic.increment().succeeded()) {
                    log.error("Something went wrong. Nonce value does not incremented");
                }
            }
            this.mutex.release();
        } catch (Exception e) {
            log.error("Zookeeper Nonce error", e);
        }
    }

    Integer loadNonce() throws Exception {
        Integer netNonce = this.loadNonceFromBlockchain(address);
        Integer savedNonce = this.nonceAtomic.get().postValue();
        if (netNonce > savedNonce) {
            if (!this.nonceAtomic.trySet(netNonce).succeeded()) {
                this.nonceAtomic.forceSet(netNonce);
            }
        }
        return this.nonceAtomic.get().postValue();
    }

    private String zNoncePath(String address) {
        return this.zNonceRoot + "/" + address;
    }

    private Integer loadNonceFromBlockchain(String address) throws IOException {
        return this.web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING)
                .send()
                .getTransactionCount()
                .intValue();
    }

}

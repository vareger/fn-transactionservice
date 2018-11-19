package com.propy.service.bctransaction.coordinator;

import com.propy.service.bctransaction.configs.ZookeeperProperties;
import com.propy.service.bctransaction.exceptions.CoordinatorException;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RefreshScope
@Slf4j
@Component
public class ZooKeeperCoordinator {

    private static final String ZNODE_LOCK_ROOT = "/lock_root";
    private static final String ZNODE_LOCK = "/lock-";

    private final String zNodeRoot;
    private final String zNodeLockRoot;
    private final String zNodeLock;

    private final ZookeeperProperties properties;

    private ZooKeeper zooKeeper;

    @Autowired
    public ZooKeeperCoordinator(ZookeeperProperties properties) {
        this.properties = properties;
        this.zNodeRoot = properties.getCoordinator().getZNodeRoot();
        this.zNodeLockRoot = this.zNodeRoot + ZNODE_LOCK_ROOT;
        this.zNodeLock = this.zNodeLockRoot + ZNODE_LOCK;
    }

    public Lock obtainLock() {
        log.debug("Obtaining lock...");
        String lockPath = null;
        try {
            lockPath = zooKeeper.create(this.zNodeLock, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            Thread.sleep(50);
            String thisNode = lockPath.substring((this.zNodeLockRoot + "/").length());
            waitForLock(thisNode);
            return new Lock(zooKeeper, lockPath);
        } catch (Throwable e) {
            if (lockPath != null) {
                try {
                    zooKeeper.delete(lockPath, -1);
                } catch (InterruptedException | KeeperException e1) {
                    log.info("Failed to delete lock", e1);
                }
            }
            throw new CoordinatorException(e);
        }
    }

    private void waitForLock(String thisNode) throws KeeperException, InterruptedException {
        log.debug("Checking lock state for: {}", thisNode);
        List<String> nodes = zooKeeper.getChildren(this.zNodeLockRoot, false);
        Collections.sort(nodes);
        log.debug("Nodes: {}", nodes);
        int index = nodes.indexOf(thisNode);
        if (index == 0) {
            log.debug("Lock obtained!");
        } else {
            String waitNode = this.zNodeLockRoot + "/" + nodes.get(index - 1);
            log.debug("Waiting on previous node: {}", waitNode);
            CountDownLatch latch = new CountDownLatch(1);
            Stat stat = zooKeeper.exists(waitNode, event -> {
                log.debug("Previous node event: {}", event);
                latch.countDown();
            });
            if (stat == null) {
                log.warn("Previous node not found, retrying check...");
                waitForLock(thisNode);
            } else {
                latch.await(10000, TimeUnit.MILLISECONDS);
                log.warn("Previous node changed or time elapsed, retrying check...");
                waitForLock(thisNode);
            }
        }
    }

    @PostConstruct
    public void start() {
        try {
            log.info("Connecting to Zookeeper at: {}", properties.getConnectString());
            final CountDownLatch latch = new CountDownLatch(1);
            zooKeeper = new ZooKeeper(properties.getConnectString(), properties.getCoordinator().getSessionTimeout(), event -> {
                log.debug("ZK event: " + event);
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    latch.countDown();
                }
            });
            log.info("Waiting for Zookeeper...");
            latch.await(properties.getCoordinator().getConnectionTimeout(), TimeUnit.MILLISECONDS);

            if (zooKeeper.exists(this.zNodeRoot, false) == null) {
                initNodes();
            }
        } catch (Exception e) {
            throw new RuntimeException("Initialization failed", e);
        }
    }

    @PreDestroy
    public void stop() {
        try {
            zooKeeper.close();
        } catch (InterruptedException ignored) {}
    }

    public void reconnect() {
        try {
            log.debug("Stopping...");
            stop();
            log.info("Stopped");
        } catch (Exception e) {
            log.error("Stop failed", e);
        }

        log.debug("Starting...");
        start();
        log.info("Started");
    }

    private void initNodes() {
        try {
            log.info("Initializing znodes...");
            zooKeeper.create(this.zNodeRoot, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zooKeeper.create(this.zNodeLockRoot, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException | InterruptedException e) {
            throw new RuntimeException("Failed to setup znodes", e);
        }
    }

    public static class Lock {
        private final ZooKeeper zk;
        private final String lockPath;

        private Lock(ZooKeeper zk, String lockPath) {
            this.zk = zk;
            this.lockPath = lockPath;
        }

        public void release() {
            try {
                log.info("Releasing lock...");
                zk.delete(lockPath, -1);
                log.info("Lock released");
            } catch (KeeperException | InterruptedException e) {
                throw new RuntimeException("Failed to release lock", e);
            }
        }
    }

}

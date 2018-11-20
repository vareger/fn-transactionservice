package com.propy.service.bctransaction.beans;

import com.propy.service.bctransaction.configs.ZookeeperProperties;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@RefreshScope
@Component
public class ZookeeperBeans {

    private final ZookeeperProperties properties;

    @Autowired
    public ZookeeperBeans(ZookeeperProperties properties) {
        this.properties = properties;
    }

    @Bean
    public CuratorFramework initCuratorFramework() {
        CuratorFramework framework = CuratorFrameworkFactory.builder()
                .connectString(properties.getConnectString())
                .sessionTimeoutMs(properties.getSessionTimeout())
                .connectionTimeoutMs(properties.getConnectionTimeout())
                .namespace(properties.getNamespace())
                .retryPolicy(new ExponentialBackoffRetry(1000, 5))
                .build();
        framework.start();
        return framework;
    }

}

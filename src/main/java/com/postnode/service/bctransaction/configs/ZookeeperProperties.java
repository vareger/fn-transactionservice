package com.postnode.service.bctransaction.configs;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "zookeeper")
@NoArgsConstructor
@Data
public class ZookeeperProperties {

    private String namespace;
    private String connectString;
    private Integer connectionTimeout;
    private Integer sessionTimeout;

}

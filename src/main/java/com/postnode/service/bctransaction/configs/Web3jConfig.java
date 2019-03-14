package com.postnode.service.bctransaction.configs;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * web3j property container.
 */
@Configuration
@ConfigurationProperties("web3j")
@Data
@NoArgsConstructor
public class Web3jConfig {

    public String clientAddress;

    public Boolean adminClient = false;

    public String networkId = "1";

    public Long httpTimeoutSeconds = 300L;

}

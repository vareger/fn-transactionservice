package com.propy.service.bctransaction.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * web3j property container.
 */
@Configuration
public class Web3jConfig {

    @Value("${web3j.client-address}")
    public String clientAddress;

    @Value("${web3j.admin-client:false}")
    public Boolean adminClient;

    @Value("${web3j.network-id:1}")
    public String networkId;

    @Value("${web3j.httpTimeoutSeconds:360}")
    public Long httpTimeoutSeconds;

}

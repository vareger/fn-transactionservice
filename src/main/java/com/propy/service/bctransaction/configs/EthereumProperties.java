package com.propy.service.bctransaction.configs;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.web3j.tx.ChainId;

import java.util.List;

@RefreshScope
@Data
@NoArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "ethereum")
public class EthereumProperties {

    @NoArgsConstructor
    @Data
    public static class Wallet {
        private String address;
        private String privateKey;
    }

    @NoArgsConstructor
    @Data
    public static class Wallets {
        private Wallet system;
        private List<Wallet> multiSig;
    }

    private Wallets wallets;
    private Byte chainId;

}

package com.propy.service.bctransaction.configs;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@RefreshScope
@Data
@NoArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "ethereum.wallets")
public class EthereumProperties {

    @NoArgsConstructor
    @Data
    public static class Wallet {
        private String address;
        private String privateKey;
    }

    private Wallet system;
    private List<Wallet> multiSig;

}

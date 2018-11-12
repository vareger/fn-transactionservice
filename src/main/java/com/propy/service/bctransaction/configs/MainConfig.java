package com.propy.service.bctransaction.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "com.propy.service.bctransaction.repositories")
@Configuration
public class MainConfig {

    @Value("${test}")
    public String test;

}

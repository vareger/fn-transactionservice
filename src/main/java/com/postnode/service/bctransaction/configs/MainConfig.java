package com.postnode.service.bctransaction.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "com.propy.service.bctransaction.database.repositories")
@Configuration
public class MainConfig {

}

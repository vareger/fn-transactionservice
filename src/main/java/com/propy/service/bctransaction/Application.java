package com.propy.service.bctransaction;

import com.propy.service.bctransaction.configs.EthereumProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@RestController
@RefreshScope
@Slf4j
public class Application {

    @Autowired
    private EthereumProperties properties;

    @Value("${test}")
    public String test;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @GetMapping("/")
    public String get() {
        return this.test;
    }

    @PostConstruct
    public void init() {
        log.info("Application initiated with wallet address {}", properties.getSystem().getAddress());
    }

}

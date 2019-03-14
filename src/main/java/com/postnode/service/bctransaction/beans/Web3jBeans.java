package com.postnode.service.bctransaction.beans;

import com.postnode.service.bctransaction.actuate.Web3jHealthIndicator;
import com.postnode.service.bctransaction.configs.Web3jConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.ipc.UnixIpcService;
import org.web3j.protocol.ipc.WindowsIpcService;
import org.web3j.protocol.websocket.WebSocketService;

import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

@Slf4j
@RefreshScope
@Component
public class Web3jBeans {

    private final Web3jConfig config;

    @Autowired
    public Web3jBeans(Web3jConfig config) {
        this.config = config;
    }

    public static Credentials initCredentials() {
        return Credentials.create("");
    }

    public static Credentials initCredentials(String privateKey) {
        return Credentials.create(privateKey);
    }

    @Bean
    public Web3j web3j() throws ConnectException {
        Web3jService web3jService = buildService();
        return Web3j.build(web3jService);
    }

    private Web3jService buildService() throws ConnectException {
        Web3jService web3jService;
        String clientAddress = config.clientAddress;

        if (clientAddress == null || clientAddress.equals("")) {
            web3jService = new HttpService(createOkHttpClient());
        } else if (clientAddress.startsWith("http") || clientAddress.startsWith("https")) {
            web3jService = new HttpService(clientAddress, createOkHttpClient(), false);
        } else if(clientAddress.startsWith("ws") || clientAddress.startsWith("wss")) {
            web3jService = new WebSocketService(clientAddress, false);
            ((WebSocketService) web3jService).connect();
        } else if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            web3jService = new WindowsIpcService(clientAddress);
        } else {
            web3jService = new UnixIpcService(clientAddress);
        }

        return web3jService;
    }

    private OkHttpClient createOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        configureTimeouts(builder);
        return builder.build();
    }

    private void configureTimeouts(OkHttpClient.Builder builder) {
        Long tos = config.httpTimeoutSeconds;
        if (tos != null) {
            builder.connectTimeout(tos, TimeUnit.SECONDS);
            builder.readTimeout(tos, TimeUnit.SECONDS);  // Sets the socket timeout too
            builder.writeTimeout(tos, TimeUnit.SECONDS);
        }
    }

    @Bean
    @ConditionalOnBean(Web3j.class)
    Web3jHealthIndicator web3jHealthIndicator(Web3j web3j) {
        return new Web3jHealthIndicator(web3j);
    }

}

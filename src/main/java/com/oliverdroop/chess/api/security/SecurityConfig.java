package com.oliverdroop.chess.api.security;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

@Configuration
public class SecurityConfig {

    @Bean
    public RestTemplate restTemplate(SslBundles sslBundles) {
        SslBundle sslBundle = sslBundles.getBundle("chess");
        SSLContext sslContext = sslBundle.createSslContext();

        DefaultClientTlsStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext);

        Registry<TlsSocketStrategy> registry = RegistryBuilder.<TlsSocketStrategy>create()
            .register("https", tlsStrategy)
            .build();

        BasicHttpClientConnectionManager connectionManager = BasicHttpClientConnectionManager.create(registry);

        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(requestFactory);
    }
}

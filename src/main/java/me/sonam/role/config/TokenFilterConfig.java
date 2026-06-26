package me.sonam.role.config;

import me.sonam.security.headerfilter.ReactiveRequestContextHolder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class TokenFilterConfig {
    @Value("${tokenExpireSeconds:1}")
    private int tokenExpireSeconds;

    @Bean
    public ReactiveRequestContextHolder reactiveRequestContextHolder(
            @Qualifier("tokenWebClientBuilder") WebClient.Builder tokenWebClientBuilder) {
        return new ReactiveRequestContextHolder(tokenWebClientBuilder, tokenExpireSeconds);
    }
}

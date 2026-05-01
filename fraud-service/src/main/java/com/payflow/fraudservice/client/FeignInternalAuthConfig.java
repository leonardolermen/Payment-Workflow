package com.payflow.fraudservice.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Injeta o header X-Internal-Token em todas as chamadas Feign,
 * autenticando o fraud-service junto ao core-service.
 */
@Configuration
public class FeignInternalAuthConfig {

    public static final String HEADER = "X-Internal-Token";

    @Bean
    public RequestInterceptor internalAuthInterceptor(
            @Value("${internal-api.token:}") String token) {
        return template -> {
            if (StringUtils.hasText(token)) {
                template.header(HEADER, token);
            }
        };
    }
}

package com.example.obo.gateway;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.client.RestClient;

@Configuration
public class OAuth2ClientConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository,
            RestClient restClient) {

        // Our custom provider that does the token-exchange HTTP call
        OAuth2AuthorizedClientProvider provider = new CustomTokenExchangeAuthorizedClientProvider(restClient);

        DefaultOAuth2AuthorizedClientManager manager = new DefaultOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                authorizedClientRepository);

        manager.setAuthorizedClientProvider(provider);

        // Propagate attributes (audience, scope, etc)
        manager.setContextAttributesMapper(authorizeRequest -> {
            Map<String, Object> attrs = new HashMap<>();
            if (authorizeRequest.getAttributes() != null) {
                attrs.putAll(authorizeRequest.getAttributes());
            }
            return attrs;
        });

        return manager;
    }
}

package com.example.obo.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

@Configuration
public class OAuth2ClientConfig {

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository) {

        var restClient = RestClient.builder().build();

        // Our custom provider that does the token-exchange HTTP call
        OAuth2AuthorizedClientProvider provider = new CustomTokenExchangeAuthorizedClientProvider(restClient);

        // Use stateless manager that doesn't require HttpServletRequest
        return new StatelessOAuth2AuthorizedClientManager(clientRegistrationRepository, provider);
    }
}

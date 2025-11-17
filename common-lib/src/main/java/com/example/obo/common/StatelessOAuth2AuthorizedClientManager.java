package com.example.obo.common;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.HashMap;
import java.util.Map;

public class StatelessOAuth2AuthorizedClientManager implements OAuth2AuthorizedClientManager {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2AuthorizedClientProvider authorizedClientProvider;

    public StatelessOAuth2AuthorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientProvider authorizedClientProvider) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.authorizedClientProvider = authorizedClientProvider;
    }

    @Override
    public OAuth2AuthorizedClient authorize(OAuth2AuthorizeRequest authorizeRequest) {
        ClientRegistration clientRegistration = clientRegistrationRepository
                .findByRegistrationId(authorizeRequest.getClientRegistrationId());
        if (clientRegistration == null) {
            throw new IllegalStateException(
                    "Client Registration with id '" + authorizeRequest.getClientRegistrationId() + "' was not found");
        }

        Map<String, Object> attributes = new HashMap<>();
        if (authorizeRequest.getAttributes() != null) {
            attributes.putAll(authorizeRequest.getAttributes());
        }

        OAuth2AuthorizationContext.Builder contextBuilder = OAuth2AuthorizationContext
                .withClientRegistration(clientRegistration)
                .principal(authorizeRequest.getPrincipal());

        attributes.forEach(contextBuilder::attribute);

        OAuth2AuthorizationContext context = contextBuilder.build();
        // Use reflection or a workaround to set the authorized client if needed
        // For now, the provider will handle the case where there's no existing client

        OAuth2AuthorizedClient authorizedClient = authorizedClientProvider.authorize(context);

        return authorizedClient;
    }
}

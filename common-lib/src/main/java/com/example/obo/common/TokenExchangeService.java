package com.example.obo.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;

@Service
//@ConditionalOnBean(OAuth2AuthorizedClientManager.class)
public class TokenExchangeService {

    private static final Logger logger = LoggerFactory.getLogger(TokenExchangeService.class);

    // This is the Spring Security client manager wired with
    // TokenExchangeOAuth2AuthorizedClientProvider
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public TokenExchangeService(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    /**
     * Exchange the current request's JWT for an OBO token using the "obo-generic"
     * client.
     *
     * NOTE:
     * - audience & scope are currently only attached as attributes, so they can be
     * picked up later by a custom TokenExchangeGrantRequestEntityConverter if you
     * add one.
     */
    public String exchangeFor(String audience, String scope) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No Authentication found in security context");
        }

        long startTime = System.currentTimeMillis();
        try {
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("obo-generic")
                    .principal(authentication)
                    .attributes(attrs -> {
                        if (audience != null) {
                            attrs.put("audience", audience);
                        }
                        if (scope != null) {
                            attrs.put("scope", scope);
                        }
                    })
                    .build();

            OAuth2AuthorizedClient client = authorizedClientManager.authorize(authorizeRequest);
            long duration = System.currentTimeMillis() - startTime;

            if (client == null || client.getAccessToken() == null) {
                throw new IllegalStateException("Failed to obtain OBO token via OAuth2AuthorizedClientManager");
            }

            logger.info(
                    "Gateway-service: Token exchange via OAuth2AuthorizedClientManager completed in {} ms (audience: {}, scope: {})",
                    duration, audience, scope);

            return client.getAccessToken().getTokenValue();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Gateway-service: Token exchange via OAuth2AuthorizedClientManager failed after {} ms: {}",
                    duration, e.getMessage(), e);
            throw new IllegalStateException(
                    "Failed to exchange token via OAuth2AuthorizedClientManager: " + e.getMessage(), e);
        }
    }
}

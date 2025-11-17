package com.example.obo.common;

import java.time.Instant;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

public class CustomTokenExchangeAuthorizedClientProvider implements OAuth2AuthorizedClientProvider {

    private static final String TOKEN_EXCHANGE_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:token-exchange";

    private final RestClient restClient;

    public CustomTokenExchangeAuthorizedClientProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public OAuth2AuthorizedClient authorize(OAuth2AuthorizationContext context) {
        // Only handle our token-exchange client(s)
        if (!TOKEN_EXCHANGE_GRANT_TYPE.equals(
                context.getClientRegistration().getAuthorizationGrantType().getValue())) {
            return null;
        }

        // If already authorized and token still valid, just return it
        OAuth2AuthorizedClient existingClient = context.getAuthorizedClient();
        if (existingClient != null && existingClient.getAccessToken() != null
                && existingClient.getAccessToken().getExpiresAt() != null
                && existingClient.getAccessToken().getExpiresAt().isAfter(Instant.now())) {
            return existingClient;
        }

        Authentication principal = context.getPrincipal();
        Object principalObj = principal.getPrincipal();
        if (!(principalObj instanceof Jwt jwt)) {
            throw new IllegalStateException("Expected Jwt as principal for token exchange");
        }

        String subjectToken = jwt.getTokenValue();

        // Build RFC 8693 request body
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", TOKEN_EXCHANGE_GRANT_TYPE);
        params.add("subject_token", subjectToken);
        params.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
        params.add("requested_token_type", "urn:ietf:params:oauth:token-type:access_token");

        // Pull extras (audience, scope, evt_type) from context attributes (set in
        // TokenExchangeService)
        Map<String, Object> attrs = context.getAttributes();
        Object audience = attrs.get("audience");
        if (audience != null) {
            params.add("audience", audience.toString());
        }
        Object scope = attrs.get("scope");
        if (scope != null) {
            params.add("scope", scope.toString());
        }
        Object evtType = attrs.get("evt_type");
        if (evtType != null) {
            params.add("evt_type", evtType.toString());
        }

        // Token endpoint from provider configuration
        String tokenUri = context.getClientRegistration()
                .getProviderDetails()
                .getTokenUri();

        Map<String, Object> response = restClient.post()
                .uri(tokenUri)
                .body(params)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });

        if (response == null || !response.containsKey("access_token")) {
            throw new IllegalStateException("Token exchange failed: no access_token in response");
        }

        String accessTokenValue = (String) response.get("access_token");
        long expiresIn = 0L;
        Object expiresObj = response.get("expires_in");
        if (expiresObj instanceof Number n) {
            expiresIn = n.longValue();
        } else if (expiresObj instanceof String s && !s.isBlank()) {
            expiresIn = Long.parseLong(s);
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = expiresIn > 0
                ? issuedAt.plusSeconds(expiresIn)
                : null;

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                accessTokenValue,
                issuedAt,
                expiresAt);

        return new OAuth2AuthorizedClient(
                context.getClientRegistration(),
                context.getPrincipal().getName(),
                accessToken);
    }
}

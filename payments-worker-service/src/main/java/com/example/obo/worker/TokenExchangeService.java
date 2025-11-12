package com.example.obo.worker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TokenExchangeService {

    @Value("${spring.security.oauth2.client.provider.sts.token-uri}")
    private String stsTokenUri;

    private final RestTemplate restTemplate = new RestTemplate();

    public String exchangeFor(String audience, String scope) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("No JWT found in security context");
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String subjectToken = jwt.getTokenValue();

        // Build token exchange request
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        params.add("subject_token", subjectToken);
        params.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
        params.add("requested_token_type", "urn:ietf:params:oauth:token-type:access_token");
        
        if (audience != null) {
            params.add("audience", audience);
        }
        if (scope != null) {
            params.add("scope", scope);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(stsTokenUri, params, Map.class);
            if (response == null || !response.containsKey("access_token")) {
                throw new IllegalStateException("Failed to obtain OBO token: invalid response");
            }
            return (String) response.get("access_token");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to exchange token: " + e.getMessage(), e);
        }
    }
}

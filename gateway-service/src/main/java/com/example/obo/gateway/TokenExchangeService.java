package com.example.obo.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(TokenExchangeService.class);

    @Value("${spring.security.oauth2.client.provider.sts.token-uri}")
    private String stsTokenUri;

    private final RestTemplate restTemplate = new RestTemplate();

    public String exchangeFor(String audience, String scope) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No JWT found in security context");
        }

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

        long startTime = System.currentTimeMillis();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(stsTokenUri, params, Map.class);
            long duration = System.currentTimeMillis() - startTime;
            
            if (response == null || !response.containsKey("access_token")) {
                throw new IllegalStateException("Failed to obtain OBO token: invalid response");
            }
            
            logger.info("Gateway-service: Token exchange completed in {} ms (audience: {}, scope: {})", 
                    duration, audience, scope);
            
            return (String) response.get("access_token");
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Gateway-service: Token exchange failed after {} ms: {}", duration, e.getMessage());
            throw new IllegalStateException("Failed to exchange token: " + e.getMessage(), e);
        }
    }
}

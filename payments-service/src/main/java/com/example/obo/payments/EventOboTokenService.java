package com.example.obo.payments;

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
public class EventOboTokenService {

    private static final Logger logger = LoggerFactory.getLogger(EventOboTokenService.class);
    private static final String SECRET = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";
    private final String stsUrl;

    public EventOboTokenService(@Value("${sts.token-uri:http://sts:8080/oauth2/token}") String stsUrl) {
        this.stsUrl = stsUrl;
    }

    public String issueEventOboToken(String eventType, String scope) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("No JWT found in security context");
        }
        Jwt currentJwt = (Jwt) authentication.getPrincipal();
        String subjectToken = currentJwt.getTokenValue();

        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        params.add("subject_token", subjectToken);
        params.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
        params.add("requested_token_type", "urn:ietf:params:oauth:token-type:access_token");
        params.add("scope", scope);
        params.add("evt_type", eventType);

        long startTime = System.currentTimeMillis();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(stsUrl, params, Map.class);
            long duration = System.currentTimeMillis() - startTime;

            if (response == null || !response.containsKey("access_token")) {
                throw new IllegalStateException("Failed to obtain event OBO token: invalid response");
            }

            logger.info("Payments-service: Event OBO token exchange completed in {} ms (eventType: {}, scope: {})",
                    duration, eventType, scope);

            return (String) response.get("access_token");
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Payments-service: Event OBO token exchange failed after {} ms: {}", duration, e.getMessage());
            throw new RuntimeException("Failed to issue event OBO token", e);
        }
    }
}

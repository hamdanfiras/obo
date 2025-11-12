package com.example.obo.sts;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
public class TokenExchangeController {

    private static final String SECRET = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";

    @PostMapping("/token")
    public Map<String, Object> exchange(@RequestParam Map<String, String> params) throws Exception {
        String subjectToken = params.get("subject_token");
        String audience = params.get("audience");
        String scope = params.get("scope");
        String evtType = params.get("evt_type");
        String requestedTokenType = params.get("requested_token_type");

        if (subjectToken == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "subject_token is required");
        }

        SignedJWT subject = SignedJWT.parse(subjectToken);
        JWSVerifier verifier = new MACVerifier(SECRET);
        if (!subject.verify(verifier)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid subject token");
        }

        // Check expiration
        Date exp = subject.getJWTClaimsSet().getExpirationTime();
        if (exp != null && exp.before(new Date())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Subject token expired");
        }

        String userSub = subject.getJWTClaimsSet().getSubject();
        String originalScope = (String) subject.getJWTClaimsSet().getClaim("scope");
        String act = (String) subject.getJWTClaimsSet().getClaim("act");

        // Build new claims
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .issuer("https://sts.internal")
                .subject(userSub)
                .expirationTime(Date.from(Instant.now().plusSeconds(evtType != null ? 30 : 120)));

        if (audience != null) {
            claimsBuilder.audience(audience);
        }

        if (scope != null) {
            claimsBuilder.claim("scope", scope);
        } else if (originalScope != null) {
            claimsBuilder.claim("scope", originalScope);
        }

        // Set actor claim
        String newAct = act != null ? act + "->service-b" : "service-b";
        claimsBuilder.claim("act", newAct);

        // Chain tracking
        Object chain = subject.getJWTClaimsSet().getClaim("chain");
        if (chain != null) {
            claimsBuilder.claim("chain", chain + "->" + newAct);
        } else {
            claimsBuilder.claim("chain", "user->" + newAct);
        }

        // Event type scoping
        if (evtType != null) {
            claimsBuilder.claim("evt_type", evtType)
                  .claim("typ", "internal_delegation+jwt");
        }

        JWTClaimsSet claims = claimsBuilder.build();
        SignedJWT oboJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        oboJwt.sign(new MACSigner(SECRET));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", oboJwt.serialize());
        response.put("token_type", "Bearer");
        response.put("expires_in", evtType != null ? 30 : 120);
        response.put("issued_token_type", "urn:ietf:params:oauth:token-type:access_token");

        return response;
    }
}


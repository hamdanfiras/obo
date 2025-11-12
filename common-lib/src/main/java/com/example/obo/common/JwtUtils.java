package com.example.obo.common;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.time.Instant;
import java.util.Date;
import java.util.List;

public class JwtUtils {
    public static final String SECRET = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";

    public static SignedJWT createUserJwt(String subject, String email) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("https://mock-idp.example.com")
                .subject(subject)
                .claim("email", email)
                .claim("scope", "openid profile payments.initiate")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET));
        return jwt;
    }

    public static boolean verifyJwt(SignedJWT jwt) throws Exception {
        JWSVerifier verifier = new MACVerifier(SECRET);
        return jwt.verify(verifier);
    }

    public static String getAudience(SignedJWT jwt) throws Exception {
        List<String> audiences = jwt.getJWTClaimsSet().getAudience();
        return audiences != null && !audiences.isEmpty() ? audiences.get(0) : null;
    }

    public static String getScope(SignedJWT jwt) throws Exception {
        Object scope = jwt.getJWTClaimsSet().getClaim("scope");
        return scope != null ? scope.toString() : null;
    }

    public static String getEventType(SignedJWT jwt) throws Exception {
        Object evtType = jwt.getJWTClaimsSet().getClaim("evt_type");
        return evtType != null ? evtType.toString() : null;
    }
}


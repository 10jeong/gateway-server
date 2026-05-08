package com.yeoljeong.tripmate.passport;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PassportProvider {

    private static final long PASSPORT_EXPIRATION_MS = 30_000L;
    private final SecretKey internalSecretKey;

    public PassportProvider(@Value("${jwt.internal-secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.internalSecretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String issue(String userId, String role) {
        Date now = new Date();
        return Jwts.builder()
            .subject(userId)
            .claim("role", role)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + PASSPORT_EXPIRATION_MS))
            .signWith(internalSecretKey)
            .compact();
    }
}

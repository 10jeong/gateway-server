package com.yeoljeong.tripmate.jwt;

import com.yeoljeong.tripmate.exception.constants.CommonErrorCode;
import com.yeoljeong.tripmate.response.GatewayResponseUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secretKey;
    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);

            String subject = claims.getSubject();
            String role = claims.get("role", String.class);

            if(subject == null || subject.isBlank()) return false;
            if(role == null || role.isBlank()) return false;

            return true;
        } catch (Exception e) {
            log.warn("[JwtProvider] 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public String getUserId(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    public String getRole(String token) {
        Claims claims = getClaims(token);
        return claims.get("role", String.class);
    }

    public String resolveToken(ServerWebExchange exchange) {
        String bearer = exchange.getRequest()
            .getHeaders()
            .getFirst(HttpHeaders.AUTHORIZATION);

        if (bearer == null || !bearer.startsWith("Bearer ")) {
            return null;
        }

        return bearer.substring(7);
    }

    // helper method
    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}

package com.yeoljeong.tripmate.filter;

import com.yeoljeong.tripmate.error.GatewayErrorCode;
import com.yeoljeong.tripmate.jwt.JwtProvider;
import com.yeoljeong.tripmate.passport.PassportProvider;
import com.yeoljeong.tripmate.properties.GatewayProperties;
import com.yeoljeong.tripmate.response.GatewayResponseUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String HEADER_PASSPORT = "X-Passport";

    private final GatewayProperties gatewayProperties;
    private final JwtProvider jwtProvider;
    private final PassportProvider passportProvider;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        List<String> whitelist = gatewayProperties.getWhitelist();

        if (isPublicPath(whitelist, path)) {
            return chain.filter(exchange);
        }

        String token = jwtProvider.resolveToken(exchange);

        if (token == null) {
            return GatewayResponseUtil.writeErrorResponse(exchange, GatewayErrorCode.UNAUTHORIZED);
        }

        if (!jwtProvider.validateToken(token)) {
            return GatewayResponseUtil.writeErrorResponse(exchange, GatewayErrorCode.UNAUTHORIZED);
        }

        String userId = jwtProvider.getUserId(token);
        String role = jwtProvider.getRole(token);

        return reactiveRedisTemplate.hasKey("BL:" + userId)
            .flatMap(isBlackListed -> {
                if (Boolean.TRUE.equals(isBlackListed)) {
                    // 블랙리스트에 있으면 401
                    return GatewayResponseUtil.writeErrorResponse(exchange,
                        GatewayErrorCode.UNAUTHORIZED);
                }

                // issue에서 예외 발생시 Mono파이프라인 밖에서 500에러가 발생할 것을 방어
                try {
                    String passport = passportProvider.issue(userId, role);

                    ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(r -> r
                            .headers(headers -> headers.remove(HEADER_PASSPORT))
                            .header(HEADER_PASSPORT, passport))
                        .build();

                    return chain.filter(mutatedExchange);

                } catch (Exception e) {
                    log.error("[Passport] 발급 실패 - userId: {}, error: {}", userId, e.getMessage());
                    return GatewayResponseUtil.writeErrorResponse(exchange,
                        GatewayErrorCode.INTERNAL_SERVER_ERROR);
                }
            });
    }

    @Override
    public int getOrder() {
        return -1;
    }

    // helper method
    private boolean isPublicPath(List<String> whitelist, String path) {
        return whitelist.stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}

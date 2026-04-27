package com.yeoljeong.tripmate.filter;

import com.yeoljeong.tripmate.exception.constants.CommonErrorCode;
import com.yeoljeong.tripmate.jwt.JwtProvider;
import com.yeoljeong.tripmate.properties.GatewayProperties;
import com.yeoljeong.tripmate.response.GatewayResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    private final GatewayProperties gatewayProperties;
    private final JwtProvider jwtProvider;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        List<String> whitelist = gatewayProperties.getWhitelist();

        if(isPublicPath(whitelist, path)){
            return chain.filter(exchange);
        }

        String token = jwtProvider.resolveToken(exchange);

        if (token == null) {
            return GatewayResponseUtil.writeErrorResponse(exchange, CommonErrorCode.UNAUTHORIZED);
        }

        if (!jwtProvider.validateToken(token)) {
            return GatewayResponseUtil.writeErrorResponse(exchange, CommonErrorCode.UNAUTHORIZED);
        }

        String userId = jwtProvider.getUserId(token);
        String role = jwtProvider.getRole(token);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.header(HEADER_USER_ID, userId)
                        .header(HEADER_USER_ROLE, role))
                .build();

        return chain.filter(mutatedExchange);
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

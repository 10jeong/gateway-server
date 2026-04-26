package com.yeoljeong.tripmate.filter;

import com.yeoljeong.tripmate.exception.constants.CommonErrorCode;
import com.yeoljeong.tripmate.response.GatewayResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class InternalPathFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        boolean isInternal = path.startsWith("/internal");

        if (isInternal) {
            log.warn("[Gateway] internal 경로 차단: {}", path);
            return GatewayResponseUtil.writeErrorResponse(exchange, CommonErrorCode.FORBIDDEN);
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -2;
    }
}

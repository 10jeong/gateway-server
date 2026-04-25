package com.yeoljeong.tripmate.response;

import com.yeoljeong.tripmate.exception.constants.ErrorCode;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public class GatewayResponseUtil {

    public static Mono<Void> writeErrorResponse(ServerWebExchange exchange, ErrorCode errorCode) {
        exchange.getResponse().setStatusCode(errorCode.getStatus());
        byte[] bytes = errorCode.getMessage().getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse()
                        .bufferFactory()
                        .wrap(bytes))
        );
    }
}
